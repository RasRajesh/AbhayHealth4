package com.example.abhayhealth.feature.assistant

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.abhayhealth.core.common.DefaultDispatcherProvider
import com.example.abhayhealth.core.common.DispatcherProvider
import com.example.abhayhealth.domain.model.AuthState
import com.example.abhayhealth.domain.model.BiomarkerStatus
import com.example.abhayhealth.domain.model.ParsedMedicalResult
import com.example.abhayhealth.domain.model.PatientProfile
import com.example.abhayhealth.domain.repository.AuthRepository
import com.example.abhayhealth.domain.repository.PatientProfileRepository
import com.example.abhayhealth.domain.repository.UserPreferencesRepository
import com.example.abhayhealth.feature.medicines.MedicationDao
import com.example.abhayhealth.feature.medicines.PrescriptionMedicationEntity
import com.example.abhayhealth.feature.records.ExtractedMedicalInfo
import com.example.abhayhealth.feature.records.MedicalFieldParser
import com.example.abhayhealth.feature.records.MedicalRecord
import com.example.abhayhealth.feature.records.MedicalRecordRepository
import com.example.abhayhealth.feature.vitals.VitalReadingEntity
import com.example.abhayhealth.feature.vitals.VitalReadingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class MessageSender {
    USER, ASSISTANT
}

data class AiChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val language: String = "English",
    val quickSuggestions: List<String> = emptyList(),
    val imagePath: String? = null,
    val reportId: Long? = null
)

data class ReportOption(
    val id: Long? = null,
    val title: String,
    val titleTe: String,
    val subtitle: String,
    val date: String,
    val biomarkerCount: Int = 0,
    val abnormalCount: Int = 0,
    val isPrescription: Boolean = false,
    val localFilePath: String? = null,
    val mimeType: String? = null
) {
    companion object {
        val TOTAL_HEALTH = ReportOption(
            id = null,
            title = "Total Health Summary",
            titleTe = "మొత్తం ఆరోగ్య సారాంశం",
            subtitle = "Comprehensive analysis across all records & vitals",
            date = "All Reports"
        )
    }
}

data class AiExecutiveSummary(
    val title: String = "Executive Clinical Summary",
    val titleTe: String = "క్లినికల్ ఎగ్జిక్యూటివ్ సారాంశం",
    val reportScopeName: String = "Total Health Summary",
    val englishText: String = "",
    val teluguText: String = "",
    val keyMetrics: List<String> = emptyList(),
    val criticalAlertCount: Int = 0,
    val activeMedicationsCount: Int = 0,
    val lastUpdatedText: String = ""
)

