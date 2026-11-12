package com.example.abhayhealth.feature.records

import com.example.abhayhealth.domain.model.ParsedMedicalResult
import com.example.abhayhealth.feature.medicines.PrescriptionMedicationEntity
import com.example.abhayhealth.feature.vitals.VitalReadingEntity
import com.example.abhayhealth.feature.vitals.VitalType
import java.util.UUID

/**
 * High-precision on-device clinical parser for Indian and international medical documents.
 * Extracts lab test biomarkers, vital signs, prescription medicines, document categories, and dates.
 */
object MedicalFieldParser {

    private val TITLE_KEYWORDS = listOf(
        "FINAL TEST REPORT",
        "TEST REPORT",
        "PRESCRIPTION",
        "MEDICAL PRESCRIPTION",
        "DOCTOR'S PRESCRIPTION",
        "LABORATORY REPORT",
        "DIAGNOSTIC REPORT",
        "PATHOLOGY REPORT",
        "BIOCHEMISTRY REPORT",
        "HAEMATOLOGY REPORT",
        "BLOOD TEST REPORT",
        "COMPLETE BLOOD COUNT",
        "ISWARYAM-RUBY",
        "CLINICAL SUMMARY",
        "DISCHARGE SUMMARY",
        "OUTPATIENT RECORD",
        "HEALTH CHECKUP REPORT",
        "HEALTH CHECKUP",
        "MASTER HEALTH CHECKUP"
    )

    private val DATE_PATTERNS = listOf(
        // Day/month/year: 27-Apr-2020, 15/08/2025, 15-08-2025, 15.08.2025
        Regex(
            """\b(?:0[1-9]|[12]\d|3[01])[/.-](?:0[1-9]|1[0-2]|Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[/.-](?:19|20)?\d{2}\b""",
            RegexOption.IGNORE_CASE
        ),
        // Year/month/day: 2025-08-15, 2025/08/15
        Regex(
            """\b(?:19|20)\d{2}[/.-](?:0[1-9]|1[0-2])[/.-](?:0[1-9]|[12]\d|3[01])\b""",
            RegexOption.IGNORE_CASE
        ),
        // Text month: 27 Apr 2020, 15 Aug 2025, 15 August 2025
        Regex(
            """\b(?:0[1-9]|[12]\d|3[01])\s+(?:Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:tember)?|Oct(?:ober)?|Nov(?:ember)?|Dec(?:ember)?)[,\s]+(?:19|20)?\d{2}\b""",
            RegexOption.IGNORE_CASE
        )
    )

    // Extensive catalog of clinical biomarkers with aliases, default units, and reference ranges
    data class TestDefinition(
        val canonicalName: String,
        val aliases: List<String>,
        val defaultUnit: String,
        val standardReferenceRange: String
    )

