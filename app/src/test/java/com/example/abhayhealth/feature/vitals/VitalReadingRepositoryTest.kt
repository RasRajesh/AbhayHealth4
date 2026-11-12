package com.example.abhayhealth.feature.vitals

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeVitalReadingDao : VitalReadingDao {
    private val readings = mutableListOf<VitalReadingEntity>()
    private val readingsFlow = MutableStateFlow<List<VitalReadingEntity>>(emptyList())

    private fun updateFlow() {
        readingsFlow.value = readings.toList()
    }

    override fun getAllReadingsByAccount(accountUid: String): Flow<List<VitalReadingEntity>> {
        return readingsFlow.map { list -> list.filter { it.accountUid == accountUid } }
    }

    override fun getReadingsByType(accountUid: String, type: String): Flow<List<VitalReadingEntity>> {
        return readingsFlow.map { list -> list.filter { it.accountUid == accountUid && it.type == type } }
    }

    override fun insertReading(reading: VitalReadingEntity): Long {
        readings.add(reading)
        updateFlow()
        return reading.id
    }

    override fun insertReadings(readings: List<VitalReadingEntity>): List<Long> {
        this.readings.addAll(readings)
        updateFlow()
        return readings.map { it.id }
    }

    override fun deleteReading(reading: VitalReadingEntity): Int {
        val removed = readings.remove(reading)
        updateFlow()
        return if (removed) 1 else 0
    }

    override fun deleteAllByAccount(accountUid: String): Int {
        val countBefore = readings.size
        readings.removeAll { it.accountUid == accountUid }
        updateFlow()
        return countBefore - readings.size
    }

    override fun deleteAllReadings(): Int {
        val count = readings.size
        readings.clear()
        updateFlow()
        return count
    }

    override fun getReadingCount(accountUid: String): Int {
        return readings.count { it.accountUid == accountUid }
    }
}

class VitalReadingRepositoryTest {

    private lateinit var fakeDao: FakeVitalReadingDao
    private lateinit var repository: VitalReadingRepository

    @Before
    fun setUp() {
        fakeDao = FakeVitalReadingDao()
        repository = VitalReadingRepository(fakeDao)
    }

    @Test
    fun testEnsureBaselineReadings_doesNotSeedMockData() = runBlocking {
        assertEquals(0, fakeDao.getReadingCount("user_123"))

        repository.ensureBaselineReadings("user_123")

        val count = fakeDao.getReadingCount("user_123")
        assertEquals("Readings must come strictly from uploaded documents or user logs", 0, count)
    }

    @Test
    fun testInsertReading_addsNewReadingToAccount() = runBlocking {
        val newReading = VitalReadingEntity(
            id = 101L,
            accountUid = "user_456",
            type = VitalType.BLOOD_PRESSURE.name,
            systolic = 120.0,
            diastolic = 80.0,
            timestamp = System.currentTimeMillis(),
            notes = "After morning walk"
        )

        repository.insertReading(newReading)

        val readings = repository.getReadingsByType("user_456", VitalType.BLOOD_PRESSURE).first()
        assertEquals(1, readings.size)
        assertEquals(120.0, readings[0].systolic ?: 0.0, 0.01)
        assertEquals(80.0, readings[0].diastolic ?: 0.0, 0.01)
        assertEquals("After morning walk", readings[0].notes)
    }

    @Test
    fun testDeleteAllByAccount_clearsAllReadingsForTargetAccount() = runBlocking {
        val reading1 = VitalReadingEntity(
            id = 1L,
            accountUid = "user_del",
            type = VitalType.WEIGHT.name,
            weightKg = 70.0,
            timestamp = System.currentTimeMillis()
        )
        val reading2 = VitalReadingEntity(
            id = 2L,
            accountUid = "user_keep",
            type = VitalType.WEIGHT.name,
            weightKg = 80.0,
            timestamp = System.currentTimeMillis()
        )

        repository.insertReading(reading1)
        repository.insertReading(reading2)
        assertEquals(2, fakeDao.getReadingCount("user_del") + fakeDao.getReadingCount("user_keep"))

        repository.deleteAllByAccount("user_del")

        assertEquals(0, fakeDao.getReadingCount("user_del"))
        assertEquals(1, fakeDao.getReadingCount("user_keep"))
    }
}
