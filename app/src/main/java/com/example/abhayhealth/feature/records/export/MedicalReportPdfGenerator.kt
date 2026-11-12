package com.example.abhayhealth.feature.records.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.abhayhealth.domain.model.ParsedMedicalResult
import com.example.abhayhealth.domain.model.PatientProfile
import com.example.abhayhealth.feature.medicines.PrescriptionMedicationEntity
import com.example.abhayhealth.feature.records.ExtractedMedicalInfo
import com.example.abhayhealth.feature.records.MedicalFieldParser
import com.example.abhayhealth.feature.records.MedicalRecord
import com.example.abhayhealth.feature.symptoms.SymptomLogEntity
import com.example.abhayhealth.feature.vitals.VitalReadingEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * High-fidelity PDF Report Generator for exporting comprehensive medical history,
 * active medication schedules, recent vitals, and diagnostic findings from the Room database.
 * Formatted specifically for clinical review by healthcare providers.
 */
class MedicalReportPdfGenerator(private val context: Context) {

    data class ReportData(
        val patientProfile: PatientProfile?,
        val medications: List<PrescriptionMedicationEntity>,
        val records: List<MedicalRecord>,
        val extractedInfoMap: Map<Long, ExtractedMedicalInfo>,
        val vitals: List<VitalReadingEntity>,
        val symptoms: List<SymptomLogEntity> = emptyList(),
        val generatedAt: Long = System.currentTimeMillis()
    )