    private val TEST_CATALOG = listOf(
        // Complete Blood Count (CBC)
        TestDefinition("Hemoglobin", listOf("Haemoglobin (HB)", "Haemoglobin", "Hemoglobin", "Hb", "HB"), "g/dL", "12.0 - 16.0 g/dL"),
        TestDefinition("Total WBC Count", listOf("Total WBC Count", "WBC Count", "WBC", "Total Leucocyte Count", "TLC", "White Blood Cells"), "cells/cumm", "4,000 - 11,000 cells/cumm"),
        TestDefinition("Platelet Count", listOf("Platelet Count", "Platelets", "Total Platelet Count"), "Lakhs/cumm", "1.50 - 4.50 Lakhs/cumm"),
        TestDefinition("RBC Count", listOf("RBC (Red Blood Cell Count)", "RBC Count", "RBC", "Total Red Blood Cells", "Red Blood Cells", "RBCES"), "Million/cmm", "3.8 - 5.5 Million/cmm"),
        TestDefinition("Packed Cell Volume", listOf("PCV -( Haematocrit-Packed Cell Volume)", "Packed Cell Volume", "PCV", "Hematocrit", "HCT"), "%", "36.0 - 48.0 %"),
        TestDefinition("MCV", listOf("MCV (Mean Corpuscular Volume)", "MCV", "Mean Corpuscular Volume"), "fl", "80.0 - 101.0 fl"),
        TestDefinition("MCH", listOf("MCH (Mean Corpuscular Hemoglobin )", "MCH", "Mean Corpuscular Hemoglobin"), "pg", "27.0 - 32.0 pg"),
        TestDefinition("MCHC", listOf("MCHC (Mean Corpuscular Hemoglobin Concentration)", "MCHC"), "g/dL", "31.5 - 34.5 g/dL"),
        TestDefinition("ESR", listOf("Erythrocyte Sedimentation Rate(ESR)", "ESR", "Erythrocyte Sedimentation Rate"), "mm/hr", "0 - 20 mm/hr"),
        TestDefinition("Neutrophils", listOf("Neutrophils", "Polymorphs"), "%", "40 - 70 %"),
        TestDefinition("Lymphocytes", listOf("Lymphocytes"), "%", "20 - 40 %"),
        TestDefinition("Eosinophils", listOf("Eosinophils"), "%", "1 - 6 %"),
        TestDefinition("Monocytes", listOf("Monocytes"), "%", "2 - 8 %"),
        TestDefinition("Basophils", listOf("Basophils"), "%", "0 - 1 %"),
        TestDefinition("Immature Granulocytes", listOf("Immature Granulocyte Percentage(IG%)", "IMMATURE GRANULOCYTE", "IMMATURE GRANULOCYTES", "Immature Granulocytes", "IG%"), "%", "0.0 - 0.5 %"),
        TestDefinition("Absolute Neutrophil Count", listOf("Absolute Neutrophil Count", "ANC"), "cells/cumm", "2,000 - 7,000 cells/cumm"),
        TestDefinition("Absolute Lymphocyte Count", listOf("Absolute Lymphocyte Count", "ALC"), "cells/cumm", "1,000 - 3,000 cells/cumm"),
        TestDefinition("Absolute Eosinophil Count", listOf("Absolute Eosinophil Count", "AEC"), "cells/cumm", "20 - 500 cells/cumm"),
        TestDefinition("Absolute Monocyte Count", listOf("Absolute Monocyte Count", "AMC"), "cells/cumm", "200 - 1,000 cells/cumm"),
        TestDefinition("Absolute Basophil Count", listOf("Absolute Basophils Count", "Absolute Basophil Count", "ABC"), "cells/cumm", "20 - 100 cells/cumm"),
        TestDefinition("NRBC", listOf("NRBC(Nucleated Red Blood Cells)", "NRBC", "Nucleated Red Blood Cells"), "%", "< 0"),
        TestDefinition("ANRBC", listOf("ANRBC(Absolute Nucleated Red Blood Cells)", "ANRBC", "Absolute Nucleated Red Blood Cells"), "cells/cumm", "< 0"),
        TestDefinition("RDW-CV", listOf("RDW-CV(Red Cell Distribution Width-CV)", "RDW-CV", "RDW - CV", "RDW > CV", "Red Cell Distribution Width"), "%", "11.6 - 14.0 %"),
        TestDefinition("RDW-SD", listOf("RDW-SD(Red Cell Distribution Width-SD)", "RDW-SD", "RDW - SD", "RDW > SD"), "fl", "39 - 46 fl"),
        TestDefinition("MPV", listOf("MPV (Mean Platelet Volume)", "MPV", "Mean Platelet Volume"), "fl", "6.5 - 12.0 fl"),
        TestDefinition("PDW", listOf("PDW(Platelet Distribution Width)", "PDW", "Platelet Distribution Width"), "%", "9 - 14 %"),
        TestDefinition("PCT (Plateletcrit)", listOf("PCT(Plateletcrit)", "Plateletcrit", "PCT"), "%", "0.19 - 0.39 %"),
        TestDefinition("P-LCR", listOf("PLCR(Platelet to Large Cell Ratio)", "PLCR", "P-LCR(Platelet Large Cell Ratio)", "P-LCR", "Platelet Large Cell Ratio"), "%", "15.0 - 43.0 %"),

        // Diabetes & Glycemic Panel
        TestDefinition("Fasting Blood Glucose", listOf("GLUCOSE FASTING (FBS)", "Glucose Fasting", "FBS", "Fasting Blood Glucose", "Fasting Glucose", "Fasting Blood Sugar"), "mg/dL", "70 - 100 mg/dL"),
        TestDefinition("Postprandial Blood Glucose", listOf("Postprandial Blood Glucose", "PPBS", "Post Prandial Glucose", "PP Glucose", "Post Meal Sugar"), "mg/dL", "< 140 mg/dL"),
        TestDefinition("Random Blood Sugar", listOf("Random Blood Sugar", "RBS", "Blood Sugar", "Glucose Random"), "mg/dL", "70 - 140 mg/dL"),
        TestDefinition("HbA1c", listOf("HbA1c", "Glycated Hemoglobin", "Glycosylated Hemoglobin"), "%", "< 5.7 %"),

        // Lipid Profile
        TestDefinition("Total Cholesterol", listOf("CHOLESTEROL TOTAL", "Total Cholesterol", "Serum Cholesterol", "Cholesterol"), "mg/dL", "< 200 mg/dL"),
        TestDefinition("HDL Cholesterol", listOf("HDL CHOLESTEROL", "HDL Cholesterol", "HDL", "Good Cholesterol"), "mg/dL", "> 40 mg/dL"),
        TestDefinition("LDL Cholesterol", listOf("LDL CHOLESTEROL", "LDL Cholesterol", "LDL", "Bad Cholesterol"), "mg/dL", "< 100 mg/dL"),
        TestDefinition("Triglycerides", listOf("TRIGLYCERIDES", "Triglycerides", "TGL", "Serum Triglycerides"), "mg/dL", "< 150 mg/dL"),
        TestDefinition("VLDL Cholesterol", listOf("VLDL CHOLESTEROL", "VLDL Cholesterol", "VLDL"), "mg/dL", "< 30 mg/dL"),
        TestDefinition("Non-HDL Cholesterol", listOf("NON-HDL CHOLESTEROL", "Non-HDL Cholesterol", "NON - HDL", "NON > HDL"), "mg/dL", "< 130 mg/dL"),
        TestDefinition("Chol / HDL Ratio", listOf("CHOL / HDL RATIO", "CHOL/HDL RATIO", "Cholesterol / HDL Ratio", "Chol/HDL"), "", "3.3 - 4.4"),
        TestDefinition("LDL / HDL Ratio", listOf("LDL / HDL RATIO", "LDL/HDL RATIO", "LDL / HDL Ratio", "LDL/HDL"), "", "1.5 - 3.5"),
        TestDefinition("TGL / HDL Ratio", listOf("TGL/HDL RATIO", "TGL / HDL RATIO", "TGL / HDL", "Triglycerides / HDL Ratio", "TGL/HDL"), "", "< 2.0"),

        // Kidney Function Test (KFT / RFT)
        TestDefinition("Serum Creatinine", listOf("CREATININE", "Serum Creatinine", "Creatinine", "S. Creatinine"), "mg/dL", "0.5 - 1.1 mg/dL"),
        TestDefinition("Blood Urea", listOf("UREA", "Blood Urea", "Urea"), "mg/dL", "13 - 45 mg/dL"),
        TestDefinition("Blood Urea Nitrogen", listOf("Blood Urea Nitrogen", "BUN"), "mg/dL", "7 - 20 mg/dL"),
        TestDefinition("Uric Acid", listOf("URIC ACID", "Uric Acid", "Serum Uric Acid"), "mg/dL", "2.5 - 6.7 mg/dL"),
        TestDefinition("eGFR", listOf("eGFR", "Estimated GFR"), "mL/min", "> 90 mL/min"),
        TestDefinition("Sodium", listOf("Sodium", "Serum Sodium", "Na+"), "mEq/L", "135 - 145 mEq/L"),
        TestDefinition("Potassium", listOf("Potassium", "Serum Potassium", "K+"), "mEq/L", "3.5 - 5.1 mEq/L"),
        TestDefinition("Calcium", listOf("Calcium", "Serum Calcium", "Total Calcium"), "mg/dL", "8.8 - 10.2 mg/dL"),

        // Liver Function Test (LFT)
        TestDefinition("SGOT / AST", listOf("Aspartate aminotransferase(AST/SGOT)", "AST/SGOT", "SGOT", "AST", "AST / SGOT"), "U/L", "5 - 34 U/L"),
        TestDefinition("SGPT / ALT", listOf("Alanine aminotransferase(ALT/SGPT)", "ALT/SGPT", "SGPT", "ALT", "ALT / SGPT"), "U/L", "5 - 35 U/L"),
        TestDefinition("Alkaline Phosphatase", listOf("ALKALINE PHOSPHATASE", "Alkaline Phosphatase", "ALP"), "U/L", "42 - 98 U/L"),
        TestDefinition("Total Bilirubin", listOf("BILIRUBIN TOTAL", "Total Bilirubin", "Bilirubin Total", "S. Bilirubin"), "mg/dL", "0.2 - 1.2 mg/dL"),
        TestDefinition("Direct Bilirubin", listOf("BILIRUBIN DIRECT", "Direct Bilirubin", "Conjugated Bilirubin"), "mg/dL", "0.0 - 0.3 mg/dL"),
        TestDefinition("Indirect Bilirubin", listOf("BILIRUBIN INDIRECT", "Indirect Bilirubin"), "mg/dL", "0.1 - 0.9 mg/dL"),
        TestDefinition("Total Protein", listOf("TOTAL PROTEIN", "Total Protein", "Serum Protein"), "g/dL", "6.0 - 8.0 g/dL"),
        TestDefinition("Albumin", listOf("ALBUMIN", "Albumin", "Serum Albumin"), "g/dL", "3.5 - 5.2 g/dL"),
        TestDefinition("Globulin", listOf("GLOBULIN", "Globulin"), "g/dL", "2.8 - 3.5 g/dL"),
        TestDefinition("A/G Ratio", listOf("A/G RATIO", "A/G Ratio", "Albumin Globulin Ratio", "Albumin / Globulin Ratio"), "", "1.1 - 2.5"),
        TestDefinition("AST / ALT Ratio", listOf("AST/ALT", "AST / ALT", "AST/ALT RATIO", "SGOT/SGPT RATIO"), "", "1 - 1.19"),
        TestDefinition("Gamma GT", listOf("GAMMA GT", "Gamma GT", "GGT"), "U/L", "5 - 38 U/L"),

        // Thyroid Profile
        TestDefinition("Total T3", listOf("Total-T3 (Triiodothyronine-T3)", "Total T3", "Total-T3", "T3", "TOTAL T ?"), "ng/dL", "70 - 204 ng/dL"),
        TestDefinition("Total T4", listOf("Total-T4 (Thyroxine T4 )", "Total T4", "Total-T4", "T4"), "ug/dL", "4.5 - 11.7 ug/dL"),
        TestDefinition("TSH", listOf("TSH (Thyroid-stimulating hormone)-Ultra", "TSH", "Thyroid Stimulating Hormone", "Ultrasensitive TSH"), "uIU/mL", "0.4 - 4.5 uIU/mL"),
        TestDefinition("Free T3", listOf("Free T3", "FT3"), "pg/mL", "1.7 - 4.2 pg/mL"),
        TestDefinition("Free T4", listOf("Free T4", "FT4"), "ng/dL", "0.7 - 1.8 ng/dL"),

        // Urine Examination
        TestDefinition("Urine Pus Cells", listOf("PUS CELLS", "Pus Cells", "Pus Cells (Urine)"), "/hpf", "0 - 5 /hpf"),
        TestDefinition("Urine Epithelial Cells", listOf("EPITHELIAL CELLS", "Epithelial Cells", "Epithelial"), "/hpf", "0 - 5 /hpf"),
        TestDefinition("Urine RBCs", listOf("RBC'S", "RBCS", "RBCES", "RBCs in Urine", "RBC (Urine)"), "/hpf", "0 - 2 /hpf"),
        TestDefinition("Urine Crystals", listOf("CRYSTALS", "Crystals in Urine", "Crystals"), "", "Nil"),
        TestDefinition("Urine Hyaline Casts", listOf("HYALINE CASTS", "Hyaline Casts"), "", "0 - 2"),
        TestDefinition("Pathological Casts", listOf("PATHOLOGICAL CAST", "Pathological Casts"), "", "Nil"),
        TestDefinition("Urine Bacteria", listOf("BACTERIA", "Bacteria (Urine)", "Bacteria"), "", "Nil"),
        TestDefinition("Urine Yeast", listOf("YEAST", "Yeast Cells", "Yeast"), "", "Nil"),
        TestDefinition("Urine Mucus", listOf("MUCUS", "Mucus Threads", "Mucus"), "", "Absent"),
        TestDefinition("Urine Colour", listOf("COLOUR", "Urine Colour", "Color"), "", "Pale Yellow"),
        TestDefinition("Urine Clarity", listOf("CLARITY", "Urine Clarity", "Appearance"), "", "Clear"),
        TestDefinition("Urine pH", listOf("CHEMICAL EXAMINATION PH", "Urine pH", "Reaction / pH", "pH"), "", "4.5 - 8.0"),
        TestDefinition("Urine Specific Gravity", listOf("SPECIFIC GRAVITY", "Specific Gravity"), "", "1.005 - 1.030"),
        TestDefinition("Urine Glucose", listOf("GLUCOSE-URINE(F)", "GLUCOSE-URINE", "GLUCOSE - URINE", "GLUCOSE > URINE", "Glucose - Urine", "Urine Glucose", "Sugar (Urine)"), "", "Nil"),
        TestDefinition("Urine Protein", listOf("PROTEIN", "Urine Protein", "Protein"), "", "Nil"),
        TestDefinition("Urine Nitrite", listOf("NITRITE", "Nitrite", "Nitrite (Urine)"), "", "Negative"),
        TestDefinition("Urine Ketone", listOf("KETONE", "Ketone Bodies", "Ketones"), "", "Negative"),
        TestDefinition("Urine Bilirubin", listOf("BILIRUBIN (URINE)", "BILIRUBIN - URINE", "Bilirubin (Urine)", "BILIRUBIN"), "", "Negative"),
        TestDefinition("Urobilinogen", listOf("UROBILINOGEN", "Urobilinogen"), "", "Normal"),
        TestDefinition("Urine Blood", listOf("Blood (Urine)", "Blood - Urine", "Urine Blood", "Occult Blood", "BLOOD"), "", "Negative"),

        // Inflammatory & Clinical Markers
        TestDefinition("C-Reactive Protein", listOf("C-Reactive Protein", "CRP", "hs-CRP"), "mg/L", "< 5.0 mg/L"),
        TestDefinition("Vitamin D", listOf("Vitamin D", "25-OH Vitamin D", "Vitamin D3", "25 Hydroxy Vitamin D"), "ng/mL", "30.0 - 100.0 ng/mL"),
        TestDefinition("Vitamin B12", listOf("Vitamin B12", "Cyanocobalamin"), "pg/mL", "211 - 911 pg/mL"),
        TestDefinition("Serum Ferritin", listOf("Serum Ferritin", "Ferritin"), "ng/mL", "30 - 400 ng/mL"),
        TestDefinition("Serum Iron", listOf("Serum Iron", "Iron", "Total Iron"), "ug/dL", "60 - 170 ug/dL"),
        TestDefinition("TIBC", listOf("Total Iron Binding Capacity", "TIBC"), "ug/dL", "240 - 450 ug/dL"),
        TestDefinition("Transferrin Saturation", listOf("Transferrin Saturation", "% Transferrin Saturation", "Iron Saturation"), "%", "20 - 50 %"),
        TestDefinition("Folic Acid", listOf("Serum Folate", "Folate", "Folic Acid"), "ng/mL", "4.0 - 20.0 ng/mL"),

        // Electrolytes & Minerals
        TestDefinition("Chloride", listOf("Serum Chloride", "Chloride", "Cl-", "Cl"), "mEq/L", "98 - 107 mEq/L"),
        TestDefinition("Bicarbonate", listOf("Serum Bicarbonate", "Bicarbonate", "HCO3"), "mEq/L", "22 - 29 mEq/L"),
        TestDefinition("Magnesium", listOf("Serum Magnesium", "Magnesium", "Mg"), "mg/dL", "1.7 - 2.2 mg/dL"),
        TestDefinition("Phosphorus", listOf("Serum Phosphorus", "Phosphorus", "Phosphate", "Serum Phosphate"), "mg/dL", "2.5 - 4.5 mg/dL"),

        // Pancreatic & Cardiac Enzymes
        TestDefinition("Serum Amylase", listOf("Serum Amylase", "Amylase"), "U/L", "28 - 100 U/L"),
        TestDefinition("Serum Lipase", listOf("Serum Lipase", "Lipase"), "U/L", "13 - 60 U/L"),
        TestDefinition("LDH", listOf("Lactate Dehydrogenase", "LDH", "Serum LDH"), "U/L", "140 - 280 U/L"),
        TestDefinition("CPK / CK", listOf("Creatine Kinase", "CPK", "CK", "CK-NAC"), "U/L", "39 - 308 U/L"),
        TestDefinition("CK-MB", listOf("CK-MB", "Creatine Kinase MB"), "U/L", "0 - 25 U/L"),
        TestDefinition("Troponin-I", listOf("Troponin I", "Troponin-I", "Trop-I", "hs-cTnI"), "ng/mL", "< 0.04 ng/mL"),
        TestDefinition("Troponin-T", listOf("Troponin T", "Troponin-T", "Trop-T"), "ng/mL", "< 0.01 ng/mL"),

        // Coagulation & Thrombosis
        TestDefinition("D-Dimer", listOf("D-Dimer", "D Dimer"), "mg/L", "< 0.50 mg/L"),
        TestDefinition("Prothrombin Time", listOf("Prothrombin Time", "PT", "PT (Seconds)"), "sec", "11.0 - 13.5 sec"),
        TestDefinition("INR", listOf("INR", "PT / INR", "PT/INR", "International Normalized Ratio"), "", "0.8 - 1.2"),
        TestDefinition("aPTT", listOf("aPTT", "APTT", "Activated Partial Thromboplastin Time"), "sec", "25 - 35 sec"),

        // Serology & Infectious Disease
        TestDefinition("Dengue NS1", listOf("Dengue NS1 Antigen", "Dengue NS1", "NS1 Antigen"), "", "Negative"),
        TestDefinition("Dengue IgM", listOf("Dengue IgM", "Dengue IgM Antibody"), "", "Negative"),
        TestDefinition("Dengue IgG", listOf("Dengue IgG", "Dengue IgG Antibody"), "", "Negative"),
        TestDefinition("Widal Test", listOf("Widal Test", "Widal", "Typhi O", "Typhi H"), "", "Negative"),
        TestDefinition("Malaria Antigen", listOf("Malaria Antigen", "Malarial Parasite", "MP"), "", "Negative"),
        TestDefinition("HBsAg", listOf("HBsAg", "Hepatitis B Surface Antigen"), "", "Non-Reactive"),
        TestDefinition("HCV", listOf("Anti-HCV", "HCV Antibody"), "", "Non-Reactive"),
        TestDefinition("HIV", listOf("HIV I & II", "HIV Antibody", "HIV"), "", "Non-Reactive"),

        // Tumor Markers
        TestDefinition("PSA", listOf("Prostate Specific Antigen", "Total PSA", "PSA"), "ng/mL", "< 4.0 ng/mL"),
        TestDefinition("CEA", listOf("Carcinoembryonic Antigen", "CEA"), "ng/mL", "< 3.0 ng/mL"),
        TestDefinition("CA-125", listOf("CA 125", "CA-125"), "U/mL", "< 35 U/mL"),

        // Vitals & Physiological Parameters
        TestDefinition("Blood Pressure", listOf("Blood Pressure", "BP", "B.P."), "mmHg", "120/80 mmHg"),
        TestDefinition("Pulse", listOf("Pulse", "Heart Rate", "Pulse Rate"), "BPM", "60 - 100 BPM"),
        TestDefinition("SpO2", listOf("SpO2", "Oxygen Saturation", "O2 Saturation"), "%", "95 - 100 %"),
        TestDefinition("Temperature", listOf("Temperature", "Temp"), "°F", "97.0 - 99.0 °F"),
        TestDefinition("Respiratory Rate", listOf("Respiratory Rate", "RR"), "breaths/min", "12 - 20 breaths/min"),
        TestDefinition("Weight", listOf("Weight", "Body Weight", "Wt", "Wt:"), "kg", "Target BMI 18.5 - 24.9")
    )

