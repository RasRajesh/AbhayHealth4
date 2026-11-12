package com.example.abhayhealth.feature.symptoms

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Room Data Access Object for persisting and querying symptom records.
 */
@Dao
interface SymptomLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSymptom(symptom: SymptomLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSymptoms(symptoms: List<SymptomLogEntity>): List<Long>

    @Delete
    fun deleteSymptom(symptom: SymptomLogEntity): Int

    @Query("SELECT * FROM symptom_logs WHERE accountUid = :accountUid ORDER BY timestamp DESC")
    fun getAllSymptoms(accountUid: String): Flow<List<SymptomLogEntity>>

    @Query("SELECT * FROM symptom_logs WHERE accountUid = :accountUid AND timestamp >= :sinceTimestamp ORDER BY timestamp DESC")
    fun getSymptomsSince(accountUid: String, sinceTimestamp: Long): Flow<List<SymptomLogEntity>>

    @Query("SELECT * FROM symptom_logs WHERE accountUid = :accountUid AND symptomName = :symptomName ORDER BY timestamp DESC")
    fun getSymptomsByName(accountUid: String, symptomName: String): Flow<List<SymptomLogEntity>>

    @Query("SELECT COUNT(*) FROM symptom_logs WHERE accountUid = :accountUid")
    fun getSymptomCount(accountUid: String): Int

    @Query("SELECT DISTINCT symptomName FROM symptom_logs WHERE accountUid = :accountUid ORDER BY symptomName ASC")
    fun getDistinctSymptoms(accountUid: String): Flow<List<String>>

    @Query("DELETE FROM symptom_logs WHERE id = :id AND accountUid = :accountUid")
    fun deleteSymptomById(id: Long, accountUid: String): Int
}
