package com.example.abhayhealth.core.security

import android.util.Log

/**
 * Security audit helper enforcing privacy-first logging policies.
 * Ensures personal health records, credentials, or sensitive patient details are never written to logcat.
 */
object SecurityAudit {
    private const val TAG = "AbhayHealth_Audit"
    
    // Set to false in production builds
    private const val IS_DEBUG_LOGGING_ALLOWED = false

    fun logEvent(event: String) {
        if (IS_DEBUG_LOGGING_ALLOWED) {
            Log.d(TAG, "Event: $event")
        }
    }

    fun logError(tag: String, message: String, throwable: Throwable? = null) {
        // Log safe message without printing sensitive health payloads
        Log.e(tag, sanitizeMessage(message), throwable)
    }

    private fun sanitizeMessage(input: String): String {
        // Redact potential patient identifiers or sensitive records
        return input.replace(Regex("(?i)(password|token|otp|ssn|health_record|medical_data)=[^&\\s]+"), "$1=[REDACTED]")
    }
}