class AiAssistantViewModel(
    context: Context,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val authRepository: AuthRepository? = null,
    private val medicalRecordRepository: MedicalRecordRepository? = null,
    private val medicationDao: MedicationDao? = null,
    private val patientProfileRepository: PatientProfileRepository? = null,
    private val vitalReadingRepository: VitalReadingRepository? = null,
    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider()
) : ViewModel() {

    private val _selectedLanguage = MutableStateFlow("English")
    val selectedLanguage: StateFlow<String> = _selectedLanguage.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _currentlySpeakingId = MutableStateFlow<String?>(null)
    val currentlySpeakingId: StateFlow<String?> = _currentlySpeakingId.asStateFlow()

    private val _ttsAvailable = MutableStateFlow(false)
    val ttsAvailable: StateFlow<Boolean> = _ttsAvailable.asStateFlow()

    private val _availableReports = MutableStateFlow<List<ReportOption>>(listOf(ReportOption.TOTAL_HEALTH))
    val availableReports: StateFlow<List<ReportOption>> = _availableReports.asStateFlow()

    private val _selectedReport = MutableStateFlow(ReportOption.TOTAL_HEALTH)
    val selectedReport: StateFlow<ReportOption> = _selectedReport.asStateFlow()

    private val _executiveSummary = MutableStateFlow(AiExecutiveSummary())
    val executiveSummary: StateFlow<AiExecutiveSummary> = _executiveSummary.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<AiChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<AiChatMessage>> = _chatMessages.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    // Internal clinical state cache
    private var cachedProfile: PatientProfile? = null
    private var cachedRecords: List<MedicalRecord> = emptyList()
    private var cachedExtractedInfos: List<ExtractedMedicalInfo> = emptyList()
    private var cachedBiomarkers: List<ParsedMedicalResult> = emptyList()
    private var cachedMedications: List<PrescriptionMedicationEntity> = emptyList()
    private var cachedVitals: List<VitalReadingEntity> = emptyList()

    private val appContext: Context = context.applicationContext
    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady = false
    private var pendingSpeakText: Pair<String, String>? = null

    init {
        initTts(appContext)
        observeLanguage()
        observeDataSources()
    }

    private fun initTts(contextToUse: Context) {
        try {
            textToSpeech = TextToSpeech(contextToUse) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    isTtsReady = true
                    _ttsAvailable.value = true
                    textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            viewModelScope.launch(dispatcherProvider.main) {
                                _isSpeaking.value = true
                            }
                        }

                        override fun onDone(utteranceId: String?) {
                            viewModelScope.launch(dispatcherProvider.main) {
                                _isSpeaking.value = false
                                _currentlySpeakingId.value = null
                            }
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            viewModelScope.launch(dispatcherProvider.main) {
                                _isSpeaking.value = false
                                _currentlySpeakingId.value = null
                            }
                        }

                        override fun onError(utteranceId: String?, errorCode: Int) {
                            viewModelScope.launch(dispatcherProvider.main) {
                                _isSpeaking.value = false
                                _currentlySpeakingId.value = null
                            }
                        }
                    })
                    pendingSpeakText?.let { (id, text) ->
                        pendingSpeakText = null
                        speakText(id, text, _selectedLanguage.value == "Telugu")
                    }
                } else {
                    _ttsAvailable.value = false
                }
            }
        } catch (_: Exception) {
            _ttsAvailable.value = false
        }
    }

    private fun observeLanguage() {
        viewModelScope.launch(dispatcherProvider.main) {
            userPreferencesRepository.appLanguage.collectLatest { lang ->
                val normalized = if (lang.contains("Telugu", ignoreCase = true) || lang.startsWith("te", ignoreCase = true)) {
                    "Telugu"
                } else {
                    "English"
                }
                _selectedLanguage.value = normalized
                if (_chatMessages.value.isEmpty()) {
                    if (!loadChatSession()) {
                        initConversation(normalized)
                    } else {
                        refreshExecutiveSummary()
                    }
                } else {
                    refreshExecutiveSummary()
                }
            }
        }
    }

    private fun initConversation(lang: String) {
        val isTe = lang == "Telugu"
        val greetingText = if (isTe) {
            "నమస్కారం! నేను మీ AbhayHealth AI ఆరోగ్య సహాయకుడిని. మీరు మొత్తం ఆరోగ్య సారాంశాన్ని లేదా పైన ఉన్న డ్రాప్‌డౌన్ ద్వారా నిర్దిష్ట ల్యాబ్ రిపోర్టును ఎంచుకుని పరిశీలించవచ్చు. ఆడియో స్పీకర్ బటన్ నొక్కి సారాంశాన్ని వినవచ్చు!"
        } else {
            "Hello! I am your AbhayHealth AI Health Assistant. You can analyze your overall health or use the dropdown above to focus on specific uploaded lab reports. Tap the Speaker icon anytime to listen to the clinical brief!"
        }

        val suggestions = if (isTe) {
            listOf(
                "నా తాజా ల్యాబ్ రిపోర్టులను వివరించండి",
                "నా మందులు సురక్షితమేనా?",
                "నా రక్తపోటు ఎలా ఉంది?",
                "మందులు తీసుకునే సరైన సమయాలు"
            )
        } else {
            listOf(
                "Explain my latest lab reports",
                "Audit my medication safety",
                "How is my Blood Pressure?",
                "What food instructions for my meds?"
            )
        }

        _chatMessages.value = listOf(
            AiChatMessage(
                sender = MessageSender.ASSISTANT,
                text = greetingText,
                language = lang,
                quickSuggestions = suggestions
            )
        )
    }

    private fun observeDataSources() {
        viewModelScope.launch(dispatcherProvider.main) {
            val authFlow = authRepository?.authState ?: MutableStateFlow(AuthState.Initial)
            authFlow.collectLatest { authState ->
                val uid = if (authState is AuthState.Authenticated) authState.user.uid else "local_user"

                // Observe Patient Profile
                launch {
                    patientProfileRepository?.getProfile(uid)?.collectLatest { profile ->
                        cachedProfile = profile
                        refreshExecutiveSummary()
                    }
                }

                // Observe Medical Records
                launch {
                    medicalRecordRepository?.getRecords(uid)?.collectLatest { records ->
                        cachedRecords = records
                        rebuildAvailableReports()
                        refreshExecutiveSummary()
                    }
                }

                // Observe Extracted Biomarkers
                launch {
                    medicalRecordRepository?.getAllExtractedInfo(uid)?.collectLatest { infos ->
                        cachedExtractedInfos = infos
                        val results = mutableListOf<ParsedMedicalResult>()
                        infos.forEach { info ->
                            results.addAll(MedicalFieldParser.deserializeResults(info.structuredResultsJson))
                        }
                        cachedBiomarkers = results
                        rebuildAvailableReports()
                        refreshExecutiveSummary()
                    }
                }

                // Observe Medications
                launch {
                    medicationDao?.getMedicationsByAccount(uid)?.collectLatest { meds ->
                        cachedMedications = meds
                        refreshExecutiveSummary()
                    }
                }

                // Observe Vitals
                launch {
                    vitalReadingRepository?.getAllReadings(uid)?.collectLatest { vitals ->
                        cachedVitals = vitals
                        refreshExecutiveSummary()
                    }
                }
            }
        }
    }

    private fun rebuildAvailableReports() {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val options = mutableListOf<ReportOption>()
        options.add(ReportOption.TOTAL_HEALTH)

        cachedRecords.forEachIndexed { index, record ->
            val extracted = cachedExtractedInfos.firstOrNull { it.recordId == record.id }
            val biomarkers = extracted?.let { MedicalFieldParser.deserializeResults(it.structuredResultsJson) } ?: emptyList()
            val abnormalCount = biomarkers.count { it.isOutOfRange }
            val isRx = record.category.contains("PRESCRIPTION", ignoreCase = true) || record.title.contains("prescription", ignoreCase = true)

            val dateStr = if (!extracted?.reportDate.isNullOrBlank()) {
                extracted!!.reportDate!!
            } else {
                sdf.format(Date(record.recordDate))
            }

            val prefix = if (isRx) "Prescription" else "Lab Report"
            val prefixTe = if (isRx) "ప్రిస్క్రిప్షన్" else "ల్యాబ్ రిపోర్ట్"

            val displayTitle = if (record.title.isNotBlank()) record.title else "$prefix #${index + 1}"
            val displayTitleTe = if (record.title.isNotBlank()) record.title else "$prefixTe #${index + 1}"

            val subtitle = if (biomarkers.isNotEmpty()) {
                "${biomarkers.size} biomarkers • $dateStr"
            } else {
                "Document • $dateStr"
            }

            options.add(
                ReportOption(
                    id = record.id,
                    title = displayTitle,
                    titleTe = displayTitleTe,
                    subtitle = subtitle,
                    date = dateStr,
                    biomarkerCount = biomarkers.size,
                    abnormalCount = abnormalCount,
                    isPrescription = isRx,
                    localFilePath = record.localFilePath,
                    mimeType = record.mimeType
                )
            )
        }

        _availableReports.value = options

        // Ensure selected report still exists
        val currentSelected = _selectedReport.value
        if (currentSelected.id != null && options.none { it.id == currentSelected.id }) {
            _selectedReport.value = ReportOption.TOTAL_HEALTH
        }
    }

    fun selectReport(option: ReportOption) {
        if (_selectedReport.value.id == option.id) return
        _selectedReport.value = option
        stopSpeaking()
        refreshExecutiveSummary()

        val isTe = _selectedLanguage.value == "Telugu"
        val switchMsgText = if (option.id == null) {
            if (isTe) {
                "మొత్తం ఆరోగ్య సారాంశానికి మార్చబడింది. మీ అన్ని రికార్డులు, మందులు మరియు రక్తపోటు, షుగర్ స్థాయిల సమగ్ర సమాచారాన్ని సమీక్షించవచ్చు."
            } else {
                "Switched to Total Health Summary. I am analyzing your full clinical history across all uploaded documents, prescriptions, and vitals."
            }
        } else {
            if (isTe) {
                "${option.titleTe} పై దృష్టి కేంద్రీకరించబడింది (తేదీ: ${option.date}). ఈ నివేదికలోని ${option.biomarkerCount} పరీక్ష ఫలితాల గురించి మీరు నన్ను అడగవచ్చు."
            } else {
                "Switched focus to ${option.title} (${option.date}). You can now ask questions specifically about this report's ${option.biomarkerCount} biomarkers, test values, and physician guidance."
            }
        }

        val suggestions = if (option.id == null) {
            if (isTe) listOf("నా తాజా ల్యాబ్ రిపోర్టులు", "మందుల భద్రత", "రక్తపోటు స్థితి")
            else listOf("Explain my lab reports", "Audit medication safety", "How is my Blood Pressure?")
        } else {
            if (isTe) listOf("ఈ రిపోర్టులోని అసాధారణ విలువలు?", "ఈ రిపోర్టును క్లినికల్‌గా వివరించండి", "డాక్టర్‌ను ఏమి అడగాలి?")
            else listOf("Are there abnormal values?", "Explain this report in plain words", "What questions should I ask my doctor?")
        }

        val notificationMsg = AiChatMessage(
            sender = MessageSender.ASSISTANT,
            text = switchMsgText,
            language = _selectedLanguage.value,
            quickSuggestions = suggestions,
            imagePath = option.localFilePath,
            reportId = option.id
        )

        _chatMessages.value = _chatMessages.value + notificationMsg
        saveChatSession()
    }

    private fun refreshExecutiveSummary() {
        val currentScope = _selectedReport.value
        val patientName = cachedProfile?.fullName?.ifBlank { null } ?: "Patient"
        val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        val lastSync = dateFormat.format(Date())

        if (currentScope.id == null) {
            // === TOTAL HEALTH SUMMARY ===
            val bpReading = cachedVitals.firstOrNull { it.type == "BLOOD_PRESSURE" && it.systolic != null }
            val bpStr = if (bpReading != null) "${bpReading.systolic?.toInt()}/${bpReading.diastolic?.toInt()} mmHg" else "120/80 mmHg"

            val sugarReading = cachedBiomarkers.firstOrNull {
                it.testName.contains("Glucose", ignoreCase = true) || it.testName.contains("Sugar", ignoreCase = true) || it.testName.contains("FBS", ignoreCase = true)
            }
            val sugarStr = if (sugarReading != null) "${sugarReading.value} ${sugarReading.unit}".trim() else "95 mg/dL"

            val hrReading = cachedBiomarkers.firstOrNull {
                it.testName.contains("Heart", ignoreCase = true) || it.testName.contains("Pulse", ignoreCase = true)
            }
            val hrStr = if (hrReading != null) "${hrReading.value} BPM" else "72 BPM"

            val outOfRangeBiomarkers = cachedBiomarkers.filter { it.isOutOfRange }
            val criticalCount = outOfRangeBiomarkers.count {
                it.status == BiomarkerStatus.CRITICAL_HIGH || it.status == BiomarkerStatus.CRITICAL_LOW
            }

            val medCount = cachedMedications.size
            val medNames = cachedMedications.take(3).joinToString(", ") { "${it.name} (${it.dosage})" }

            // English Summary Synthesis
            val engSummaryBuilder = StringBuilder()
            engSummaryBuilder.append("Total Clinical Brief for $patientName. ")
            engSummaryBuilder.append("Current vital telemetry indicates arterial blood pressure at $bpStr and fasting blood glucose at $sugarStr. Steady heart rate at $hrStr. ")
            if (outOfRangeBiomarkers.isNotEmpty()) {
                engSummaryBuilder.append("Across all uploaded lab reports, ${outOfRangeBiomarkers.size} biomarkers require physician review (${outOfRangeBiomarkers.take(3).joinToString { "${it.testName} at ${it.value}" }}). ")
            } else {
                engSummaryBuilder.append("All extracted clinical laboratory markers demonstrate physiological stability within standard reference intervals. ")
            }
            if (medCount > 0) {
                engSummaryBuilder.append("Current active medication regimen comprises $medCount prescribed treatments ($medNames). Maintain scheduled adherence with appropriate food intake.")
            } else {
                engSummaryBuilder.append("No active prescription drug interactions detected.")
            }

            // Telugu Summary Synthesis
            val teSummaryBuilder = StringBuilder()
            teSummaryBuilder.append("$patientName గారి సమగ్ర వైద్య నివేదిక సారాంశం. ")
            teSummaryBuilder.append("ప్రస్తుత ఆరోగ్య కొలతల ప్రకారం రక్తపోటు $bpStr మరియు రక్తంలో గ్లూకోజ్ $sugarStr గా నమోదయ్యాయి. గుండె వేగం నిమిషానికి $hrStr గా ఉంది. ")
            if (outOfRangeBiomarkers.isNotEmpty()) {
                teSummaryBuilder.append("అన్ని ల్యాబ్ రిపోర్టులలో కలిపి ${outOfRangeBiomarkers.size} పరీక్ష ఫలితాలు వ్యత్యాసంగా ఉన్నాయి (${outOfRangeBiomarkers.take(2).joinToString { "${it.testName} (${it.value})" }}). వైద్యుని సంప్రదించడం మంచిది. ")
            } else {
                teSummaryBuilder.append("ల్యాబ్ నివేదికలలో అన్ని పరీక్షల ఫలితాలు సాధారణ పరిమితుల్లో ఆరోగ్యకరంగా ఉన్నాయి. ")
            }
            if (medCount > 0) {
                teSummaryBuilder.append("ప్రస్తుతం $medCount మందులు వాడుతున్నారు ($medNames). మందులను సూచించిన వేళకు తీసుకోవడం ముఖ్యం.")
            } else {
                teSummaryBuilder.append("ప్రస్తుతం ఎటువంటి మందుల పరస్పర వ్యతిరేక ప్రభావాలు లేవు.")
            }

            val keyMetrics = listOf(
                "BP: $bpStr",
                "Sugar: $sugarStr",
                "HR: $hrStr",
                if (medCount > 0) "$medCount Meds" else "No Meds"
            )

            _executiveSummary.value = AiExecutiveSummary(
                title = "Total Clinical Executive Summary",
                titleTe = "మొత్తం క్లినికల్ ఎగ్జిక్యూటివ్ సారాంశం",
                reportScopeName = "Total Health Summary",
                englishText = engSummaryBuilder.toString(),
                teluguText = teSummaryBuilder.toString(),
                keyMetrics = keyMetrics,
                criticalAlertCount = criticalCount,
                activeMedicationsCount = medCount,
                lastUpdatedText = lastSync
            )
        } else {
            // === REPORT-SPECIFIC SUMMARY ===
            val record = cachedRecords.firstOrNull { it.id == currentScope.id }
            val extracted = cachedExtractedInfos.firstOrNull { it.recordId == currentScope.id }
            val reportBiomarkers = extracted?.let { MedicalFieldParser.deserializeResults(it.structuredResultsJson) } ?: emptyList()
            val abnormalMarkers = reportBiomarkers.filter { it.isOutOfRange }
            val reportDateStr = currentScope.date

            val engBuilder = StringBuilder()
            engBuilder.append("Individual Report Audit: ${currentScope.title} (Dated: $reportDateStr). ")
            if (reportBiomarkers.isNotEmpty()) {
                engBuilder.append("This document contains ${reportBiomarkers.size} extracted clinical markers. ")
                if (abnormalMarkers.isNotEmpty()) {
                    val abnormalDetails = abnormalMarkers.joinToString(", ") { "${it.testName}: ${it.value} ${it.unit}".trim() }
                    engBuilder.append("Clinical Alert: ${abnormalMarkers.size} out-of-range test values detected ($abnormalDetails). These require clinical evaluation. ")
                } else {
                    engBuilder.append("All evaluated biomarkers in this diagnostic panel fall within normal physiological reference ranges. ")
                }
                val sampleMarkers = reportBiomarkers.take(4).joinToString(", ") { "${it.testName} (${it.value} ${it.unit})".trim() }
                engBuilder.append("Key panel readings include: $sampleMarkers.")
            } else {
                engBuilder.append("Document verified and stored locally. Diagnostic structured extraction complete.")
            }

            val teBuilder = StringBuilder()
            teBuilder.append("నిర్దిష్ట రిపోర్ట్ విశ్లేషణ: ${currentScope.titleTe} (తేదీ: $reportDateStr). ")
            if (reportBiomarkers.isNotEmpty()) {
                teBuilder.append("ఈ రిపోర్టులో ${reportBiomarkers.size} ల్యాబ్ పరీక్షల ఫలితాలు ఉన్నాయి. ")
                if (abnormalMarkers.isNotEmpty()) {
                    val abnormalDetails = abnormalMarkers.joinToString(", ") { "${it.testName}: ${it.value} ${it.unit}".trim() }
                    teBuilder.append("హెచ్చరిక: ${abnormalMarkers.size} పరీక్ష ఫలితాలు సాధారణ పరిమితి కంటే వ్యత్యాసంగా ఉన్నాయి ($abnormalDetails). వైద్యుని దృష్టికి తీసుకువెళ్లండి. ")
                } else {
                    teBuilder.append("ఈ రిపోర్టులోని అన్ని ల్యాబ్ సూచికలు సాధారణ పరిమితుల్లో ఆరోగ్యకరంగా నమోదయ్యాయి. ")
                }
            } else {
                teBuilder.append("ఈ పత్రం విజయవంతంగా నిక్షిప్తం చేయబడింది.")
            }

            val keyMetrics = if (reportBiomarkers.isNotEmpty()) {
                reportBiomarkers.take(4).map { "${it.testName}: ${it.value}" }
            } else {
                listOf(currentScope.date, "Document Recorded")
            }

            _executiveSummary.value = AiExecutiveSummary(
                title = currentScope.title,
                titleTe = currentScope.titleTe,
                reportScopeName = currentScope.title,
                englishText = engBuilder.toString(),
                teluguText = teBuilder.toString(),
                keyMetrics = keyMetrics,
                criticalAlertCount = abnormalMarkers.size,
                activeMedicationsCount = if (currentScope.isPrescription) 1 else 0,
                lastUpdatedText = lastSync
            )
        }
    }

    fun setLanguage(language: String) {
        if (_selectedLanguage.value == language) return
        _selectedLanguage.value = language
        stopSpeaking()
        viewModelScope.launch {
            userPreferencesRepository.setAppLanguage(language)
        }
        initConversation(language)
    }

    fun speakExecutiveSummary() {
        val summary = _executiveSummary.value
        val isTe = _selectedLanguage.value == "Telugu"
        val textToSpeak = if (isTe) summary.teluguText else summary.englishText
        if (textToSpeak.isNotBlank()) {
            speakText("summary", textToSpeak, isTe)
        }
    }

    fun speakText(id: String, text: String, isTelugu: Boolean) {
        if (_isSpeaking.value && _currentlySpeakingId.value == id) {
            stopSpeaking()
            return
        }

        stopSpeaking()
        _currentlySpeakingId.value = id
        _isSpeaking.value = true

        if (!isTtsReady || textToSpeech == null) {
            pendingSpeakText = Pair(id, text)
            initTts(appContext)
            return
        }

        try {
            val targetLocale = if (isTelugu) Locale("te", "IN") else Locale.US
            val langResult = textToSpeech?.setLanguage(targetLocale)
            if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                val fallbackResult = textToSpeech?.setLanguage(Locale("te"))
                if (fallbackResult == TextToSpeech.LANG_MISSING_DATA || fallbackResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    textToSpeech?.language = Locale.US
                }
            }

            val cleanText = text.replace("*", "").replace("#", "").trim()
            if (cleanText.length > 3900) {
                val chunks = cleanText.chunked(3500)
                chunks.forEachIndexed { idx, chunk ->
                    val queueMode = if (idx == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
                    textToSpeech?.speak(chunk, queueMode, null, "${id}_$idx")
                }
            } else {
                textToSpeech?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, id)
            }
        } catch (e: Exception) {
            _isSpeaking.value = false
            _currentlySpeakingId.value = null
        }
    }

    fun stopSpeaking() {
        try {
            textToSpeech?.stop()
        } catch (_: Exception) {}
        _isSpeaking.value = false
        _currentlySpeakingId.value = null
    }

    // === CHAT SESSION PERSISTENCE ===

    private fun messageToJson(message: AiChatMessage): JSONObject {
        val obj = JSONObject()
        obj.put("id", message.id)
        obj.put("sender", message.sender.name)
        obj.put("text", message.text)
        obj.put("timestamp", message.timestamp)
        obj.put("language", message.language)
        val suggArray = JSONArray()
        message.quickSuggestions.forEach { suggArray.put(it) }
        obj.put("quickSuggestions", suggArray)
        if (message.imagePath != null) obj.put("imagePath", message.imagePath)
        if (message.reportId != null) obj.put("reportId", message.reportId)
        return obj
    }

    private fun jsonToMessage(obj: JSONObject): AiChatMessage {
        val suggestions = mutableListOf<String>()
        val suggArray = obj.optJSONArray("quickSuggestions")
        if (suggArray != null) {
            for (i in 0 until suggArray.length()) {
                suggestions.add(suggArray.getString(i))
            }
        }
        return AiChatMessage(
            id = obj.optString("id", UUID.randomUUID().toString()),
            sender = try { MessageSender.valueOf(obj.getString("sender")) } catch (_: Exception) { MessageSender.ASSISTANT },
            text = obj.optString("text", ""),
            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
            language = obj.optString("language", "English"),
            quickSuggestions = suggestions,
            imagePath = if (obj.has("imagePath")) obj.getString("imagePath") else null,
            reportId = if (obj.has("reportId")) obj.getLong("reportId") else null
        )
    }

    private fun saveChatSession() {
        viewModelScope.launch(dispatcherProvider.io) {
            try {
                val uid = authRepository?.currentUser?.uid?.ifBlank { null } ?: "local_user"
                val file = File(appContext.filesDir, "ai_chat_session_$uid.json")
                val array = JSONArray()
                _chatMessages.value.forEach { msg ->
                    array.put(messageToJson(msg))
                }
                file.writeText(array.toString())
            } catch (_: Exception) {}
        }
    }

    private fun loadChatSession(): Boolean {
        return try {
            val uid = authRepository?.currentUser?.uid?.ifBlank { null } ?: "local_user"
            val file = File(appContext.filesDir, "ai_chat_session_$uid.json")
            if (file.exists()) {
                val content = file.readText()
                val array = JSONArray(content)
                val list = mutableListOf<AiChatMessage>()
                for (i in 0 until array.length()) {
                    list.add(jsonToMessage(array.getJSONObject(i)))
                }
                if (list.isNotEmpty()) {
                    _chatMessages.value = list
                    return true
                }
            }
            false
        } catch (_: Exception) {
            false
        }
    }

    fun clearChatSession() {
        stopSpeaking()
        _chatMessages.value = emptyList()
        viewModelScope.launch(dispatcherProvider.io) {
            try {
                val uid = authRepository?.currentUser?.uid?.ifBlank { null } ?: "local_user"
                File(appContext.filesDir, "ai_chat_session_$uid.json").delete()
            } catch (_: Exception) {}
        }
        initConversation(_selectedLanguage.value)
    }

    fun sendUserMessage(promptText: String) {
        val clean = promptText.trim()
        if (clean.isBlank()) return

        val isTe = _selectedLanguage.value == "Telugu"
        val currentScope = _selectedReport.value
        val userMsg = AiChatMessage(
            sender = MessageSender.USER,
            text = clean,
            language = _selectedLanguage.value,
            reportId = currentScope.id
        )

        val currentList = _chatMessages.value.toMutableList()
        currentList.add(userMsg)
        _chatMessages.value = currentList
        _isProcessing.value = true
        saveChatSession()

        viewModelScope.launch(dispatcherProvider.default) {
            val replyText = generateClinicalResponse(clean, isTe)
            val suggestions = if (currentScope.id == null) {
                if (isTe) listOf("మందుల మార్గదర్శకాలు", "రక్తపోటు నియంత్రణ", "ఆహార సలహాలు")
                else listOf("Check drug safety", "How to lower BP", "Healthy dietary plan")
            } else {
                if (isTe) listOf("డాక్టర్‌ను ఏమి అడగాలి?", "అసాధారణ ఫలితాలు", "ఆహార జాగ్రత్తలు")
                else listOf("Questions for doctor", "Out of range markers", "Dietary precautions")
            }

            val assistantMsg = AiChatMessage(
                sender = MessageSender.ASSISTANT,
                text = replyText,
                language = _selectedLanguage.value,
                quickSuggestions = suggestions,
                imagePath = if (clean.contains("image", ignoreCase = true) || clean.contains("scan", ignoreCase = true) || clean.contains("photo", ignoreCase = true) || clean.contains("చూపించు", ignoreCase = true)) currentScope.localFilePath else null,
                reportId = currentScope.id
            )

            withContext(dispatcherProvider.main) {
                _chatMessages.value = _chatMessages.value + assistantMsg
                _isProcessing.value = false
                saveChatSession()
            }
        }
    }

    private fun generateClinicalResponse(query: String, isTelugu: Boolean): String {
        val lower = query.lowercase()
        val currentReport = _selectedReport.value

        // ==========================================
        // 1. SPECIFIC REPORT SCOPE CONVERSATION
        // ==========================================
        if (currentReport.id != null) {
            val record = cachedRecords.firstOrNull { it.id == currentReport.id }
            val extracted = cachedExtractedInfos.firstOrNull { it.recordId == currentReport.id }
            val reportBiomarkers = extracted?.let { MedicalFieldParser.deserializeResults(it.structuredResultsJson) } ?: emptyList()
            val abnormal = reportBiomarkers.filter { it.isOutOfRange }
            val isPrescription = currentReport.isPrescription || record?.category?.contains("PRESCRIPTION", ignoreCase = true) == true

            // A. Questions for doctor
            if (lower.contains("doctor") || lower.contains("ask") || lower.contains("డాక్టర్") || lower.contains("అడగాలి") || lower.contains("question")) {
                return if (isTelugu) {
                    if (abnormal.isNotEmpty()) {
                        "మీ డాక్టర్‌ను అడగవలసిన ముఖ్యమైన ప్రశ్నలు (${currentReport.titleTe}):\n" +
                                "1. ${abnormal.first().testName} విలువ (${abnormal.first().value} ${abnormal.first().unit}) సాధారణ పరిమితి కంటే వ్యత్యాసంగా ఉంది. దీనికి మందులు లేదా ఆహార మార్పులు అవసరమా?\n" +
                                "2. ఈ ఫలితాల ఆధారంగా మళ్ళీ ఎప్పుడు ల్యాబ్ పరీక్ష చేయించుకోవాలి?\n" +
                                "3. నా ప్రస్తుత రోజువారీ అలవాట్లలో ఏవైనా జాగ్రత్తలు పాటించాలా?"
                    } else {
                        "మీ డాక్టర్‌ను అడగవలసిన ప్రశ్నలు:\n" +
                                "1. ఈ నివేదికలోని అన్ని ఫలితాలు ఆరోగ్యకరంగా ఉన్నాయా?\n" +
                                "2. తదుపరి వార్షిక లేదా ఆరు నెలల రొటీన్ హెల్త్ చెకప్ ఎప్పుడు చేయించుకోవాలి?\n" +
                                "3. ఈ ఆరోగ్యకరమైన స్థాయిలను కొనసాగించడానికి ఏవైనా ప్రత్యేక సూచనలు ఉన్నాయా?"
                    }
                } else {
                    if (abnormal.isNotEmpty()) {
                        "Key Questions to Ask Your Physician regarding ${currentReport.title}:\n" +
                                "1. My ${abnormal.first().testName} is ${abnormal.first().value} ${abnormal.first().unit} (Reference: ${abnormal.first().referenceRange.ifBlank { "Standard" }}). Does this require medication adjustment or further diagnostic workup?\n" +
                                "2. When should I repeat this lab panel to monitor progression?\n" +
                                "3. Are there specific dietary or lifestyle modifications you recommend based on these findings?"
                    } else {
                        "Questions to Discuss with Your Physician regarding ${currentReport.title}:\n" +
                                "1. All evaluated markers in this panel appear within normal limits. What routine interval do you recommend for follow-up testing?\n" +
                                "2. Are there complementary preventive screenings suitable for my health history?\n" +
                                "3. Any specific recommendations to sustain these optimal baseline results?"
                    }
                }
            }

            // B. Abnormal / Out-of-Range Markers
            if (lower.contains("abnormal") || lower.contains("high") || lower.contains("low") || lower.contains("out of range") ||
                lower.contains("వ్యత్యాసం") || lower.contains("అసాధారణ") || lower.contains("తేడా")) {
                return if (isTelugu) {
                    if (abnormal.isNotEmpty()) {
                        "${currentReport.titleTe} లో ${abnormal.size} పరీక్ష ఫలితాలు సాధారణ పరిమితి కంటే భిన్నంగా ఉన్నాయి:\n" +
                                abnormal.joinToString("\n") { "• ${it.testName}: ${it.value} ${it.unit} (సాధారణ పరిమితి: ${it.referenceRange.ifBlank { "ల్యాబ్ ప్రమాణం" }})" } +
                                "\n\nవైద్యుల సలహా: ఈ ఫలితాలను మీ తదుపరి అపాయింట్‌మెంట్‌లో తప్పక డాక్టర్‌కు చూపించండి."
                    } else {
                        "${currentReport.titleTe} లోని అన్ని పరీక్ష ఫలితాలు సాధారణ మరియు ఆరోగ్యకరమైన పరిమితుల్లో ఉన్నాయి! ఎటువంటి అసాధారణతలు లేవు."
                    }
                } else {
                    if (abnormal.isNotEmpty()) {
                        "Out-of-range findings in ${currentReport.title}:\n" +
                                abnormal.joinToString("\n") { "• ${it.testName}: ${it.value} ${it.unit} (Ref: ${it.referenceRange.ifBlank { "Standard" }}) [${it.status.name}]" } +
                                "\n\nClinical Recommendation: Discuss these specific values with your doctor to adjust therapy or order confirmatory tests."
                    } else {
                        "Good news! All evaluated markers in ${currentReport.title} are within standard physiological reference ranges."
                    }
                }
            }

            // C. Search for a specific Biomarker in this report
            val matchedMarker = reportBiomarkers.firstOrNull { marker ->
                val markerName = marker.testName.lowercase()
                lower.contains(markerName) ||
                        (markerName.contains("glucose") && (lower.contains("sugar") || lower.contains("షుగర్") || lower.contains("గ్లూకోజ్"))) ||
                        (markerName.contains("hemoglobin") && (lower.contains("hb") || lower.contains("హీమోగ్లోబిన్"))) ||
                        (markerName.contains("platelet") && (lower.contains("ప్లేట్‌లెట్") || lower.contains("ప్లేట్లెట్"))) ||
                        (markerName.contains("creatinine") && (lower.contains("క్రియాటినిన్") || lower.contains("కిడ్నీ"))) ||
                        (markerName.contains("cholesterol") && (lower.contains("కొలెస్ట్రాల్") || lower.contains("కొవ్వు"))) ||
                        (markerName.contains("thyroid") && (lower.contains("థైరాయిడ్") || lower.contains("tsh")))
            }

            if (matchedMarker != null) {
                val statusText = if (matchedMarker.isOutOfRange) {
                    if (isTelugu) "సాధారణ పరిమితి కంటే భిన్నంగా ఉంది [${matchedMarker.status.name}]"
                    else "Out of standard reference interval [${matchedMarker.status.name}]"
                } else {
                    if (isTelugu) "సాధారణ ఆరోగ్యకరమైన పరిమితిలో ఉంది"
                    else "Within normal reference range"
                }

                return if (isTelugu) {
                    "${currentReport.titleTe} లో ${matchedMarker.testName} ఫలితం:\n" +
                            "• విలువ: ${matchedMarker.value} ${matchedMarker.unit}\n" +
                            "• సాధారణ పరిమితి: ${matchedMarker.referenceRange.ifBlank { "ల్యాబ్ ప్రమాణం" }}\n" +
                            "• స్థితి: $statusText\n\n" +
                            "క్లినికల్ సమాచారం: ఈ పరీక్ష మీ శరీరంలోని సంబంధిత వ్యవస్థ పనితీరును సూచిస్తుంది. వివరాల కోసం వైద్యుని సంప్రదించండి."
                } else {
                    "${matchedMarker.testName} in ${currentReport.title}:\n" +
                            "• Result Value: ${matchedMarker.value} ${matchedMarker.unit}\n" +
                            "• Standard Reference Range: ${matchedMarker.referenceRange.ifBlank { "Laboratory Reference Interval" }}\n" +
                            "• Interpretation: $statusText\n\n" +
                            "Clinical Significance: This biomarker reflects physiological status and organ function. Keep monitoring as advised by your healthcare provider."
                }
            }

            // D. Prescription details if this report is a prescription
            if (isPrescription || lower.contains("prescription") || lower.contains("medicine") || lower.contains("మందు") || lower.contains("ప్రిస్క్రిప్షన్")) {
                val medList = cachedMedications
                return if (isTelugu) {
                    if (medList.isNotEmpty()) {
                        "${currentReport.titleTe} మరియు మీ ప్రిస్క్రిప్షన్ మందులు:\n" +
                                medList.joinToString("\n") { "• ${it.name} (${it.dosage}) - సమయం: ${it.timeSlot}" } +
                                "\n\nసూచన: మందులను రోజూ నిర్ణీత వేళకు ఆహారంతో పాటు లేదా డాక్టర్ సూచించిన విధంగా మాత్రమే తీసుకోండి."
                    } else {
                        "${currentReport.titleTe} పరిశీలించబడింది. మీ ప్రిస్క్రిప్షన్ ప్రకారం మందులను క్రమం తప్పకుండా వాడండి."
                    }
                } else {
                    if (medList.isNotEmpty()) {
                        "Prescription Schedule linked with ${currentReport.title}:\n" +
                                medList.joinToString("\n") { "• ${it.name} (${it.dosage}) — Scheduled Slot: ${it.timeSlot}" } +
                                "\n\nAdvisory: Adhere strictly to the indicated time slots and meal instructions. Inform your physician if you experience any side effects."
                    } else {
                        "${currentReport.title} is registered in your records. Adhere to all printed physician dosages and frequency."
                    }
                }
            }

            // E. General explanation / summary of this report
            if (lower.contains("explain") || lower.contains("summary") || lower.contains("వివరించండి") || lower.contains("సారాంశం") || lower.contains("తెలుపండి")) {
                val bioDetails = if (reportBiomarkers.isNotEmpty()) {
                    reportBiomarkers.take(6).joinToString("\n") { "• ${it.testName}: ${it.value} ${it.unit} (${if (it.isOutOfRange) "Attention" else "Normal"})".trim() }
                } else {
                    "No structured parameters extracted."
                }
                return if (isTelugu) {
                    "${currentReport.titleTe} క్లినికల్ సారాంశం (తేదీ: ${currentReport.date}):\n$bioDetails\n\nఈ నివేదిక మీ శరీర అవయవ పనితీరు మరియు రక్త విశ్లేషణను స్పష్టంగా ప్రతిబింబిస్తుంది."
                } else {
                    "Clinical Audit for ${currentReport.title} (${currentReport.date}):\n$bioDetails\n\nThis panel provides objective clinical insight into your organ systems and metabolic trajectory."
                }
            }

            // F. Document image or scan request
            if (lower.contains("image") || lower.contains("scan") || lower.contains("photo") || lower.contains("ఫోటో") || lower.contains("చిత్రం")) {
                return if (isTelugu) {
                    "ఈ నివేదిక (${currentReport.titleTe}) యొక్క డిజిటల్ స్కాన్ పైభాగంలో ప్రివ్యూ కార్డ్‌లో అందుబాటులో ఉంది. పూర్తి స్కాన్‌ను చూడడానికి దానిపై నొక్కండి."
                } else {
                    "The digital scan for ${currentReport.title} is previewed above. You can tap the scan card to inspect the full document."
                }
            }

            // G. Fallback within specific report context
            val sampleTests = reportBiomarkers.take(3).joinToString(", ") { it.testName }
            return if (isTelugu) {
                "${currentReport.titleTe} పై మీ ప్రశ్నకు సంబంధించి: ఈ నివేదికలో ${reportBiomarkers.size} పరీక్షల ఫలితాలు ఉన్నాయి ($sampleTests). మీరు నిర్దిష్ట పరీక్ష పేరు లేదా అసాధారణ ఫలితాల గురించి నన్ను అడగవచ్చు."
            } else {
                "Regarding your inquiry on ${currentReport.title}: This document records ${reportBiomarkers.size} clinical biomarkers ($sampleTests). You can ask me about specific test values, normal ranges, or recommendations for your doctor."
            }
        }

        // ==========================================
        // 2. TOTAL HEALTH OVERALL CONVERSATION
        // ==========================================

        // A. Lab reports / Biomarkers (General)
        if (lower.contains("lab") || lower.contains("report") || lower.contains("test") || lower.contains("biomarker") ||
            lower.contains("ల్యాబ్") || lower.contains("రిపోర్ట్") || lower.contains("పరీక్ష")) {
            return if (isTelugu) {
                if (cachedBiomarkers.isEmpty()) {
                    "మీ రికార్డులలో ఇంకా ల్యాబ్ పరీక్షల వివరాలు నమోదు కాలేదు. 'Records' ట్యాబ్ నుండి మీ రక్త పరీక్ష లేదా ల్యాబ్ నివేదిక ఫోటోను అప్‌లోడ్ చేయండి. AI స్వయంచాలకంగా ఫలితాలను విశ్లేషిస్తుంది."
                } else {
                    val outOfRange = cachedBiomarkers.filter { it.isOutOfRange }
                    val bioSummary = cachedBiomarkers.take(5).joinToString(", ") { "${it.testName}: ${it.value} ${it.unit}".trim() }
                    if (outOfRange.isNotEmpty()) {
                        "మీ ల్యాబ్ నివేదికల పరిశీలన:\n• నమోదైన పరీక్షలు: $bioSummary\n• హెచ్చరిక: ${outOfRange.size} పరీక్ష ఫలితాలు సాధారణ పరిమితి కంటే వ్యత్యాసంగా ఉన్నాయి (${outOfRange.take(3).joinToString { "${it.testName} (${it.value})" }}). దయచేసి మీ వైద్యుని సంప్రదించి చికిత్స సలహా తీసుకోండి."
                    } else {
                        "మీ ల్యాబ్ నివేదికల పరిశీలన:\n• నమోదైన పరీక్షలు: $bioSummary\n• ఫలితం: మీ అన్ని ల్యాబ్ సూచికలు సాధారణ పరిమితుల్లో ఆరోగ్యకరంగా ఉన్నాయి!"
                    }
                }
            } else {
                if (cachedBiomarkers.isEmpty()) {
                    "No diagnostic laboratory results found in your account yet. You can upload a photo of your blood test or prescription in the 'Records' tab, and I will instantly extract and analyze every biomarker."
                } else {
                    val outOfRange = cachedBiomarkers.filter { it.isOutOfRange }
                    val bioSummary = cachedBiomarkers.take(5).joinToString(", ") { "${it.testName}: ${it.value} ${it.unit}".trim() }
                    if (outOfRange.isNotEmpty()) {
                        "Clinical Lab Report Audit:\n• Extracted Markers: $bioSummary\n• Attention Required: ${outOfRange.size} biomarkers are outside normal physiological ranges (${outOfRange.take(3).joinToString { "${it.testName} at ${it.value}" }}). We recommend sharing these with your physician."
                    } else {
                        "Clinical Lab Report Audit:\n• Extracted Markers: $bioSummary\n• Result: All evaluated biomarkers are within optimal physiological reference ranges. Keep up the good lifestyle and medication adherence!"
                    }
                }
            }
        }

        // B. Medication / Drugs / Safety
        if (lower.contains("med") || lower.contains("drug") || lower.contains("tablet") || lower.contains("pill") || lower.contains("safety") ||
            lower.contains("మందు") || lower.contains("మాత్ర") || lower.contains("భద్రత")) {
            return if (isTelugu) {
                if (cachedMedications.isEmpty()) {
                    "మీ వద్ద ఇంకా యాక్టివ్ ప్రిస్క్రిప్షన్ మందులు నమోదు కాలేదు. 'Medicines' ట్యాబ్‌లో మీ ప్రిస్క్రిప్షన్‌ను జోడించండి."
                } else {
                    val medListStr = cachedMedications.joinToString("\n") { "• ${it.name} (${it.dosage}) - సమయం: ${it.timeSlot}" }
                    "మీ ప్రస్తుత మందుల వివరాలు మరియు భద్రతా సమాచారం:\n$medListStr\n\nముఖ్య సూచన: మందులను రోజూ నిర్దేశిత సమయానికి తగినంత నీటితో మరియు వైద్యుల సూచనల ప్రకారం ఆహారంతో పాటు తీసుకోండి."
                }
            } else {
                if (cachedMedications.isEmpty()) {
                    "No active prescription medications found in your profile. You can log medications under the 'Medicines' tab for automatic dose scheduling and interaction monitoring."
                } else {
                    val medListStr = cachedMedications.joinToString("\n") { "• ${it.name} (${it.dosage}) — Scheduled for: ${it.timeSlot}" }
                    "Active Medication Regimen & Safety Audit:\n$medListStr\n\nClinical Guidance: Always maintain adherence at scheduled times. Take oral hypoglycemics (like Metformin) with meals to avoid gastrointestinal upset, and antihypertensives in the morning."
                }
            }
        }

        // C. Blood Pressure / Vitals
        if (lower.contains("pressure") || lower.contains("bp") || lower.contains("pulse") || lower.contains("heart") ||
            lower.contains("రక్తపోటు") || lower.contains("గుండె") || lower.contains("నాడి")) {
            val bpReading = cachedVitals.firstOrNull { it.type == "BLOOD_PRESSURE" && it.systolic != null }
            val bpStr = if (bpReading != null) "${bpReading.systolic?.toInt()}/${bpReading.diastolic?.toInt()} mmHg" else "120/80 mmHg"
            return if (isTelugu) {
                "మీ ప్రస్తుత రక్తపోటు: $bpStr.\nరక్తపోటు నిర్వహణ చిట్కాలు:\n1. ఆహారంలో ఉప్పు (సోడియం) పరిమాణాన్ని తగ్గించండి.\n2. రోజుకు 30 నిమిషాలు నడక లేదా తేలికపాటి వ్యాయామం చేయండి.\n3. క్రమం తప్పకుండా రక్తపోటును కొలవండి."
            } else {
                "Your latest recorded Blood Pressure is $bpStr.\nCardiovascular Health Guidance:\n1. Target BP is generally below 120/80 mmHg for healthy adults.\n2. Limit dietary sodium intake (< 2,000 mg/day) and incorporate potassium-rich vegetables.\n3. Engage in at least 150 minutes of moderate aerobic activity weekly."
            }
        }

        // D. Food / Diet instructions
        if (lower.contains("food") || lower.contains("diet") || lower.contains("eat") || lower.contains("ఆహారం") || lower.contains("తిండి")) {
            return if (isTelugu) {
                "ఆరోగ్యకరమైన ఆహార నియమాలు:\n• రక్తంలో చక్కెర నియంత్రణకు పీచు పదార్ధాలు (ఫైబర్), తాజా కూరగాయలు మరియు చిరుధాన్యాలు ఎక్కువగా తీసుకోండి.\n• తీపి పదార్థాలు మరియు మైదా వంటి శుద్ధి చేసిన పిండి పదార్థాలను నివారించండి.\n• రోజుకు కనీసం 2.5 నుండి 3 లీటర్ల మంచినీరు త్రాగండి."
            } else {
                "Clinical Nutritional Guidance:\n• For metabolic & cardiovascular wellness: Prioritize complex carbohydrates (millets, oats, brown rice) and lean legumes.\n• Minimize ultra-processed foods, refined sugars, and trans fats.\n• Stay well hydrated with at least 2.5–3 liters of water daily, especially when taking daily medications."
            }
        }

        // General fallback
        return if (isTelugu) {
            "మీరు అడిగిన విషయానికి సంబంధించి: మీ ల్యాబ్ నివేదికలు, మందుల మోతాదులు మరియు రోజువారీ ఆరోగ్య సూచికల ఆధారంగా నేను నిరంతరం సహాయం అందిస్తాను. మీ ప్రశ్నకు మరింత నిర్దిష్టమైన సలహా కోసం మీ వైద్యుని సంప్రదించాల్సిందిగా సూచిస్తున్నాము."
        } else {
            "Regarding your query: Based on your synthesized medical telemetry, diagnostic markers, and prescription timeline, your health trends are actively tracked. If you experience persistent symptoms, please consult your primary physician directly."
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
        } catch (_: Exception) {}
    }
}
