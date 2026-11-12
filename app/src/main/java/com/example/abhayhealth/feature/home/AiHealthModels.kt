package com.example.abhayhealth.feature.home

import com.example.abhayhealth.domain.model.ParsedMedicalResult
import com.example.abhayhealth.feature.medicines.PrescriptionMedicationEntity

enum class HealthStatusTier {
    OPTIMAL,
    STABLE,
    ATTENTION_NEEDED
}

enum class AiInsightCategory {
    MEDICATION,
    LAB_VITAL,
    ALLERGY_SAFETY,
    CARE_ADVICE
}

enum class AiSeverity {
    INFO,
    POSITIVE,
    WARNING
}

data class AiInsightItem(
    val id: String,
    val title: String,
    val description: String,
    val category: AiInsightCategory,
    val severity: AiSeverity
)

enum class AiSummaryScope {
    TOTAL_HEALTH,
    SPECIFIC_REPORT
}

data class AiReportOption(
    val recordId: Long,
    val title: String,
    val date: String,
    val labOrDoctorName: String? = null,
    val biomarkerCount: Int = 0,
    val abnormalCount: Int = 0,
    val isPrescription: Boolean = false
)

data class AiHealthSummaryUiState(
    val isEnabled: Boolean = false,
    val isLoading: Boolean = false,
    val hasData: Boolean = false,
    val selectedScope: AiSummaryScope = AiSummaryScope.TOTAL_HEALTH,
    val selectedReportId: Long? = null,
    val availableReports: List<AiReportOption> = emptyList(),
    val summaryTitle: String = "Total Health Summary",
    val summarySubtitle: String = "Synthesized across all uploaded documents & clinical history",
    val healthScore: Int = 85,
    val statusTier: HealthStatusTier = HealthStatusTier.STABLE,
    val totalMedications: Int = 0,
    val takenMedications: Int = 0,
    val nextMedication: PrescriptionMedicationEntity? = null,
    val totalRecords: Int = 0,
    val latestRecordTitle: String? = null,
    val latestRecordDate: String? = null,
    val extractedVitalsSummary: String? = null,
    val reportBiomarkers: List<ParsedMedicalResult> = emptyList(),
    val reportAbnormalCount: Int = 0,
    val reportDoctorQuestions: List<String> = emptyList(),
    val insights: List<AiInsightItem> = emptyList(),
    val actionRecommendations: List<String> = emptyList(),
    val activeConditions: List<String> = emptyList(),
    val activeAllergies: List<String> = emptyList(),
    val lastAnalyzedTime: String = "Just now"
)

enum class AiDialogType {
    NONE,
    AI_ASSISTANT_MODAL,
    MEDICATION_SAFETY,
    DOCTOR_BRIEF,
    LAB_EXPLAINER
}
