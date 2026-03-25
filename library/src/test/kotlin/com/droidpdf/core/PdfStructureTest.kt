package com.droidpdf.core

import com.droidpdf.annotations.PdfHighlightAnnotation
import com.droidpdf.annotations.PdfStampAnnotation
import com.droidpdf.annotations.PdfTextAnnotation
import com.droidpdf.content.PdfCanvas
import com.droidpdf.content.PdfFont
import com.droidpdf.forms.CheckboxField
import com.droidpdf.forms.PdfAcroForm
import com.droidpdf.forms.TextField
import com.droidpdf.layout.Color
import com.droidpdf.layout.Document
import com.droidpdf.layout.Paragraph
import com.droidpdf.layout.Table
import com.droidpdf.manipulation.PdfMerger
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Validates that generated PDFs have correct binary structure.
 * Each test generates a PDF, runs it through PdfValidator, and
 * saves the output for manual inspection.
 */
class PdfStructureTest {
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
        // Save for manual inspection
        File(outputDir, "$name.pdf").writeBytes(pdfBytes)

        // Validate structure
        val result = PdfValidator.validate(pdfBytes)
        assertTrue(result.valid) {
            "PDF '$name' has structural errors:\n$result"
        }
    }

    @Test
    fun `empty single page PDF`() {
        val out = ByteArrayOutputStream()
        val doc = PdfDocument(out)
        doc.addNewPage()
        doc.close()
        validateAndSave(out.toByteArray(), "01-empty-page")
    }

    @Test
    fun `multiple empty pages`() {
        val out = ByteArrayOutputStream()
        val doc = PdfDocument(out)
        doc.addNewPage(PageSize.A4)
        doc.addNewPage(PageSize.LETTER)
        doc.addNewPage(PageSize.A3)
        doc.close()
        validateAndSave(out.toByteArray(), "02-multi-page")
    }

    @Test
    fun `text with standard font via canvas`() {
        val out = ByteArrayOutputStream()
        val doc = PdfDocument(out)
        val page = doc.addNewPage()
        val font = PdfFont.helvetica()
        val fontName = font.registerOn(page)

        val canvas = PdfCanvas(page)
        canvas.beginText()
            .setFont(fontName, 14f)
            .moveText(72f, 750f)
            .showText("Hello, World!")
            .endText()
            .flush()

        doc.close()
        validateAndSave(out.toByteArray(), "03-text-canvas")
    }

    @Test
    fun `text with layout API`() {
        val out = ByteArrayOutputStream()
        val pdfDoc = PdfDocument(out)
        val doc = Document(pdfDoc)

        doc.add(
            Paragraph("Title Text")
                .setFont(PdfFont.helveticaBold())
                .setFontSize(24f),
        )
        doc.add(Paragraph("Body text in regular Helvetica at 12pt."))
        doc.add(
            Paragraph("Colored text")
                .setColor(Color.RED)
                .setFontSize(16f),
        )
        doc.close()
        validateAndSave(out.toByteArray(), "04-text-layout")
    }

    @Test
    fun `table`() {
        val out = ByteArrayOutputStream()
        val pdfDoc = PdfDocument(out)
        val doc = Document(pdfDoc)

        doc.add(
            Table(3).apply {
                addCell("Name")
                addCell("Age")
                addCell("City")
                addCell("Alice")
                addCell("30")
                addCell("Tokyo")
                addCell("Bob")
                addCell("25")
                addCell("Osaka")
            },
        )
        doc.close()
        validateAndSave(out.toByteArray(), "05-table")
    }

    @Test
    fun `mixed content document`() {
        val out = ByteArrayOutputStream()
        val pdfDoc = PdfDocument(out)
        val doc = Document(pdfDoc)

        doc.add(
            Paragraph("Invoice #1234")
                .setFont(PdfFont.helveticaBold())
                .setFontSize(20f)
                .setAlignment(Paragraph.Alignment.CENTER),
        )
        doc.add(Paragraph("Date: 2026-03-25"))
        doc.add(
            Table(4).apply {
                addCell("Item")
                addCell("Qty")
                addCell("Price")
                addCell("Total")
                addCell("Widget A")
                addCell("10")
                addCell("$9.99")
                addCell("$99.90")
                addCell("Widget B")
                addCell("5")
                addCell("$19.99")
                addCell("$99.95")
            },
        )
        doc.add(
            Paragraph("Total: $199.85")
                .setFont(PdfFont.helveticaBold())
                .setAlignment(Paragraph.Alignment.RIGHT),
        )
        doc.close()
        validateAndSave(out.toByteArray(), "06-mixed-content")
    }

    @Test
    fun `annotations`() {
        val out = ByteArrayOutputStream()
        val doc = PdfDocument(out)
        val page = doc.addNewPage()

        // Add some text first
        val font = PdfFont.helvetica()
        val fn = font.registerOn(page)
        val canvas = PdfCanvas(page)
        canvas.beginText()
            .setFont(fn, 12f)
            .moveText(72f, 750f)
            .showText("This text has annotations below")
            .endText()
            .flush()

        // Text baseline is at y=750, font size 12pt
        // Ascent ~9.6pt, descent ~2.4pt → text visual range: y≈748 to y≈760
        PdfTextAnnotation(Rectangle(280f, 748f, 24f, 24f))
            .setContents("A sticky note")
            .setColor(Color.YELLOW)
            .addTo(page)

        PdfHighlightAnnotation(Rectangle(72f, 748f, 200f, 12f))
            .setColor(Color.YELLOW)
            .addTo(page)

        PdfStampAnnotation(Rectangle(300f, 600f, 150f, 50f), PdfStampAnnotation.APPROVED)
            .addTo(page)

        doc.close()
        validateAndSave(out.toByteArray(), "07-annotations")
    }

    @Test
    fun `form fields`() {
        val out = ByteArrayOutputStream()
        val doc = PdfDocument(out)
        val page = doc.addNewPage()

        val form = PdfAcroForm()
        form.addField(
            TextField("name", Rectangle(72f, 700f, 200f, 20f)).apply { setValue("John") },
            page,
        )
        form.addField(
            CheckboxField("agree", Rectangle(72f, 660f, 15f, 15f)).apply { setChecked(true) },
            page,
        )

        doc.close()
        validateAndSave(out.toByteArray(), "08-forms")
    }

    @Test
    fun `merged PDF`() {
        // Create two source PDFs
        fun makePdf(text: String): ByteArray {
            val o = ByteArrayOutputStream()
            val d = PdfDocument(o)
            val p = d.addNewPage()
            val f = PdfFont.helvetica()
            val fn = f.registerOn(p)
            PdfCanvas(p).beginText().setFont(fn, 12f).moveText(72f, 750f)
                .showText(text).endText().flush()
            d.close()
            return o.toByteArray()
        }

        val pdf1 = makePdf("Page from PDF 1")
        val pdf2 = makePdf("Page from PDF 2")

        val out = ByteArrayOutputStream()
        val merger = PdfMerger(out)
        merger.merge(ByteArrayInputStream(pdf1))
        merger.merge(ByteArrayInputStream(pdf2))
        merger.close()

        validateAndSave(out.toByteArray(), "09-merged")
    }

    @Test
    fun `rotated page`() {
        val out = ByteArrayOutputStream()
        val doc = PdfDocument(out)
        val page = doc.addNewPage()
        page.setRotation(90)

        val font = PdfFont.helvetica()
        val fn = font.registerOn(page)
        PdfCanvas(page).beginText().setFont(fn, 14f).moveText(72f, 400f)
            .showText("Rotated 90 degrees").endText().flush()

        doc.close()
        validateAndSave(out.toByteArray(), "10-rotated")
    }

    @Test
    fun `read back and validate catalog structure`() {
        val out = ByteArrayOutputStream()
        val pdfDoc = PdfDocument(out)
        val doc = Document(pdfDoc)
        doc.add(Paragraph("Test document for read-back"))
        doc.add(Paragraph("Second paragraph"))
        doc.close()

        val bytes = out.toByteArray()
        validateAndSave(bytes, "11-readback")

        // Read it back and verify structure
        val reader = PdfReader(ByteArrayInputStream(bytes))
        val catalog = reader.getCatalog()
        assertTrue(catalog != null) { "Catalog should exist" }
        assertTrue(catalog!!.getAsName("Type")?.value == "Catalog") { "Type should be Catalog" }

        val pagesRef = catalog[PdfName.PAGES]
        val pagesDict = reader.resolve(pagesRef) as? PdfDictionary
        assertTrue(pagesDict != null) { "Pages dict should exist" }
        assertTrue(pagesDict!!.getAsName("Type")?.value == "Pages") { "Type should be Pages" }

        val count = pagesDict.getAsInteger("Count")?.value
        assertTrue(count != null && count > 0) { "Count should be > 0, got $count" }
    }

    @Test
    fun `text extraction round-trip`() {
        val out = ByteArrayOutputStream()
        val pdfDoc = PdfDocument(out)
        val doc = Document(pdfDoc)
        doc.add(Paragraph("Extract me please"))
        doc.add(Paragraph("And this line too"))
        doc.close()

        val bytes = out.toByteArray()
        validateAndSave(bytes, "12-extraction")

        val reader = PdfReader(ByteArrayInputStream(bytes))
        val extractor = TextExtractor(reader)
        val text = extractor.extractAll()
        assertTrue(text.contains("Extract me please")) { "Should find first text, got: $text" }
        assertTrue(text.contains("And this line too")) { "Should find second text, got: $text" }
    }
}