    companion object {
        private const val PAGE_WIDTH = 595 // Standard A4 points (72 dpi)
        private const val PAGE_HEIGHT = 842
        private const val MARGIN_LEFT = 36f
        private const val MARGIN_RIGHT = 559f
        private const val MARGIN_TOP = 36f
        private const val MARGIN_BOTTOM = 800f
        private const val CONTENT_WIDTH = MARGIN_RIGHT - MARGIN_LEFT

        // Brand Palette
        private const val COLOR_PRIMARY = 0xFF0D5C75.toInt() // Deep Teal
        private const val COLOR_PRIMARY_DARK = 0xFF063342.toInt()
        private const val COLOR_SECONDARY = 0xFF14B8A6.toInt() // Emerald Teal
        private const val COLOR_BG_LIGHT = 0xFFF8FAFC.toInt() // Slate 50
        private const val COLOR_CARD_BORDER = 0xFFE2E8F0.toInt() // Slate 200
        private const val COLOR_TEXT_PRIMARY = 0xFF0F172A.toInt() // Slate 900
        private const val COLOR_TEXT_SECONDARY = 0xFF475569.toInt() // Slate 600
        private const val COLOR_TEXT_MUTED = 0xFF64748B.toInt() // Slate 500
        private const val COLOR_ACCENT_RED = 0xFFDC2626.toInt() // Red 600
        private const val COLOR_ACCENT_GREEN = 0xFF16A34A.toInt() // Green 600
        private const val COLOR_ROW_ALT = 0xFFF1F5F9.toInt() // Slate 100
    }

    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 15f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E0F2FE")
        textSize = 8.5f
    }

    private val sectionHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = COLOR_PRIMARY
        textSize = 11f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val bodyBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = COLOR_TEXT_PRIMARY
        textSize = 9f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val bodyRegularPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = COLOR_TEXT_PRIMARY
        textSize = 8.5f
    }

    private val bodySecondaryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = COLOR_TEXT_SECONDARY
        textSize = 8f
    }

    private val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = COLOR_TEXT_MUTED
        textSize = 7.5f
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = COLOR_CARD_BORDER
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    fun generatePdfReport(data: ReportData, destinationFile: File): File {
        val document = PdfDocument()
        var pageNumber = 1
        var currentPageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        var currentPage = document.startPage(currentPageInfo)
        var canvas = currentPage.canvas

        val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val shortDateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val generatedDateStr = dateFormat.format(Date(data.generatedAt))

        // Draw First Page Header
        var yCursor = drawPageHeader(canvas, generatedDateStr, isFirstPage = true)

        // Helper to handle multi-page overflow
        fun ensureVerticalSpace(neededHeight: Float) {
            if (yCursor + neededHeight > MARGIN_BOTTOM) {
                // Finish current page
                drawPageFooter(canvas, pageNumber)
                document.finishPage(currentPage)

                // Start new page
                pageNumber++
                currentPageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                currentPage = document.startPage(currentPageInfo)
                canvas = currentPage.canvas

                // Running header for continuation pages
                yCursor = drawPageHeader(canvas, generatedDateStr, isFirstPage = false)
            }
        }

        // ==========================================
        // 1. PATIENT DEMOGRAPHICS & CLINICAL IDENTITY
        // ==========================================
        ensureVerticalSpace(115f)
        yCursor = drawPatientDemographicsCard(canvas, yCursor, data.patientProfile)

        // ==========================================
        // 2. ACTIVE PRESCRIPTION MEDICATION SCHEDULE
        // ==========================================
        ensureVerticalSpace(50f)
        yCursor = drawSectionTitle(canvas, yCursor, "ACTIVE PRESCRIPTION MEDICATIONS & SCHEDULE", data.medications.size)

        if (data.medications.isEmpty()) {
            ensureVerticalSpace(30f)
            yCursor = drawEmptyStateNotice(canvas, yCursor, "No active prescription medications recorded in local database.")
        } else {
            ensureVerticalSpace(24f)
            yCursor = drawMedicationTableHeader(canvas, yCursor)

            data.medications.forEachIndexed { index, med ->
                ensureVerticalSpace(26f)
                yCursor = drawMedicationTableRow(canvas, yCursor, med, index % 2 == 1)
            }
        }

        yCursor += 12f

        // ==========================================
        // 3. RECENT VITALS & BIOMETRIC LOGS
        // ==========================================
        ensureVerticalSpace(50f)
        yCursor = drawSectionTitle(canvas, yCursor, "RECENT VITALS & BIOMETRIC TRENDS", data.vitals.size)

        if (data.vitals.isEmpty()) {
            ensureVerticalSpace(30f)
            yCursor = drawEmptyStateNotice(canvas, yCursor, "No vital readings recorded yet in local Room database.")
        } else {
            ensureVerticalSpace(24f)
            yCursor = drawVitalsTableHeader(canvas, yCursor)

            // Show latest 8 vital readings
            val sortedVitals = data.vitals.sortedByDescending { it.timestamp }.take(8)
            sortedVitals.forEachIndexed { index, vital ->
                ensureVerticalSpace(24f)
                yCursor = drawVitalTableRow(canvas, yCursor, vital, shortDateFormat, index % 2 == 1)
            }
        }

        yCursor += 12f

        // ==========================================
        // 4. DIAGNOSTIC ARCHIVE & MEDICAL HISTORY
        // ==========================================
        ensureVerticalSpace(50f)
        yCursor = drawSectionTitle(canvas, yCursor, "DIAGNOSTIC ARCHIVE & CLINICAL RECORDS", data.records.size)

        if (data.records.isEmpty()) {
            ensureVerticalSpace(30f)
            yCursor = drawEmptyStateNotice(canvas, yCursor, "No diagnostic records or lab reports uploaded yet.")
        } else {
            val sortedRecords = data.records.sortedByDescending { it.recordDate }
            sortedRecords.forEach { record ->
                val info = data.extractedInfoMap[record.id]
                val structuredResults = info?.let {
                    MedicalFieldParser.deserializeResults(it.structuredResultsJson)
                } ?: emptyList()

                val cardHeight = 36f + (if (structuredResults.isNotEmpty()) (structuredResults.size * 18f) + 24f else 22f)
                ensureVerticalSpace(cardHeight)
                yCursor = drawMedicalRecordCard(canvas, yCursor, record, info, structuredResults, shortDateFormat)
                yCursor += 8f
            }
        }

        // ==========================================
        // 5. CLINICIAN ATTESTATION & SIGNATURE AREA
        // ==========================================
        ensureVerticalSpace(110f)
        yCursor = drawClinicianSignatureArea(canvas, yCursor)

        // Draw footer on the final page
        drawPageFooter(canvas, pageNumber)
        document.finishPage(currentPage)

        // Write to destination file
        destinationFile.parentFile?.mkdirs()
        FileOutputStream(destinationFile).use { outputStream ->
            document.writeTo(outputStream)
        }
        document.close()

        return destinationFile
    }

    private fun drawPageHeader(canvas: Canvas, generatedDateStr: String, isFirstPage: Boolean): Float {
        val headerHeight = if (isFirstPage) 72f else 38f
        val rect = RectF(MARGIN_LEFT, MARGIN_TOP, MARGIN_RIGHT, MARGIN_TOP + headerHeight)

        // Header Background with rounded corners
        fillPaint.color = COLOR_PRIMARY
        canvas.drawRoundRect(rect, 8f, 8f, fillPaint)

        if (isFirstPage) {
            // Draw Medical Cross Icon inside white emblem
            val emblemCx = MARGIN_LEFT + 28f
            val emblemCy = MARGIN_TOP + 36f
            fillPaint.color = Color.WHITE
            canvas.drawCircle(emblemCx, emblemCy, 18f, fillPaint)

            // Red/Teal medical cross
            fillPaint.color = COLOR_PRIMARY
            canvas.drawRect(emblemCx - 3.5f, emblemCy - 11f, emblemCx + 3.5f, emblemCy + 11f, fillPaint)
            canvas.drawRect(emblemCx - 11f, emblemCy - 3.5f, emblemCx + 11f, emblemCy + 3.5f, fillPaint)

            // Brand Titles
            canvas.drawText("ABHAY HEALTH", MARGIN_LEFT + 56f, MARGIN_TOP + 30f, titlePaint)
            canvas.drawText("Comprehensive Patient Medical & Medication Summary Report", MARGIN_LEFT + 56f, MARGIN_TOP + 46f, subtitlePaint)
            canvas.drawText("Verified Local Health Record • Doctor Clinical Presentation", MARGIN_LEFT + 56f, MARGIN_TOP + 58f, subtitlePaint)

            // Generated date & confidentiality badge on right side
            val badgeRect = RectF(MARGIN_RIGHT - 165f, MARGIN_TOP + 12f, MARGIN_RIGHT - 12f, MARGIN_TOP + 32f)
            fillPaint.color = Color.parseColor("#1E293B")
            canvas.drawRoundRect(badgeRect, 4f, 4f, fillPaint)

            val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#FDE047") // Yellow gold
                textSize = 7.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText("CONFIDENTIAL MEDICAL SUMMARY", MARGIN_RIGHT - 160f, MARGIN_TOP + 25f, badgeTextPaint)

            val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#E2E8F0")
                textSize = 7.5f
            }
            canvas.drawText("Generated: $generatedDateStr", MARGIN_RIGHT - 165f, MARGIN_TOP + 54f, datePaint)

            return MARGIN_TOP + headerHeight + 14f
        } else {
            // Concise continuation header
            canvas.drawText("ABHAY HEALTH — PATIENT MEDICAL SUMMARY (Continued)", MARGIN_LEFT + 14f, MARGIN_TOP + 24f, titlePaint.apply { textSize = 10f })
            val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 7.5f
            }
            canvas.drawText(generatedDateStr, MARGIN_RIGHT - 110f, MARGIN_TOP + 24f, datePaint)

            return MARGIN_TOP + headerHeight + 12f
        }
    }

    private fun drawPatientDemographicsCard(canvas: Canvas, yStart: Float, profile: PatientProfile?): Float {
        val cardHeight = 84f
        val cardRect = RectF(MARGIN_LEFT, yStart, MARGIN_RIGHT, yStart + cardHeight)

        fillPaint.color = COLOR_BG_LIGHT
        canvas.drawRoundRect(cardRect, 8f, 8f, fillPaint)

        borderPaint.color = COLOR_CARD_BORDER
        canvas.drawRoundRect(cardRect, 8f, 8f, borderPaint)

        // Accent left edge
        fillPaint.color = COLOR_SECONDARY
        canvas.drawRoundRect(RectF(MARGIN_LEFT, yStart, MARGIN_LEFT + 4f, yStart + cardHeight), 4f, 4f, fillPaint)

        val col1X = MARGIN_LEFT + 16f
        val col2X = MARGIN_LEFT + 180f
        val col3X = MARGIN_LEFT + 340f

        val patientName = profile?.fullName?.ifBlank { "Patient / Not Provided" } ?: "Patient / Not Provided"
        val dob = profile?.dateOfBirth?.ifBlank { "Not documented" } ?: "Not documented"
        val gender = profile?.gender?.ifBlank { "Not specified" } ?: "Not specified"
        val bloodGroup = profile?.bloodGroup?.ifBlank { "Unknown" } ?: "Unknown"
        val allergies = profile?.allergies?.ifBlank { "None documented" } ?: "None documented"
        val conditions = profile?.healthConditions?.ifBlank { "None reported" } ?: "None reported"
        val emergency = if (!profile?.emergencyContactName.isNullOrBlank()) {
            "${profile?.emergencyContactName} (${profile?.emergencyContactPhone ?: "N/A"})"
        } else {
            "Not provided"
        }

        // Row 1
        canvas.drawText("PATIENT NAME", col1X, yStart + 18f, bodySecondaryPaint)
        canvas.drawText(patientName, col1X, yStart + 31f, bodyBoldPaint.apply { textSize = 10.5f })

        canvas.drawText("DATE OF BIRTH / GENDER", col2X, yStart + 18f, bodySecondaryPaint)
        canvas.drawText("$dob • $gender", col2X, yStart + 31f, bodyBoldPaint.apply { textSize = 9f })

        canvas.drawText("BLOOD GROUP", col3X, yStart + 18f, bodySecondaryPaint)
        // Blood Group Pill Badge
        val bgRect = RectF(col3X, yStart + 21f, col3X + 55f, yStart + 35f)
        fillPaint.color = if (bloodGroup != "Unknown") Color.parseColor("#FEE2E2") else Color.parseColor("#E2E8F0")
        canvas.drawRoundRect(bgRect, 4f, 4f, fillPaint)

        val bgTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (bloodGroup != "Unknown") COLOR_ACCENT_RED else COLOR_TEXT_MUTED
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(bloodGroup, col3X + 8f, yStart + 32f, bgTextPaint)

        // Divider
        canvas.drawLine(col1X, yStart + 42f, MARGIN_RIGHT - 16f, yStart + 42f, borderPaint)

        // Row 2
        canvas.drawText("ALLERGIES", col1X, yStart + 56f, bodySecondaryPaint)
        val allergyPaint = if (allergies != "None documented") {
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_ACCENT_RED
                textSize = 8.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
        } else bodyRegularPaint
        canvas.drawText(allergies.take(30), col1X, yStart + 69f, allergyPaint)

        canvas.drawText("CHRONIC CONDITIONS", col2X, yStart + 56f, bodySecondaryPaint)
        canvas.drawText(conditions.take(32), col2X, yStart + 69f, bodyRegularPaint)

        canvas.drawText("EMERGENCY CONTACT", col3X, yStart + 56f, bodySecondaryPaint)
        canvas.drawText(emergency.take(30), col3X, yStart + 69f, bodyRegularPaint)

        return yStart + cardHeight + 14f
    }

    private fun drawSectionTitle(canvas: Canvas, yStart: Float, title: String, count: Int): Float {
        fillPaint.color = COLOR_PRIMARY
        canvas.drawRect(MARGIN_LEFT, yStart + 2f, MARGIN_LEFT + 3f, yStart + 14f, fillPaint)

        canvas.drawText(title, MARGIN_LEFT + 8f, yStart + 12f, sectionHeaderPaint)

        // Count Badge
        val countText = "$count Recorded"
        val countPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_MUTED
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(countText, MARGIN_RIGHT - countPaint.measureText(countText), yStart + 12f, countPaint)

        canvas.drawLine(MARGIN_LEFT, yStart + 18f, MARGIN_RIGHT, yStart + 18f, borderPaint)
        return yStart + 24f
    }

    private fun drawEmptyStateNotice(canvas: Canvas, yStart: Float, message: String): Float {
        val rect = RectF(MARGIN_LEFT, yStart, MARGIN_RIGHT, yStart + 24f)
        fillPaint.color = COLOR_BG_LIGHT
        canvas.drawRoundRect(rect, 4f, 4f, fillPaint)
        canvas.drawRoundRect(rect, 4f, 4f, borderPaint)

        canvas.drawText("ℹ  $message", MARGIN_LEFT + 12f, yStart + 15f, bodySecondaryPaint)
        return yStart + 30f
    }

    private fun drawMedicationTableHeader(canvas: Canvas, yStart: Float): Float {
        val rect = RectF(MARGIN_LEFT, yStart, MARGIN_RIGHT, yStart + 18f)
        fillPaint.color = Color.parseColor("#E2E8F0")
        canvas.drawRoundRect(rect, 4f, 4f, fillPaint)

        val headerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_PRIMARY
            textSize = 7.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        canvas.drawText("MEDICINE & FORM", MARGIN_LEFT + 8f, yStart + 12f, headerTextPaint)
        canvas.drawText("DOSAGE / FREQUENCY", MARGIN_LEFT + 180f, yStart + 12f, headerTextPaint)
        canvas.drawText("TIMING / SLOT", MARGIN_LEFT + 330f, yStart + 12f, headerTextPaint)
        canvas.drawText("DAILY STATUS", MARGIN_LEFT + 440f, yStart + 12f, headerTextPaint)

        return yStart + 22f
    }

    private fun drawMedicationTableRow(
        canvas: Canvas,
        yStart: Float,
        med: PrescriptionMedicationEntity,
        isAlt: Boolean
    ): Float {
        val rowHeight = 22f
        val rect = RectF(MARGIN_LEFT, yStart, MARGIN_RIGHT, yStart + rowHeight)

        fillPaint.color = if (isAlt) COLOR_ROW_ALT else Color.WHITE
        canvas.drawRect(rect, fillPaint)
        canvas.drawLine(MARGIN_LEFT, yStart + rowHeight, MARGIN_RIGHT, yStart + rowHeight, borderPaint)

        // Medicine Name
        canvas.drawText(med.name.take(30), MARGIN_LEFT + 8f, yStart + 14f, bodyBoldPaint)

        // Dosage
        val dosage = med.dosage.ifBlank { "As prescribed" }
        canvas.drawText(dosage.take(28), MARGIN_LEFT + 180f, yStart + 14f, bodyRegularPaint)

        // Time slot
        canvas.drawText(med.timeSlot, MARGIN_LEFT + 330f, yStart + 14f, bodySecondaryPaint)

        // Intake Status pill
        val statusText = if (med.isTaken) "Taken Today" else "Scheduled"
        val statusBg = if (med.isTaken) Color.parseColor("#DCFCE7") else Color.parseColor("#FEF3C7")
        val statusFg = if (med.isTaken) COLOR_ACCENT_GREEN else Color.parseColor("#D97706")

        val pillRect = RectF(MARGIN_LEFT + 440f, yStart + 3f, MARGIN_LEFT + 515f, yStart + 17f)
        fillPaint.color = statusBg
        canvas.drawRoundRect(pillRect, 4f, 4f, fillPaint)

        val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = statusFg
            textSize = 7f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(statusText, MARGIN_LEFT + 446f, yStart + 13f, pillPaint)

        return yStart + rowHeight
    }

    private fun drawVitalsTableHeader(canvas: Canvas, yStart: Float): Float {
        val rect = RectF(MARGIN_LEFT, yStart, MARGIN_RIGHT, yStart + 18f)
        fillPaint.color = Color.parseColor("#E2E8F0")
        canvas.drawRoundRect(rect, 4f, 4f, fillPaint)

        val headerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_PRIMARY
            textSize = 7.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        canvas.drawText("RECORDED DATE", MARGIN_LEFT + 8f, yStart + 12f, headerTextPaint)
        canvas.drawText("METRIC TYPE", MARGIN_LEFT + 130f, yStart + 12f, headerTextPaint)
        canvas.drawText("MEASUREMENT VALUE", MARGIN_LEFT + 250f, yStart + 12f, headerTextPaint)
        canvas.drawText("CLINICAL EVALUATION / NOTES", MARGIN_LEFT + 380f, yStart + 12f, headerTextPaint)

        return yStart + 22f
    }

    private fun drawVitalTableRow(
        canvas: Canvas,
        yStart: Float,
        vital: VitalReadingEntity,
        dateFormat: SimpleDateFormat,
        isAlt: Boolean
    ): Float {
        val rowHeight = 22f
        val rect = RectF(MARGIN_LEFT, yStart, MARGIN_RIGHT, yStart + rowHeight)

        fillPaint.color = if (isAlt) COLOR_ROW_ALT else Color.WHITE
        canvas.drawRect(rect, fillPaint)
        canvas.drawLine(MARGIN_LEFT, yStart + rowHeight, MARGIN_RIGHT, yStart + rowHeight, borderPaint)

        val dateStr = dateFormat.format(Date(vital.timestamp))
        canvas.drawText(dateStr, MARGIN_LEFT + 8f, yStart + 14f, bodySecondaryPaint)

        val typeStr = if (vital.type.equals("BP", ignoreCase = true)) "Blood Pressure" else "Body Weight"
        canvas.drawText(typeStr, MARGIN_LEFT + 130f, yStart + 14f, bodyBoldPaint)

        val valueStr = if (vital.type.equals("BP", ignoreCase = true)) {
            val sys = vital.systolic?.toInt() ?: 0
            val dia = vital.diastolic?.toInt() ?: 0
            "$sys / $dia mmHg"
        } else {
            "${String.format(Locale.US, "%.1f", vital.weightKg ?: 0.0)} kg"
        }
        canvas.drawText(valueStr, MARGIN_LEFT + 250f, yStart + 14f, bodyBoldPaint)

        val evaluation = if (vital.type.equals("BP", ignoreCase = true)) {
            val sys = vital.systolic ?: 120.0
            val dia = vital.diastolic ?: 80.0
            when {
                sys < 120 && dia < 80 -> "Optimal (<120/<80)"
                sys in 120.0..129.9 && dia < 80 -> "Elevated (120-129/<80)"
                else -> "Hypertension Stage (≥130/≥80)"
            }
        } else {
            vital.notes.ifBlank { "Stable biometric tracking" }
        }
        canvas.drawText(evaluation.take(30), MARGIN_LEFT + 380f, yStart + 14f, bodySecondaryPaint)

        return yStart + rowHeight
    }

    private fun drawMedicalRecordCard(
        canvas: Canvas,
        yStart: Float,
        record: MedicalRecord,
        info: ExtractedMedicalInfo?,
        structuredResults: List<ParsedMedicalResult>,
        dateFormat: SimpleDateFormat
    ): Float {
        val resultsHeight = if (structuredResults.isNotEmpty()) (structuredResults.size * 18f) + 20f else 22f
        val cardHeight = 32f + resultsHeight
        val rect = RectF(MARGIN_LEFT, yStart, MARGIN_RIGHT, yStart + cardHeight)

        fillPaint.color = Color.WHITE
        canvas.drawRoundRect(rect, 6f, 6f, fillPaint)
        canvas.drawRoundRect(rect, 6f, 6f, borderPaint)

        // Sub-header band
        val headerRect = RectF(MARGIN_LEFT, yStart, MARGIN_RIGHT, yStart + 26f)
        fillPaint.color = COLOR_BG_LIGHT
        canvas.drawRoundRect(headerRect, 6f, 6f, fillPaint)
        canvas.drawLine(MARGIN_LEFT, yStart + 26f, MARGIN_RIGHT, yStart + 26f, borderPaint)

        // Category Tag
        val categoryRect = RectF(MARGIN_LEFT + 8f, yStart + 5f, MARGIN_LEFT + 75f, yStart + 21f)
        fillPaint.color = Color.parseColor("#E0F2FE")
        canvas.drawRoundRect(categoryRect, 3f, 3f, fillPaint)

        val tagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_PRIMARY
            textSize = 7f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(record.category.uppercase(), MARGIN_LEFT + 12f, yStart + 16f, tagPaint)

        // Document Title
        canvas.drawText(record.title.take(45), MARGIN_LEFT + 85f, yStart + 17f, bodyBoldPaint)

        // Document Date
        val docDate = dateFormat.format(Date(record.recordDate))
        canvas.drawText("Date: $docDate", MARGIN_RIGHT - 110f, yStart + 17f, bodySecondaryPaint)

        var yPos = yStart + 30f

        if (structuredResults.isNotEmpty()) {
            // Test results table sub-header
            val subTableH = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_TEXT_MUTED
                textSize = 7f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText("TEST PARAMETER", MARGIN_LEFT + 16f, yPos + 10f, subTableH)
            canvas.drawText("RESULT", MARGIN_LEFT + 220f, yPos + 10f, subTableH)
            canvas.drawText("UNIT", MARGIN_LEFT + 320f, yPos + 10f, subTableH)
            canvas.drawText("REFERENCE INTERVAL", MARGIN_LEFT + 400f, yPos + 10f, subTableH)
            yPos += 14f

            structuredResults.forEach { test ->
                canvas.drawText(test.testName.take(35), MARGIN_LEFT + 16f, yPos + 10f, bodyRegularPaint)
                canvas.drawText(test.value, MARGIN_LEFT + 220f, yPos + 10f, bodyBoldPaint)
                canvas.drawText(test.unit.ifBlank { "-" }, MARGIN_LEFT + 320f, yPos + 10f, bodySecondaryPaint)
                canvas.drawText(test.referenceRange.ifBlank { "Standard" }, MARGIN_LEFT + 400f, yPos + 10f, bodySecondaryPaint)
                yPos += 18f
            }
        } else {
            val summaryText = if (!info?.rawText.isNull_or_blank()) {
                "Extracted text summary: " + info!!.rawText.take(80).replace("\n", " ") + "…"
            } else {
                "Verified archive file stored securely on patient's device."
            }
            canvas.drawText(summaryText, MARGIN_LEFT + 16f, yPos + 14f, bodySecondaryPaint)
            yPos += 22f
        }

        return yStart + cardHeight
    }

    private fun drawClinicianSignatureArea(canvas: Canvas, yStart: Float): Float {
        val boxHeight = 88f
        val rect = RectF(MARGIN_LEFT, yStart, MARGIN_RIGHT, yStart + boxHeight)

        fillPaint.color = COLOR_BG_LIGHT
        canvas.drawRoundRect(rect, 6f, 6f, fillPaint)
        canvas.drawRoundRect(rect, 6f, 6f, borderPaint)

        canvas.drawText("HEALTHCARE PROVIDER / CLINICAL NOTES & ATTESTATION", MARGIN_LEFT + 12f, yStart + 16f, sectionHeaderPaint.apply { textSize = 8.5f })

        // Ruled lines for doctor's clinical review notes
        val rulePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CBD5E1")
            strokeWidth = 0.8f
        }
        canvas.drawLine(MARGIN_LEFT + 12f, yStart + 32f, MARGIN_RIGHT - 180f, yStart + 32f, rulePaint)
        canvas.drawLine(MARGIN_LEFT + 12f, yStart + 48f, MARGIN_RIGHT - 180f, yStart + 48f, rulePaint)
        canvas.drawLine(MARGIN_LEFT + 12f, yStart + 64f, MARGIN_RIGHT - 180f, yStart + 64f, rulePaint)

        // Doctor Signature line on the right
        val signLineX = MARGIN_RIGHT - 160f
        canvas.drawLine(signLineX, yStart + 60f, MARGIN_RIGHT - 16f, yStart + 60f, rulePaint)
        canvas.drawText("Physician Signature & Stamp", signLineX + 8f, yStart + 72f, bodySecondaryPaint)
        canvas.drawText("Registration No: _________________", signLineX + 8f, yStart + 82f, bodySecondaryPaint)

        return yStart + boxHeight + 12f
    }

    private fun drawPageFooter(canvas: Canvas, pageNumber: Int) {
        val y = MARGIN_BOTTOM + 16f
        canvas.drawLine(MARGIN_LEFT, y - 6f, MARGIN_RIGHT, y - 6f, borderPaint)

        canvas.drawText(
            "AbhayHealth Personal Health Record (PHR) • Confidential • Local-First Architecture",
            MARGIN_LEFT,
            y + 6f,
            footerPaint
        )

        val pageStr = "Page $pageNumber"
        canvas.drawText(pageStr, MARGIN_RIGHT - footerPaint.measureText(pageStr), y + 6f, footerPaint)
    }

    private fun String?.isNull_or_blank(): Boolean = this == null || this.isBlank()
}
