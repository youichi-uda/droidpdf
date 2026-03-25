package com.droidpdf.content

import com.droidpdf.core.PageSize
import com.droidpdf.core.PdfDocument
import com.droidpdf.core.PdfLog
import com.droidpdf.core.PdfValidator
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.File

class PdfFontEmbedTest {
    companion object {
        private lateinit var outputDir: File

        @JvmStatic
        @BeforeAll
        fun setupDir() {
            outputDir = File("build/test-pdfs")
            outputDir.mkdirs()
        }
    }

    @BeforeEach
    fun setup() {
        PdfLog.logger = PdfLog.NoOpLogger()
    }

    @Test
    fun `embed TTF font and generate PDF`() {
        val ttfStream = javaClass.classLoader.getResourceAsStream("test-font.ttf")
        assertNotNull(ttfStream) { "test-font.ttf not found in test resources" }

        val font = PdfFont.createFromTtf(ttfStream!!)
        assertNotNull(font)

        val out = ByteArrayOutputStream()
        val doc = PdfDocument(out)
        val page = doc.addNewPage(PageSize.A4)
        val fontName = font.registerOn(page)

        val canvas = PdfCanvas(page)
        canvas.beginText()
            .setFont(fontName, 16f)
            .moveText(72f, 750f)
            .showText("Hello with embedded TTF font!")
            .endText()
            .flush()

        doc.close()

        val bytes = out.toByteArray()
        File(outputDir, "embed-01-ttf.pdf").writeBytes(bytes)

        val result = PdfValidator.validate(bytes)
        assertTrue(result.valid) { "TTF embedded PDF invalid:\n$result" }

        val content = String(bytes, Charsets.ISO_8859_1)
        assertTrue(content.contains("/Subtype /TrueType")) { "Should contain TrueType subtype" }
        assertTrue(content.contains("/FontDescriptor")) { "Should contain FontDescriptor" }
        assertTrue(content.contains("/FontFile2")) { "Should contain embedded font data" }
        assertTrue(content.contains("Hello with embedded TTF font!"))
    }

    @Test
    fun `TTF font text width measurement`() {
        val ttfStream = javaClass.classLoader.getResourceAsStream("test-font.ttf")!!
        val font = PdfFont.createFromTtf(ttfStream)

        val width = font.getTextWidth("Hello", 12f)
        assertTrue(width > 0f) { "Width should be positive, got $width" }
        assertTrue(width < 100f) { "Width should be reasonable, got $width" }

        // Wider text should have greater width
        val widerWidth = font.getTextWidth("Hello World", 12f)
        assertTrue(widerWidth > width) { "Wider text should have greater width" }
    }

    @Test
    fun `TTF font metrics`() {
        val ttfStream = javaClass.classLoader.getResourceAsStream("test-font.ttf")!!
        val font = PdfFont.createFromTtf(ttfStream)

        val ascent = font.getAscent(12f)
        val descent = font.getDescent(12f)

        assertTrue(ascent > 0f) { "Ascent should be positive: $ascent" }
        assertTrue(descent < 0f) { "Descent should be negative: $descent" }
        assertTrue(ascent > -descent) { "Ascent should be larger than |descent|" }
    }

    @Test
    fun `TTF font with multiple sizes`() {
        val ttfStream = javaClass.classLoader.getResourceAsStream("test-font.ttf")!!
        val font = PdfFont.createFromTtf(ttfStream)

        val out = ByteArrayOutputStream()
        val doc = PdfDocument(out)
        val page = doc.addNewPage()
        val fontName = font.registerOn(page)

        val canvas = PdfCanvas(page)
        canvas.beginText()

        var y = 750f
        for (size in listOf(8f, 10f, 12f, 14f, 18f, 24f, 36f)) {
            canvas.setFont(fontName, size)
                .moveText(72f, y)
                .showText("Size ${size.toInt()}pt: ABCabc123")
            y -= size * 1.5f
        }

        canvas.endText().flush()
        doc.close()

        val bytes = out.toByteArray()
        File(outputDir, "embed-02-ttf-sizes.pdf").writeBytes(bytes)

        val result = PdfValidator.validate(bytes)
        assertTrue(result.valid) { "Multi-size TTF PDF invalid:\n$result" }
    }

    @Test
    fun `TTF font with layout API`() {
        val ttfStream = javaClass.classLoader.getResourceAsStream("test-font.ttf")!!
        val font = PdfFont.createFromTtf(ttfStream)

        val out = ByteArrayOutputStream()
        val pdfDoc = PdfDocument(out)
        val doc = com.droidpdf.layout.Document(pdfDoc)

        doc.add(
            com.droidpdf.layout.Paragraph("Title in embedded font")
                .setFont(font)
                .setFontSize(24f),
        )
        doc.add(
            com.droidpdf.layout.Paragraph(
                "Body text using the same embedded TrueType font. " +
                    "This tests that font width measurement works correctly " +
                    "for line wrapping with embedded fonts.",
            ).setFont(font).setFontSize(12f),
        )
        doc.close()

        val bytes = out.toByteArray()
        File(outputDir, "embed-03-ttf-layout.pdf").writeBytes(bytes)

        val result = PdfValidator.validate(bytes)
        assertTrue(result.valid) { "TTF layout PDF invalid:\n$result" }
    }
}
