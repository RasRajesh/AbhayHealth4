package com.example.abhayhealth.feature.records

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import com.example.abhayhealth.domain.model.ParsedMedicalResult

class MedicalFieldParserTest {

    @Test
    fun testParseAarthiScansSampleReport() {
        val ocrSample = """
            AARTHI SCANS & LABS
            Name : R MAMATHA
            PID : 0102315363 SID No. : 124014712
            Age / Sex : 28 Y / Female Reg Date & Time : 29/10/2026 10:06:31
            COMPLETE BLOOD COUNT(CBC) :EDTA WHOLE BLOOD
            RBC (Red Blood Cell Count)
            ( Method : WB/Automated)
            ( Specimen: EDTA WHOLE BLOOD)
            4.23 Million/cmm 3.8-4.8
            Haemoglobin (HB)
            ( Method : WB/Automated)
            ( Specimen: EDTA WHOLE BLOOD)
            9.1 g/dl 12.0-15.0
            PCV -( Haematocrit-Packed Cell Volume)
            ( Method : WB/Automated)
            29.6 % 36-46
            MCV (Mean Corpuscular Volume)
            ( Method : WB/Automated)
            69.9 fl 83-101
            MCH (Mean Corpuscular Hemoglobin )
            ( Method : Calculated)
            21.4 pg 27-31
            MCHC (Mean Corpuscular Hemoglobin Concentration)
            ( Method : WB/Automated)
            30.7 % 31.5-34.5
            Total WBC Count
            ( Method : WB/Automated)
            ( Specimen: EDTA WHOLE BLOOD)
            7180 cells/cumm 4000-10000
            Neutrophils
            57.1 % 40-70
            Lymphocytes
            31.0 % 20-40
            Monocytes
            5.2 % 2-8
            Eosinophils
            5.2 % 1-6
            Basophils
            0.7 % 0-1
            Immature Granulocyte Percentage(IG%)
            0.8 % 0-0.5
            Platelet Count
            ( Method : WB/Automated)
            3.36 Lakhs/cumm 1.5-4.5
            Erythrocyte Sedimentation Rate(ESR)
            17 mm/hr <20
            RDW-CV(Red Cell Distribution Width-CV)
            14.2 % 11.6-14.0
            RDW-SD(Red Cell Distribution Width-SD)
            43.0 fl 39-46
            MPV (Mean Platelet Volume)
            9.0 fl 6.5-12.0
            PDW(Platelet Distribution Width)
            16.0 % 9-14
            PCT(Plateletcrit)
            0.31 % 0.19-0.39
            P-LCR(Platelet Large Cell Ratio)
            22 % 15.0-43.0
            Absolute Neutrophil Count
            4000 cells/cumm 2000-7000
            Absolute Lymphocyte Count
            2100 cells/cumm 1000-3000
            Absolute Eosinophil Count
            370 cells/cumm 20-500
            Absolute Monocyte Count
            370 cells/cumm 200-1000
            Absolute Basophil Count
            37 cells/cumm 20-100
            GLUCOSE FASTING (FBS)
            ( Method : Glucose Oxidase - Peroxidase)
            104.0 mg/dL Normal:74-100
            UREA
            ( Method : Urease)
            20.2 mg/dL 13-45
            CREATININE
            ( Method : Creatinine amidohydrolase)
            0.5 mg/dL 0.5-1.1
            URIC ACID
            4.2 mg/dL 2.5-6.7
            CHOLESTEROL TOTAL
            128.0 mg/dL <200
            HDL CHOLESTEROL
            33.5 mg/dL 50-60
            LDL CHOLESTEROL
            72.5 mg/dL <100
            TRIGLYCERIDES
            110.0 mg/dL <150
            VLDL CHOLESTEROL
            22.0 mg/dL <30
            NON-HDL CHOLESTEROL
            94.5 mg/dL <130
            CHOL / HDL RATIO
            3.8 3.3-4.4
            LDL / HDL RATIO
            2.1 1.5-3.5
            BILIRUBIN TOTAL
            0.34 mg/dL 0.2-1.2
            BILIRUBIN DIRECT
            0.15 mg/dL 0.0-0.3
            BILIRUBIN INDIRECT
            0.19 mg/dL 0.1-0.9
            Aspartate aminotransferase(AST/SGOT)
            14.6 U/L 5-34
            Alanine aminotransferase(ALT/SGPT)
            7.9 U/L 5-35
            ALKALINE PHOSPHATASE
            57.2 U/L 42-98
            TOTAL PROTEIN
            7.01 g/dL 6.0-8.0
            ALBUMIN
            4.22 g/dL 3.5-5.2
            GLOBULIN
            2.79 g/dL 2.8-3.5
            A/G RATIO
            1.51 1.1-2.5
            GAMMA GT
            12.0 U/L 5-38
            Total-T3 (Triiodothyronine-T3)
            108.0 ng/dl 70-204
            Total-T4 (Thyroxine T4 )
            5.9 ug/dl 4.5-11.7
            TSH (Thyroid-stimulating hormone)-Ultra
            4.520 uIU/ml 0.4-4.5
            PUS CELLS
            2-4 /hpf 0-5
            EPITHELIAL CELLS
            4-6 /hpf 0-5
            RBCES
            4-6 /hpf 0-2
            CRYSTALS
            Nil
            HYALINE CASTS
            Nil
            COLOUR
            Pale Yellow
            CLARITY
            Clear
            CHEMICAL EXAMINATION PH
            6.0 4.5-8.0
            SPECIFIC GRAVITY
            1.015 1.005-1.030
            GLUCOSE - URINE
            Nil
            PROTEIN
            Trace
            NITRITE
            Negative
            KETONE
            Negative
            UROBILINOGEN
            Normal
            BLOOD
            Trace
        """.trimIndent()

        val results = MedicalFieldParser.parseTestResults(ocrSample)
        val names = results.map { it.testName }.toSet()

        println("Extracted ${results.size} tests: $names")
        assertTrue("Extracted count should be >= 45, was ${results.size}", results.size >= 45)

        // Essential Clinical Biomarkers
        assertTrue("Should extract Hemoglobin", names.contains("Hemoglobin"))
        assertTrue("Should extract Platelet Count", names.contains("Platelet Count"))
        assertTrue("Should extract Fasting Blood Glucose", names.contains("Fasting Blood Glucose"))
        assertTrue("Should extract Serum Creatinine", names.contains("Serum Creatinine"))
        assertTrue("Should extract Total Cholesterol", names.contains("Total Cholesterol"))
        assertTrue("Should extract TSH", names.contains("TSH"))
        assertTrue("Should extract Total T3", names.contains("Total T3"))
        assertTrue("Should extract Total T4", names.contains("Total T4"))
        assertTrue("Should extract Total Bilirubin", names.contains("Total Bilirubin"))
        assertTrue("Should extract SGOT / AST", names.contains("SGOT / AST"))
        assertTrue("Should extract SGPT / ALT", names.contains("SGPT / ALT"))
        assertTrue("Should extract Alkaline Phosphatase", names.contains("Alkaline Phosphatase"))
        assertTrue("Should extract Total Protein", names.contains("Total Protein"))
        assertTrue("Should extract Albumin", names.contains("Albumin"))
        assertTrue("Should extract Globulin", names.contains("Globulin"))
        assertTrue("Should extract A/G Ratio", names.contains("A/G Ratio"))
        assertTrue("Should extract Triglycerides", names.contains("Triglycerides"))
        assertTrue("Should extract HDL Cholesterol", names.contains("HDL Cholesterol"))
        assertTrue("Should extract LDL Cholesterol", names.contains("LDL Cholesterol"))
        assertTrue("Should extract Non-HDL Cholesterol", names.contains("Non-HDL Cholesterol"))
        assertTrue("Should extract Uric Acid", names.contains("Uric Acid"))
        assertTrue("Should extract Blood Urea", names.contains("Blood Urea"))
        assertTrue("Should extract ESR", names.contains("ESR"))
        assertTrue("Should extract Neutrophils", names.contains("Neutrophils"))
        assertTrue("Should extract Lymphocytes", names.contains("Lymphocytes"))
        assertTrue("Should extract Monocytes", names.contains("Monocytes"))
        assertTrue("Should extract Eosinophils", names.contains("Eosinophils"))
        assertTrue("Should extract Basophils", names.contains("Basophils"))
        assertTrue("Should extract Immature Granulocytes", names.contains("Immature Granulocytes"))
        assertTrue("Should extract RDW-CV", names.contains("RDW-CV"))
        assertTrue("Should extract RDW-SD", names.contains("RDW-SD"))
        assertTrue("Should extract MPV", names.contains("MPV"))
        assertTrue("Should extract PDW", names.contains("PDW"))
        assertTrue("Should extract PCT", names.contains("PCT (Plateletcrit)"))
        assertTrue("Should extract P-LCR", names.contains("P-LCR"))
        assertTrue("Should extract Urine Pus Cells", names.contains("Urine Pus Cells"))
        assertTrue("Should extract Urine Epithelial Cells", names.contains("Urine Epithelial Cells"))
        assertTrue("Should extract Urine RBCs", names.contains("Urine RBCs"))
        assertTrue("Should extract Urine Specific Gravity", names.contains("Urine Specific Gravity"))
        assertTrue("Should extract Urine pH", names.contains("Urine pH"))
        assertTrue("Should extract Urine Protein", names.contains("Urine Protein"))
        assertTrue("Should extract Urine Glucose", names.contains("Urine Glucose"))
        assertTrue("Should extract Urine Nitrite", names.contains("Urine Nitrite"))
        assertTrue("Should extract Urine Ketone", names.contains("Urine Ketone"))

        val esr = results.firstOrNull { it.testName == "ESR" }
        assertNotNull("ESR result should be present", esr)
        assertEquals("17", esr?.value)

        val hb = results.firstOrNull { it.testName == "Hemoglobin" }
        assertNotNull("Hemoglobin result should be present", hb)
        assertEquals("9.1", hb?.value)

        val fbg = results.firstOrNull { it.testName == "Fasting Blood Glucose" }
        assertNotNull("FBG result should be present", fbg)
        assertEquals("104.0", fbg?.value)
    }

