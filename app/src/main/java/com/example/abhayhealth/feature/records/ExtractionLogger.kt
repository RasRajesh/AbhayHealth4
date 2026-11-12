package com.example.abhayhealth.feature.records

import android.util.Log

enum class ExtractionStage {
    INITIALIZING,
    VALIDATING,
    FILE_SELECTION,
    URI_ACCESS,
    FILE_VALIDATION,
    READING_PDF,
    RENDERING_PAGE,
    IMAGE_PROCESSING,
    PDF_RENDERING,
    OCR_INITIALIZATION,
    OCR_PROCESSING,
    TEXT_EXTRACTION,
    CLASSIFYING,
    MEDICAL_PARSING,
    PARSING_MEDICINES,
    PARSING_VITALS,
    PARSING_LABS,
    CONSOLIDATING,
    REVIEW_MAPPING,
    DATABASE_SAVE
}

/**
 * Structured, privacy-safe logger for medical document processing pipeline.
 * Ensures diagnostic logging occurs without printing sensitive patient identifiers,
 * phone numbers, raw medical text, or individual health measurements.
 */
object ExtractionLogger {
    private const val TAG = "AbhayHealth_Extraction"

    fun logStage(
        stage: ExtractionStage,
        fileType: String? = null,
        pageNumber: Int? = null,
        success: Boolean,
        diagnosticInfo: String? = null,
        exception: Throwable? = null
    ) {
        val pageStr = if (pageNumber != null) " | Page: $pageNumber" else ""
        val typeStr = if (!fileType.isNullOrBlank()) " | Type: $fileType" else ""
        val diagStr = if (!diagnosticInfo.isNullOrBlank()) " | Info: $diagnosticInfo" else ""
        val statusStr = if (success) "SUCCESS" else "FAILED"
        val exStr = if (exception != null) " | Exception: ${exception.javaClass.simpleName}: ${exception.message}" else ""

        val logMessage = "Stage: [${stage.name}] | Status: $statusStr$typeStr$pageStr$diagStr$exStr"
        if (success) {
            Log.i(TAG, logMessage)
        } else {
            Log.e(TAG, logMessage, exception)
        }
    }
}
