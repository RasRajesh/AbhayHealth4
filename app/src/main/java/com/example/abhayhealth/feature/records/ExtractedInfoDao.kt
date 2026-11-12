package com.example.abhayhealth.feature.records

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Room Data Access Object for Extracted Medical Information.
 */
@Dao
interface ExtractedInfoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdate(info: ExtractedMedicalInfo): Long

    @Query("SELECT * FROM extracted_medical_info WHERE recordId = :recordId AND accountUid = :accountUid")
    fun getExtractedInfo(recordId: Long, accountUid: String): Flow<ExtractedMedicalInfo?>

    @Query("SELECT * FROM extracted_medical_info WHERE recordId = :recordId AND accountUid = :accountUid")
    fun getExtractedInfoSync(recordId: Long, accountUid: String): ExtractedMedicalInfo?

    @Query("SELECT * FROM extracted_medical_info WHERE accountUid = :accountUid")
    fun getAllExtractedInfoFlow(accountUid: String): Flow<List<ExtractedMedicalInfo>>

    @Query("DELETE FROM extracted_medical_info WHERE recordId = :recordId AND accountUid = :accountUid")
    fun deleteExtractedInfo(recordId: Long, accountUid: String): Int
}
