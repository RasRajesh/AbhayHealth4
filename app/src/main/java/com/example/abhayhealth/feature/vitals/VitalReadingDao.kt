package com.example.abhayhealth.feature.vitals

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VitalReadingDao {

    @Query("SELECT * FROM vital_readings WHERE accountUid = :accountUid ORDER BY timestamp ASC")
    fun getAllReadingsByAccount(accountUid: String): Flow<List<VitalReadingEntity>>

    @Query("SELECT * FROM vital_readings WHERE accountUid = :accountUid AND type = :type ORDER BY timestamp ASC")
    fun getReadingsByType(accountUid: String, type: String): Flow<List<VitalReadingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertReading(reading: VitalReadingEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertReadings(readings: List<VitalReadingEntity>): List<Long>

    @Delete
    fun deleteReading(reading: VitalReadingEntity): Int

    @Query("DELETE FROM vital_readings WHERE accountUid = :accountUid")
    fun deleteAllByAccount(accountUid: String): Int

    @Query("DELETE FROM vital_readings")
    fun deleteAllReadings(): Int

    @Query("SELECT COUNT(*) FROM vital_readings WHERE accountUid = :accountUid")
    fun getReadingCount(accountUid: String): Int
}
