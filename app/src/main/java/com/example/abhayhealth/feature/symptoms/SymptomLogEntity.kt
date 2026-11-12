package com.example.abhayhealth.feature.symptoms

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity representing a patient's logged symptom with severity level,
 * clinical notes, triggers, and precise timestamp for longitudinal trend analysis.
 */
@Entity(tableName = "symptom_logs")
data class SymptomLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val accountUid: String,
    val symptomName: String,
    val severity: String, // "MILD", "MODERATE", "SEVERE", "CRITICAL"
    val severityScore: Int, // 1 (Mild), 2 (Moderate), 3 (Severe), 4 (Critical)
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String = "",
    val triggers: String = "",
    val durationMinutes: Int = 0
) {
    companion object {
        const val SEVERITY_MILD = "MILD"
        const val SEVERITY_MODERATE = "MODERATE"
        const val SEVERITY_SEVERE = "SEVERE"
        const val SEVERITY_CRITICAL = "CRITICAL"

        fun getScoreForSeverity(severity: String): Int {
            return when (severity.uppercase()) {
                SEVERITY_MILD -> 1
                SEVERITY_MODERATE -> 2
                SEVERITY_SEVERE -> 3
                SEVERITY_CRITICAL -> 4
                else -> 1
            }
        }
    }
}
