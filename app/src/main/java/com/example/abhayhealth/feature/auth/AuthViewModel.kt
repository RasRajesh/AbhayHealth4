package com.example.abhayhealth.feature.auth

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.abhayhealth.domain.model.AuthState
import com.example.abhayhealth.domain.repository.AuthRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    val authState: StateFlow<AuthState> = authRepository.authState

    private val _countryCode = MutableStateFlow("+91")
    val countryCode: StateFlow<String> = _countryCode.asStateFlow()

    private val _phoneNumber = MutableStateFlow("")
    val phoneNumber: StateFlow<String> = _phoneNumber.asStateFlow()

    private val _otpCode = MutableStateFlow("")
    val otpCode: StateFlow<String> = _otpCode.asStateFlow()

    private val _resendCountdown = MutableStateFlow(0)
    val resendCountdown: StateFlow<Int> = _resendCountdown.asStateFlow()

    private var countdownJob: Job? = null

    fun onCountryCodeChanged(code: String) {
        _countryCode.value = code
    }

    fun onPhoneNumberChanged(number: String) {
        // Strip non-digits and cap at 10 digits
        val digitsOnly = number.filter { it.isDigit() }.take(10)
        _phoneNumber.value = digitsOnly
    }

    fun onOtpCodeChanged(code: String) {
        val digitsOnly = code.filter { it.isDigit() }.take(6)
        _otpCode.value = digitsOnly
    }

    fun sendOtp(activity: Activity? = null) {
        val rawNumber = _phoneNumber.value.trim()
        if (rawNumber.length != 10) {
            return
        }
        val fullPhoneNumber = "${_countryCode.value}$rawNumber"
        authRepository.sendOtp(fullPhoneNumber, activity)
        startResendTimer()
    }

    fun verifyOtp() {
        val currentState = authState.value
        if (currentState is AuthState.CodeSent) {
            authRepository.verifyOtp(currentState.verificationId, _otpCode.value.trim())
        }
    }

    fun resendOtp(activity: Activity? = null) {
        val currentState = authState.value
        if (currentState is AuthState.CodeSent && _resendCountdown.value == 0) {
            val fullPhoneNumber = "${_countryCode.value}${_phoneNumber.value.trim()}"
            authRepository.resendOtp(fullPhoneNumber, activity, currentState.resendToken)
            startResendTimer()
        }
    }

    private fun startResendTimer() {
        countdownJob?.cancel()
        _resendCountdown.value = 60
        countdownJob = viewModelScope.launch {
            while (_resendCountdown.value > 0) {
                delay(1000)
                _resendCountdown.value = _resendCountdown.value - 1
            }
        }
    }

    fun signOut() {
        authRepository.signOut()
        _phoneNumber.value = ""
        _otpCode.value = ""
        countdownJob?.cancel()
        _resendCountdown.value = 0
    }
}
