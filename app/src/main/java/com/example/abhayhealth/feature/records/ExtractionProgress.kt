package com.example.abhayhealth.feature.records

/**
 * Continuous progress tracking state model for medical document extraction.
 * Tracks progress stage, current page, total pages, percentage complete,
 * diagnostic message, and page-level success/error counts.
 */
data class ExtractionProgress(
    val currentPage: Int = 0,
    val totalPages: Int = 0,
    val percentage: Int = 0,
    val stage: ExtractionStage = ExtractionStage.FILE_SELECTION,
    val message: String = "Initializing document processing...",
    val pagesCompleted: Int = 0,
    val pagesFailed: Int = 0,
    val isIndeterminate: Boolean = true,
    val failedPagesInfo: List<String> = emptyList()
)
