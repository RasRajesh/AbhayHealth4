package com.example.abhayhealth.feature.records

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint

/**
 * Image pre-processing utility for on-device OCR (ML Kit Text Recognition).
 *
 * Enhances low-quality medical document photos (prescriptions, lab reports, discharge summaries)
 * by applying:
 * 1. Grayscale Conversion: Eliminates colored paper casts (e.g. pink, green, blue prescription pads)
 *    and chromatic noise from uneven camera lighting.
 * 2. Contrast Enhancement: Expands dynamic range so faint ink, carbon copy text, and dot-matrix
 *    printouts stand out sharply from the background.
 * 3. Background Brightness Normalization: Lifts shadowed or off-white paper towards clean white while
 *    preserving dark ink strokes.
 */
object DocumentImagePreprocessor {

    const val DEFAULT_CONTRAST = 1.4f
    const val DEFAULT_BRIGHTNESS_BOOST = 12f

    /**
     * Calculates the translation offset required to anchor contrast scaling at midpoint (128)
     * and apply background brightness normalization.
     */
    fun calculateContrastOffset(
        contrast: Float = DEFAULT_CONTRAST,
        brightnessBoost: Float = DEFAULT_BRIGHTNESS_BOOST
    ): Float {
        return (128f * (1f - contrast)) + brightnessBoost
    }

    /**
     * Builds a ColorMatrix combining luminance desaturation with contrast expansion and brightness boost.
     *
     * Transformation formula per RGB channel:
     *   Output = Contrast * (Luminance - 128) + 128 + BrightnessBoost
     *   Offset = 128 * (1 - Contrast) + BrightnessBoost
     */
    fun createOcrColorMatrix(
        contrast: Float = DEFAULT_CONTRAST,
        brightnessBoost: Float = DEFAULT_BRIGHTNESS_BOOST
    ): ColorMatrix {
        val matrix = ColorMatrix()
        // 1. Desaturate to pure luminance grayscale
        matrix.setSaturation(0f)

        // 2. Contrast & brightness offset matrix
        val offset = calculateContrastOffset(contrast, brightnessBoost)
        val contrastMatrix = ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, offset,
                0f, contrast, 0f, 0f, offset,
                0f, 0f, contrast, 0f, offset,
                0f, 0f, 0f, 1f, 0f
            )
        )

        matrix.postConcat(contrastMatrix)
        return matrix
    }

    /**
     * Pre-processes [source] bitmap by applying grayscale conversion and contrast enhancement.
     * Produces a new [Bitmap] configured with [Bitmap.Config.ARGB_8888] for optimal OCR accuracy
     * and full compatibility with ML Kit vision models.
     *
     * The caller is responsible for recycling the returned bitmap when processing completes.
     */
    fun preprocessForOcr(
        source: Bitmap,
        contrast: Float = DEFAULT_CONTRAST,
        brightnessBoost: Float = DEFAULT_BRIGHTNESS_BOOST
    ): Bitmap {
        return try {
            val width = source.width.coerceAtLeast(1)
            val height = source.height.coerceAtLeast(1)

            val preprocessed = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(preprocessed)

            val colorMatrix = createOcrColorMatrix(contrast, brightnessBoost)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                colorFilter = ColorMatrixColorFilter(colorMatrix)
            }

            canvas.drawBitmap(source, 0f, 0f, paint)
            preprocessed
        } catch (_: Throwable) {
            // Graceful fallback to raw source bitmap on memory pressure
            source
        }
    }
}