    @Test
    fun testParsePediatricPrescriptionSample() {
        val ocrPrescription = """
            SRINIVASA CHILDREN'S CLINIC
            Dr. D. Sreenivasulu Reddy M.B.B.S., D.C.H., NALS, PALS
            Name: HASHITHA Age: 4y Wt: 16.3 Date: 15.9.26
            CRP 8.2 mg/dl
            1 Syp CALPOL 250 5ml SOS every 6 hrs
            2 Syp ZIFI 100 6ml BD After food
            3 Electral Sachet 200ml water
        """.trimIndent()

        val category = MedicalFieldParser.parseDocumentCategory(ocrPrescription)
        assertEquals("PRESCRIPTION", category)

        val medicines = MedicalFieldParser.parsePrescriptionMedicines(ocrPrescription, "user_1")
        val medNames = medicines.map { it.name }
        println("Extracted medicines: $medNames")

        assertTrue("Should extract Calpol", medNames.any { it.contains("Calpol", ignoreCase = true) })
        assertTrue("Should extract Zifi", medNames.any { it.contains("Zifi", ignoreCase = true) })
        assertTrue("Should extract Electral", medNames.any { it.contains("Electral", ignoreCase = true) })

        val tests = MedicalFieldParser.parseTestResults(ocrPrescription)
        val vitals = MedicalFieldParser.extractVitalsFromResults(tests, "user_1")
        val weightVital = vitals.firstOrNull { it.type == "WEIGHT" }

        assertNotNull("Should extract Weight vital from prescription", weightVital)
        assertEquals(16.3, weightVital?.weightKg ?: 0.0, 0.01)
    }

