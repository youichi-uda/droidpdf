package com.droidpdf.layout

import com.droidpdf.content.PdfFont
import com.droidpdf.core.PageSize
import com.droidpdf.core.PdfDocument
import com.droidpdf.core.PdfLog
import com.droidpdf.core.PdfValidator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.File

class LayoutStressTest {
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

    private fun validateAndSave(
        pdfBytes: ByteArray,
        name: String,
    ) {
        File(outputDir, "$name.pdf").writeBytes(pdfBytes)
        val result = PdfValidator.validate(pdfBytes)
        assertTrue(result.valid) { "PDF '$name' has structural errors:\n$result" }
    }

    @Test
    fun `many paragraphs cause page break`() {
        val out = ByteArrayOutputStream()
        val pdfDoc = PdfDocument(out)
        val doc = Document(pdfDoc, PageSize.A4)

        for (i in 1..80) {
            doc.add(Paragraph("Line $i: The quick brown fox jumps over the lazy dog."))
        }
        doc.close()

        val bytes = out.toByteArray()
        validateAndSave(bytes, "stress-01-many-paragraphs")
        assertTrue(pdfDoc.numberOfPages > 1) { "Should have multiple pages, got ${pdfDoc.numberOfPages}" }
    }

    @Test
    fun `large table causes page break`() {
        val out = ByteArrayOutputStream()
        val pdfDoc = PdfDocument(out)
        val doc = Document(pdfDoc, PageSize.A4)

        doc.add(Paragraph("Large Table Test").setFontSize(16f))
        doc.add(
            Table(4).apply {
                addCell("ID")
                addCell("Name")
                addCell("Email")
                addCell("Status")
                for (i in 1..100) {
                    addCell("$i")
                    addCell("User $i")
                    addCell("user$i@example.com")
                    addCell(if (i % 2 == 0) "Active" else "Inactive")
                }
            },
        )
        doc.close()

        val bytes = out.toByteArray()
        validateAndSave(bytes, "stress-02-large-table")
        assertTrue(pdfDoc.numberOfPages > 1) { "Should have multiple pages" }
    }

    @Test
    fun `mixed elements across pages`() {
        val out = ByteArrayOutputStream()
        val pdfDoc = PdfDocument(out)
        val doc = Document(pdfDoc, PageSize.A4)

        for (section in 1..5) {
            doc.add(
                Paragraph("Section $section")
                    .setFont(PdfFont.helveticaBold())
                    .setFontSize(18f),
            )
            for (p in 1..10) {
                doc.add(Paragraph("Section $section, paragraph $p. Lorem ipsum dolor sit amet."))
            }
            doc.add(
                Table(3).apply {
                    addCell("A")
                    addCell("B")
                    addCell("C")
                    for (r in 1..5) {
                        addCell("$section-$r")
                        addCell("data")
                        addCell("value")
                    }
                },
            )
        }
        doc.close()

        val bytes = out.toByteArray()
        validateAndSave(bytes, "stress-03-mixed-multipage")
        assertTrue(pdfDoc.numberOfPages >= 3) { "Should have at least 3 pages" }
    }

    @Test
    fun `long paragraph wraps correctly`() {
        val out = ByteArrayOutputStream()
        val pdfDoc = PdfDocument(out)
        val doc = Document(pdfDoc, PageSize.A4)

        val longText =
            "This is a very long paragraph that should wrap across multiple lines. " +
                "The layout engine needs to calculate text width and break at word boundaries. " +
                "If it works correctly, this text will fill several lines on the page without " +
                "overflowing the margins. Each word should be measured and the line should break " +
                "when the accumulated width exceeds the available content width."
        doc.add(Paragraph(longText))
        doc.close()

        val bytes = out.toByteArray()
        validateAndSave(bytes, "stress-04-long-paragraph")
    }

    @Test
    fun `different page sizes`() {
        val sizes =
            listOf(
                PageSize.A3 to "A3",
                PageSize.A4 to "A4",
                PageSize.A5 to "A5",
                PageSize.LETTER to "Letter",
                PageSize.LEGAL to "Legal",
                PageSize(400f, 300f) to "Custom",
            )

        for ((size, name) in sizes) {
            val out = ByteArrayOutputStream()
            val pdfDoc = PdfDocument(out)
            val doc = Document(pdfDoc, size)
            doc.add(Paragraph("Page size: $name (${size.width} x ${size.height})"))
            doc.close()
            validateAndSave(out.toByteArray(), "stress-05-size-$name")
        }
    }

    @Test
    fun `custom margins`() {
        val out = ByteArrayOutputStream()
        val pdfDoc = PdfDocument(out)
        val doc = Document(pdfDoc, PageSize.A4)
        doc.setMargins(72f) // 1 inch all around

        doc.add(Paragraph("1-inch margins on all sides"))
        doc.add(
            Paragraph("Available width: ${doc.pageContentWidth}pt")
                .setColor(Color.GRAY),
        )
        doc.close()

        validateAndSave(out.toByteArray(), "stress-06-margins")
        // A4 = 595pt wide, minus 72*2 = 451pt
        assertEquals(451f, doc.pageContentWidth, 1f)
    }

    @Test
    fun `all text alignments`() {
        val out = ByteArrayOutputStream()
        val pdfDoc = PdfDocument(out)
        val doc = Document(pdfDoc)

        doc.add(Paragraph("Left aligned (default)").setAlignment(Paragraph.Alignment.LEFT))
        doc.add(Paragraph("Center aligned").setAlignment(Paragraph.Alignment.CENTER))
        doc.add(Paragraph("Right aligned").setAlignment(Paragraph.Alignment.RIGHT))
        doc.close()

        validateAndSave(out.toByteArray(), "stress-07-alignment")
    }

    @Test
    fun `all standard fonts`() {
        val out = ByteArrayOutputStream()
        val pdfDoc = PdfDocument(out)
        val doc = Document(pdfDoc)

        val fonts =
            listOf(
                "Helvetica" to PdfFont.helvetica(),
                "Helvetica-Bold" to PdfFont.helveticaBold(),
                "Times-Roman" to PdfFont.timesRoman(),
                "Times-Bold" to PdfFont.timesBold(),
                "Courier" to PdfFont.courier(),
                "Courier-Bold" to PdfFont.courierBold(),
            )

        for ((name, font) in fonts) {
            doc.add(Paragraph("$name: The quick brown fox").setFont(font))
        }
        doc.close()

        validateAndSave(out.toByteArray(), "stress-08-all-fonts")
    }
}
