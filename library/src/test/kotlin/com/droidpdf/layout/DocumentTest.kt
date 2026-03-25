package com.droidpdf.layout

import com.droidpdf.content.PdfFont
import com.droidpdf.core.PageSize
import com.droidpdf.core.PdfDocument
import com.droidpdf.core.PdfLog
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream

class DocumentTest {
    @BeforeEach
    fun setup() {
        PdfLog.logger = PdfLog.NoOpLogger()
    }

    @Test
    fun `creates document with paragraph`() {
        val output = ByteArrayOutputStream()
        val pdfDoc = PdfDocument(output)
        val doc = Document(pdfDoc)

        doc.add(Paragraph("Hello, DroidPDF!").setFontSize(16f))
        doc.close()

        val pdf = output.toString("US-ASCII")
        assertTrue(pdf.startsWith("%PDF-1.7"))
        assertTrue(pdf.contains("Hello, DroidPDF!"))
    }

    @Test
    fun `creates document with multiple paragraphs`() {
        val output = ByteArrayOutputStream()
        val pdfDoc = PdfDocument(output)
        val doc = Document(pdfDoc)

        doc.add(Paragraph("First paragraph").setFontSize(14f))
        doc.add(Paragraph("Second paragraph").setFontSize(12f))
        doc.add(Paragraph("Third paragraph"))
        doc.close()

        val pdf = output.toString("US-ASCII")
        assertTrue(pdf.contains("First paragraph"))
        assertTrue(pdf.contains("Second paragraph"))
        assertTrue(pdf.contains("Third paragraph"))
    }

    @Test
    fun `creates document with styled paragraph`() {
        val output = ByteArrayOutputStream()
        val pdfDoc = PdfDocument(output)
        val doc = Document(pdfDoc)

        doc.add(
            Paragraph("Bold Title")
                .setFont(PdfFont.helveticaBold())
                .setFontSize(24f)
                .setColor(Color.RED),
        )
        doc.close()

        val pdf = output.toString("US-ASCII")
        assertTrue(pdf.contains("Bold Title"))
        assertTrue(pdf.contains("1 0 0 rg")) // Red color
    }

    @Test
    fun `creates document with table`() {
        val output = ByteArrayOutputStream()
        val pdfDoc = PdfDocument(output)
        val doc = Document(pdfDoc)

        doc.add(
            Table(3).apply {
                addCell("Name")
                addCell("Age")
                addCell("City")
                addCell("Alice")
                addCell("30")
                addCell("Tokyo")
            },
        )
        doc.close()

        val pdf = output.toString("US-ASCII")
        assertTrue(pdf.contains("Name"))
        assertTrue(pdf.contains("Alice"))
        assertTrue(pdf.contains("Tokyo"))
    }

    @Test
    fun `creates document with custom margins`() {
        val output = ByteArrayOutputStream()
        val pdfDoc = PdfDocument(output)
        val doc = Document(pdfDoc, PageSize.LETTER)

        doc.setMargins(72f) // 1 inch margins
        doc.add(Paragraph("Content with margins"))
        doc.close()

        val pdf = output.toString("US-ASCII")
        assertTrue(pdf.contains("Content with margins"))
    }

    @Test
    fun `creates document with mixed elements`() {
        val output = ByteArrayOutputStream()
        val pdfDoc = PdfDocument(output)
        val doc = Document(pdfDoc)

        doc.add(
            Paragraph("Document Title")
                .setFont(PdfFont.helveticaBold())
                .setFontSize(20f)
                .setAlignment(Paragraph.Alignment.CENTER),
        )
        doc.add(Paragraph("Some introductory text goes here."))
        doc.add(
            Table(2).apply {
                addCell("Key")
                addCell("Value")
                addCell("Library")
                addCell("DroidPDF")
            },
        )
        doc.add(Paragraph("Footer text"))
        doc.close()

        val pdf = output.toString("US-ASCII")
        assertTrue(pdf.contains("Document Title"))
        assertTrue(pdf.contains("DroidPDF"))
        assertTrue(pdf.contains("Footer text"))
    }

    @Test
    fun `page content dimensions are correct`() {
        val output = ByteArrayOutputStream()
        val pdfDoc = PdfDocument(output)
        val doc = Document(pdfDoc, PageSize.A4)

        doc.setMargins(36f)
        // A4 = 595x842, margins = 36 each side
        assertTrue(doc.pageContentWidth > 500f)
        assertTrue(doc.pageContentHeight > 700f)

        doc.close()
    }
}
