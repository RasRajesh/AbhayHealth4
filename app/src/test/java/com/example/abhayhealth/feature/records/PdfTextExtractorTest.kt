package com.example.abhayhealth.feature.records

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.Deflater

class PdfTextExtractorTest {

    @Test
    fun testExtractTextFromPdfBytesSimpleTj() {
        val pdfContent = """
            %PDF-1.4
            1 0 obj
            << /Length 65 >>
            stream
            BT
            /F1 12 Tf
            (COMPLETE BLOOD COUNT) Tj
            T*
            (Haemoglobin 9.1 g/dl) Tj
            ET
            endstream
            endobj
        """.trimIndent()

        val text = PdfTextExtractor.extractTextFromPdfBytes(pdfContent.toByteArray(Charsets.US_ASCII))
        assertTrue(text.contains("COMPLETE BLOOD COUNT"))
        assertTrue(text.contains("Haemoglobin 9.1 g/dl"))
    }

    @Test
    fun testExtractTextFromPdfBytesWithTjArray() {
        val pdfContent = """
            %PDF-1.4
            1 0 obj
            << /Length 120 >>
            stream
            BT
            /F1 10 Tf
            [(AARTHI ) -200 (SCANS ) -200 (& LABS)] TJ
            T*
            [(RBC ) -200 (4.23 ) -200 (Million/cmm ) -200 (3.8-4.8)] TJ
            ET
            endstream
            endobj
        """.trimIndent()

        val text = PdfTextExtractor.extractTextFromPdfBytes(pdfContent.toByteArray(Charsets.US_ASCII))
        assertTrue(text.contains("AARTHI SCANS & LABS"))
        assertTrue(text.contains("RBC 4.23 Million/cmm 3.8-4.8"))
    }

    @Test
    fun testExtractTextFromCompressedFlateStream() {
        val rawStream = """
            BT
            /F1 12 Tf
            (GLUCOSE FASTING 104.0 mg/dL 74-100) Tj
            ET
        """.trimIndent().toByteArray(Charsets.ISO_8859_1)

        val deflater = Deflater()
        deflater.setInput(rawStream)
        deflater.finish()
        val compressedStream = ByteArrayOutputStream()
        val buf = ByteArray(1024)
        while (!deflater.finished()) {
            val count = deflater.deflate(buf)
            compressedStream.write(buf, 0, count)
        }
        deflater.end()

        val header = "%PDF-1.4\n1 0 obj\n<< /Length ${compressedStream.size()} /Filter /FlateDecode >>\nstream\n".toByteArray(Charsets.US_ASCII)
        val footer = "\nendstream\nendobj\n".toByteArray(Charsets.US_ASCII)

        val pdfBytes = header + compressedStream.toByteArray() + footer

        val text = PdfTextExtractor.extractTextFromPdfBytes(pdfBytes)
        assertTrue(text.contains("GLUCOSE FASTING 104.0 mg/dL 74-100"))
    }

    @Test
    fun testSkipBinaryImageStream() {
        val pdfContent = """
            %PDF-1.4
            1 0 obj
            << /Type /XObject /Subtype /Image /Width 100 /Height 100 /Filter /DCTDecode >>
            stream
            (This is raw image binary that should be skipped by the text extractor)
            endstream
            endobj
            2 0 obj
            << /Length 50 >>
            stream
            BT
            (Real Report Text) Tj
            ET
            endstream
            endobj
        """.trimIndent()

        val text = PdfTextExtractor.extractTextFromPdfBytes(pdfContent.toByteArray(Charsets.US_ASCII))
        assertFalse(text.contains("This is raw image binary"))
        assertTrue(text.contains("Real Report Text"))
    }

    @Test
    fun testMalformedStreamResilience() {
        // Stream with unclosed parenthesis and random binary characters
        val malformedStream = """
            %PDF-1.4
            1 0 obj
            << /Length 200 >>
            stream
            BT
            (Unclosed string without closing paren
            some binary \x00 \xFF (another paren
            (Valid Text After) Tj
            ET
            endstream
            endobj
        """.trimIndent()

        val text = PdfTextExtractor.extractTextFromPdfBytes(malformedStream.toByteArray(Charsets.US_ASCII))
        // Should complete without infinite loop, out of memory, or throwing
        assertFalse(text.isBlank())
    }

