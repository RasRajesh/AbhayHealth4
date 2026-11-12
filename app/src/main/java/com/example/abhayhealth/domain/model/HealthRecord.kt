package com.example.abhayhealth.domain.model

/**
 * Domain model representing a structured medical document / report entry in the patient's history.
 */
data class HealthRecord(
    val id: String,
    val title: String,
    val category: HealthRecordCategory,
    val recordDateTimestamp: Long,
    val summary: String? = null,
    val documentUri: String? = null
)

enum class HealthRecordCategory {
    LAB_REPORT,
    PRESCRIPTION,
    DISCHARGE_SUMMARY,
    VACCINATION,
    GENERAL_NOTE
}
