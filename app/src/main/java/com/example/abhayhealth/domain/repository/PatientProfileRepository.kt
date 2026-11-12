package com.example.abhayhealth.domain.repository

import com.example.abhayhealth.domain.model.PatientProfile
import kotlinx.coroutines.flow.Flow

/**
 * Local persistence repository contract for patient health profile.
 */
interface PatientProfileRepository {
    fun getProfile(accountUid: String): Flow<PatientProfile?>
    suspend fun saveProfile(profile: PatientProfile)
    suspend fun deleteProfile(accountUid: String)
    suspend fun hasProfile(accountUid: String): Boolean
    suspend fun clearCurrentProfileMemory()
}