    @Test
    fun testBiomarkerEvaluationAndOrganSystem() {
        val normalHb = ParsedMedicalResult(
            testName = "Haemoglobin (HB)",
            value = "13.5",
            unit = "g/dL",
            referenceRange = "12.0 - 15.0"
        )
        assertEquals(com.example.abhayhealth.domain.model.BiomarkerStatus.NORMAL, normalHb.status)
        assertEquals(com.example.abhayhealth.domain.model.OrganSystem.HEMIC, normalHb.organSystem)
        assertFalse(normalHb.isOutOfRange)

        val lowHb = ParsedMedicalResult(
            testName = "Haemoglobin (HB)",
            value = "9.2",
            unit = "g/dL",
            referenceRange = "12.0 - 15.0"
        )
        assertEquals(com.example.abhayhealth.domain.model.BiomarkerStatus.LOW, lowHb.status)
        assertTrue(lowHb.isOutOfRange)

        val highGlucose = ParsedMedicalResult(
            testName = "GLUCOSE FASTING (FBS)",
            value = "142",
            unit = "mg/dL",
            referenceRange = "70 - 100"
        )
        assertEquals(com.example.abhayhealth.domain.model.BiomarkerStatus.HIGH, highGlucose.status)
        assertEquals(com.example.abhayhealth.domain.model.OrganSystem.METABOLIC, highGlucose.organSystem)
        assertTrue(highGlucose.isOutOfRange)

        val highChol = ParsedMedicalResult(
            testName = "TOTAL CHOLESTEROL",
            value = "240",
            unit = "mg/dL",
            referenceRange = "< 200"
        )
        assertEquals(com.example.abhayhealth.domain.model.BiomarkerStatus.HIGH, highChol.status)
        assertEquals(com.example.abhayhealth.domain.model.OrganSystem.CARDIO, highChol.organSystem)

        val renalTest = ParsedMedicalResult(
            testName = "Serum Creatinine",
            value = "0.9",
            unit = "mg/dL",
            referenceRange = "0.6 - 1.2"
        )
        assertEquals(com.example.abhayhealth.domain.model.BiomarkerStatus.NORMAL, renalTest.status)
        assertEquals(com.example.abhayhealth.domain.model.OrganSystem.RENAL, renalTest.organSystem)

        val qualitativePus = ParsedMedicalResult(
            testName = "Urine Pus Cells",
            value = "4-6 /hpf",
            referenceRange = "0 - 5 /hpf"
        )
        assertEquals(com.example.abhayhealth.domain.model.OrganSystem.RENAL, qualitativePus.organSystem)

        val urineProteinAbnormal = ParsedMedicalResult(
            testName = "Urine Protein",
            value = "Trace (+)",
            referenceRange = "Nil"
        )
        assertEquals(com.example.abhayhealth.domain.model.BiomarkerStatus.QUALITATIVE_ABNORMAL, urineProteinAbnormal.status)
        assertTrue(urineProteinAbnormal.isOutOfRange)
    }

