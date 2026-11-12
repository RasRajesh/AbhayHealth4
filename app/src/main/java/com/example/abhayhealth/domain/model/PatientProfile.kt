package com.example.abhayhealth.domain.model

/**
 * Domain model representing the Patient's personal health profile & baseline health identity.
 * Persisted locally and scoped to the authenticated account UID.
 */
data class PatientProfile(
    val accountUid: String,
    val fullName: String,
    val dateOfBirth: String? = null,
    val gender: String? = null,
    val city: String? = null,
    val state: String? = null,
    val preferredLanguage: String = "English",
    val photoUri: String? = null,
    val bloodGroup: String? = null,
    val allergies: String? = null,
    val healthConditions: String? = null,
    val emergencyContactName: String? = null,
    val emergencyContactPhone: String? = null,
    val additionalNotes: String? = null,
    val updatedAtTimestamp: Long = System.currentTimeMillis()
)
