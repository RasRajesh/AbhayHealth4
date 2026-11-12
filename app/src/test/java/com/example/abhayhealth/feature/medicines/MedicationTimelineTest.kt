package com.example.abhayhealth.feature.medicines

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MedicationTimelineTest {

    @Test
    fun testTimelinePeriodFilterHours() {
        assertEquals(0f, TimelinePeriodFilter.ALL.startHour, 0.01f)
        assertEquals(24f, TimelinePeriodFilter.ALL.endHour, 0.01f)

        assertEquals(6f, TimelinePeriodFilter.MORNING.startHour, 0.01f)
        assertEquals(12f, TimelinePeriodFilter.MORNING.endHour, 0.01f)

        assertEquals(12f, TimelinePeriodFilter.AFTERNOON.startHour, 0.01f)
        assertEquals(17f, TimelinePeriodFilter.AFTERNOON.endHour, 0.01f)

        assertEquals(17f, TimelinePeriodFilter.EVENING.startHour, 0.01f)
        assertEquals(21f, TimelinePeriodFilter.EVENING.endHour, 0.01f)

        assertEquals(21f, TimelinePeriodFilter.NIGHT.startHour, 0.01f)
        assertEquals(24f, TimelinePeriodFilter.NIGHT.endHour, 0.01f)
    }

    @Test
    fun testTimelineDoseItemMapping() {
        val entity = PrescriptionMedicationEntity(
            id = "med_1",
            accountUid = "test_user",
            name = "Metformin",
            dosage = "500 mg",
            timeSlot = "MORNING",
            isTaken = true
        )

        val item = TimelineDoseItem(
            id = entity.id,
            name = entity.name,
            dosage = entity.dosage,
            timeSlot = entity.timeSlot,
            isTaken = entity.isTaken
        )

        assertEquals("med_1", item.id)
        assertEquals("Metformin", item.name)
        assertEquals("500 mg", item.dosage)
        assertEquals("MORNING", item.timeSlot)
        assertTrue(item.isTaken)
    }

    @Test
    fun testPrescriptionMedicationConversion() {
        val model = PrescriptionMedication(
            id = "med_2",
            name = "Atorvastatin",
            dosage = "20 mg",
            timeSlot = "NIGHT",
            isTaken = false
        )

        val item = TimelineDoseItem(
            id = model.id,
            name = model.name,
            dosage = model.dosage,
            timeSlot = model.timeSlot,
            isTaken = model.isTaken
        )

        assertEquals("med_2", item.id)
        assertEquals("Atorvastatin", item.name)
        assertFalse(item.isTaken)
    }
}