    @Test
    fun testMedicationAdvisoryHelper() {
        val calpolGuidance = com.example.abhayhealth.feature.medicines.MedicationAdvisoryHelper.getGuidance("Syp Calpol 250")
        assertNotNull(calpolGuidance)
        assertTrue(calpolGuidance!!.foodInstruction.contains("food", ignoreCase = true))

        val zifiGuidance = com.example.abhayhealth.feature.medicines.MedicationAdvisoryHelper.getGuidance("Syp Zifi 100")
        assertNotNull(zifiGuidance)
        assertTrue(zifiGuidance!!.tip.contains("Antibiotic", ignoreCase = true))

        val electralGuidance = com.example.abhayhealth.feature.medicines.MedicationAdvisoryHelper.getGuidance("Electral Sachet")
        assertNotNull(electralGuidance)
        assertTrue(electralGuidance!!.tip.contains("Rehydration", ignoreCase = true))
    }

    @Test
    fun testComprehensiveLabTestExtraction() {
        val labReportText = """
            APOLLO DIAGNOSTICS
            Patient: John Doe, Age: 45
            Date: 12-Nov-2026
            
            INVESTIGATION           RESULT   UNITS    REFERENCE INTERVAL
            Serum Chloride          102.0    mEq/L    98 - 107
            Serum Magnesium         2.1      mg/dL    1.7 - 2.2
            Serum Iron              85.0     ug/dL    60 - 170
            Total Iron Binding Capacity 320  ug/dL    240 - 450
            D-Dimer                 0.35     mg/L     < 0.50
            Troponin-I              0.02     ng/mL    < 0.04
            Serum Amylase           65       U/L      28 - 100
            Dengue NS1 Antigen      Negative          Negative
        """.trimIndent()

        val results = MedicalFieldParser.parseTestResults(labReportText)
        assertTrue("Should extract all lab parameters", results.size >= 8)

        val chloride = results.firstOrNull { it.testName.contains("Chloride", ignoreCase = true) }
        assertNotNull(chloride)
        assertEquals("102.0", chloride!!.value)
        assertEquals("mEq/L", chloride.unit)

        val dDimer = results.firstOrNull { it.testName.contains("D-Dimer", ignoreCase = true) }
        assertNotNull(dDimer)
        assertEquals("0.35", dDimer!!.value)

        val troponin = results.firstOrNull { it.testName.contains("Troponin", ignoreCase = true) }
        assertNotNull(troponin)
        assertEquals("0.02", troponin!!.value)
    }

