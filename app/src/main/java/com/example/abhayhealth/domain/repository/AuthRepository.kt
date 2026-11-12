package com.example.abhayhealth.domain.repository

import android.app.Activity
import com.example.abhayhealth.domain.model.AuthState
import com.example.abhayhealth.domain.model.AuthUser
import kotlinx.coroutines.flow.StateFlow

/**
 * Authentication Repository interface for Firebase Phone Authentication.
 */
interface AuthRepository {
    val authState: StateFlow<AuthState>
    val currentUser: AuthUser?
    val isAuthenticated: Boolean

    fun sendOtp(phoneNumber: String, activity: Activity? = null)
    fun verifyOtp(verificationId: String, code: String)
    fun resendOtp(phoneNumber: String, activity: Activity? = null, resendToken: Any? = null)
    fun signOut()
    fun deleteAccount(onComplete: (Boolean, String?) -> Unit)
}
