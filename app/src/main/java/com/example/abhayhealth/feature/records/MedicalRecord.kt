package com.example.abhayhealth.feature.records

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity representing a Medical Record metadata entry.
 */
@Entity(tableName = "medical_records")
data class MedicalRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val accountUid: String,
    val title: String,
    val category: String,
    val recordDate: Long,
    val localFilePath: String? = null,
    val mimeType: String? = null,
    val createdAtTimestamp: Long = System.currentTimeMillis()
)
