package com.droidpdf.manipulation

import com.droidpdf.core.PdfLog
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class PdfSplitterTest {
    @BeforeEach
    fun setup() {
        PdfLog.logger = PdfLog.NoOpLogger()
    }

    @Test
    fun `reports correct page count`() {
        val pdf = TestHelper.createTestPdf(5)
        val splitter = PdfSplitter(ByteArrayInputStream(pdf))
        assertEquals(5, splitter.numberOfPages)
    }

    @Test
    fun `extracts page range`() {
        val pdf = TestHelper.createTestPdf(5)
        val splitter = PdfSplitter(ByteArrayInputStream(pdf))
        val output = ByteArrayOutputStream()

        splitter.extractPages(2, 4, output)

        val resultPdf = output.toByteArray()
        assertTrue(resultPdf.isNotEmpty())
        assertTrue(TestHelper.pdfContainsText(resultPdf, "%PDF-1.7"))
    }

    @Test
    fun `extracts specific pages`() {
        val pdf = TestHelper.createTestPdf(5)
        val splitter = PdfSplitter(ByteArrayInputStream(pdf))
        val output = ByteArrayOutputStream()

        splitter.extractPages(listOf(1, 3, 5), output)

        assertTrue(output.toByteArray().isNotEmpty())
    }

    @Test
    fun `splits by page`() {
        val pdf = TestHelper.createTestPdf(3)
        val splitter = PdfSplitter(ByteArrayInputStream(pdf))
        val outputs = mutableMapOf<Int, ByteArrayOutputStream>()

        splitter.splitByPage { pageNum ->
            ByteArrayOutputStream().also { outputs[pageNum] = it }
        }

        assertEquals(3, outputs.size)
        outputs.values.forEach { output ->
            assertTrue(output.toByteArray().isNotEmpty())
            assertTrue(TestHelper.pdfContainsText(output.toByteArray(), "%PDF-1.7"))
        }
    }
}
