package com.example.abhayhealth.feature.symptoms

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeSymptomLogDao : SymptomLogDao {
    private val symptoms = mutableListOf<SymptomLogEntity>()
    private val symptomsFlow = MutableStateFlow<List<SymptomLogEntity>>(emptyList())

    private fun updateFlow() {
        symptomsFlow.value = symptoms.sortedByDescending { it.timestamp }
    }

    override fun insertSymptom(symptom: SymptomLogEntity): Long {
        val entry = if (symptom.id == 0L) symptom.copy(id = (symptoms.size + 1).toLong()) else symptom
        symptoms.add(entry)
        updateFlow()
        return entry.id
    }

    override fun insertSymptoms(symptoms: List<SymptomLogEntity>): List<Long> {
        val ids = mutableListOf<Long>()
        symptoms.forEach {
            ids.add(insertSymptom(it))
        }
        return ids
    }

    override fun deleteSymptom(symptom: SymptomLogEntity): Int {
        val removed = symptoms.removeIf { it.id == symptom.id }
        updateFlow()
        return if (removed) 1 else 0
    }

    override fun getAllSymptoms(accountUid: String): Flow<List<SymptomLogEntity>> {
        return symptomsFlow.map { list -> list.filter { it.accountUid == accountUid } }
    }

    override fun getSymptomsSince(accountUid: String, sinceTimestamp: Long): Flow<List<SymptomLogEntity>> {
        return symptomsFlow.map { list -> list.filter { it.accountUid == accountUid && it.timestamp >= sinceTimestamp } }
    }

    override fun getSymptomsByName(accountUid: String, symptomName: String): Flow<List<SymptomLogEntity>> {
        return symptomsFlow.map { list -> list.filter { it.accountUid == accountUid && it.symptomName.equals(symptomName, ignoreCase = true) } }
    }

    override fun getSymptomCount(accountUid: String): Int {
        return symptoms.count { it.accountUid == accountUid }
    }

    override fun getDistinctSymptoms(accountUid: String): Flow<List<String>> {
        return symptomsFlow.map { list -> list.filter { it.accountUid == accountUid }.map { it.symptomName }.distinct().sorted() }
    }

    override fun deleteSymptomById(id: Long, accountUid: String): Int {
        val removed = symptoms.removeIf { it.id == id && it.accountUid == accountUid }
        updateFlow()
        return if (removed) 1 else 0
    }
}

class SymptomLogRepositoryTest {

    private lateinit var fakeDao: FakeSymptomLogDao
    private lateinit var repository: SymptomLogRepository

    @Before
    fun setUp() {
        fakeDao = FakeSymptomLogDao()
        repository = SymptomLogRepository(fakeDao)
    }

    @Test
    fun testLogSymptomAndRetrieve() = runBlocking {
        val symptom = SymptomLogEntity(
            accountUid = "user_123",
            symptomName = "Migraine Headache",
            severity = SymptomLogEntity.SEVERITY_SEVERE,
            severityScore = 3,
            timestamp = 1000L,
            notes = "Throbbing left temple, light sensitivity",
            triggers = "Stress",
            durationMinutes = 60
        )

        val id = repository.logSymptom(symptom)
        assertTrue(id > 0)

        val retrieved = repository.getAllSymptoms("user_123").first()
        assertEquals(1, retrieved.size)
        assertEquals("Migraine Headache", retrieved[0].symptomName)
        assertEquals(SymptomLogEntity.SEVERITY_SEVERE, retrieved[0].severity)
        assertEquals(3, retrieved[0].severityScore)
        assertEquals("Stress", retrieved[0].triggers)
        assertEquals(60, retrieved[0].durationMinutes)
    }

    @Test
    fun testSeverityScoreHelper() {
        assertEquals(1, SymptomLogEntity.getScoreForSeverity(SymptomLogEntity.SEVERITY_MILD))
        assertEquals(2, SymptomLogEntity.getScoreForSeverity(SymptomLogEntity.SEVERITY_MODERATE))
        assertEquals(3, SymptomLogEntity.getScoreForSeverity(SymptomLogEntity.SEVERITY_SEVERE))
        assertEquals(4, SymptomLogEntity.getScoreForSeverity(SymptomLogEntity.SEVERITY_CRITICAL))
    }

    @Test
    fun testDistinctSymptomsQuery() = runBlocking {
        repository.logSymptom(SymptomLogEntity(accountUid = "user_1", symptomName = "Fever", severity = "MILD", severityScore = 1))
        repository.logSymptom(SymptomLogEntity(accountUid = "user_1", symptomName = "Headache", severity = "MODERATE", severityScore = 2))
        repository.logSymptom(SymptomLogEntity(accountUid = "user_1", symptomName = "Fever", severity = "SEVERE", severityScore = 3))

        val distinct = repository.getDistinctSymptoms("user_1").first()
        assertEquals(listOf("Fever", "Headache"), distinct)
    }

    @Test
    fun testDeleteSymptom() = runBlocking {
        val s = SymptomLogEntity(id = 1L, accountUid = "user_1", symptomName = "Cough", severity = "MILD", severityScore = 1)
        repository.logSymptom(s)

        val countBefore = repository.getSymptomCount("user_1")
        assertEquals(1, countBefore)

        repository.deleteSymptom(s)
        val countAfter = repository.getSymptomCount("user_1")
        assertEquals(0, countAfter)
    }
}
