package com.droidpdf.annotations

import com.droidpdf.core.PageSize
import com.droidpdf.core.PdfDocument
import com.droidpdf.core.PdfLog
import com.droidpdf.core.Rectangle
import com.droidpdf.layout.Color
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream

class PdfAnnotationTest {
    @BeforeEach
    fun setup() {
        PdfLog.logger = PdfLog.NoOpLogger()
    }

    private fun createPdfWithAnnotation(annotation: PdfAnnotation): String {
        val output = ByteArrayOutputStream()
        val doc = PdfDocument(output)
        val page = doc.addNewPage(PageSize.A4)
        annotation.addTo(page)
        doc.close()
        return output.toString("US-ASCII")
    }

    @Test
    fun `text annotation produces valid PDF`() {
        val annot =
            PdfTextAnnotation(Rectangle(100f, 700f, 50f, 50f))
                .setContents("A note")
                .setColor(Color.YELLOW) as PdfTextAnnotation
        annot.setOpen(true)

        val pdf = createPdfWithAnnotation(annot)
        assertTrue(pdf.contains("/Subtype /Text"))
        assertTrue(pdf.contains("(A note)"))
        assertTrue(pdf.contains("/Annots"))
    }

    @Test
    fun `highlight annotation`() {
        val annot =
            PdfHighlightAnnotation(Rectangle(72f, 700f, 200f, 14f))
                .setColor(Color.YELLOW)

        val pdf = createPdfWithAnnotation(annot)
        assertTrue(pdf.contains("/Subtype /Highlight"))
        assertTrue(pdf.contains("/QuadPoints"))
    }

    @Test
    fun `underline annotation`() {
        val annot =
            PdfUnderlineAnnotation(Rectangle(72f, 700f, 200f, 14f))
                .setColor(Color.RED)

        val pdf = createPdfWithAnnotation(annot)
        assertTrue(pdf.contains("/Subtype /Underline"))
    }

    @Test
    fun `strikeout annotation`() {
        val annot = PdfStrikeoutAnnotation(Rectangle(72f, 700f, 200f, 14f))

        val pdf = createPdfWithAnnotation(annot)
        assertTrue(pdf.contains("/Subtype /StrikeOut"))
    }

    @Test
    fun `free text annotation`() {
        val annot = PdfFreeTextAnnotation(Rectangle(100f, 600f, 200f, 50f))
        annot.setContents("Direct text on page")
        annot.setFontSize(16f)

        val pdf = createPdfWithAnnotation(annot)
        assertTrue(pdf.contains("/Subtype /FreeText"))
        assertTrue(pdf.contains("/DA"))
    }

    @Test
    fun `stamp annotation`() {
        val annot =
            PdfStampAnnotation(
                Rectangle(100f, 500f, 200f, 60f),
                PdfStampAnnotation.APPROVED,
            )

        val pdf = createPdfWithAnnotation(annot)
        assertTrue(pdf.contains("/Subtype /Stamp"))
        assertTrue(pdf.contains("/Name /Approved"))
    }

    @Test
    fun `square annotation`() {
        val annot = PdfSquareAnnotation(Rectangle(100f, 400f, 150f, 100f))
        annot.setBorderWidth(2f)
        annot.setColor(Color.RED)
        annot.setInteriorColor(Color.LIGHT_GRAY)

        val pdf = createPdfWithAnnotation(annot)
        assertTrue(pdf.contains("/Subtype /Square"))
        assertTrue(pdf.contains("/BS"))
        assertTrue(pdf.contains("/IC"))
    }

    @Test
    fun `circle annotation`() {
        val annot = PdfCircleAnnotation(Rectangle(100f, 300f, 100f, 100f))
        annot.setColor(Color.BLUE)

        val pdf = createPdfWithAnnotation(annot)
        assertTrue(pdf.contains("/Subtype /Circle"))
    }

    @Test
    fun `line annotation`() {
        val annot =
            PdfLineAnnotation(
                Rectangle(100f, 200f, 300f, 10f),
                100f,
                205f,
                400f,
                205f,
            )
        annot.setColor(Color.BLACK)

        val pdf = createPdfWithAnnotation(annot)
        assertTrue(pdf.contains("/Subtype /Line"))
        assertTrue(pdf.contains("/L"))
    }

    @Test
    fun `ink annotation`() {
        val annot = PdfInkAnnotation(Rectangle(100f, 100f, 200f, 100f))
        annot.addPath(
            listOf(
                100f to 150f,
                150f to 180f,
                200f to 130f,
                250f to 160f,
            ),
        )
        annot.setColor(Color.BLUE)

        val pdf = createPdfWithAnnotation(annot)
        assertTrue(pdf.contains("/Subtype /Ink"))
        assertTrue(pdf.contains("/InkList"))
    }

    @Test
    fun `link annotation with URI`() {
        val annot = PdfLinkAnnotation(Rectangle(72f, 700f, 200f, 14f))
        annot.setUri("https://example.com")

        val pdf = createPdfWithAnnotation(annot)
        assertTrue(pdf.contains("/Subtype /Link"))
        assertTrue(pdf.contains("/URI"))
        assertTrue(pdf.contains("https://example.com"))
    }

    @Test
    fun `link annotation with page destination`() {
        val annot = PdfLinkAnnotation(Rectangle(72f, 700f, 200f, 14f))
        annot.setDestinationPage(0)

        val pdf = createPdfWithAnnotation(annot)
        assertTrue(pdf.contains("/Subtype /Link"))
        assertTrue(pdf.contains("/Dest"))
    }

    @Test
    fun `multiple annotations on one page`() {
        val output = ByteArrayOutputStream()
        val doc = PdfDocument(output)
        val page = doc.addNewPage()

        PdfHighlightAnnotation(Rectangle(72f, 750f, 200f, 14f))
            .setColor(Color.YELLOW)
            .addTo(page)
        PdfTextAnnotation(Rectangle(300f, 750f, 30f, 30f))
            .setContents("Comment")
            .addTo(page)
        PdfSquareAnnotation(Rectangle(72f, 600f, 100f, 50f))
            .setColor(Color.RED)
            .addTo(page)

        doc.close()

        val pdf = output.toString("US-ASCII")
        assertTrue(pdf.contains("/Highlight"))
        assertTrue(pdf.contains("/Text"))
        assertTrue(pdf.contains("/Square"))
    }

    @Test
    fun `annotation opacity`() {
        val annot =
            PdfHighlightAnnotation(Rectangle(72f, 700f, 200f, 14f))
                .setOpacity(0.5f)
                .setColor(Color.YELLOW)

        val pdf = createPdfWithAnnotation(annot)
        assertTrue(pdf.contains("/CA"))
    }
}
