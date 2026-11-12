package com.example.abhayhealth.feature.medicines

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Room Entity for persistent daily prescription medication schedule.
 */
@Entity(tableName = "prescription_medications")
data class PrescriptionMedicationEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val accountUid: String,
    val name: String,
    val dosage: String,
    val timeSlot: String, // MORNING (08:30), AFTERNOON (13:30), EVENING (19:00), NIGHT (21:30)
    val isTaken: Boolean = false,
    val createdAtTimestamp: Long = System.currentTimeMillis()
)
