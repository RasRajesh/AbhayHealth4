package com.example.abhayhealth.feature.records

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * Room Entity representing extracted medical text and structured results for a Medical Record.
 * Linked to MedicalRecord via recordId with CASCADE deletion.
 */
@Entity(
    tableName = "extracted_medical_info",
    foreignKeys = [
        ForeignKey(
            entity = MedicalRecord::class,
            parentColumns = ["id"],
            childColumns = ["recordId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ExtractedMedicalInfo(
    @PrimaryKey
    val recordId: Long,
    val accountUid: String,
    val rawText: String,
    val documentTitle: String,
    val reportDate: String? = null,
    val structuredResultsJson: String = "[]",
    val reviewStatus: String = STATUS_UNREVIEWED,
    val processedPages: Int = 1,
    val updatedAtTimestamp: Long = System.currentTimeMillis()
) {
    companion object {
        const val STATUS_UNREVIEWED = "UNREVIEWED"
        const val STATUS_REVIEWED_AND_SAVED = "REVIEWED_AND_SAVED"
        const val STATUS_EDITED = "EDITED"
    }
}
