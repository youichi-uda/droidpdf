package com.droidpdf.core

import com.droidpdf.annotations.PdfHighlightAnnotation
import com.droidpdf.annotations.PdfTextAnnotation
import com.droidpdf.content.PdfCanvas
import com.droidpdf.content.PdfFont
import com.droidpdf.layout.Color
import com.droidpdf.layout.Document
import com.droidpdf.layout.Paragraph
import com.droidpdf.manipulation.PdfMerger
import com.droidpdf.manipulation.PdfSplitter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * Tests that write PDFs and read them back to verify data integrity.
 */
class RoundTripTest {
    @BeforeEach
    fun setup() {
        PdfLog.logger = PdfLog.NoOpLogger()
    }

    @Test
    fun `write and read back - catalog and pages tree`() {
        val out = ByteArrayOutputStream()
        val doc = PdfDocument(out)
        doc.addNewPage(PageSize.A4)
        doc.addNewPage(PageSize.LETTER)
        doc.addNewPage(PageSize.A3)
        doc.close()

        val reader = PdfReader(ByteArrayInputStream(out.toByteArray()))
        val catalog = reader.getCatalog()
        assertNotNull(catalog)
        assertEquals("Catalog", catalog!!.getAsName("Type")?.value)

        val pagesDict = reader.resolve(catalog[PdfName.PAGES]) as? PdfDictionary
        assertNotNull(pagesDict)
        assertEquals("Pages", pagesDict!!.getAsName("Type")?.value)

        val count = pagesDict.getAsInteger("Count")?.value?.toInt()
        assertEquals(3, count)

        val kids = pagesDict.getAsArray("Kids")
        assertNotNull(kids)
        assertEquals(3, kids!!.size())
    }

    @Test
    fun `write and read back - page media box`() {
        val out = ByteArrayOutputStream()
        val doc = PdfDocument(out)
        doc.addNewPage(PageSize.LETTER) // 612 x 792
        doc.close()

        val reader = PdfReader(ByteArrayInputStream(out.toByteArray()))
        val readDoc = PdfDocument(reader)
        assertEquals(1, readDoc.numberOfPages)

        val page = readDoc.getPage(0)
        assertEquals(612f, page.width)
        assertEquals(792f, page.height)
    }

    @Test
    fun `write and read back - text extraction`() {
        val out = ByteArrayOutputStream()
        val pdfDoc = PdfDocument(out)
        val doc = Document(pdfDoc)
        doc.add(Paragraph("Round trip test text"))
        doc.add(Paragraph("Second paragraph here"))
        doc.close()

        val reader = PdfReader(ByteArrayInputStream(out.toByteArray()))
        val extractor = TextExtractor(reader)
        val text = extractor.extractAll()
        assertTrue(text.contains("Round trip test text"))
        assertTrue(text.contains("Second paragraph here"))
    }

    @Test
    fun `write and read back - multiple fonts`() {
        val out = ByteArrayOutputStream()
        val doc = PdfDocument(out)
        val page = doc.addNewPage()

        val helv = PdfFont.helvetica()
        val times = PdfFont.timesRoman()
        val courier = PdfFont.courier()

        val fn1 = helv.registerOn(page)
        val fn2 = times.registerOn(page)
        val fn3 = courier.registerOn(page)

        val canvas = PdfCanvas(page)
        canvas.beginText()
            .setFont(fn1, 12f).moveText(72f, 750f).showText("Helvetica text")
            .setFont(fn2, 12f).moveText(0f, -20f).showText("Times text")
            .setFont(fn3, 12f).moveText(0f, -20f).showText("Courier text")
            .endText().flush()

        doc.close()

        val reader = PdfReader(ByteArrayInputStream(out.toByteArray()))
        val extractor = TextExtractor(reader)
        val text = extractor.extractAll()
        assertTrue(text.contains("Helvetica text"))
        assertTrue(text.contains("Times text"))
        assertTrue(text.contains("Courier text"))
    }

