package com.example.abhayhealth.feature.records

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.example.abhayhealth.core.security.SecurityAudit
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File

sealed interface ExtractionResult {
    data class Success(
        val rawText: String,
        val pagesProcessed: Int,
        val isPartial: Boolean = false
    ) : ExtractionResult

    data class Error(val message: String) : ExtractionResult
}

object DocumentExtractionEngine {

    const val MAX_OCR_PDF_PAGES = 12
    private const val PAGE_OCR_TIMEOUT_MS = 15_000L
    private const val IMAGE_OCR_TIMEOUT_MS = 15_000L
    private const val IMAGE_FALLBACK_OCR_TIMEOUT_MS = 8_000L

    suspend fun extractText(
        record: MedicalRecord,
        onProgress: (ExtractionProgress) -> Unit = {}
    ): ExtractionResult = withContext(Dispatchers.IO) {
        onProgress(
            ExtractionProgress(
                percentage = 5,
                stage = ExtractionStage.VALIDATING,
                message = "Validating document file...",
                isIndeterminate = false
            )
        )

        val path = record.localFilePath
        if (path.isNullOrBlank()) {
            return@withContext ExtractionResult.Error("Local document file path is missing.")
        }

        val file = File(path)
        if (!file.exists() || !file.canRead()) {
            return@withContext ExtractionResult.Error("Document file not found or unreadable on device.")
        }

        val isPdf = record.mimeType?.contains("pdf", ignoreCase = true) == true ||
            path.endsWith(".pdf", ignoreCase = true)

        try {
            if (isPdf) {
                extractFromPdf(file, onProgress)
            } else {
                extractFromImage(file, onProgress)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (t: Throwable) {
            SecurityAudit.logError("DocumentExtractionEngine", "Failed during document extraction", t)
            ExtractionLogger.logStage(
                stage = ExtractionStage.TEXT_EXTRACTION,
                fileType = if (isPdf) "PDF" else "IMAGE",
                success = false,
                exception = t
            )
            ExtractionResult.Error(t.localizedMessage ?: "Failed to extract text from document.")
        }
    }

    private suspend fun extractFromPdf(
        file: File,
        onProgress: (ExtractionProgress) -> Unit
    ): ExtractionResult {
        onProgress(
            ExtractionProgress(
                percentage = 12,
                stage = ExtractionStage.READING_PDF,
                message = "Reading digital PDF text...",
                isIndeterminate = false
            )
        )

        // 1. FAST PATH: Direct digital stream text extraction (Zero-bitmap, < 50ms, zero memory/crash risk)
        var fallbackHeaderText: String? = null
        try {
            val directText = PdfTextExtractor.extractText(file)
            if (!directText.isNullOrBlank()) {
                val hasClinicalFields = directText.length >= 100 && (
                    MedicalFieldParser.parseTestResults(directText).isNotEmpty() ||
                        MedicalFieldParser.parsePrescriptionMedicines(directText, "").isNotEmpty()
                    )
                if (directText.length >= 400 || hasClinicalFields) {
                    val pageCount = getPdfPageCount(file) ?: 1
                    ExtractionLogger.logStage(
                        stage = ExtractionStage.READING_PDF,
                        fileType = "PDF_STREAM",
                        success = true,
                        diagnosticInfo = "Pages: $pageCount, Chars: ${directText.length}"
                    )
                    onProgress(
                        ExtractionProgress(
                            currentPage = pageCount,
                            totalPages = pageCount,
                            percentage = 85,
                            stage = ExtractionStage.TEXT_EXTRACTION,
                            message = "Extracted digital text from $pageCount page(s)",
                            pagesCompleted = pageCount,
                            isIndeterminate = false
                        )
                    )
                    return ExtractionResult.Success(
                        rawText = directText,
                        pagesProcessed = pageCount,
                        isPartial = false
                    )
                } else {
                    fallbackHeaderText = directText
                }
            }
        } catch (t: Throwable) {
            SecurityAudit.logError("DocumentExtractionEngine", "Direct stream extraction failed, falling back to OCR", t)
        }

        // 2. OCR FALLBACK: For scanned image PDFs, render with memory-safe dimensions
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        var recognizer: TextRecognizer? = null

        return try {
            pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            if (pfd == null) {
                return ExtractionResult.Error("Unable to open document file descriptor.")
            }

            renderer = try {
                PdfRenderer(pfd)
            } catch (t: Throwable) {
                SecurityAudit.logError("DocumentExtractionEngine", "PdfRenderer initialization failed", t)
                return ExtractionResult.Error("Cannot render PDF document: ${t.localizedMessage ?: "Invalid or encrypted PDF"}")
            }

            val pageCount = renderer.pageCount
            if (pageCount == 0) {
                return ExtractionResult.Error("PDF document contains no readable pages.")
            }

            val pagesToProcess = minOf(pageCount, MAX_OCR_PDF_PAGES)
            onProgress(
                ExtractionProgress(
                    currentPage = 0,
                    totalPages = pagesToProcess,
                    percentage = 18,
                    stage = ExtractionStage.OCR_INITIALIZATION,
                    message = "Initializing OCR for $pagesToProcess page(s)...",
                    isIndeterminate = false
                )
            )

            recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val fullTextBuilder = StringBuilder()
            var lastError: Throwable? = null
            var pagesCompleted = 0
            var pagesFailed = 0
            val failedPagesInfo = mutableListOf<String>()

            for (i in 0 until pagesToProcess) {
                var page: PdfRenderer.Page? = null
                var renderedBitmap: Bitmap? = null
                var recycleDeferredToTask = false

                val pageNum = i + 1
                val basePercent = 20 + ((i * 65) / pagesToProcess)
                onProgress(
                    ExtractionProgress(
                        currentPage = pageNum,
                        totalPages = pagesToProcess,
                        percentage = basePercent,
                        stage = ExtractionStage.RENDERING_PAGE,
                        message = "Rendering page $pageNum of $pagesToProcess...",
                        pagesCompleted = pagesCompleted,
                        pagesFailed = pagesFailed,
                        isIndeterminate = false,
                        failedPagesInfo = failedPagesInfo.toList()
                    )
                )

                try {
                    page = renderer.openPage(i)
                    val targetMaxDim = 1000
                    val currentMaxDim = maxOf(page.width, page.height)
                    val scale = if (currentMaxDim > 0) {
                        (targetMaxDim.toFloat() / currentMaxDim).coerceIn(0.2f, 1.2f)
                    } else 1.0f
                    val width = (page.width * scale).toInt().coerceIn(100, targetMaxDim)
                    val height = (page.height * scale).toInt().coerceIn(100, targetMaxDim)

                    // Safe allocation capped at 1000px (~4 MB)
                    renderedBitmap = Bitmap.createBitmap(
                        width,
                        height,
                        Bitmap.Config.ARGB_8888
                    )
                    renderedBitmap.eraseColor(android.graphics.Color.WHITE)

                    page.render(
                        renderedBitmap,
                        null,
                        null,
                        PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                    )

                    // Close native page immediately after rendering
                    try {
                        page.close()
                    } catch (_: Exception) {}
                    page = null

                    val ocrPercent = 20 + (((i * 65) + 32) / pagesToProcess)
                    onProgress(
                        ExtractionProgress(
                            currentPage = pageNum,
                            totalPages = pagesToProcess,
                            percentage = ocrPercent.coerceAtMost(85),
                            stage = ExtractionStage.OCR_PROCESSING,
                            message = "Extracting text from page $pageNum of $pagesToProcess...",
                            pagesCompleted = pagesCompleted,
                            pagesFailed = pagesFailed,
                            isIndeterminate = false,
                            failedPagesInfo = failedPagesInfo.toList()
                        )
                    )

                    val inputImage = InputImage.fromBitmap(renderedBitmap, 0)
                    val ocrTask = recognizer.process(inputImage)
                    // Only recycle bitmap once ML Kit task has finished to prevent native SIGSEGV/SIGABRT
                    val bitmapRef = renderedBitmap
                    recycleDeferredToTask = true
                    ocrTask.addOnCompleteListener {
                        try {
                            bitmapRef.recycle()
                        } catch (_: Throwable) {}
                    }

                    val visionText = try {
                        withTimeoutOrNull(PAGE_OCR_TIMEOUT_MS) {
                            ocrTask.await()
                        }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (t: Throwable) {
                        lastError = t
                        SecurityAudit.logError("DocumentExtractionEngine", "OCR process failed for page $pageNum", t)
                        null
                    }

                    if (visionText != null && visionText.text.isNotBlank()) {
                        if (fullTextBuilder.isNotEmpty()) {
                            fullTextBuilder.append("\n--- Page $pageNum ---\n")
                        }
                        fullTextBuilder.append(visionText.text)
                        pagesCompleted++
                    } else {
                        pagesFailed++
                        failedPagesInfo.add("Page $pageNum: No text recognized")
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (t: Throwable) {
                    lastError = t
                    pagesFailed++
                    failedPagesInfo.add("Page $pageNum: ${t.localizedMessage ?: "Render error"}")
                    SecurityAudit.logError("DocumentExtractionEngine", "Skipped rendering PDF page $pageNum due to error", t)
                } finally {
                    try {
                        page?.close()
                    } catch (_: Exception) {}
                    if (!recycleDeferredToTask) {
                        try {
                            renderedBitmap?.recycle()
                        } catch (_: Throwable) {}
                    }
                }

                val donePercent = 20 + ((pageNum * 65) / pagesToProcess)
                onProgress(
                    ExtractionProgress(
                        currentPage = pageNum,
                        totalPages = pagesToProcess,
                        percentage = donePercent.coerceAtMost(85),
                        stage = ExtractionStage.TEXT_EXTRACTION,
                        message = "Completed page $pageNum of $pagesToProcess",
                        pagesCompleted = pagesCompleted,
                        pagesFailed = pagesFailed,
                        isIndeterminate = false,
                        failedPagesInfo = failedPagesInfo.toList()
                    )
                )
            }

            val ocrText = fullTextBuilder.toString().trim()
            val extractedText = when {
                ocrText.isNotBlank() && !fallbackHeaderText.isNullOrBlank() && ocrText.length < fallbackHeaderText.length ->
                    "$fallbackHeaderText\n$ocrText".trim()
                ocrText.isNotBlank() -> ocrText
                !fallbackHeaderText.isNullOrBlank() -> fallbackHeaderText
                else -> ""
            }

            if (extractedText.isBlank()) {
                val errorMsg = lastError?.localizedMessage
                    ?: "No readable medical text could be detected from this document ($pagesToProcess page(s) analyzed)."
                ExtractionResult.Error(errorMsg)
            } else {
                ExtractionResult.Success(
                    rawText = extractedText,
                    pagesProcessed = pagesToProcess,
                    isPartial = pageCount > MAX_OCR_PDF_PAGES
                )
            }
        } finally {
            try {
                renderer?.close()
            } catch (_: Exception) {}

            try {
                pfd?.close()
            } catch (_: Exception) {}

            try {
                recognizer?.close()
            } catch (_: Exception) {}
        }
    }

    private fun getPdfPageCount(file: File): Int? {
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        return try {
            pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY) ?: return null
            renderer = PdfRenderer(pfd)
            renderer.pageCount
        } catch (_: Throwable) {
            null
        } finally {
            try { renderer?.close() } catch (_: Exception) {}
            try { pfd?.close() } catch (_: Exception) {}
        }
    }

    private suspend fun extractFromImage(
        file: File,
        onProgress: (ExtractionProgress) -> Unit
    ): ExtractionResult {
        var recognizer: TextRecognizer? = null
        var rawBitmap: Bitmap? = null
        var preprocessedBitmap: Bitmap? = null
        var rawRecycleDeferred = false
        var prepRecycleDeferred = false

        return try {
            onProgress(
                ExtractionProgress(
                    currentPage = 1,
                    totalPages = 1,
                    percentage = 20,
                    stage = ExtractionStage.IMAGE_PROCESSING,
                    message = "Decoding and optimizing image...",
                    isIndeterminate = false
                )
            )

            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(file.absolutePath, boundsOptions)

            val width = boundsOptions.outWidth
            val height = boundsOptions.outHeight

            if (width <= 0 || height <= 0) {
                return ExtractionResult.Error("Selected image could not be read.")
            }

            var sampleSize = 1
            val maxDimension = 1200

            while (
                width / sampleSize > maxDimension ||
                height / sampleSize > maxDimension
            ) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            rawBitmap = BitmapFactory.decodeFile(
                file.absolutePath,
                decodeOptions
            ) ?: return ExtractionResult.Error("Failed to decode image file.")

            // Read EXIF orientation to ensure camera photos captured in portrait or landscape are not inverted
            val exif = try {
                android.media.ExifInterface(file.absolutePath)
            } catch (_: Throwable) {
                null
            }
            val rotationDegrees = when (exif?.getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION, android.media.ExifInterface.ORIENTATION_NORMAL)) {
                android.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90
                android.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180
                android.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }

            onProgress(
                ExtractionProgress(
                    currentPage = 1,
                    totalPages = 1,
                    percentage = 40,
                    stage = ExtractionStage.OCR_INITIALIZATION,
                    message = "Enhancing contrast for OCR...",
                    isIndeterminate = false
                )
            )

            recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

            // Apply grayscale desaturation and contrast enhancement to improve OCR recognition on low-quality/faint photos
            preprocessedBitmap = try {
                DocumentImagePreprocessor.preprocessForOcr(rawBitmap)
            } catch (_: Throwable) {
                rawBitmap
            }

            onProgress(
                ExtractionProgress(
                    currentPage = 1,
                    totalPages = 1,
                    percentage = 60,
                    stage = ExtractionStage.OCR_PROCESSING,
                    message = "Recognizing medical text from image...",
                    isIndeterminate = false
                )
            )

            val inputImage = InputImage.fromBitmap(preprocessedBitmap, rotationDegrees)
            val prepTask = recognizer.process(inputImage)
            if (preprocessedBitmap !== rawBitmap) {
                val prepRef = preprocessedBitmap
                prepRecycleDeferred = true
                prepTask.addOnCompleteListener {
                    try { prepRef?.recycle() } catch (_: Throwable) {}
                }
            }

            val visionText = try {
                withTimeoutOrNull(IMAGE_OCR_TIMEOUT_MS) {
                    prepTask.await()
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (t: Throwable) {
                SecurityAudit.logError("DocumentExtractionEngine", "Image OCR failed", t)
                null
            }
            var text = visionText?.text?.trim() ?: ""

            // Resilient fallback: If preprocessing produced little or no text (< 15 chars),
            // test the original raw bitmap in case the document had unusual color inversion
            if (text.length < 15 && preprocessedBitmap !== rawBitmap) {
                onProgress(
                    ExtractionProgress(
                        currentPage = 1,
                        totalPages = 1,
                        percentage = 75,
                        stage = ExtractionStage.OCR_PROCESSING,
                        message = "Retrying OCR with original image colors...",
                        isIndeterminate = false
                    )
                )
                try {
                    val rawInput = InputImage.fromBitmap(rawBitmap, rotationDegrees)
                    val rawTask = recognizer.process(rawInput)
                    val rawRef = rawBitmap
                    rawRecycleDeferred = true
                    rawTask.addOnCompleteListener {
                        try { rawRef?.recycle() } catch (_: Throwable) {}
                    }
                    val rawVision = withTimeoutOrNull(IMAGE_FALLBACK_OCR_TIMEOUT_MS) {
                        rawTask.await()
                    }
                    val fallbackText = rawVision?.text?.trim() ?: ""
                    if (fallbackText.length > text.length) {
                        text = fallbackText
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (t: Throwable) {
                    SecurityAudit.logError("DocumentExtractionEngine", "Raw fallback OCR attempt skipped", t)
                }
            }

            onProgress(
                ExtractionProgress(
                    currentPage = 1,
                    totalPages = 1,
                    percentage = 85,
                    stage = ExtractionStage.TEXT_EXTRACTION,
                    message = "Image text recognition complete",
                    pagesCompleted = if (text.isNotBlank()) 1 else 0,
                    pagesFailed = if (text.isBlank()) 1 else 0,
                    isIndeterminate = false
                )
            )

            if (text.isBlank()) {
                ExtractionResult.Error("No readable medical text recognized in this document image.")
            } else {
                ExtractionResult.Success(
                    rawText = text,
                    pagesProcessed = 1,
                    isPartial = false
                )
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (t: Throwable) {
            SecurityAudit.logError("DocumentExtractionEngine", "Image extraction crashed", t)
            ExtractionResult.Error("Image processing failed: ${t.localizedMessage ?: "Unknown error"}")
        } finally {
            if (!prepRecycleDeferred && preprocessedBitmap !== rawBitmap) {
                try { preprocessedBitmap?.recycle() } catch (_: Throwable) {}
            }
            if (!rawRecycleDeferred) {
                try { rawBitmap?.recycle() } catch (_: Throwable) {}
            }
            try {
                recognizer?.close()
            } catch (_: Exception) {}
        }
    }
}