    @Test
    fun testRxPrescriptionNumberedExtraction() {
        val prescriptionText = """
            DR. SHARMA'S CLINIC
            Date: 03/11/2026
            Rx:
            1. Tab Pan-D 40mg 1-0-0 Before Food x 15 days
            2. Cap Amoxicillin 500mg 1-0-1 After Food x 5 days
            3. Tab Dolo 650 1-0-1 SOS for fever
            4. Syp Grilinctus 10ml TDS
        """.trimIndent()

        val medicines = MedicalFieldParser.parsePrescriptionMedicines(prescriptionText, "user_test")
        assertTrue("Should extract all prescribed medicines", medicines.isNotEmpty())

        val panD = medicines.firstOrNull { it.name.contains("Pan", ignoreCase = true) }
        assertNotNull(panD)
        assertTrue(panD!!.dosage.contains("Before Food", ignoreCase = true))

        val amox = medicines.firstOrNull { it.name.contains("Amoxicillin", ignoreCase = true) }
        assertNotNull(amox)

        val dolo = medicines.firstOrNull { it.name.contains("Dolo", ignoreCase = true) }
        assertNotNull(dolo)
    }

    @Test
    fun testFullAarthiScans8PageReport() {
        val page1 = """
            Name : R MAMATHA
            PID : 0102315363 SID No. : 124014712
            Age / Sex : 28 Y / Female Reg Date & Time : 29/10/2026 10:06:31
            Referrer : Self Coll Date & Time : 29/10/2026 10:35:19
            Branch : HOME COLLECTION HYD Report Date & Time : 29/10/2026 12:05:07
            Bill No : 124-014717-26
            Final Test Report
            INVESTIGATION / METHOD RESULT UNITS BIOLOGICAL REFERENCE INTERVAL
            HAEMATOLOGY
            ISWARYAM-RUBY - MHC
            COMPLETE BLOOD COUNT(CBC) :EDTA WHOLE BLOOD
            RBC (Red Blood Cell Count)
            ( Method : WB/Automated)
            ( Specimen: EDTA WHOLE BLOOD)
            4.23 Million/cmm 3.8-4.8
            Haemoglobin (HB)
            ( Method : WB/Automated)
            ( Specimen: EDTA WHOLE BLOOD)
            9.1 g/dl 12.0-15.0
            PCV -( Haematocrit-Packed Cell Volume)
            ( Method : WB/Automated)
            29.6 % 36-46
            MCV (Mean Corpuscular Volume)
            ( Method : WB/Automated)
            69.9 fl 83-101
            MCH (Mean Corpuscular Hemoglobin )
            ( Method : Calculated)
            21.4 pg 27-31
            MCHC (Mean Corpuscular Hemoglobin
            Concentration)
            ( Method : WB/Automated)
            30.7 % 31.5-34.5
            Total WBC Count
            ( Method : WB/Automated)
            ( Specimen: EDTA WHOLE BLOOD)
            7180 cells/cumm 4000-10000
            DIFFERENTIAL COUNT(DC)
            (Optical(light scatter)Microscopy)
            Neutrophils 52.80 % 40-80
            Lymphocytes 36.30 % 20-40
            Monocytes 5.10 % 2-8
            Eosinophils 5.10 % 1-6
            Basophils 0.70 % 0-1
            IMMATURE GRANULOCYTE
            PERCENTAGE(IG%)
            ( Method : Fluorescent Flow Cytometry-SF Cube)
            0.1 % 0 - 0.4
            Page : 1 of 8
        """.trimIndent()

        val page2 = """
            Platelet Count
            ( Method : WB/Automated)
            ( Specimen: EDTA WHOLE BLOOD)
            3.36 Lakhs/cumm 1.5-4.5
            Erythrocyte Sedimentation Rate(ESR)
            (Westergran Method)
            1 Hour
            ( Specimen: EDTA WHOLE BLOOD)
            17 mm/hr <12.0
            Absolute Basophils Count
            ( Method : Fluorescent Flow Cytometry-SF Cube)
            50 cells/cumm 20-100
            Absolute Eosinophil Count
            ( Method : WB/Automated)
            370 cells/cumm 20-500
            Absolute Lymphocyte Count
            ( Method : Fluorescent Flow Cytometry-SF Cube)
            2600 cells/cumm 1000-3000
            Absolute Monocytes Count
            ( Method : Fluorescent Flow Cytometry-SF Cube)
            370 cells/cumm 200-1000
            Absolute Neutrophil Count
            ( Method : Fluorescent Flow Cytometry-SF Cube)
            3790 cells/cumm 2000-7000
            NRBC(Nucleated Red Blood Cells)
            ( Method : Fluorescent Flow Cytometry-SF Cube)
            0.00 % <0
            ANRBC(Absolute Nucleated Red Blood Cells)
            ( Method : Fluorescent Flow Cytometry-SF Cube)
            0.00 cells/cumm <0
            RDW-CV(Red Cell Distribution Width-CV)
            ( Method : Derived from RBC Histogram)
            17 % 11.6-14.0
            RDW-SD(Red Cell Distribution Width-SD)
            ( Method : Derived from RBC Histogram)
            43 fl 39-46
            MPV (Mean Platelet Volume)
            ( Method : Derived from Platelet Histogram)
            9 fl 6.5-12.0
            PDW(Platelet Distribution Width)
            ( Method : Derived from Platelet Histogram)
            16 % 9-14
            Notes : *Unit and reference range changed as per hematology analyzer reporting
            PCT(Plateletcrit)
            ( Method : Calculated)
            0.31 % 0.19-0.39
            PLCR(Platelet to Large Cell Ratio) 22 % 19.7-42.4
            Page : 2 of 8
        """.trimIndent()

        val page3 = """
            BIOCHEMISTRY.
            ISWARYAM-RUBY - MHC
            GLUCOSE FASTING (FBS)
            ( Method : Glucose Oxidase - Peroxidase)
            ( Specimen: FLUORIDE PLASMA)
            104.0 mg/dL Normal:74-100
            Pre Diabetes:100-125
            Diabetes :>=126
            ADA guidelines
            BIOCHEMISTRY
            ISWARYAM-RUBY - MHC
            RENAL PROFILE(RFT or KFT)
            UREA
            ( Method : Urease)
            ( Specimen: SERUM)
            20.2 mg/dL 13-45
            CREATININE
            ( Method : Creatinine amidohydrolase)
            ( Specimen: SERUM)
            0.5 mg/dL 0.5-1.1
            URIC ACID
            ( Method : Uricase)
            ( Specimen: SERUM)
            4.2 mg/dL 2.5-6.7
            LIPID PROFILE
            CHOLESTEROL TOTAL
            ( Method : Cholesterol Oxidase,esterase,Peroxidase)
            ( Specimen: SERUM)
            128.0 mg/dL Desirable :<200
            Borderline high :200-239
            High :>240
            HDL CHOLESTEROL
            ( Method : Direct)
            ( Specimen: SERUM)
            33.5 mg/dL 50-60
            NCEP ATP III guidelines
            Notes : NCEP RECOMMENDATIONS : Low :<40 mg/dL High:>=60 mg/dl
            LDL CHOLESTEROL
            ( Method : Direct)
            ( Specimen: SERUM)
            85 mg/dL Optimal :<100
            Near Optimal/above Optimal:100-
            129
            Borderline high :130-159
            High :159-189
            VeryHigh :>190
            NCEP ATP III guidelines
            Page : 3 of 8
        """.trimIndent()

        val page4 = """
            TRIGLYCERIDES
            ( Method : Lipase/Glycerol Dehydrogenase)
            ( Specimen: SERUM)
            110.0 mg/dL Normal :<150 mg/dl
            Borderline high:150-199 mg/dl
            High :200-499 mg/dl
            very high :>500 mg/dl
            NCEP ATP III guidelines
            Note: When serum Triglycerides are > 400 mg/dL, VLDL cholesterol calculation is not applicable, as the Friedewald
            formula is not valid beyond this range. Hence, VLDL value is not reported.
            VLDL CHOLESTEROL
            ( Method : Calculation)
            ( Specimen: SERUM)
            22.0 mg/dL <30
            NON-HDL CHOLESTEROL
            ( Method : Calculation)
            ( Specimen: SERUM)
            94.5 mg/dL <130
            CHO / HDL RATIO
            ( Method : Calculation)
            ( Specimen: SERUM)
            3.8 Ratio Optimal<3.5
            Goal <5.0
            LDL/HDL RATIO
            ( Specimen: SERUM)
            2.5 Ratio 1.5-3.5
            NCEP ATP III guidelines
            TGL/HDL RATIO
            ( Method : Calculated)
            ( Specimen: SERUM)
            3.3 Ratio Ideal :<2.0
            High risk :>4.0
            Very high risk:6.0
            A lipid profile provides assessment of atherogenic risk and guides cardiovascular risk management.Borderline and high
            values should be correlated with patient’s clinical history, risk factors (diabetes, hypertension, smoking, family history),
            and other investigations. Management includes lifestyle modification, dietary regulation, physical activity, and
            pharmacotherapy (e.g., statins, fibrates, niacin, PCSK9 inhibitors) where indicated.
            LIVER FUNCTION TEST (With GGT )
            BILIRUBIN TOTAL
            ( Method : Diazo)
            ( Specimen: SERUM)
            0.34 mg/dL <1.2
            BILIRUBIN DIRECT
            ( Method : Diazo)
            ( Specimen: SERUM)
            0.15 mg/dL 0-0.3
            BILIRUBIN INDIRECT
            ( Method : Diazo)
            ( Specimen: SERUM)
            0.19 mg/dL 0.1 - 0.9
            Aspartate aminotransferase(AST/SGOT)
            ( Method : IFCC)
            ( Specimen: SERUM)
            12.5 U/L 5-30
            Alanine aminotransferase(ALT/SGPT)
            ( Method : IFCC)
            ( Specimen: SERUM)
            7.9 U/L 5-35
            Page : 4 of 8
        """.trimIndent()

        val page5 = """
            ALKALINE PHOSPHATASE
            ( Method : AMP)
            ( Specimen: SERUM)
            57.2 U/L 42-98
            TOTAL PROTEIN
            ( Method : Biuret)
            ( Specimen: SERUM)
            7.01 g/dl 6.0-8.0
            ALBUMIN
            ( Method : Bromocresol Green(BCG))
            ( Specimen: SERUM)
            4.22 g/dl 3.5-5.2
            GLOBULIN
            ( Method : Calculation)
            ( Specimen: SERUM)
            2.79 g/dl 2.8-3.5
            A/G RATIO
            ( Specimen: SERUM)
            1.5 Ratio 1.1-2.5
            AST/ALT
            ( Method : Calculated)
            ( Specimen: SERUM)
            1.6 % 1-1.19
            GAMMA GT
            ( Method : Glutamyltransferase)
            ( Specimen: SERUM)
            12.0 U/L 5-35
            Page : 5 of 8
        """.trimIndent()

        val page6 = """
            CLINICAL PATHOLOGY
            ISWARYAM-RUBY - MHC
            URINE COMPLETE ANALYSIS (CUE)-URINE
            PHYSICAL EXAMINATION
            COLOUR
            ( Method :Visual Inspection)
            Yellow Pale Yellow
            CLARITY
            ( Method :Visual Inspection)
            Clear Clear
            CHEMICAL EXAMINATION
            PH
            ( Method :MethylRed sodiumsalt, bromothymol blue)
            6 4.6 - 8.0
            [4.6-6.9 Acidic
            7.0 Neutral
            7.1-9.0 Alkaline]
            SPECIFIC GRAVITY
            ( Method :Bromothymol Blue indicator)
            1.025 1.005-1.030
            GLUCOSE-URINE(F)
            ( Method :Glucose oxidase /Peroxidase)
            Nil Nil
            PROTEIN
            ( Method :Tetrabromophenol blue/ Sulphosalicylic Acid)
            Nil Negative
            NITRITE
            ( Method :p-arsanilicacid/diazonium)
            Negative Negative
            KETONE
            ( Method :Sodium nitroprusside reaction)
            Negative Negative
            BILIRUBIN
            ( Method :Diazonium)
            Negative Absent
            UROBILINOGEN
            ( Method :2,4-dichloroaniline diaznium salt)
            Normal Normal
            BLOOD
            ( Method :Pseudoperoxidase activity)
            Positive Negative
            MICROSCOPIC EXAMINATION
            PUS CELLS
            ( Method :Digital Microscopy)
            2-4 0-5/hpf
            Page : 6 of 8
        """.trimIndent()

        val page7 = """
            EPITHELIAL CELLS
            ( Method :Digital Microscopy)
            4-6 0-5/hpf
            RBC'S
            ( Method :Digital Microscopy)
            4-6 0-2/hpf
            CRYSTALS
            ( Method :Digital Microscopy)
            Nil Nil
            HYALINE CASTS
            ( Method :Digital Microscopy)
            Nil 0-2 hyaline cast /lpf
            PATHOLOGICAL CAST
            ( Method :Digital Microscopy)
            Nil Nil
            BACTERIA
            ( Method :Digital Microscopy)
            Nil None
            YEAST
            ( Method :Digital Microscopy)
            Nil None
            MUCUS
            ( Method :Digital Microscopy)
            Present Absent
            Page : 7 of 8
        """.trimIndent()

        val page8 = """
            IMMUNOLOGY
            ISWARYAM-RUBY - MHC
            THYROID PROFILE TEST - TOTAL
            Total-T3 (Triiodothyronine-T3)
            ( Method : CLIA)
            ( Specimen: SERUM)
            108.0 ng/dl 70-204
            Total-T4 (Thyroxine T4 )
            ( Method : CLIA)
            ( Specimen: SERUM)
            5.9 ug/dl 4.5-11.7
            TSH (Thyroid-stimulating hormone)-Ultra
            ( Method : CLIA)
            ( Specimen: SERUM)
            4.520 uIU/ml 0.4-4.5
            End of the Report
            DR.SYEDA FARHEEN OSMAN MBBS.,DCP
            Pathologist
            Page : 8 of 8
        """.trimIndent()

        val fullReport = listOf(page1, page2, page3, page4, page5, page6, page7, page8).joinToString("\n")
        val results = MedicalFieldParser.parseTestResults(fullReport)
        println("Extracted ${results.size} test results from 8 pages:")
        for (r in results) {
            println(" -> ${r.testName}: ${r.value} ${r.unit} (${r.referenceRange})")
        }

        assertTrue("Should extract at least 30 parameters from 8 pages", results.size >= 30)

        val urineBlood = results.firstOrNull { it.testName == "Urine Blood" }
        assertNotNull("Urine Blood result should be present", urineBlood)
        assertEquals("Positive", urineBlood?.value)

        val tsh = results.firstOrNull { it.testName == "TSH" }
        assertNotNull("TSH result should be present", tsh)
        assertEquals("4.520", tsh?.value)

        val totalT3 = results.firstOrNull { it.testName == "Total T3" }
        assertNotNull("Total T3 result should be present", totalT3)
        assertEquals("108.0", totalT3?.value)
    }

