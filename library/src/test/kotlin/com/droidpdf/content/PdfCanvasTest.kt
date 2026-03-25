package com.droidpdf.content

import com.droidpdf.core.PageSize
import com.droidpdf.core.PdfDocument
import com.droidpdf.core.PdfLog
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream

class PdfCanvasTest {
    @BeforeEach
    fun setup() {
        PdfLog.logger = PdfLog.NoOpLogger()
    }

    @Test
    fun `canvas generates text operations`() {
        val output = ByteArrayOutputStream()
        val doc = PdfDocument(output)
        val page = doc.addNewPage(PageSize.A4)
        val font = PdfFont.helvetica()
        val fontName = font.registerOn(page)

        val canvas = PdfCanvas(page)
        canvas.beginText()
            .setFont(fontName, 12f)
            .moveText(72f, 700f)
            .showText("Hello World")
            .endText()
            .flush()

        doc.close()

        val pdf = output.toString("US-ASCII")
        assertTrue(pdf.contains("BT"))
        assertTrue(pdf.contains("/$fontName 12 Tf"))
        assertTrue(pdf.contains("72 700 Td"))
        assertTrue(pdf.contains("(Hello World) Tj"))
        assertTrue(pdf.contains("ET"))
    }

    @Test
    fun `canvas generates color operations`() {
        val output = ByteArrayOutputStream()
        val doc = PdfDocument(output)
        val page = doc.addNewPage()

        val canvas = PdfCanvas(page)
        canvas.setFillColor(1f, 0f, 0f)
            .setStrokeColor(0f, 0f, 1f)
            .flush()

        doc.close()

        val pdf = output.toString("US-ASCII")
        assertTrue(pdf.contains("1 0 0 rg"))
        assertTrue(pdf.contains("0 0 1 RG"))
    }

    @Test
    fun `canvas generates rectangle`() {
        val output = ByteArrayOutputStream()
        val doc = PdfDocument(output)
        val page = doc.addNewPage()

        val canvas = PdfCanvas(page)
        canvas.rectangle(100f, 200f, 300f, 50f)
            .stroke()
            .flush()

        doc.close()

        val pdf = output.toString("US-ASCII")
        assertTrue(pdf.contains("100 200 300 50 re"))
        assertTrue(pdf.contains("S"))
    }

    @Test
    fun `canvas state save and restore`() {
        val output = ByteArrayOutputStream()
        val doc = PdfDocument(output)
        val page = doc.addNewPage()

        val canvas = PdfCanvas(page)
        canvas.saveState()
            .setFillGray(0.5f)
            .restoreState()
            .flush()

        doc.close()

        val pdf = output.toString("US-ASCII")
        assertTrue(pdf.contains("q"))
        assertTrue(pdf.contains("0.5 g"))
        assertTrue(pdf.contains("Q"))
    }

    @Test
    fun `formatFloat avoids unnecessary decimals`() {
        org.junit.jupiter.api.Assertions.assertEquals("12", PdfCanvas.formatFloat(12f))
        org.junit.jupiter.api.Assertions.assertEquals("3.14", PdfCanvas.formatFloat(3.14f))
        org.junit.jupiter.api.Assertions.assertEquals("0", PdfCanvas.formatFloat(0f))
    }
}
