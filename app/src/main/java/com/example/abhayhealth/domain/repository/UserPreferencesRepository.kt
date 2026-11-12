package com.example.abhayhealth.domain.repository

import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    val isOnboardingCompleted: Flow<Boolean>
    suspend fun setOnboardingCompleted(completed: Boolean)
    val patientName: Flow<String?>
    suspend fun setPatientName(name: String)
    val isAiSummaryEnabled: Flow<Boolean>
    suspend fun setAiSummaryEnabled(enabled: Boolean)
    val appLanguage: Flow<String>
    suspend fun setAppLanguage(language: String)
    val isMedRemindersEnabled: Flow<Boolean>
    suspend fun setMedRemindersEnabled(enabled: Boolean)
}
