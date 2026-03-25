package com.droidpdf.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class PdfWriterReaderTest {
    @BeforeEach
    fun setup() {
        PdfLog.logger = PdfLog.NoOpLogger()
    }

    @Test
    fun `writes minimal valid PDF`() {
        val output = ByteArrayOutputStream()
        val writer = PdfWriter(output)

        // Create a minimal page
        val pageContent = PdfStream()
        pageContent.data = "BT /F1 12 Tf (Hello) Tj ET".toByteArray(Charsets.US_ASCII)
        val contentRef = writer.addObject(pageContent)

        // Pages dictionary
        val pages = PdfDictionary()
        pages.put(PdfName.TYPE, PdfName.PAGES)
        pages.put(PdfName.COUNT, PdfInteger(1))
        val pagesRef = writer.addObject(pages)

        // Font
        val font = PdfDictionary()
        font.put(PdfName.TYPE, PdfName.FONT)
        font.put(PdfName.SUBTYPE, PdfName("Type1"))
        font.put("BaseFont", PdfName("Helvetica"))

        // Page dictionary
        val page = PdfDictionary()
        page.put(PdfName.TYPE, PdfName.PAGE)
        page.put(PdfName.PARENT, pagesRef)
        page.put(PdfName.MEDIABOX, PdfArray(PdfInteger(0), PdfInteger(0), PdfReal(595f), PdfReal(842f)))
        page.put(PdfName.CONTENTS, contentRef)
        val resources = PdfDictionary()
        val fontDict = PdfDictionary()
        fontDict.put("F1", font)
        resources.put(PdfName.FONT, fontDict)
        page.put(PdfName.RESOURCES, resources)
        val pageRef = writer.addObject(page)

        // Update pages kids
        pages.put(PdfName.KIDS, PdfArray(pageRef))

        // Catalog
        val catalog = PdfDictionary()
        catalog.put(PdfName.TYPE, PdfName.CATALOG)
        catalog.put(PdfName.PAGES, pagesRef)
        val catalogRef = writer.addObject(catalog)

        val trailer = PdfDictionary()
        trailer.put("Root", catalogRef)
        writer.write(trailer)

        val pdfBytes = output.toByteArray()
        val pdfString = String(pdfBytes, Charsets.US_ASCII)

        assertTrue(pdfString.startsWith("%PDF-1.7")) { "Should start with PDF header" }
        assertTrue(pdfString.contains("xref")) { "Should contain xref" }
        assertTrue(pdfString.contains("trailer")) { "Should contain trailer" }
        assertTrue(pdfString.endsWith("%%EOF")) { "Should end with %%EOF" }
    }

    @Test
    fun `reader parses writer output`() {
        // Write a minimal PDF
        val output = ByteArrayOutputStream()
        val writer = PdfWriter(output)

        val pages = PdfDictionary()
        pages.put(PdfName.TYPE, PdfName.PAGES)
        pages.put(PdfName.COUNT, PdfInteger(0))
        pages.put(PdfName.KIDS, PdfArray())
        val pagesRef = writer.addObject(pages)

        val catalog = PdfDictionary()
        catalog.put(PdfName.TYPE, PdfName.CATALOG)
        catalog.put(PdfName.PAGES, pagesRef)
        val catalogRef = writer.addObject(catalog)

        val trailer = PdfDictionary()
        trailer.put("Root", catalogRef)
        writer.write(trailer)

        // Read it back
        val reader = PdfReader(ByteArrayInputStream(output.toByteArray()))

        assertTrue(reader.objectCount() >= 2) { "Should have at least 2 objects (pages + catalog)" }

        val readCatalog = reader.getCatalog()
        assertNotNull(readCatalog) { "Should find catalog" }
        assertEquals(PdfName.CATALOG, readCatalog?.getAsName("Type"))
    }

    @Test
    fun `round-trip preserves page count`() {
        val output = ByteArrayOutputStream()
        val writer = PdfWriter(output)

        val pages = PdfDictionary()
        pages.put(PdfName.TYPE, PdfName.PAGES)
        pages.put(PdfName.COUNT, PdfInteger(3))
        pages.put(PdfName.KIDS, PdfArray())
        val pagesRef = writer.addObject(pages)

        val catalog = PdfDictionary()
        catalog.put(PdfName.TYPE, PdfName.CATALOG)
        catalog.put(PdfName.PAGES, pagesRef)
        val catalogRef = writer.addObject(catalog)

        val trailer = PdfDictionary()
        trailer.put("Root", catalogRef)
        writer.write(trailer)

        val reader = PdfReader(ByteArrayInputStream(output.toByteArray()))
        val readCatalog = reader.getCatalog()!!
        val pagesObj = reader.resolve(readCatalog[PdfName.PAGES]) as PdfDictionary
        assertEquals(PdfInteger(3), pagesObj.getAsInteger("Count"))
    }
}
