package com.example.abhayhealth.feature.records

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.abhayhealth.core.common.DefaultDispatcherProvider
import com.example.abhayhealth.core.common.DispatcherProvider
import com.example.abhayhealth.domain.model.AuthState
import com.example.abhayhealth.domain.repository.AuthRepository
import com.example.abhayhealth.domain.repository.PatientProfileRepository
import com.example.abhayhealth.feature.medicines.MedicationAlarmScheduler
import com.example.abhayhealth.feature.medicines.MedicationDao
import com.example.abhayhealth.feature.records.export.ExportReportState
import com.example.abhayhealth.feature.records.export.MedicalReportExportManager
import com.example.abhayhealth.feature.vitals.VitalReadingRepository
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RecordsViewModel(
    private val medicalRecordRepository: MedicalRecordRepository? = null,
    private val authRepository: AuthRepository? = null,
    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider(),
    private val medicationDao: MedicationDao? = null,
    private val vitalReadingRepository: VitalReadingRepository? = null,
    private val patientProfileRepository: PatientProfileRepository? = null
) : ViewModel() {

    private val _records = MutableStateFlow<List<MedicalRecord>>(emptyList())
    val records: StateFlow<List<MedicalRecord>> = _records.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    private val _extractionStatusMessage = MutableStateFlow("Extracting clinical parameters, medications, and vitals...")
    val extractionStatusMessage: StateFlow<String> = _extractionStatusMessage.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _infoMessage = MutableStateFlow<String?>(null)
    val infoMessage: StateFlow<String?> = _infoMessage.asStateFlow()

    private val _recordToDelete = MutableStateFlow<MedicalRecord?>(null)
    val recordToDelete: StateFlow<MedicalRecord?> = _recordToDelete.asStateFlow()

    private val _exportState = MutableStateFlow<ExportReportState>(ExportReportState.Idle)
    val exportState: StateFlow<ExportReportState> = _exportState.asStateFlow()

    private val _showExportDialog = MutableStateFlow(false)
    val showExportDialog: StateFlow<Boolean> = _showExportDialog.asStateFlow()

    private val _medicationCount = MutableStateFlow(0)
    val medicationCount: StateFlow<Int> = _medicationCount.asStateFlow()

    private val _vitalCount = MutableStateFlow(0)
    val vitalCount: StateFlow<Int> = _vitalCount.asStateFlow()

    private var observeJob: Job? = null

    init {
        observeAuthState()
    }

    private fun observeAuthState() {
        val authRepo = authRepository ?: run {
            observeRecordsForUid("local_user")
            return
        }
        viewModelScope.launch(dispatcherProvider.main) {
            authRepo.authState.collectLatest { state ->
                when (state) {
                    is AuthState.Authenticated -> {
                        observeRecordsForUid(state.user.uid)
                    }
                    else -> {
                        val currentUid = authRepo.currentUser?.uid?.ifBlank { null } ?: "local_user"
                        observeRecordsForUid(currentUid)
                    }
                }
            }
        }
    }

    fun observeRecords() {
        val uid = authRepository?.currentUser?.uid?.ifBlank { null } ?: "local_user"
        observeRecordsForUid(uid)
    }

    private fun observeRecordsForUid(uid: String) {
        val repo = medicalRecordRepository ?: return
        observeJob?.cancel()
        observeJob = viewModelScope.launch(dispatcherProvider.main) {
            if (_searchQuery.value.isBlank()) {
                repo.getRecords(uid).collectLatest { list ->
                    _records.value = list
                }
            } else {
                repo.searchRecords(uid, _searchQuery.value).collectLatest { list ->
                    _records.value = list
                }
            }
        }
    }

    private fun clearAllState() {
        observeJob?.cancel()
        _records.value = emptyList()
        _searchQuery.value = ""
        _errorMessage.value = null
        _infoMessage.value = null
        _recordToDelete.value = null
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        val uid = authRepository?.currentUser?.uid?.ifBlank { null } ?: "local_user"
        observeRecordsForUid(uid)
    }

    fun clearMessages() {
        _errorMessage.value = null
        _infoMessage.value = null
    }

    fun importDocument(uri: Uri, context: Context, onImported: ((Long) -> Unit)? = null) {
        val uid = authRepository?.currentUser?.uid?.ifBlank { null } ?: "local_user"
        val repo = medicalRecordRepository
        if (repo == null) {
            _errorMessage.value = "Local database unavailable."
            return
        }

        viewModelScope.launch(dispatcherProvider.main) {
            _isImporting.value = true
            _errorMessage.value = null
            _extractionStatusMessage.value = "Analyzing document and reading medical fields..."

            val result = withContext(Dispatchers.IO) {
                DocumentImportManager.importDocument(uri, context)
            }

            when (result) {
                is DocumentImportResult.Success -> {
                    val record = MedicalRecord(
                        accountUid = uid,
                        title = result.title,
                        category = result.category,
                        recordDate = result.recordDate,
                        localFilePath = result.localFilePath,
                        mimeType = result.mimeType
                    )
                    try {
                        val assignedId = repo.insertRecord(record)
                        _infoMessage.value = "Document uploaded successfully. Loading fields for review..."
                        _isImporting.value = false
                        onImported?.invoke(assignedId)
                    } catch (t: Throwable) {
                        DocumentImportManager.deleteFile(result.localFilePath, context)
                        _errorMessage.value = "Failed to save record to local database."
                        _isImporting.value = false
                    }
                }
                is DocumentImportResult.Error -> {
                    _errorMessage.value = result.message
                    _isImporting.value = false
                }
            }
        }
    }

    fun openRecord(record: MedicalRecord, context: Context) {
        val uid = authRepository?.currentUser?.uid?.ifBlank { null } ?: "local_user"
        val error = DocumentImportManager.openRecord(record, context, uid)
        if (error != null) {
            _errorMessage.value = error
        }
    }

    fun requestDeleteRecord(record: MedicalRecord) {
        val uid = authRepository?.currentUser?.uid?.ifBlank { null } ?: "local_user"
        if (record.accountUid != uid && record.accountUid != "local_user") {
            _errorMessage.value = "Unauthorized delete request."
            return
        }
        _recordToDelete.value = record
    }

    fun cancelDeleteRecord() {
        _recordToDelete.value = null
    }

    fun confirmDeleteRecord(context: Context) {
        val record = _recordToDelete.value ?: return
        val uid = authRepository?.currentUser?.uid?.ifBlank { null } ?: "local_user"
        if (record.accountUid != uid && record.accountUid != "local_user") {
            _errorMessage.value = "Unauthorized delete request."
            _recordToDelete.value = null
            return
        }

        val repo = medicalRecordRepository ?: return
        viewModelScope.launch(dispatcherProvider.main) {
            try {
                repo.deleteRecord(record)
                withContext(Dispatchers.IO) {
                    DocumentImportManager.deleteFile(record.localFilePath, context, record, uid)
                }
                _infoMessage.value = "Record deleted."
            } catch (_: Exception) {
                _errorMessage.value = "Failed to delete record."
            } finally {
                _recordToDelete.value = null
            }
        }
    }

    fun openExportDialog() {
        val uid = authRepository?.currentUser?.uid?.ifBlank { null } ?: "local_user"
        refreshCounts(uid)
        _exportState.value = ExportReportState.Idle
        _showExportDialog.value = true
    }

    fun dismissExportDialog() {
        _showExportDialog.value = false
        _exportState.value = ExportReportState.Idle
    }

    private fun refreshCounts(uid: String) {
        viewModelScope.launch(dispatcherProvider.main) {
            try {
                medicationDao?.getMedicationsByAccount(uid)?.first()?.let {
                    _medicationCount.value = it.size
                }
                vitalReadingRepository?.getAllReadings(uid)?.first()?.let {
                    _vitalCount.value = it.size
                }
            } catch (_: Exception) {}
        }
    }

    fun exportAndShareReport(context: Context) {
        performExport(context) { manager, file, patientName ->
            manager.shareReport(file, patientName)
        }
    }

    fun exportAndViewReport(context: Context) {
        performExport(context) { manager, file, _ ->
            manager.viewReport(file)
        }
    }

    fun exportAndPrintReport(context: Context) {
        performExport(context) { manager, file, _ ->
            manager.printReport(file)
        }
    }

    private fun performExport(context: Context, onReady: (MedicalReportExportManager, File, String) -> Unit) {
        val uid = authRepository?.currentUser?.uid?.ifBlank { null } ?: "local_user"

        val recordRepo = medicalRecordRepository
        val medDao = medicationDao
        val vitalRepo = vitalReadingRepository
        val profileRepo = patientProfileRepository

        if (recordRepo == null || medDao == null || vitalRepo == null || profileRepo == null) {
            _exportState.value = ExportReportState.Error("Database services unavailable.")
            return
        }

        viewModelScope.launch(dispatcherProvider.main) {
            _exportState.value = ExportReportState.Generating
            try {
                val exportManager = MedicalReportExportManager(
                    context = context.applicationContext,
                    medicalRecordRepository = recordRepo,
                    medicationDao = medDao,
                    vitalReadingRepository = vitalRepo,
                    patientProfileRepository = profileRepo
                )
                val file = exportManager.generateReport(uid)
                val profile = try { profileRepo.getProfile(uid).first() } catch (_: Exception) { null }
                val patientName = profile?.fullName ?: "Patient"

                _exportState.value = ExportReportState.Success(file)
                onReady(exportManager, file, patientName)
            } catch (e: Exception) {
                _exportState.value = ExportReportState.Error(e.message ?: "Failed to generate PDF report.")
            }
        }
    }
}
