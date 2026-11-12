package com.example.abhayhealth.feature.records

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Room Data Access Object for Medical Record metadata.
 */
@Dao
interface MedicalRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertRecord(record: MedicalRecord): Long

    @Query("SELECT * FROM medical_records WHERE accountUid = :accountUid ORDER BY recordDate DESC")
    fun getRecordsByAccount(accountUid: String): Flow<List<MedicalRecord>>

    @Query("SELECT * FROM medical_records WHERE accountUid = :accountUid AND (title LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%') ORDER BY recordDate DESC")
    fun searchRecords(accountUid: String, query: String): Flow<List<MedicalRecord>>

    @Query("SELECT * FROM medical_records WHERE id = :id AND accountUid = :accountUid")
    fun getRecordById(id: Long, accountUid: String): MedicalRecord?

    @Delete
    fun deleteRecord(record: MedicalRecord): Int

    @Query("DELETE FROM medical_records WHERE id = :id AND accountUid = :accountUid")
    fun deleteRecordById(id: Long, accountUid: String): Int
}
