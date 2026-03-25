package com.droidpdf.manipulation

import com.droidpdf.core.PdfDocument
import com.droidpdf.core.PdfLog
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class PageOperationsTest {
    @BeforeEach
    fun setup() {
        PdfLog.logger = PdfLog.NoOpLogger()
    }

    @Test
    fun `rotate pages`() {
        val pdf = TestHelper.createTestPdf(3)
        val ops = PageOperations(ByteArrayInputStream(pdf))

        ops.rotatePages(listOf(1, 3), 90)

        val output = ByteArrayOutputStream()
        ops.writeTo(output)

        val result = output.toByteArray()
        assertTrue(result.isNotEmpty())
        assertTrue(TestHelper.pdfContainsText(result, "%PDF-1.7"))
    }

    @Test
    fun `rotate all pages`() {
        val pdf = TestHelper.createTestPdf(3)
        val ops = PageOperations(ByteArrayInputStream(pdf))

        ops.rotateAllPages(180)

        val output = ByteArrayOutputStream()
        ops.writeTo(output)
        assertTrue(output.toByteArray().isNotEmpty())
    }

    @Test
    fun `remove pages`() {
        val pdf = TestHelper.createTestPdf(5)
        val ops = PageOperations(ByteArrayInputStream(pdf))
        assertEquals(5, ops.numberOfPages)

        ops.removePages(listOf(2, 4))

        val output = ByteArrayOutputStream()
        ops.writeTo(output)
        assertTrue(output.toByteArray().isNotEmpty())
    }

    @Test
    fun `reorder pages`() {
        val pdf = TestHelper.createTestPdf(3)
        val ops = PageOperations(ByteArrayInputStream(pdf))

        ops.reorderPages(listOf(3, 1, 2))

        val output = ByteArrayOutputStream()
        ops.writeTo(output)
        assertTrue(output.toByteArray().isNotEmpty())
    }

    @Test
    fun `page rotation values`() {
        val output1 = ByteArrayOutputStream()
        val doc = PdfDocument(output1)
        val page = doc.addNewPage()

        assertEquals(0, page.getRotation())
        page.setRotation(90)
        assertEquals(90, page.getRotation())
        page.setRotation(270)
        assertEquals(270, page.getRotation())
        page.setRotation(-90)
        assertEquals(270, page.getRotation())
        page.setRotation(360)
        assertEquals(0, page.getRotation())

        doc.close()
    }

    @Test
    fun `document page manipulation`() {
        val output = ByteArrayOutputStream()
        val doc = PdfDocument(output)
        doc.addNewPage()
        doc.addNewPage()
        doc.addNewPage()

        assertEquals(3, doc.numberOfPages)

        doc.removePage(1)
        assertEquals(2, doc.numberOfPages)

        doc.movePage(1, 0)
        assertEquals(2, doc.numberOfPages)

        doc.close()
    }

    @Test
    fun `chained operations produce valid PDF`() {
        val pdf = TestHelper.createTestPdf(5)
        val ops = PageOperations(ByteArrayInputStream(pdf))

        ops.rotatePages(listOf(1), 90)
            .removePages(listOf(3))
            .reorderPages(listOf(4, 1, 2, 3))

        val output = ByteArrayOutputStream()
        ops.writeTo(output)

        val resultPdf = String(output.toByteArray(), Charsets.US_ASCII)
        assertTrue(resultPdf.startsWith("%PDF-1.7"))
        assertTrue(resultPdf.endsWith("%%EOF"))
    }
}
