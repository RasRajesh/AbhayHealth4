package com.example.abhayhealth.feature.records

enum class DocumentMedicalStatus {
    MEDICAL,
    NON_MEDICAL,
    UNCERTAIN
}

data class ClassificationResult(
    val status: DocumentMedicalStatus,
    val confidenceScore: Double,
    val matchedMedicalKeywords: List<String>,
    val detectedCategory: String
)

/**
 * Intelligent deterministic on-device classifier for medical documents.
 * Evaluates document content for clinical terminology, laboratory test names,
 * medical units, prescription formatting, patient identifiers, and health measurements.
 */
object MedicalDocumentClassifier {

    private val STRONG_CLINICAL_KEYWORDS = listOf(
        "HAEMATOLOGY", "BIOCHEMISTRY", "PATHOLOGY", "MICROBIOLOGY", "SEROLOGY",
        "COMPLETE BLOOD COUNT", "HEMOGLOBIN", "HAEMOGLOBIN", "CREATININE", "BILIRUBIN",
        "CHOLESTEROL", "TRIGLYCERIDES", "GLUCOSE FASTING", "POSTPRANDIAL", "HBA1C",
        "THYROID STIMULATING HORMONE", "TSH", "SGOT", "SGPT", "URINE ANALYSIS",
        "DISCHARGE SUMMARY", "PATIENT HISTORY", "CLINICAL DIAGNOSIS", "PRESCRIPTION",
        "MEDICAL PRESCRIPTION", "DOCTOR'S PRESCRIPTION", "OUTPATIENT RECORD",
        "HEALTH CHECKUP REPORT", "DIAGNOSTIC REPORT", "ULTRASOUND REPORT", "X-RAY REPORT"
    )

    private val GENERAL_MEDICAL_KEYWORDS = listOf(
        "PATIENT", "DOCTOR", "DR.", "DR ", "HOSPITAL", "CLINIC", "DIAGNOSTICS", "LABORATORY",
        "SPECIMEN", "INVESTIGATION", "REFERENCE RANGE", "NORMAL RANGE", "OBSERVED VALUE",
        "BLOOD PRESSURE", "PULSE", "SPO2", "HEART RATE", "TEMPERATURE", "WEIGHT", "HEIGHT",
        "DOSAGE", "TABLET", "CAPSULE", "SYRUP", "INJECTION", "AFTER FOOD", "BEFORE FOOD",
        "1-0-1", "1-0-0", "0-0-1", "1-1-1", "OD", "BD", "TID", "QID", "SOS", "RX"
    )

    private val MEDICAL_UNITS = listOf(
        "MG/DL", "G/DL", "CELLS/CUMM", "LAKHS/CUMM", "MILLION/CMM", "FL", "PG", "MM/HR",
        "U/L", "UIU/ML", "NG/DL", "UG/DL", "MEQ/L", "MMHG", "BPM", "%", "/HPF"
    )

    private val NON_MEDICAL_INDICATORS = listOf(
        "INVOICE NUMBER", "PURCHASE ORDER", "TAX INVOICE", "SHIPPING ADDRESS", "BILL TO",
        "ATTENDANCE REGISTER", "MARK SHEET", "HALL TICKET", "SEMESTER", "CURRICULUM VITAE",
        "RESUME", "BOARDING PASS", "E-TICKET", "ELECTRICITY BILL", "WATER BILL", "BANK STATEMENT"
    )

    fun classifyDocument(rawText: String): ClassificationResult {
        val cleanText = rawText.trim()
        if (cleanText.isBlank() || cleanText.length < 15) {
            return ClassificationResult(
                status = DocumentMedicalStatus.UNCERTAIN,
                confidenceScore = 0.0,
                matchedMedicalKeywords = emptyList(),
                detectedCategory = "UNKNOWN"
            )
        }

        val upper = cleanText.uppercase()

        // 1. Check for strong non-medical invoice/school/travel indicators
        val nonMedicalMatches = NON_MEDICAL_INDICATORS.filter { upper.contains(it) }

        // 2. Count medical evidence matches
        val matchedKeywords = mutableListOf<String>()

        for (kw in STRONG_CLINICAL_KEYWORDS) {
            if (upper.contains(kw)) {
                matchedKeywords.add(kw)
            }
        }

        for (kw in GENERAL_MEDICAL_KEYWORDS) {
            if (upper.contains(kw)) {
                matchedKeywords.add(kw)
            }
        }

        var unitMatches = 0
        for (unit in MEDICAL_UNITS) {
            if (upper.contains(unit)) {
                unitMatches++
            }
        }

        val category = MedicalFieldParser.parseDocumentCategory(cleanText)
        val hasClinicalCategory = category != "MEDICAL_RECORD"

        val totalEvidenceScore = matchedKeywords.size * 2.0 + unitMatches * 1.5 + (if (hasClinicalCategory) 5.0 else 0.0)

        val status = when {
            // Non-medical document indicators found without explicit strong clinical keywords
            nonMedicalMatches.isNotEmpty() && matchedKeywords.none { it in STRONG_CLINICAL_KEYWORDS } ->
                DocumentMedicalStatus.NON_MEDICAL

            // Strong medical evidence
            totalEvidenceScore >= 4.0 || matchedKeywords.size >= 2 || hasClinicalCategory ->
                DocumentMedicalStatus.MEDICAL

            // Plain text with zero medical keywords and zero units
            matchedKeywords.isEmpty() && unitMatches == 0 && cleanText.length > 50 ->
                DocumentMedicalStatus.NON_MEDICAL

            else ->
                DocumentMedicalStatus.UNCERTAIN
        }

        return ClassificationResult(
            status = status,
            confidenceScore = totalEvidenceScore,
            matchedMedicalKeywords = matchedKeywords.distinct(),
            detectedCategory = category
        )
    }
}
