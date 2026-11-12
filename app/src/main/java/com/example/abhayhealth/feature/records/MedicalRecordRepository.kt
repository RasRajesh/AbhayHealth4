package com.example.abhayhealth.feature.records

import com.example.abhayhealth.core.common.DefaultDispatcherProvider
import com.example.abhayhealth.core.common.DispatcherProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Repository for managing local medical record metadata and extracted information.
 */
class MedicalRecordRepository(
    private val medicalRecordDao: MedicalRecordDao,
    private val extractedInfoDao: ExtractedInfoDao,
    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider()
) {
    fun getRecords(accountUid: String): Flow<List<MedicalRecord>> {
        return medicalRecordDao.getRecordsByAccount(accountUid)
    }

    fun searchRecords(accountUid: String, query: String): Flow<List<MedicalRecord>> {
        return medicalRecordDao.searchRecords(accountUid, query)
    }

    suspend fun getRecordById(id: Long, accountUid: String): MedicalRecord? = withContext(dispatcherProvider.io) {
        medicalRecordDao.getRecordById(id, accountUid)
    }

    suspend fun insertRecord(record: MedicalRecord): Long = withContext(dispatcherProvider.io) {
        medicalRecordDao.insertRecord(record)
    }

    suspend fun deleteRecord(record: MedicalRecord) = withContext(dispatcherProvider.io) {
        medicalRecordDao.deleteRecord(record)
    }

    suspend fun deleteRecordById(id: Long, accountUid: String): Int = withContext(dispatcherProvider.io) {
        medicalRecordDao.deleteRecordById(id, accountUid)
    }

    // Extracted Information operations
    fun getExtractedInfo(recordId: Long, accountUid: String): Flow<ExtractedMedicalInfo?> {
        return extractedInfoDao.getExtractedInfo(recordId, accountUid)
    }

    fun getAllExtractedInfo(accountUid: String): Flow<List<ExtractedMedicalInfo>> {
        return extractedInfoDao.getAllExtractedInfoFlow(accountUid)
    }

    suspend fun getExtractedInfoSync(recordId: Long, accountUid: String): ExtractedMedicalInfo? = withContext(dispatcherProvider.io) {
        extractedInfoDao.getExtractedInfoSync(recordId, accountUid)
    }

    suspend fun saveExtractedInfo(info: ExtractedMedicalInfo) = withContext(dispatcherProvider.io) {
        extractedInfoDao.insertOrUpdate(info)
    }

    suspend fun saveExtractedInfoWithTitleUpdate(
        info: ExtractedMedicalInfo,
        newTitle: String? = null
    ) = withContext(dispatcherProvider.io) {
        extractedInfoDao.insertOrUpdate(info)
        if (!newTitle.isNullOrBlank()) {
            val record = medicalRecordDao.getRecordById(info.recordId, info.accountUid)
            if (record != null && record.title != newTitle) {
                medicalRecordDao.insertRecord(record.copy(title = newTitle))
            }
        }
    }

    suspend fun deleteExtractedInfo(recordId: Long, accountUid: String) = withContext(dispatcherProvider.io) {
        extractedInfoDao.deleteExtractedInfo(recordId, accountUid)
    }
}
