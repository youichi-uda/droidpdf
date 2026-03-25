package com.droidpdf.content

import com.droidpdf.core.PageSize
import com.droidpdf.core.PdfDocument
import com.droidpdf.core.PdfLog
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream

class PdfFontTest {
    @BeforeEach
    fun setup() {
        PdfLog.logger = PdfLog.NoOpLogger()
    }

    @Test
    fun `creates standard fonts`() {
        val font = PdfFont.helvetica()
        assertNotNull(font)
    }

    @Test
    fun `creates all standard 14 fonts`() {
        val fonts =
            listOf(
                PdfFont.helvetica(),
                PdfFont.helveticaBold(),
                PdfFont.timesRoman(),
                PdfFont.timesBold(),
                PdfFont.courier(),
                PdfFont.courierBold(),
                PdfFont.createStandard("Helvetica-Oblique"),
                PdfFont.createStandard("Helvetica-BoldOblique"),
                PdfFont.createStandard("Times-Italic"),
                PdfFont.createStandard("Times-BoldItalic"),
                PdfFont.createStandard("Courier-Oblique"),
                PdfFont.createStandard("Courier-BoldOblique"),
                PdfFont.createStandard("Symbol"),
                PdfFont.createStandard("ZapfDingbats"),
            )
        assertEquals(14, fonts.size)
    }

    @Test
    fun `rejects unknown standard font`() {
        assertThrows(IllegalArgumentException::class.java) {
            PdfFont.createStandard("NotAFont")
        }
    }

    @Test
    fun `font registers on page`() {
        val output = ByteArrayOutputStream()
        val doc = PdfDocument(output)
        val page = doc.addNewPage(PageSize.A4)
        val font = PdfFont.helvetica()

        val name = font.registerOn(page)
        assertTrue(name.startsWith("F"))
    }

    @Test
    fun `standard font text width is reasonable`() {
        val font = PdfFont.helvetica()
        val width = font.getTextWidth("Hello", 12f)
        assertTrue(width > 0f)
        assertTrue(width < 100f)
    }

    @Test
    fun `font metrics return reasonable values`() {
        val font = PdfFont.helvetica()
        val ascent = font.getAscent(12f)
        val descent = font.getDescent(12f)
        assertTrue(ascent > 0f)
        assertTrue(descent < 0f)
    }

    @Test
    fun `standard font produces valid PDF`() {
        val output = ByteArrayOutputStream()
        val doc = PdfDocument(output)
        val page = doc.addNewPage()
        val font = PdfFont.helvetica()
        val fontName = font.registerOn(page)

        val canvas = PdfCanvas(page)
        canvas.beginText()
            .setFont(fontName, 14f)
            .moveText(72f, 700f)
            .showText("Standard Font Test")
            .endText()
            .flush()
        doc.close()

        val pdf = output.toString("US-ASCII")
        assertTrue(pdf.contains("/Helvetica"))
        assertTrue(pdf.contains("Standard Font Test"))
    }
}
