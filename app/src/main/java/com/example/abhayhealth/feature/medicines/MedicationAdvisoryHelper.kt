package com.example.abhayhealth.feature.medicines

object MedicationAdvisoryHelper {

    data class MedicationGuidance(
        val tip: String,
        val foodInstruction: String,
        val cautionaryNote: String? = null
    )

    fun getGuidance(medicationName: String): MedicationGuidance? {
        val name = medicationName.lowercase()
        return when {
            name.contains("calpol") || name.contains("paracetamol") || name.contains("dolo") || name.contains("crocin") ->
                MedicationGuidance(
                    tip = "Antipyretic / Analgesic",
                    foodInstruction = "Take with water after food.",
                    cautionaryNote = "Maintain 4-6 hrs between doses. Max 4 doses in 24h."
                )

            name.contains("zifi") || name.contains("cefixime") || name.contains("amox") || name.contains("azith") ||
            name.contains("clav") || name.contains("cipro") || name.contains("oflox") ->
                MedicationGuidance(
                    tip = "Antibiotic Course",
                    foodInstruction = "Take with or immediately after meals.",
                    cautionaryNote = "Complete the entire prescribed days even if feeling better."
                )

            name.contains("pantop") || name.contains("omep") || name.contains("rabep") || name.contains("esomep") ->
                MedicationGuidance(
                    tip = "Gastric Acid Reducer",
                    foodInstruction = "Best taken 30 minutes before morning breakfast.",
                    cautionaryNote = "Swallow whole; do not crush or chew."
                )

            name.contains("electral") || name.contains("ors") || name.contains("electrolyte") ->
                MedicationGuidance(
                    tip = "Oral Rehydration Salt",
                    foodInstruction = "Dissolve fully in clean drinking water.",
                    cautionaryNote = "Consume within 24 hours of preparation."
                )

            name.contains("metformin") || name.contains("glycomet") || name.contains("glimepiride") ->
                MedicationGuidance(
                    tip = "Glycemic Control",
                    foodInstruction = "Take with or right after meals.",
                    cautionaryNote = "Helps minimize gastric irritation and maintains stable blood sugar."
                )

            name.contains("telmisartan") || name.contains("amlodipine") || name.contains("atenolol") || name.contains("losartan") ->
                MedicationGuidance(
                    tip = "Blood Pressure Support",
                    foodInstruction = "Take at a fixed time each day with water.",
                    cautionaryNote = "Do not stop abruptly without doctor consultation."
                )

            name.contains("cetirizine") || name.contains("montair") || name.contains("allegra") || name.contains("levocet") ->
                MedicationGuidance(
                    tip = "Antiallergic / Respiratory",
                    foodInstruction = "Can be taken with or without food.",
                    cautionaryNote = "Best taken at bedtime; avoid driving if drowsy."
                )

            name.contains("iron") || name.contains("ferrous") || name.contains("orofer") ->
                MedicationGuidance(
                    tip = "Iron Supplement",
                    foodInstruction = "Best with citrus juice or vitamin C.",
                    cautionaryNote = "Avoid taking with milk, tea, or antacids (space by 2 hours)."
                )

            name.contains("calcium") || name.contains("shelcal") || name.contains("vitamin d") ->
                MedicationGuidance(
                    tip = "Bone & Mineral Supplement",
                    foodInstruction = "Take after a heavy meal.",
                    cautionaryNote = "Space apart from iron supplements by at least 2 hours."
                )

            else -> null
        }
    }
}
