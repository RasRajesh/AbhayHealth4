package com.example.abhayhealth.feature.records

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.Deflater

class MedicalExtractionEngineUpgradeTest {

    @Test
    fun testClassificationValidMedicalPrescription() {
        val text = """
            SRI RAMA CLINIC
            Dr. A. Sharma M.D.
            Patient Name: Ramesh Age: 45 Y
            Date: 12/10/2026
            Rx:
            1. Tab Telma 40mg 1-0-0 Before Food
            2. Tab Metformin 500mg 1-0-1 After Food
        """.trimIndent()

        val classification = MedicalDocumentClassifier.classifyDocument(text)
        assertEquals(DocumentMedicalStatus.MEDICAL, classification.status)
        assertEquals("PRESCRIPTION", classification.detectedCategory)
    }

    @Test
    fun testClassificationValidLaboratoryReport() {
        val text = """
            APOLLO DIAGNOSTICS
            Patient Name: Sunita Devi
            COMPLETE BLOOD COUNT (CBC)
            Hemoglobin: 13.5 g/dL (12.0 - 16.0)
            Platelet Count: 2.5 Lakhs/cumm (1.5 - 4.5)
            Serum Creatinine: 0.9 mg/dL (0.6 - 1.2)
        """.trimIndent()

        val classification = MedicalDocumentClassifier.classifyDocument(text)
        assertEquals(DocumentMedicalStatus.MEDICAL, classification.status)
        assertEquals("LAB_REPORT", classification.detectedCategory)
    }

    @Test
    fun testClassificationNonMedicalBusinessDocument() {
        val text = """
            GLOBAL TRADING CORPORATION
            TAX INVOICE / PURCHASE ORDER
            Invoice Number: INV-2026-9912
            Bill To: Acme Enterprises
            Item 1: Office Chairs Qty: 10 Unit Price: $150
            Shipping Address: 100 Main Street, Suite 400
            Total Amount Due: $1,500.00
        """.trimIndent()

        val classification = MedicalDocumentClassifier.classifyDocument(text)
        assertEquals(DocumentMedicalStatus.NON_MEDICAL, classification.status)
    }

    @Test
    fun testClassificationNonMedicalSchoolCertificate() {
        val text = """
            ST. XAVIER HIGH SCHOOL
            ATTENDANCE REGISTER & MARK SHEET
            Student Name: John Doe Roll No: 42 Semester: 2
            Mathematics: 85/100 Physics: 90/100 Chemistry: 88/100
            Grade: A+
        """.trimIndent()

        val classification = MedicalDocumentClassifier.classifyDocument(text)
        assertEquals(DocumentMedicalStatus.NON_MEDICAL, classification.status)
    }

    @Test
    fun testMultiPageSyntheticPdfStreamExtraction() {
        val page1 = "BT /F1 12 Tf (COMPLETE BLOOD COUNT) Tj T* (Hemoglobin 13.2 g/dL 12.0-16.0) Tj ET"
        val page2 = "BT /F1 12 Tf (GLUCOSE FASTING 104.0 mg/dL 70-100) Tj ET"

        val pdfBytes = createTwoPageSyntheticPdf(page1, page2)

        val extractedText = PdfTextExtractor.extractTextFromPdfBytes(pdfBytes)
        assertTrue(extractedText.contains("COMPLETE BLOOD COUNT"))
        assertTrue(extractedText.contains("Hemoglobin 13.2 g/dL"))
        assertTrue(extractedText.contains("GLUCOSE FASTING"))

        val classification = MedicalDocumentClassifier.classifyDocument(extractedText)
        assertEquals(DocumentMedicalStatus.MEDICAL, classification.status)

        val results = MedicalFieldParser.parseTestResults(extractedText)
        assertTrue("Should extract biomarkers across pages", results.size >= 2)
    }

    @Test
    fun testLargePageProgressAndDeduplication() {
        val textPage1 = "Patient Name: Rajesh Kumar Age: 42 Y\nBlood Pressure: 130/80 mmHg\nPulse: 72 BPM"
        val textPage2 = "GLUCOSE FASTING (FBS): 104.0 mg/dL\nCHOLESTEROL TOTAL: 180.0 mg/dL"
        val textPage3 = "Rx:\n1. Tab Telma 40mg 1-0-0\n2. Tab Dolo 650 1-0-1"

        val combinedText = "$textPage1\n--- Page 2 ---\n$textPage2\n--- Page 3 ---\n$textPage3"

        val results = MedicalFieldParser.parseTestResults(combinedText)
        val medicines = MedicalFieldParser.parsePrescriptionMedicines(combinedText, "uid_user")

        val bp = results.firstOrNull { it.testName == "Blood Pressure" }
        assertNotNull("Should extract Blood Pressure from Page 1", bp)

        val fbg = results.firstOrNull { it.testName == "Fasting Blood Glucose" }
        assertNotNull("Should extract Fasting Glucose from Page 2", fbg)

        assertTrue("Should extract medicines from Page 3", medicines.isNotEmpty())
    }

    @Test
    fun testEmptyOrInvalidTextHandling() {
        val classification = MedicalDocumentClassifier.classifyDocument("")
        assertEquals(DocumentMedicalStatus.UNCERTAIN, classification.status)

        val unreadableText = "12345 67890 !@#$%^&*()_+"
        val classification2 = MedicalDocumentClassifier.classifyDocument(unreadableText)
        assertEquals(DocumentMedicalStatus.UNCERTAIN, classification2.status)
    }

    private fun createTwoPageSyntheticPdf(page1Content: String, page2Content: String): ByteArray {
        val stream1 = deflateBytes(page1Content.toByteArray(Charsets.ISO_8859_1))
        val stream2 = deflateBytes(page2Content.toByteArray(Charsets.ISO_8859_1))

        val pdfStr = """
            %PDF-1.4
            1 0 obj
            << /Type /Catalog /Pages 2 0 R >>
            endobj
            2 0 obj
            << /Type /Pages /Count 2 /Kids [ 3 0 R 4 0 R ] >>
            endobj
            3 0 obj
            << /Type /Page /Parent 2 0 R /Contents 5 0 R >>
            endobj
            4 0 obj
            << /Type /Page /Parent 2 0 R /Contents 6 0 R >>
            endobj
            5 0 obj
            << /Length ${stream1.size} /Filter /FlateDecode >>
            stream
        """.trimIndent().toByteArray(Charsets.US_ASCII) + stream1 + "\nendstream\nendobj\n".toByteArray(Charsets.US_ASCII) +
                "6 0 obj\n<< /Length ${stream2.size} /Filter /FlateDecode >>\nstream\n".toByteArray(Charsets.US_ASCII) + stream2 + "\nendstream\nendobj\n%%EOF".toByteArray(Charsets.US_ASCII)

        return pdfStr
    }

    private fun deflateBytes(input: ByteArray): ByteArray {
        val deflater = Deflater()
        deflater.setInput(input)
        deflater.finish()
        val out = ByteArrayOutputStream()
        val buf = ByteArray(1024)
        while (!deflater.finished()) {
            val count = deflater.deflate(buf)
            out.write(buf, 0, count)
        }
        deflater.end()
        return out.toByteArray()
    }
}