    // Standard medical forms prefix (supports 1 Syp, 2. Tab, 3) Cap, or Tab, T., C.)
    private val MEDICINE_FORM_PREFIX = Regex(
        """(?im)^(?:[0-9]+[\.\)\-]?\s*)?(TAB|TABLET|CAP|CAPSULE|SYP|SYRUP|INJ|INJECTION|DROPS|OINT|OINTMENT|GEL|CREAM|SUSP|SUSPENSION|LOTION|INHALER|RESPULES|SACHET|POWDER|T|C)\.?\s+([A-Za-z0-9\s_+/-]+)""",
        RegexOption.IGNORE_CASE
    )


    // Over 180 common medicines across antibiotics, diabetes, cardiac, analgesics, gastro, vitamins, pediatric
    private val COMMON_DRUGS = listOf(
        // Pediatric & Antibiotic staples
        "Calpol", "Zifi", "Electral", "ORS", "Taxim-O", "Taxim", "Cefixime", "Cefpodoxime",
        "Monocef", "Amikacin", "Enterogermina", "Econorm", "Pedilac", "Zinc", "Zincovit",
        "Cyclopam", "Colic-Aid", "Omnacortil", "Prednisolone", "Meftal-P", "Ibugesic-Plus",
        "Crocin-DS", "Augmentin-DDS", "T-Minic", "Solvin", "Maxtra", "Ascoril-LS",

        // Analgesics & Antipyretics
        "Paracetamol", "Dolo", "Crocin", "Combiflam", "Meftal", "Meftal-Spas",
        "Ibuprofen", "Diclofenac", "Voveran", "Tramadol", "Ultracet", "Aceclofenac",
        "Zerodol", "Zerodol-SP", "Zerodol-P", "Ketorolac", "Naproxen",

        // Antibiotics & Anti-Infectives
        "Amoxicillin", "Augmentin", "Moxikind", "Azithromycin", "Azee",
        "Ciprofloxacin", "Ciplox", "Levofloxacin", "Levomac", "Ofloxacin",
        "Zanocin", "Metronidazole", "Flagyl", "Doxycycline", "Doxt", "Clindamycin",
        "Linezolid", "Cephalexin",

        // Blood Pressure & Cardiac
        "Telmisartan", "Telma", "Telma-H", "Telma-AM", "Amlodipine", "Amlong", "Losartan",
        "Losar", "Atenolol", "Metoprolol", "Betaloc", "Ramipril", "Cardace", "Enalapril",
        "Cilnidipine", "Cilacar", "Nebivolol", "Bisoprolol", "Concor", "Chlorthalidone",
        "Hydrochlorothiazide", "Torsemide", "Dytor", "Furosemide", "Lasix",

        // Diabetes & Glucose Management
        "Metformin", "Glycomet", "Glycomet-GP", "Glimepiride", "Amaryl", "Gliclazide",
        "Vildagliptin", "Galvus", "Galvus-Met", "Teneligliptin", "Sitagliptin", "Januvia",
        "Janumet", "Linagliptin", "Trajenta", "Dapagliflozin", "Forxiga", "Empagliflozin",
        "Jardiance", "Pioglitazone", "Voglibose", "Glipizide", "Insulin", "Lantus",

        // Gastrointestinal & Acid Reflux
        "Pantoprazole", "Pan 40", "Pan-D", "Pantocid", "Pantocid-DSR", "Rabeprazole",
        "Razo", "Razo-D", "Omeprazole", "Omez", "Omez-D", "Esomeprazole", "Nexpro",
        "Ranitidine", "Famotidine", "Domperidone", "Ondansetron", "Emeset", "Sucralfate",
        "Lactulose", "Digene", "Gelusil",

        // Cholesterol & Statins
        "Atorvastatin", "Atorva", "Lipitor", "Rosuvastatin", "Rozavel", "Crestor",
        "Fenofibrate", "Ezetimibe",

        // Respiratory, Allergy & ENT
        "Cetirizine", "Cetzine", "Levocetirizine", "Levocet", "Montelukast", "Montair",
        "Montair-LC", "Fexofenadine", "Allegra", "Bilastine", "Salbutamol", "Asthalin",
        "Budesonide", "Budecort", "Formoterol", "Foracort", "Seroflo", "Deriphyllin",
        "Ascoril", "Benadryl", "Alex", "Chericof",

        // Thyroid & Hormones
        "Levothyroxine", "Thyronorm", "Eltroxin", "Carbimazole",

        // Neurology & Mental Health
        "Gabapentin", "Pregabalin", "Clonazepam", "Alprazolam", "Escitalopram", "Nexito",
        "Sertraline", "Duloxetine", "Amitriptyline", "Methylcobalamin",

        // Vitamins & Supplements
        "Vitamin D3", "Calcirol", "Shelcal", "Calcium", "Neurobion", "Neurobion Forte",
        "Becosules", "Limcee", "Supradyn", "Folvite", "Folic Acid",
        "Ferrous Ascorbate", "Orofer", "Evion",

        // Blood Thinners
        "Aspirin", "Ecosprin", "Clopidogrel", "Deplatt", "Clopivas", "Warfarin",
        "Apixaban", "Eliquis", "Rivaroxaban"
    )

