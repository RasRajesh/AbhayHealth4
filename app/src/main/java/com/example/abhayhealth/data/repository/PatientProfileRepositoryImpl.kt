package com.example.abhayhealth.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.abhayhealth.core.storage.datastore.dataStore
import com.example.abhayhealth.domain.model.PatientProfile
import com.example.abhayhealth.domain.repository.PatientProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class PatientProfileRepositoryImpl(
    private val context: Context
) : PatientProfileRepository {

    private val activeProfileFlow = MutableStateFlow<PatientProfile?>(null)

    private fun getProfileKey(accountUid: String) = stringPreferencesKey("profile_data_$accountUid")

    override fun getProfile(accountUid: String): Flow<PatientProfile?> {
        val key = getProfileKey(accountUid)
        return context.dataStore.data.map { prefs ->
            val serialized = prefs[key]
            if (serialized != null && serialized.isNotBlank()) {
                deserializeProfile(serialized, accountUid)
            } else {
                null
            }
        }
    }

    override suspend fun saveProfile(profile: PatientProfile) {
        val key = getProfileKey(profile.accountUid)
        val serialized = serializeProfile(profile)
        context.dataStore.edit { prefs ->
            prefs[key] = serialized
        }
        activeProfileFlow.value = profile
    }

    override suspend fun deleteProfile(accountUid: String) {
        if (accountUid.isBlank()) return
        val key = getProfileKey(accountUid)
        context.dataStore.edit { prefs ->
            prefs.remove(key)
        }
        if (activeProfileFlow.value?.accountUid == accountUid) {
            activeProfileFlow.value = null
        }
    }

    override suspend fun hasProfile(accountUid: String): Boolean {
        if (accountUid.isBlank()) return false
        val key = getProfileKey(accountUid)
        val prefs = context.dataStore.data.first()
        val serialized = prefs[key]
        return serialized != null && serialized.isNotBlank()
    }

    override suspend fun clearCurrentProfileMemory() {
        activeProfileFlow.value = null
    }

    private fun serializeProfile(profile: PatientProfile): String {
        return listOf(
            profile.fullName,
            profile.dateOfBirth ?: "",
            profile.gender ?: "",
            profile.city ?: "",
            profile.state ?: "",
            profile.preferredLanguage,
            profile.photoUri ?: "",
            profile.bloodGroup ?: "",
            profile.allergies ?: "",
            profile.healthConditions ?: "",
            profile.emergencyContactName ?: "",
            profile.emergencyContactPhone ?: "",
            profile.additionalNotes ?: "",
            profile.updatedAtTimestamp.toString()
        ).joinToString("|||")
    }

    private fun deserializeProfile(serialized: String, accountUid: String): PatientProfile {
        val parts = serialized.split("|||")
        return PatientProfile(
            accountUid = accountUid,
            fullName = parts.getOrNull(0) ?: "",
            dateOfBirth = parts.getOrNull(1).takeIf { !it.isNull_or_blank() },
            gender = parts.getOrNull(2).takeIf { !it.isNull_or_blank() },
            city = parts.getOrNull(3).takeIf { !it.isNull_or_blank() },
            state = parts.getOrNull(4).takeIf { !it.isNull_or_blank() },
            preferredLanguage = parts.getOrNull(5).takeIf { !it.isNull_or_blank() } ?: "English",
            photoUri = parts.getOrNull(6).takeIf { !it.isNull_or_blank() },
            bloodGroup = parts.getOrNull(7).takeIf { !it.isNull_or_blank() },
            allergies = parts.getOrNull(8).takeIf { !it.isNull_or_blank() },
            healthConditions = parts.getOrNull(9).takeIf { !it.isNull_or_blank() },
            emergencyContactName = parts.getOrNull(10).takeIf { !it.isNull_or_blank() },
            emergencyContactPhone = parts.getOrNull(11).takeIf { !it.isNull_or_blank() },
            additionalNotes = parts.getOrNull(12).takeIf { !it.isNull_or_blank() },
            updatedAtTimestamp = parts.getOrNull(13)?.toLongOrNull() ?: System.currentTimeMillis()
        )
    }
}

private fun String?.isNull_or_blank(): Boolean = this == null || this.isBlank()
