package com.example.abhayhealth.core.storage.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "abhay_health_preferences")

class UserPreferences(private val context: Context) {

    companion object {
        val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val KEY_USER_ID = stringPreferencesKey("user_id")
        val KEY_PATIENT_NAME = stringPreferencesKey("patient_name")
        val KEY_AI_SUMMARY_ENABLED = booleanPreferencesKey("ai_summary_enabled")
        val KEY_APP_LANGUAGE = stringPreferencesKey("app_language")
        val KEY_MED_REMINDERS_ENABLED = booleanPreferencesKey("med_reminders_enabled")
    }

    val isOnboardingCompleted: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_ONBOARDING_COMPLETED] ?: false
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_ONBOARDING_COMPLETED] = completed
        }
    }

    val patientName: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_PATIENT_NAME]
    }

    suspend fun setPatientName(name: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_PATIENT_NAME] = name
        }
    }

    val isAiSummaryEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_AI_SUMMARY_ENABLED] ?: false
    }

    suspend fun setAiSummaryEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_AI_SUMMARY_ENABLED] = enabled
        }
    }

    val appLanguage: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_APP_LANGUAGE] ?: "English"
    }

    suspend fun setAppLanguage(language: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_APP_LANGUAGE] = language
        }
    }

    val isMedRemindersEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_MED_REMINDERS_ENABLED] ?: true
    }

    suspend fun setMedRemindersEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_MED_REMINDERS_ENABLED] = enabled
        }
    }
}