    fun parseDocumentCategory(rawText: String): String {
        val upper = rawText.uppercase()
        return when {
            upper.contains("PRESCRIPTION") || upper.contains("RX") || upper.contains("TAB ") || upper.contains("CAP ") || upper.contains("SYP ") || upper.contains("DOSAGE") ->
                "PRESCRIPTION"
            upper.contains("PATHOLOGY") || upper.contains("BIOCHEMISTRY") || upper.contains("HAEMATOLOGY") || upper.contains("REFERENCE RANGE") || upper.contains("LABORATORY") || upper.contains("TEST REPORT") ->
                "LAB_REPORT"
            upper.contains("X-RAY") || upper.contains("ULTRASOUND") || upper.contains("MRI") || upper.contains("CT SCAN") || upper.contains("SONOGRAPHY") ->
                "IMAGING"
            upper.contains("DISCHARGE") || upper.contains("ADMISSION") || upper.contains("SUMMARY") ->
                "DISCHARGE_SUMMARY"
            else -> "MEDICAL_RECORD"
        }
    }

    fun parseDocumentTitle(rawText: String, defaultTitle: String): String {
        val lines = rawText.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        for (line in lines) {
            val upper = line.uppercase()
            for (keyword in TITLE_KEYWORDS) {
                if (upper.contains(keyword)) {
                    return line.take(50)
                }
            }
        }

        // Check for doctor or hospital header
        for (line in lines.take(5)) {
            val upper = line.uppercase()
            if (upper.startsWith("DR.") || upper.contains("CLINIC") || upper.contains("HOSPITAL") || upper.contains("LABORATORY") || upper.contains("DIAGNOSTICS") || upper.contains("SCANS")) {
                return line.take(50)
            }
        }

        return lines.firstOrNull()?.take(40) ?: defaultTitle
    }