    @Test
    fun testPdfTextExtractorWithRawAndCompressedStreams() {
        val streamContent = "BT\n/F1 12 Tf\n(AARTHI SCANS & LABS) Tj\nT*\n(Haemoglobin: 12.5 g/dl) Tj\nET"
        val out = java.io.ByteArrayOutputStream()
        val deflater = java.util.zip.Deflater()
        deflater.setInput(streamContent.toByteArray(Charsets.ISO_8859_1))
        deflater.finish()
        val buf = ByteArray(1024)
        while (!deflater.finished()) {
            val count = deflater.deflate(buf)
            out.write(buf, 0, count)
        }
        deflater.end()
        val compressedBytes = out.toByteArray()

        val fakePdf = "1 0 obj\n<< /Length ${compressedBytes.size} /Filter /FlateDecode >>\nstream\n".toByteArray(Charsets.ISO_8859_1) +
                compressedBytes +
                "\nendstream\nendobj\n%%EOF".toByteArray(Charsets.ISO_8859_1)

        val extracted = PdfTextExtractor.extractTextFromPdfBytes(fakePdf)
        assertTrue("Should extract text from compressed PDF stream", extracted.contains("AARTHI SCANS & LABS"))
        assertTrue("Should extract Haemoglobin", extracted.contains("Haemoglobin: 12.5 g/dl"))
    }
}
