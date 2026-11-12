package com.example.abhayhealth.domain.model

/**
 * Domain model representing a medicine item tracked for daily usage and reminders.
 */
data class Medication(
    val id: String,
    val name: String,
    val dosage: String,
    val frequency: String,
    val timeOfDay: String,
    val isTakenToday: Boolean = false,
    val reminderEnabled: Boolean = true
)