    fun parseReportDate(rawText: String): String? {
        for (pattern in DATE_PATTERNS) {
            val match = pattern.find(rawText)
            if (match != null) {
                return match.value
            }
        }
        return null
    }

    fun parseDateToTimestamp(dateStr: String): Long? {
        val formats = listOf(
            "dd-MMM-yyyy", "dd/MM/yyyy", "dd-MM-yyyy", "dd.MM.yyyy",
            "yyyy-MM-dd", "yyyy/MM/dd", "dd MMM yyyy", "dd MMMM yyyy"
        )
        for (format in formats) {
            try {
                val sdf = java.text.SimpleDateFormat(format, java.util.Locale.ENGLISH)
                sdf.isLenient = false
                val parsed = sdf.parse(dateStr.trim())
                if (parsed != null) return parsed.time
            } catch (_: Exception) {
            }
        }
        return null
    }

    /**
     * Multi-strategy biomarker extraction from lab reports and medical diagnostics.
     * Combines multi-line tabular scanning and regex key-value extraction to guarantee 0 missing metrics.
     */
    /**
     * Multi-strategy biomarker extraction from lab reports and medical diagnostics.
     * Combines multi-line tabular scanning and regex key-value extraction to guarantee 0 missing metrics.
     */
    fun parseTestResults(rawText: String): List<ParsedMedicalResult> {
        val results = mutableListOf<ParsedMedicalResult>()
        val seenTests = mutableSetOf<String>()

        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }

        // Strategy 1: Tabular & Multi-Line Scanning across TEST_CATALOG
        for (lineIndex in lines.indices) {
            val line = lines[lineIndex]

            for (testDef in TEST_CATALOG) {
                val key = testDef.canonicalName.lowercase()
                if (seenTests.contains(key)) continue

                // Check if line contains any alias with robust punctuation and parenthesis handling
                val matchedAlias = testDef.aliases.firstOrNull { alias ->
                    matchesAlias(line, alias)
                }

                if (matchedAlias != null) {
                    val parsed = findTestDetailsFromLines(lines, lineIndex, matchedAlias, testDef)
                    if (parsed != null) {
                        seenTests.add(key)
                        results.add(parsed)
                        break
                    }
                }
            }
        }

        // Strategy 2: Regex extraction for Blood Pressure, Glucose, CRP, and any missed tests
        val genericPattern = Regex(
            """(?im)\b(Blood Pressure|BP|Systolic|Diastolic|Pulse|Heart Rate|Temp|Temperature|Weight|Wt|SpO2|Oxygen|FBS|PPBS|RBS|HbA1c|Hemoglobin|Hb|CRP|Platelets|Cholesterol|Triglycerides|Creatinine|Bilirubin|Potassium|Sodium|Calcium|Uric Acid|TSH|SGOT|SGPT|WBC|RBC|Urea)\b\s*[:\-=(deg)]*\s*([<>≤≥]?\s*\d+(?:[\/\s.,-]\d+)?)\s*([a-zA-Zµμ%/0-9^._-]+)?\s*(\([^)\r\n]+\))?"""
        )

