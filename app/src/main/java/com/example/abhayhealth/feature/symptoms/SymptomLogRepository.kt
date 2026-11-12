package com.example.abhayhealth.feature.symptoms

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Repository providing an abstraction over SymptomLogDao for logging,
 * querying, and analyzing patient symptoms.
 */
class SymptomLogRepository(
    private val symptomDao: SymptomLogDao
) {

    fun getAllSymptoms(accountUid: String): Flow<List<SymptomLogEntity>> {
        return symptomDao.getAllSymptoms(accountUid)
    }

    fun getSymptomsSince(accountUid: String, sinceTimestamp: Long): Flow<List<SymptomLogEntity>> {
        return symptomDao.getSymptomsSince(accountUid, sinceTimestamp)
    }

    fun getSymptomsByName(accountUid: String, symptomName: String): Flow<List<SymptomLogEntity>> {
        return symptomDao.getSymptomsByName(accountUid, symptomName)
    }

    fun getDistinctSymptoms(accountUid: String): Flow<List<String>> {
        return symptomDao.getDistinctSymptoms(accountUid)
    }

    suspend fun logSymptom(symptom: SymptomLogEntity): Long = withContext(Dispatchers.IO) {
        symptomDao.insertSymptom(symptom)
    }

    suspend fun deleteSymptom(symptom: SymptomLogEntity): Int = withContext(Dispatchers.IO) {
        symptomDao.deleteSymptom(symptom)
    }

    suspend fun deleteSymptomById(id: Long, accountUid: String): Int = withContext(Dispatchers.IO) {
        symptomDao.deleteSymptomById(id, accountUid)
    }

    suspend fun getSymptomCount(accountUid: String): Int = withContext(Dispatchers.IO) {
        symptomDao.getSymptomCount(accountUid)
    }
}
