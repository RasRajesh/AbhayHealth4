package com.example.abhayhealth.domain.model

/**
 * Domain model representing an authenticated user account.
 */
data class AuthUser(
    val uid: String,
    val phoneNumber: String?
)