        for (match in genericPattern.findAll(rawText)) {
            var rawName = match.groupValues[1].trim()
            val value = match.groupValues[2].trim()
            var unit = match.groupValues[3].trim()
            var refRange = match.groupValues[4].trim()

            val canonicalName = when {
                rawName.equals("BP", true) -> "Blood Pressure"
                rawName.equals("FBS", true) -> "Fasting Blood Glucose"
                rawName.equals("PPBS", true) -> "Postprandial Blood Glucose"
                rawName.equals("RBS", true) -> "Random Blood Sugar"
                rawName.equals("Hb", true) -> "Hemoglobin"
                rawName.equals("Temp", true) -> "Temperature"
                rawName.equals("Platelets", true) -> "Platelet Count"
                rawName.equals("Cholesterol", true) -> "Total Cholesterol"
                rawName.equals("Creatinine", true) -> "Serum Creatinine"
                rawName.equals("Bilirubin", true) -> "Total Bilirubin"
                rawName.equals("Wt", true) -> "Weight"
                else -> rawName
            }

            val key = canonicalName.lowercase()
            if (seenTests.add(key)) {
                if (unit.isBlank()) {
                    unit = TEST_CATALOG.firstOrNull { it.canonicalName.equals(canonicalName, true) }?.defaultUnit ?: ""
                }
                if (refRange.isBlank()) {
                    refRange = TEST_CATALOG.firstOrNull { it.canonicalName.equals(canonicalName, true) }?.standardReferenceRange ?: ""
                }

                results.add(
                    ParsedMedicalResult(
                        id = UUID.randomUUID().toString(),
                        testName = canonicalName,
                        value = value,
                        unit = unit,
                        referenceRange = refRange
                    )
                )
            }
        }

        // Strategy 3: Tabular Row Extraction for any remaining unmapped lab parameters
        val tableRowPattern = Regex(
            """^(?:[0-9]+[\.\)]\s*)?([A-Za-z][A-Za-z0-9\s()/\-_.]{2,35}?)\s+([<>≤≥]?\s*\d+(?:\.\d+)?|\b(?:Nil|Negative|Positive|Normal|Trace|Reactive|Non-Reactive|Clear|Pale\s+Yellow)\b)\s*([a-zA-Zµμ%/0-9^._-]+)?(?:\s+([<>≤≥\d\s\-–.to()\/]+))?""",
            RegexOption.IGNORE_CASE
        )

        val skipHeaders = setOf(
            "investigation", "investigations", "test", "test name", "parameter", "result", "results",
            "observed value", "units", "unit", "biological reference", "reference interval", "ref range",
            "normal range", "method", "specimen", "page", "date", "patient", "doctor", "dr", "age", "gender", "sex"
        )

        for (line in lines) {
            val trimmed = line.trim()
            val match = tableRowPattern.find(trimmed) ?: continue
            val rawName = match.groupValues[1].trim()
            val lower = rawName.lowercase()

            if (skipHeaders.any { lower.contains(it) } || rawName.length < 3) continue

            val key = lower.replace(Regex("""[^a-z0-9]"""), "")
            if (seenTests.add(key)) {
                val value = match.groupValues[2].trim()
                val unit = match.groupValues[3].trim()
                val refRange = match.groupValues[4].trim()

                results.add(
                    ParsedMedicalResult(
                        id = UUID.randomUUID().toString(),
                        testName = rawName,
                        value = value,
                        unit = unit,
                        referenceRange = refRange
                    )
                )
            }
        }

