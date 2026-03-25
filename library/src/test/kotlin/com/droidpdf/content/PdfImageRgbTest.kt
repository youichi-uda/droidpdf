package com.droidpdf.content

import com.droidpdf.core.PageSize
import com.droidpdf.core.PdfDocument
import com.droidpdf.core.PdfLog
import com.droidpdf.core.PdfValidator
import com.droidpdf.layout.Document
import com.droidpdf.layout.Image
import com.droidpdf.layout.Paragraph
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Tests PdfImage with real RGB pixel data (no Android/Robolectric needed).
 */
class PdfImageRgbTest {
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

    private fun createGradientRgb(
        width: Int,
        height: Int,
    ): ByteArray {
        val data = ByteArray(width * height * 3)
        var i = 0
        for (y in 0 until height) {
            for (x in 0 until width) {
                data[i++] = (x * 255 / width).toByte() // R
                data[i++] = (y * 255 / height).toByte() // G
                data[i++] = 128.toByte() // B
            }
        }
        return data
    }

    @Test
    fun `RGB image via canvas`() {
        val rgbData = createGradientRgb(100, 80)
        val image = PdfImage.fromRgbBytes(rgbData, 100, 80)

        val out = ByteArrayOutputStream()
        val doc = PdfDocument(out)
        val page = doc.addNewPage(PageSize.A4)

        val canvas = PdfCanvas(page)
        image.drawOn(canvas, page, 72f, 600f, 200f, 160f)
        canvas.flush()
        doc.close()

        val bytes = out.toByteArray()
        File(outputDir, "image-rgb-01-canvas.pdf").writeBytes(bytes)

        val result = PdfValidator.validate(bytes)
        assertTrue(result.valid) { "RGB image PDF invalid:\n$result" }
    }

    @Test
    fun `RGB image via layout API`() {
        val rgbData = createGradientRgb(200, 150)
        val image = PdfImage.fromRgbBytes(rgbData, 200, 150)

        val out = ByteArrayOutputStream()
        val pdfDoc = PdfDocument(out)
        val doc = Document(pdfDoc)

        doc.add(Paragraph("Image Test").setFont(PdfFont.helveticaBold()).setFontSize(18f))
        doc.add(Image(image).scaleToFit(400f))
        doc.add(Paragraph("Caption below image"))
        doc.close()

        val bytes = out.toByteArray()
        File(outputDir, "image-rgb-02-layout.pdf").writeBytes(bytes)

        val result = PdfValidator.validate(bytes)
        assertTrue(result.valid) { "RGB layout PDF invalid:\n$result" }
    }

    @Test
    fun `small red square`() {
        // 10x10 solid red
        val data = ByteArray(10 * 10 * 3)
        for (i in data.indices step 3) {
            data[i] = 255.toByte() // R
            data[i + 1] = 0 // G
            data[i + 2] = 0 // B
        }
        val image = PdfImage.fromRgbBytes(data, 10, 10)

        val out = ByteArrayOutputStream()
        val doc = PdfDocument(out)
        val page = doc.addNewPage()
        val canvas = PdfCanvas(page)
        image.drawOn(canvas, page, 100f, 700f, 50f, 50f)
        canvas.flush()
        doc.close()

        val bytes = out.toByteArray()
        File(outputDir, "image-rgb-03-red-square.pdf").writeBytes(bytes)

        val result = PdfValidator.validate(bytes)
        assertTrue(result.valid) { "Red square PDF invalid:\n$result" }
    }
}
