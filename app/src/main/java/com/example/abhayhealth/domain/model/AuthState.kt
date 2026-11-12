package com.example.abhayhealth.domain.model

/**
 * Domain state for Phone OTP Authentication flow.
 */
sealed interface AuthState {
    data object Initial : AuthState
    data object Loading : AuthState
    data class Authenticated(val user: AuthUser) : AuthState
    data object Unauthenticated : AuthState
    data class CodeSent(
        val verificationId: String,
        val phoneNumber: String,
        val resendToken: Any? = null
    ) : AuthState
    data class Error(
        val message: String,
        val isConfigurationError: Boolean = false
    ) : AuthState
}