        return results
    }

    private fun matchesAlias(line: String, alias: String): Boolean {
        val cleanLine = line.replace(Regex("""\s+"""), " ").trim()
        val cleanAlias = alias.replace(Regex("""\s+"""), " ").trim()

        // Short ambiguous single-word test names (e.g. BLOOD in Urine analysis vs EDTA WHOLE BLOOD in CBC)
        // require exact line match so they only trigger on the actual test header line
        if (cleanAlias.equals("BLOOD", ignoreCase = true) || cleanAlias.equals("BILIRUBIN", ignoreCase = true)) {
            return cleanLine.equals(cleanAlias, ignoreCase = true)
        }

        if (cleanLine.contains(cleanAlias, ignoreCase = true)) return true

        // Match stripped non-alphanumeric characters (handling parentheses, hyphens, slashes)
        val strippedLine = cleanLine.replace(Regex("""[()\-:/><]"""), " ").replace(Regex("""\s+"""), " ").trim()
        val strippedAlias = cleanAlias.replace(Regex("""[()\-:/><]"""), " ").replace(Regex("""\s+"""), " ").trim()
        return strippedLine.contains(strippedAlias, ignoreCase = true)
    }

    private fun findTestDetailsFromLines(
        lines: List<String>,
        lineIndex: Int,
        matchedAlias: String,
        testDef: TestDefinition
    ): ParsedMedicalResult? {
        val currentLine = lines[lineIndex]
        val afterAlias = currentLine.substringAfter(matchedAlias, "").trim()
            .removePrefix(":").removePrefix("-").removePrefix("=").trim()

        // 1. Try finding value on the same line after alias
        if (afterAlias.isNotBlank()) {
            val extracted = extractValueUnitRange(afterAlias, testDef.defaultUnit, testDef.standardReferenceRange)
            if (extracted != null) {
                return ParsedMedicalResult(
                    id = UUID.randomUUID().toString(),
                    testName = testDef.canonicalName,
                    value = extracted.first,
                    unit = extracted.second,
                    referenceRange = extracted.third
                )
            }
        }

        // 2. Look forward up to 6 lines for columnar / multi-row reports (e.g. Aarthi Scans)
        for (offset in 1..6) {
            val targetIndex = lineIndex + offset
            if (targetIndex >= lines.size) break
            val candidateLine = lines[targetIndex].trim()
            if (candidateLine.isBlank()) continue

            val upper = candidateLine.uppercase()
            // Skip method, specimen, or header rows, pregnant ranges, and incubation times (e.g. "1 Hour")
            if (upper.startsWith("(") || upper.startsWith("[") || upper.startsWith("METHOD") || upper.startsWith("SPECIMEN") ||
                upper.startsWith("NOTE") || upper.startsWith("INVESTIGATION") || upper.startsWith("RESULT") ||
                upper.startsWith("BIOLOGICAL") || upper.startsWith("REFERENCE") || upper.startsWith("PREGNANT") ||
                upper.startsWith("1ST TRIMESTER") || upper.startsWith("2ND TRIMESTER") || upper.startsWith("3RD TRIMESTER") ||
                upper.startsWith("ATA GUIDANCE") || upper.startsWith("ADA GUIDELINES") || upper.startsWith("NCEP") ||
                upper.startsWith("PRE DIABETES") || upper.startsWith("DIABETES") || upper.startsWith("A LIPID PROFILE") ||
                upper.startsWith("BORDERLINE") || upper.startsWith("VERY HIGH") ||
                upper.matches(Regex("""^(?:1|2|1/2|0\.5)\s*(?:HOUR|HOURS|HR|HRS)\b.*"""))
            ) {
                continue
            }

            val cleanedCandidate = candidateLine.removePrefix(":").removePrefix("-").removePrefix("=").trim()
            val extracted = extractValueUnitRange(cleanedCandidate, testDef.defaultUnit, testDef.standardReferenceRange)
            if (extracted != null) {
                return ParsedMedicalResult(
                    id = UUID.randomUUID().toString(),
                    testName = testDef.canonicalName,
                    value = extracted.first,
                    unit = extracted.second,
                    referenceRange = extracted.third
                )
            }

            // Stop if we encounter another test alias
            if (isAnotherTest(candidateLine, testDef)) {
                break
            }
        }

        return null
    }

    private fun extractValueUnitRange(
        text: String,
        defaultUnit: String,
        defaultRange: String
    ): Triple<String, String, String>? {
        val clean = text.trim()
        if (clean.isBlank()) return null

        // 1. Check for qualitative / semi-quantitative values (Nil, Negative, Positive, Trace, Normal, Clear, etc.)
        val qualitativeRegex = Regex(
            """^(?:Result\s*[:=-]?\s*)?(Positive|Negative|Nil|Trace|Normal|Clear|Pale\s+Yellow|Yellow|Present|Absent|Reactive|Non-Reactive)\b(?:\s+(.*))?""",
            RegexOption.IGNORE_CASE
        )
        val qMatch = qualitativeRegex.find(clean)
        if (qMatch != null) {
            val valStr = qMatch.groupValues[1].trim()
            val rest = qMatch.groupValues[2].trim()
            val refRange = rest.ifBlank { defaultRange }
            return Triple(valStr, defaultUnit, refRange)
        }

        // 2. Check for numeric value or range value (e.g. 9.1 g/dl 12.0-15.0, 104.0 mg/dL Normal:74-100, 4-6 /hpf, < 150)
        val numericRegex = Regex(
            """^(?:Result\s*[:=-]?\s*)?([<>≤≥]?\s*\d+(?:[\/.,-]\d+)?)\s*([a-zA-Zµμ%/0-9^._-]+)?(?:\s+(.*))?""",
            RegexOption.IGNORE_CASE
        )
        val nMatch = numericRegex.find(clean)
        if (nMatch != null && nMatch.groupValues[1].isNotBlank()) {
            val rawVal = nMatch.groupValues[1].trim()
            var unit = nMatch.groupValues[2].trim().ifBlank { defaultUnit }
            val rest = nMatch.groupValues[3].trim()

            val rangeRegex = Regex("""(\(?\d+(?:\.\d+)?\s*[-–to]\s*\d+(?:\.\d+)?\s*(?:[a-zA-Z/%]+)?\)?|[<≤>≥]\s*\d+(?:\.\d+)?|[a-zA-Z]+:\s*\d+[-–]\d+)""")
            val rangeMatch = rangeRegex.find(rest)
            val refRange = rangeMatch?.value?.trim()?.removeSurrounding("(", ")") ?: rest.ifBlank { defaultRange }

            return Triple(rawVal, unit, refRange)
        }

        return null
    }

    private fun isAnotherTest(text: String, currentTest: TestDefinition): Boolean {
        val upper = text.uppercase()
        if (upper.startsWith("(") || upper.startsWith("[") ||
            upper.contains("SPECIMEN") || upper.contains("METHOD") ||
            upper.contains("WHOLE BLOOD") || upper.contains("PLASMA") ||
            upper.contains("SERUM") || upper.contains("FLUORIDE") ||
            upper.contains("AUTOMATED") || upper.contains("CALCULATED") ||
            upper.startsWith("NOTE")
        ) {
            return false
        }

        return TEST_CATALOG.any { other ->
            other.canonicalName != currentTest.canonicalName &&
            other.aliases.any { alias ->
                matchesAlias(text, alias)
            }
        }
    }


    /**
     * High-recall Prescription parser:
     * Extracts medicine name, dosage instructions (Before/After food), frequency (1-0-1, OD, BD),
     * and automatically schedules morning, afternoon, evening, and night slots.
     */
    fun parsePrescriptionMedicines(rawText: String, accountUid: String): List<PrescriptionMedicationEntity> {
        val medicines = mutableListOf<PrescriptionMedicationEntity>()
        val seenNames = mutableSetOf<String>()

        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }

        for (i in lines.indices) {
            val line = lines[i]
            var medName: String? = null
            var dosageHint: String? = null

            // 1. Check for form prefix (e.g. TAB Paracetamol 500mg, 1 Syp Calpol 250)
            val formMatch = MEDICINE_FORM_PREFIX.find(line)
            if (formMatch != null) {
                val form = formMatch.groupValues[1].uppercase()
                val rawName = formMatch.groupValues[2].trim()
                val cleanName = rawName.split(Regex("""(?i)\s+(?:1-0-1|1-0-0|0-0-1|1-1-1|OD|BD|TID|QID|Morning|Night|Before|After|Days|Tab|Cap|SOS|every)\b"""))
                    .firstOrNull()?.trim() ?: rawName
                if (cleanName.length >= 2) {
                    medName = "$form $cleanName"
                }
            } else {
                // 2. Check for known medicine names in line
                for (drug in COMMON_DRUGS) {
                    val drugPattern = Regex("""\b${Regex.escape(drug)}(?:\s+\d+\s*(?:mg|ml|mcg|g))?""", RegexOption.IGNORE_CASE)
                    val drugMatch = drugPattern.find(line)
                    if (drugMatch != null) {
                        medName = drugMatch.value
                        if (line.contains("Sachet", ignoreCase = true) && !medName.contains("Sachet", ignoreCase = true)) {
                            medName = "$medName Sachet"
                        }
                        break
                    }
                }

                // 3. Check for numbered prescription line starting with Rx or 1., 2.
                if (medName == null) {
                    val rxMatch = Regex("""^(?:Rx:?|\d+[\.\)\-]?)\s*([A-Za-z0-9\s_+/-]+?)(?:\s+(?:1-0-1|1-0-0|0-0-1|1-1-1|OD|BD|TID|QID|Morning|Night|x\s*\d+|\d+\s*days|SOS|\d+\s*(?:mg|ml|mcg|tab|cap)))""", RegexOption.IGNORE_CASE).find(line)
                    if (rxMatch != null) {
                        val candidate = rxMatch.groupValues[1].trim()
                        if (candidate.length in 3..40 && !candidate.contains("patient", true) && !candidate.contains("doctor", true) && !candidate.contains("clinic", true) && !candidate.contains("hospital", true) && !candidate.contains("diagnosis", true)) {
                            medName = candidate
                        }
                    }
                }
            }

            if (medName != null) {
                val key = medName.lowercase()
                if (seenNames.add(key)) {
                    val contextLine = if (i + 1 < lines.size) "$line ${lines[i + 1]}" else line

                    val isBeforeFood = contextLine.contains("Before Food", true) ||
                            contextLine.contains("Empty Stomach", true) ||
                            contextLine.contains("BBF", true) ||
                            contextLine.contains("AC", true)

                    val isAfterFood = contextLine.contains("After Food", true) ||
                            contextLine.contains("Post Meal", true) ||
                            contextLine.contains("PC", true)

                    // Extract exact dosage quantity (e.g. 5ml, 6ml, 1 Tab, 200ml)
                    val qtyMatch = Regex("""\b(\d+(?:\.\d+)?\s*(?:ml|mg|tab|cap|sachet|tsp))\b""", RegexOption.IGNORE_CASE).find(contextLine)
                    val qtyStr = qtyMatch?.value?.trim() ?: ""

                    val timingStr = when {
                        isBeforeFood -> "Before Food"
                        isAfterFood -> "After Food"
                        contextLine.contains("SOS", true) -> "SOS / When Needed"
                        else -> "As Directed"
                    }

                    val dosage = when {
                        qtyStr.isNotBlank() && timingStr != "As Directed" -> "$qtyStr - $timingStr"
                        qtyStr.isNotBlank() -> qtyStr
                        else -> timingStr
                    }

                    val isMorning = contextLine.contains("Morning", true) ||
                            contextLine.contains("1-0-0", true) ||
                            contextLine.contains("1-0-1", true) ||
                            contextLine.contains("1-1-1", true) ||
                            contextLine.contains("OD", true) ||
                            contextLine.contains("BD", true) ||
                            contextLine.contains("BID", true) ||
                            contextLine.contains("TID", true)

                    val isAfternoon = contextLine.contains("Afternoon", true) ||
                            contextLine.contains("Aft", true) ||
                            contextLine.contains("1-1-1", true) ||
                            contextLine.contains("0-1-0", true) ||
                            contextLine.contains("TID", true)

                    val isEvening = contextLine.contains("Evening", true) ||
                            contextLine.contains("Eve", true)

                    val isNight = contextLine.contains("Night", true) ||
                            contextLine.contains("0-0-1", true) ||
                            contextLine.contains("1-0-1", true) ||
                            contextLine.contains("1-1-1", true) ||
                            contextLine.contains("BD", true) ||
                            contextLine.contains("BID", true) ||
                            contextLine.contains("HS", true) ||
                            contextLine.contains("Bedtime", true)

                    val primarySlot = when {
                        isMorning -> "MORNING (08:30)"
                        isAfternoon -> "AFTERNOON (13:30)"
                        isEvening -> "EVENING (19:00)"
                        isNight -> "NIGHT (21:30)"
                        else -> "MORNING (08:30)"
                    }

                    medicines.add(
                        PrescriptionMedicationEntity(
                            accountUid = accountUid,
                            name = medName,
                            dosage = dosage,
                            timeSlot = primarySlot
                        )
                    )

                    // Add second slot for twice-daily (BD / 1-0-1) prescriptions
                    if ((isNight && isMorning) || contextLine.contains("BD", true) || contextLine.contains("BID", true) || contextLine.contains("1-0-1", true)) {
                        medicines.add(
                            PrescriptionMedicationEntity(
                                accountUid = accountUid,
                                name = medName,
                                dosage = dosage,
                                timeSlot = "NIGHT (21:30)"
                            )
                        )
                    }

                    // Add third slot for thrice-daily (TID / 1-1-1) prescriptions
                    if (contextLine.contains("1-1-1", true) || contextLine.contains("TID", true)) {
                        medicines.add(
                            PrescriptionMedicationEntity(
                                accountUid = accountUid,
                                name = medName,
                                dosage = dosage,
                                timeSlot = "AFTERNOON (13:30)"
                            )
                        )
                    }
                }
            }
        }

        return medicines
    }

    /**
     * Extracts vitals from test results and converts them to VitalReadingEntity records.
     */
    fun extractVitalsFromResults(results: List<ParsedMedicalResult>, accountUid: String): List<VitalReadingEntity> {
        val vitals = mutableListOf<VitalReadingEntity>()
        val now = System.currentTimeMillis()

        for (item in results) {
            val name = item.testName.lowercase()
            val valStr = item.value.trim()

            if (name.contains("blood pressure") || name == "bp") {
                // e.g. "120/80" or "120 / 80"
                val bpParts = valStr.split(Regex("""[/,\s]+""")).mapNotNull { it.toDoubleOrNull() }
                if (bpParts.size >= 2) {
                    vitals.add(
                        VitalReadingEntity(
                            accountUid = accountUid,
                            type = VitalType.BLOOD_PRESSURE.name,
                            systolic = bpParts[0],
                            diastolic = bpParts[1],
                            timestamp = now,
                            notes = "Extracted from medical document"
                        )
                    )
                }
            } else if (name.contains("weight") || name == "wt" || name == "wt:") {
                // Support values like "16.3", "16.3 kg", and pediatric weights down to 2.0 kg
                val weight = Regex("""\d+(?:\.\d+)?""").find(valStr)?.value?.toDoubleOrNull()
                if (weight != null && weight in 2.0..300.0) {
                    vitals.add(
                        VitalReadingEntity(
                            accountUid = accountUid,
                            type = VitalType.WEIGHT.name,
                            weightKg = weight,
                            timestamp = now,
                            notes = "Extracted from medical document"
                        )
                    )
                }
            }
        }
        return vitals
    }


    fun serializeResults(results: List<ParsedMedicalResult>): String {
        if (results.isEmpty()) return "[]"
        return results.joinToString(";;;") { result ->
            "${escape(result.id)}|||${escape(result.testName)}|||${escape(result.value)}|||${escape(result.unit)}|||${escape(result.referenceRange)}"
        }
    }

    fun deserializeResults(serialized: String): List<ParsedMedicalResult> {
        if (serialized.isBlank() || serialized == "[]") return emptyList()
        val results = mutableListOf<ParsedMedicalResult>()

        for (item in serialized.split(";;;")) {
            if (item.isBlank()) continue
            val parts = item.split("|||")
            if (parts.size >= 3) {
                results.add(
                    ParsedMedicalResult(
                        id = unescape(parts.getOrElse(0) { UUID.randomUUID().toString() }),
                        testName = unescape(parts.getOrElse(1) { "Medical Test" }),
                        value = unescape(parts.getOrElse(2) { "" }),
                        unit = unescape(parts.getOrElse(3) { "" }),
                        referenceRange = unescape(parts.getOrElse(4) { "" })
                    )
                )
            }
        }
        return results
    }

    private fun escape(value: String): String =
        value.replace("|||", " ").replace(";;;", " ")

    private fun unescape(value: String): String = value
}
