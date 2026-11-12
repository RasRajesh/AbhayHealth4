package com.example.abhayhealth.domain.model

/**
 * Domain model representing a doctor prescription document or entry.
 */
data class Prescription(
    val id: String,
    val title: String,
    val doctorName: String? = null,
    val issueDateTimestamp: Long,
    val notes: String? = null
)
