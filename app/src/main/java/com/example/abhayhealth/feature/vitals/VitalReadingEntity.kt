package com.example.abhayhealth.feature.vitals

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class VitalType {
    BLOOD_PRESSURE,
    WEIGHT
}

@Entity(tableName = "vital_readings")
data class VitalReadingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val accountUid: String,
    val type: String,
    val systolic: Double? = null,
    val diastolic: Double? = null,
    val weightKg: Double? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String = ""
)
