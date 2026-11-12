package com.example.abhayhealth.feature.records

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentImagePreprocessorTest {

    @Test
    fun testDefaultConstants() {
        assertEquals(1.4f, DocumentImagePreprocessor.DEFAULT_CONTRAST, 0.001f)
        assertEquals(12f, DocumentImagePreprocessor.DEFAULT_BRIGHTNESS_BOOST, 0.001f)
    }

    @Test
    fun testContrastAndBrightnessOffsetCalculations() {
        // Standard case: contrast = 1.4, brightness = 12
        // Offset = 128 * (1 - 1.4) + 12 = 128 * (-0.4) + 12 = -51.2 + 12 = -39.2
        val defaultOffset = DocumentImagePreprocessor.calculateContrastOffset(
            contrast = 1.4f,
            brightnessBoost = 12f
        )
        assertEquals(-39.2f, defaultOffset, 0.01f)

        // Unity contrast without brightness shift should have zero offset
        val unityOffset = DocumentImagePreprocessor.calculateContrastOffset(
            contrast = 1.0f,
            brightnessBoost = 0f
        )
        assertEquals(0f, unityOffset, 0.001f)

        // High contrast test: contrast = 1.5, brightness = 10
        // Offset = 128 * (1 - 1.5) + 10 = -64 + 10 = -54
        val highContrastOffset = DocumentImagePreprocessor.calculateContrastOffset(
            contrast = 1.5f,
            brightnessBoost = 10f
        )
        assertEquals(-54.0f, highContrastOffset, 0.01f)
    }

    @Test
    fun testOffsetIncreasesWithBrightnessBoost() {
        val lowBrightness = DocumentImagePreprocessor.calculateContrastOffset(1.4f, 5f)
        val highBrightness = DocumentImagePreprocessor.calculateContrastOffset(1.4f, 20f)
        assertTrue(highBrightness > lowBrightness)
        assertEquals(15f, highBrightness - lowBrightness, 0.001f)
    }

    @Test
    fun testOffsetDecreasesWithHigherContrast() {
        // Higher contrast darkens mid-tones and requires lower offset
        val contrast1 = DocumentImagePreprocessor.calculateContrastOffset(1.2f, 10f)
        val contrast2 = DocumentImagePreprocessor.calculateContrastOffset(1.6f, 10f)
        assertTrue(contrast1 > contrast2)
    }
}
