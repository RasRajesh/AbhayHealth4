package com.example.abhayhealth.domain.model

import java.util.UUID

/**
 * Domain model representing a structured medical test result extracted from a report.
 */
data class ParsedMedicalResult(
    val id: String = UUID.randomUUID().toString(),
    val testName: String,
    val value: String,
    val unit: String = "",
    val referenceRange: String = ""
) {
    val status: BiomarkerStatus
        get() = evaluateStatus()

    val organSystem: OrganSystem
        get() = determineOrganSystem()

    val isOutOfRange: Boolean
        get() = status in setOf(
            BiomarkerStatus.LOW,
            BiomarkerStatus.HIGH,
            BiomarkerStatus.CRITICAL_LOW,
            BiomarkerStatus.CRITICAL_HIGH,
            BiomarkerStatus.QUALITATIVE_ABNORMAL
        )

    fun evaluateStatus(): BiomarkerStatus {
        val cleanVal = value.trim()
        val lowerVal = cleanVal.lowercase()

        // 1. Qualitative check
        if (lowerVal in listOf("nil", "negative", "normal", "clear", "pale yellow", "absent", "non reactive")) {
            return BiomarkerStatus.NORMAL
        }
        if (lowerVal in listOf("positive", "present", "reactive", "cloudy", "hazy", "turbid") || lowerVal.startsWith("trace") || lowerVal.startsWith("+")) {
            return BiomarkerStatus.QUALITATIVE_ABNORMAL
        }

        // 2. Numeric range evaluation
        val num = extractLeadingNumber(cleanVal) ?: return BiomarkerStatus.UNKNOWN
        val cleanRef = referenceRange.replace("–", "-").replace("—", "-").trim()

        // Pattern: min - max (e.g., "12.0 - 15.0", "70-100")
        val rangeRegex = Regex("""([0-9]+(?:\.[0-9]+)?)\s*-\s*([0-9]+(?:\.[0-9]+)?)""")
        rangeRegex.find(cleanRef)?.let { match ->
            val min = match.groupValues[1].toDoubleOrNull()
            val max = match.groupValues[2].toDoubleOrNull()
            if (min != null && max != null) {
                if (num < min) {
                    val criticalLowThreshold = min * 0.65
                    return if (num <= criticalLowThreshold) BiomarkerStatus.CRITICAL_LOW else BiomarkerStatus.LOW
                }
                if (num > max) {
                    val criticalHighThreshold = max * 1.5
                    return if (num >= criticalHighThreshold) BiomarkerStatus.CRITICAL_HIGH else BiomarkerStatus.HIGH
                }
                return BiomarkerStatus.NORMAL
            }
        }

        // Pattern: < max (e.g., "< 200", "<100")
        val lessThanRegex = Regex("""<\s*([0-9]+(?:\.[0-9]+)?)""")
        lessThanRegex.find(cleanRef)?.let { match ->
            val max = match.groupValues[1].toDoubleOrNull()
            if (max != null) {
                if (num > max) {
                    return if (num >= max * 1.5) BiomarkerStatus.CRITICAL_HIGH else BiomarkerStatus.HIGH
                }
                return BiomarkerStatus.NORMAL
            }
        }

        // Pattern: > min (e.g., "> 60")
        val greaterThanRegex = Regex(""">\s*([0-9]+(?:\.[0-9]+)?)""")
        greaterThanRegex.find(cleanRef)?.let { match ->
            val min = match.groupValues[1].toDoubleOrNull()
            if (min != null) {
                if (num < min) {
                    return if (num <= min * 0.65) BiomarkerStatus.CRITICAL_LOW else BiomarkerStatus.LOW
                }
                return BiomarkerStatus.NORMAL
            }
        }

        return BiomarkerStatus.NORMAL
    }

    private fun determineOrganSystem(): OrganSystem {
        val name = testName.lowercase()
        return when {
            name.contains("blood pressure") || name.contains("heart rate") || name.contains("pulse") ||
            name.contains("spo2") || name.contains("cholesterol") || name.contains("triglyceride") ||
            name.contains("hdl") || name.contains("ldl") || name.contains("vldl") || name.contains("lipid") ->
                OrganSystem.CARDIO

            name.contains("glucose") || name.contains("sugar") || name.contains("fbs") || name.contains("ppbs") ||
            name.contains("hba1c") || name.contains("tsh") || name.contains("t3") || name.contains("t4") ||
            name.contains("thyroid") || name.contains("insulin") ->
                OrganSystem.METABOLIC

            name.contains("bilirubin") || name.contains("sgot") || name.contains("sgpt") || name.contains("ast") ||
            name.contains("alt") || name.contains("alkaline phosphatase") || name.contains("protein") && !name.contains("urine") ||
            name.contains("albumin") && !name.contains("urine") || name.contains("globulin") || name.contains("a/g ratio") ||
            name.contains("ggt") ->
                OrganSystem.HEPATIC

            name.contains("creatinine") || name.contains("urea") || name.contains("uric acid") ||
            name.contains("bun") || name.contains("egfr") || name.contains("urine") || name.contains("pus cells") ||
            name.contains("epithelial") || name.contains("casts") || name.contains("crystals") ||
            name.contains("specific gravity") ->
                OrganSystem.RENAL

            name.contains("haemoglobin") || name.contains("hemoglobin") || name.contains("wbc") ||
            name.contains("rbc") || name.contains("platelet") || name.contains("pcv") || name.contains("mcv") ||
            name.contains("mch") || name.contains("neutrophil") || name.contains("lymphocyte") ||
            name.contains("monocyte") || name.contains("eosinophil") || name.contains("basophil") ||
            name.contains("esr") || name.contains("leucocyte") ->
                OrganSystem.HEMIC

            else -> OrganSystem.ALL
        }
    }

    private fun extractLeadingNumber(str: String): Double? {
        val match = Regex("""([0-9]+(?:\.[0-9]+)?)""").find(str)
        return match?.groupValues?.get(1)?.toDoubleOrNull()
    }
}

enum class BiomarkerStatus {
    NORMAL,
    LOW,
    HIGH,
    CRITICAL_LOW,
    CRITICAL_HIGH,
    QUALITATIVE_ABNORMAL,
    UNKNOWN
}

enum class OrganSystem(val displayName: String, val code: String) {
    ALL("All Systems", "ALL"),
    CARDIO("Cardiovascular", "CARDIO"),
    METABOLIC("Metabolic", "METAB"),
    HEPATIC("Hepatic / Liver", "LIVER"),
    RENAL("Renal / Kidney", "RENAL"),
    HEMIC("Blood / CBC", "HEMIC")
}

