package com.example.abhayhealth.feature.vitals

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class VitalReadingRepository(
    private val vitalReadingDao: VitalReadingDao
) {

    fun getAllReadings(accountUid: String): Flow<List<VitalReadingEntity>> {
        return vitalReadingDao.getAllReadingsByAccount(accountUid)
    }

    fun getReadingsByType(accountUid: String, type: VitalType): Flow<List<VitalReadingEntity>> {
        return vitalReadingDao.getReadingsByType(accountUid, type.name)
    }

    suspend fun insertReading(reading: VitalReadingEntity): Long = withContext(Dispatchers.IO) {
        vitalReadingDao.insertReading(reading)
    }

    suspend fun deleteReading(reading: VitalReadingEntity): Int = withContext(Dispatchers.IO) {
        vitalReadingDao.deleteReading(reading)
    }

    suspend fun deleteAllByAccount(accountUid: String): Int = withContext(Dispatchers.IO) {
        vitalReadingDao.deleteAllByAccount(accountUid)
    }

    suspend fun clearAllReadings(): Int = withContext(Dispatchers.IO) {
        vitalReadingDao.deleteAllReadings()
    }

    suspend fun ensureBaselineReadings(accountUid: String) = withContext(Dispatchers.IO) {
        // Readings are derived strictly from uploaded records and patient-logged vitals
    }
}
