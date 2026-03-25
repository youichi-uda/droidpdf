package com.droidpdf.core

import com.droidpdf.content.PdfCanvas
import com.droidpdf.content.PdfFont
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class TextExtractorTest {
    @BeforeEach
    fun setup() {
        PdfLog.logger = PdfLog.NoOpLogger()
    }

    private fun createPdfWithText(vararg texts: String): ByteArray {
        val output = ByteArrayOutputStream()
        val doc = PdfDocument(output)
        val font = PdfFont.helvetica()

        val page = doc.addNewPage()
        val fontName = font.registerOn(page)
        val canvas = PdfCanvas(page)

        canvas.beginText()
        canvas.setFont(fontName, 12f)
        var y = 700f
        for (text in texts) {
            canvas.moveText(72f, y)
            canvas.showText(text)
            y -= 20f
        }
        canvas.endText()
        canvas.flush()

        doc.close()
        return output.toByteArray()
    }

    @Test
    fun `extracts simple text`() {
        val pdf = createPdfWithText("Hello World")
        val reader = PdfReader(ByteArrayInputStream(pdf))
        val extractor = TextExtractor(reader)

        val text = extractor.extractAll()
        assertTrue(text.contains("Hello World"))
    }

    @Test
    fun `extracts multiple text strings`() {
        val pdf = createPdfWithText("First line", "Second line")
        val reader = PdfReader(ByteArrayInputStream(pdf))
        val extractor = TextExtractor(reader)

        val text = extractor.extractAll()
        assertTrue(text.contains("First line"))
        assertTrue(text.contains("Second line"))
    }

    @Test
    fun `extracts from specific page`() {
        val output = ByteArrayOutputStream()
        val doc = PdfDocument(output)
        val font = PdfFont.helvetica()

        // Page 1
        val page1 = doc.addNewPage()
        val fn1 = font.registerOn(page1)
        val c1 = PdfCanvas(page1)
        c1.beginText().setFont(fn1, 12f).moveText(72f, 700f).showText("Page One").endText().flush()

        // Page 2
        val page2 = doc.addNewPage()
        val fn2 = font.registerOn(page2)
        val c2 = PdfCanvas(page2)
        c2.beginText().setFont(fn2, 12f).moveText(72f, 700f).showText("Page Two").endText().flush()

        doc.close()

        val reader = PdfReader(ByteArrayInputStream(output.toByteArray()))
        val extractor = TextExtractor(reader)

        val page1Text = extractor.extractFromPage(0)
        val page2Text = extractor.extractFromPage(1)

        assertTrue(page1Text.contains("Page One"))
        assertTrue(page2Text.contains("Page Two"))
    }

    @Test
    fun `search finds text`() {
        val pdf = createPdfWithText("Hello World", "Goodbye World")
        val reader = PdfReader(ByteArrayInputStream(pdf))
        val extractor = TextExtractor(reader)

        val results = extractor.search("World")
        assertTrue(results.isNotEmpty())
        assertEquals(0, results[0].pageIndex)
    }

    @Test
    fun `search is case insensitive`() {
        val pdf = createPdfWithText("Hello WORLD")
        val reader = PdfReader(ByteArrayInputStream(pdf))
        val extractor = TextExtractor(reader)

        val results = extractor.search("world")
        assertTrue(results.isNotEmpty())
    }

    @Test
    fun `search returns empty for no match`() {
        val pdf = createPdfWithText("Hello World")
        val reader = PdfReader(ByteArrayInputStream(pdf))
        val extractor = TextExtractor(reader)

        val results = extractor.search("xyz123")
        assertTrue(results.isEmpty())
    }

    @Test
    fun `extract from empty page returns empty`() {
        val output = ByteArrayOutputStream()
        val doc = PdfDocument(output)
        doc.addNewPage()
        doc.close()

        val reader = PdfReader(ByteArrayInputStream(output.toByteArray()))
        val extractor = TextExtractor(reader)

        val text = extractor.extractFromPage(0)
        assertTrue(text.isEmpty())
    }
}