    @Test
    fun testEightPageMultiStreamPdfExtractionAndParsing() {
        // Build an 8-page PDF with 6 compressed FlateDecode content streams per page (48 streams total > old 35 limit)
        val pageStreams = listOf(
            listOf("AARTHI SCANS & LABS", "Final Test Report", "Report Date : 29/10/2026", "Haemoglobin (HB)", "9.1 g/dl 12.0-15.0", "Page : 1 of 8"),
            listOf("HAEMATOLOGY", "Platelet Count", "3.36 Lakhs/cumm 1.5-4.5", "ESR", "17 mm/hr <12.0", "Page : 2 of 8"),
            listOf("BIOCHEMISTRY", "GLUCOSE FASTING (FBS)", "104.0 mg/dL 74-100", "CREATININE", "0.5 mg/dL 0.5-1.1", "Page : 3 of 8"),
            listOf("LIPID PROFILE", "CHOLESTEROL TOTAL", "128.0 mg/dL <200", "TRIGLYCERIDES", "110.0 mg/dL <150", "Page : 4 of 8"),
            listOf("LIVER FUNCTION TEST", "TOTAL PROTEIN", "7.01 g/dl 6.0-8.0", "ALBUMIN", "4.22 g/dl 3.5-5.2", "Page : 5 of 8"),
            listOf("URINE COMPLETE ANALYSIS", "SPECIFIC GRAVITY", "1.025 1.005-1.030", "BLOOD", "Positive Negative", "Page : 6 of 8"),
            listOf("MICROSCOPIC EXAMINATION", "PUS CELLS", "2-4 0-5/hpf", "EPITHELIAL CELLS", "4-6 0-5/hpf", "Page : 7 of 8"),
            listOf("IMMUNOLOGY", "THYROID PROFILE", "Total-T3 (Triiodothyronine-T3)", "108.0 ng/dl 70-204", "TSH (Thyroid-stimulating hormone)-Ultra", "4.520 uIU/ml 0.4-4.5")
        )

        val pdfOut = ByteArrayOutputStream()
        pdfOut.write("%PDF-1.4\n".toByteArray(Charsets.US_ASCII))
        var objId = 1
        for (streamsOnPage in pageStreams) {
            for (line in streamsOnPage) {
                val raw = "BT\n/F1 12 Tf\n($line) Tj\nET".toByteArray(Charsets.ISO_8859_1)
                val deflater = Deflater()
                deflater.setInput(raw)
                deflater.finish()
                val compressed = ByteArrayOutputStream()
                val buf = ByteArray(1024)
                while (!deflater.finished()) {
                    val count = deflater.deflate(buf)
                    compressed.write(buf, 0, count)
                }
                deflater.end()
                val compBytes = compressed.toByteArray()
                pdfOut.write("$objId 0 obj\n<< /Length ${compBytes.size} /Filter /FlateDecode >>\nstream\n".toByteArray(Charsets.US_ASCII))
                pdfOut.write(compBytes)
                pdfOut.write("\nendstream\nendobj\n".toByteArray(Charsets.US_ASCII))
                objId++
            }
        }
        pdfOut.write("%%EOF\n".toByteArray(Charsets.US_ASCII))

        val extracted = PdfTextExtractor.extractTextFromPdfBytes(pdfOut.toByteArray())
        assertTrue("Page 1 Hemoglobin should be extracted", extracted.contains("Haemoglobin (HB)"))
        assertTrue("Page 8 TSH should be extracted", extracted.contains("4.520 uIU/ml"))

        val parsed = MedicalFieldParser.parseTestResults(extracted)
        val names = parsed.map { it.testName }.toSet()
        assertTrue("Should parse Hemoglobin from page 1", names.contains("Hemoglobin"))
        assertTrue("Should parse Serum Creatinine from page 3", names.contains("Serum Creatinine"))
        assertTrue("Should parse Albumin from page 5", names.contains("Albumin"))
        assertTrue("Should parse Urine Blood from page 6", names.contains("Urine Blood"))
        assertTrue("Should parse TSH from page 8", names.contains("TSH"))

        // Verify Room serialization round-trip preserves exact data for Home & Trends
        val serialized = MedicalFieldParser.serializeResults(parsed)
        val deserialized = MedicalFieldParser.deserializeResults(serialized)
        assertEquals(parsed.size, deserialized.size)
        assertEquals(parsed.map { it.testName to it.value }, deserialized.map { it.testName to it.value })
    }
}
