package com.droidpdf.manipulation

import com.droidpdf.content.PdfCanvas
import com.droidpdf.content.PdfFont
import com.droidpdf.core.PdfDocument
import com.droidpdf.core.PdfLog
import java.io.ByteArrayOutputStream

/**
 * Helper to create test PDFs with identifiable page content.
 */
object TestHelper {
    init {
        PdfLog.logger = PdfLog.NoOpLogger()
    }

    /**
     * Create a PDF with the given number of pages.
     * Each page contains the text "Page N" for identification.
     */
    fun createTestPdf(pageCount: Int): ByteArray {
        val output = ByteArrayOutputStream()
        val doc = PdfDocument(output)
        val font = PdfFont.helvetica()

        for (i in 1..pageCount) {
            val page = doc.addNewPage()
            val fontName = font.registerOn(page)
            val canvas = PdfCanvas(page)
            canvas.beginText()
                .setFont(fontName, 12f)
                .moveText(72f, 700f)
                .showText("Page $i")
                .endText()
                .flush()
        }
        doc.close()
        return output.toByteArray()
    }

    /**
     * Check if a PDF byte array contains a specific text string.
     */
    fun pdfContainsText(
        pdfBytes: ByteArray,
        text: String,
    ): Boolean {
        return String(pdfBytes, Charsets.US_ASCII).contains(text)
    }
}
