package com.droidpdf.manipulation

import com.droidpdf.core.PdfLog
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class PdfMergerTest {
    @BeforeEach
    fun setup() {
        PdfLog.logger = PdfLog.NoOpLogger()
    }

    @Test
    fun `merges two PDFs`() {
        val pdf1 = TestHelper.createTestPdf(2)
        val pdf2 = TestHelper.createTestPdf(3)

        val output = ByteArrayOutputStream()
        val merger = PdfMerger(output)
        merger.merge(ByteArrayInputStream(pdf1))
        merger.merge(ByteArrayInputStream(pdf2))
        merger.close()

        val mergedPdf = output.toByteArray()
        assertTrue(mergedPdf.isNotEmpty())
        assertTrue(TestHelper.pdfContainsText(mergedPdf, "%PDF-1.7"))
    }

    @Test
    fun `merge tracks page count`() {
        val pdf1 = TestHelper.createTestPdf(2)
        val pdf2 = TestHelper.createTestPdf(3)

        val output = ByteArrayOutputStream()
        val merger = PdfMerger(output)
        merger.merge(ByteArrayInputStream(pdf1))
        assertEquals(2, merger.numberOfPages)
        merger.merge(ByteArrayInputStream(pdf2))
        assertEquals(5, merger.numberOfPages)
        merger.close()
    }

    @Test
    fun `merges partial page range`() {
        val pdf = TestHelper.createTestPdf(5)

        val output = ByteArrayOutputStream()
        val merger = PdfMerger(output)
        merger.merge(ByteArrayInputStream(pdf), 2, 4)
        assertEquals(3, merger.numberOfPages)
        merger.close()
    }

    @Test
    fun `merges single page`() {
        val pdf = TestHelper.createTestPdf(5)

        val output = ByteArrayOutputStream()
        val merger = PdfMerger(output)
        merger.merge(ByteArrayInputStream(pdf), 3, 3)
        assertEquals(1, merger.numberOfPages)
        merger.close()
    }

    @Test
    fun `merged PDF is valid`() {
        val pdf1 = TestHelper.createTestPdf(1)
        val pdf2 = TestHelper.createTestPdf(1)

        val output = ByteArrayOutputStream()
        val merger = PdfMerger(output)
        merger.merge(ByteArrayInputStream(pdf1))
        merger.merge(ByteArrayInputStream(pdf2))
        merger.close()

        val pdfString = String(output.toByteArray(), Charsets.US_ASCII)
        assertTrue(pdfString.startsWith("%PDF-1.7"))
        assertTrue(pdfString.endsWith("%%EOF"))
        assertTrue(pdfString.contains("xref"))
        assertTrue(pdfString.contains("trailer"))
    }
}
