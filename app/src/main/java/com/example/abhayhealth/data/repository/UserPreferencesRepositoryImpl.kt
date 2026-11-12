package com.example.abhayhealth.data.repository

import com.example.abhayhealth.core.storage.datastore.UserPreferences
import com.example.abhayhealth.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow

class UserPreferencesRepositoryImpl(
    private val userPreferences: UserPreferences
) : UserPreferencesRepository {

    override val isOnboardingCompleted: Flow<Boolean> = userPreferences.isOnboardingCompleted

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        userPreferences.setOnboardingCompleted(completed)
    }

    override val patientName: Flow<String?> = userPreferences.patientName

    override suspend fun setPatientName(name: String) {
        userPreferences.setPatientName(name)
    }

    override val isAiSummaryEnabled: Flow<Boolean> = userPreferences.isAiSummaryEnabled

    override suspend fun setAiSummaryEnabled(enabled: Boolean) {
        userPreferences.setAiSummaryEnabled(enabled)
    }

    override val appLanguage: Flow<String> = userPreferences.appLanguage

    override suspend fun setAppLanguage(language: String) {
        userPreferences.setAppLanguage(language)
    }

    override val isMedRemindersEnabled: Flow<Boolean> = userPreferences.isMedRemindersEnabled

    override suspend fun setMedRemindersEnabled(enabled: Boolean) {
        userPreferences.setMedRemindersEnabled(enabled)
    }
}