    @Test
    fun `write and read back - rotation preserved`() {
        val out = ByteArrayOutputStream()
        val doc = PdfDocument(out)
        val page = doc.addNewPage()
        page.setRotation(90)
        doc.close()

        val reader = PdfReader(ByteArrayInputStream(out.toByteArray()))
        val readDoc = PdfDocument(reader)
        assertEquals(90, readDoc.getPage(0).getRotation())
    }

    @Test
    fun `merge and read back - page count correct`() {
        fun makePdf(pages: Int): ByteArray {
            val o = ByteArrayOutputStream()
            val d = PdfDocument(o)
            repeat(pages) {
                val p = d.addNewPage()
                val f = PdfFont.helvetica()
                val fn = f.registerOn(p)
                PdfCanvas(p).beginText().setFont(fn, 12f).moveText(72f, 750f)
                    .showText("Page ${it + 1}").endText().flush()
            }
            d.close()
            return o.toByteArray()
        }

        val pdf1 = makePdf(2)
        val pdf2 = makePdf(3)

        val mergedOut = ByteArrayOutputStream()
        val merger = PdfMerger(mergedOut)
        merger.merge(ByteArrayInputStream(pdf1))
        merger.merge(ByteArrayInputStream(pdf2))
        merger.close()

        val reader = PdfReader(ByteArrayInputStream(mergedOut.toByteArray()))
        val catalog = reader.getCatalog()!!
        val pagesDict = reader.resolve(catalog[PdfName.PAGES]) as PdfDictionary
        val count = pagesDict.getAsInteger("Count")?.value?.toInt()
        assertEquals(5, count)
    }

    @Test
    fun `split and read back - correct page count`() {
        val out = ByteArrayOutputStream()
        val pdfDoc = PdfDocument(out)
        repeat(5) {
            val page = pdfDoc.addNewPage()
            val f = PdfFont.helvetica()
            val fn = f.registerOn(page)
            PdfCanvas(page).beginText().setFont(fn, 12f).moveText(72f, 750f)
                .showText("Original page ${it + 1}").endText().flush()
        }
        pdfDoc.close()

        val splitter = PdfSplitter(ByteArrayInputStream(out.toByteArray()))
        val splitOut = ByteArrayOutputStream()
        splitter.extractPages(2, 4, splitOut)

        val reader = PdfReader(ByteArrayInputStream(splitOut.toByteArray()))
        val catalog = reader.getCatalog()!!
        val pagesDict = reader.resolve(catalog[PdfName.PAGES]) as PdfDictionary
        val count = pagesDict.getAsInteger("Count")?.value?.toInt()
        assertEquals(3, count)
    }

    @Test
    fun `complex document - annotations survive round-trip structure`() {
        val out = ByteArrayOutputStream()
        val doc = PdfDocument(out)
        val page = doc.addNewPage()

        val f = PdfFont.helvetica()
        val fn = f.registerOn(page)
        PdfCanvas(page).beginText().setFont(fn, 12f).moveText(72f, 750f)
            .showText("Annotated document").endText().flush()

        PdfTextAnnotation(Rectangle(72f, 700f, 30f, 30f))
            .setContents("Note").setColor(Color.YELLOW).addTo(page)
        PdfHighlightAnnotation(Rectangle(72f, 740f, 200f, 14f))
            .setColor(Color.YELLOW).addTo(page)

        doc.close()

        val bytes = out.toByteArray()
        val result = PdfValidator.validate(bytes)
        assertTrue(result.valid) { "Annotated PDF invalid:\n$result" }

        // Verify annotations are in the page dict
        val content = String(bytes, Charsets.ISO_8859_1)
        assertTrue(content.contains("/Annots"))
        assertTrue(content.contains("/Subtype /Text"))
        assertTrue(content.contains("/Subtype /Highlight"))
    }
}
