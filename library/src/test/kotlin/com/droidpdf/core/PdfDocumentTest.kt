package com.droidpdf.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class PdfDocumentTest {
    @BeforeEach
    fun setup() {
        PdfLog.logger = PdfLog.NoOpLogger()
    }

    @Test
    fun `creates empty document`() {
        val output = ByteArrayOutputStream()
        val doc = PdfDocument(output)
        doc.addNewPage()
        doc.close()

        val pdfString = output.toString("US-ASCII")
        assertTrue(pdfString.startsWith("%PDF-1.7"))
        assertTrue(pdfString.endsWith("%%EOF"))
    }

    @Test
    fun `creates document with multiple pages`() {
        val output = ByteArrayOutputStream()
        val doc = PdfDocument(output)
        doc.addNewPage(PageSize.A4)
        doc.addNewPage(PageSize.LETTER)
        doc.addNewPage(PageSize.A3)
        doc.close()

        assertEquals(3, doc.numberOfPages)
    }

    @Test
    fun `creates document with custom page size`() {
        val output = ByteArrayOutputStream()
        val doc = PdfDocument(output)
        val page = doc.addNewPage(PageSize(300f, 400f))
        doc.close()

        assertEquals(300f, page.width)
        assertEquals(400f, page.height)
    }

    @Test
    fun `creates document with content`() {
        val output = ByteArrayOutputStream()
        val doc = PdfDocument(output)
        val page = doc.addNewPage()
        page.addContent("BT /F1 12 Tf (Hello DroidPDF!) Tj ET")
        doc.close()

        val pdfString = output.toString("US-ASCII")
        assertTrue(pdfString.contains("Hello DroidPDF!"))
    }

    @Test
    fun `reads back written document`() {
        // Write
        val output = ByteArrayOutputStream()
        val doc = PdfDocument(output)
        doc.addNewPage(PageSize.A4)
        doc.addNewPage(PageSize.LETTER)
        doc.close()

        // Read
        val reader = PdfReader(ByteArrayInputStream(output.toByteArray()))
        val catalog = reader.getCatalog()
        assertNotNull(catalog)
        assertEquals(PdfName.CATALOG, catalog?.getAsName("Type"))
    }

    @Test
    fun `page sizes are correct`() {
        assertEquals(595f, PageSize.A4.width)
        assertEquals(842f, PageSize.A4.height)
        assertEquals(612f, PageSize.LETTER.width)
        assertEquals(792f, PageSize.LETTER.height)
    }
}
