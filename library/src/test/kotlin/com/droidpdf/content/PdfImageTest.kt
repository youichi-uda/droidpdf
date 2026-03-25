package com.droidpdf.content

import android.graphics.Bitmap
import android.graphics.Color
import com.droidpdf.core.PageSize
import com.droidpdf.core.PdfDocument
import com.droidpdf.core.PdfLog
import com.droidpdf.core.PdfValidator
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class PdfImageTest {
    companion object {
        private val outputDir = File("build/test-pdfs").also { it.mkdirs() }
    }

    private fun setup() {
        PdfLog.logger = PdfLog.NoOpLogger()
    }

    private fun createTestBitmap(
        width: Int,
        height: Int,
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        // Draw a simple gradient pattern
        for (y in 0 until height) {
            for (x in 0 until width) {
                val r = (x * 255 / width)
                val g = (y * 255 / height)
                val b = 128
                bitmap.setPixel(x, y, Color.rgb(r, g, b))
            }
        }
        return bitmap
    }

    @Test
    fun `embed bitmap image in PDF`() {
        setup()
        val bitmap = createTestBitmap(100, 80)
        val image = PdfImage.fromBitmap(bitmap)

        assertNotNull(image)
        assertTrue(image.width == 100)
        assertTrue(image.height == 80)

        val out = ByteArrayOutputStream()
        val doc = PdfDocument(out)
        val page = doc.addNewPage(PageSize.A4)

        val canvas = PdfCanvas(page)
        image.drawOn(canvas, page, 72f, 600f, 200f, 160f)
        canvas.flush()

        doc.close()

        val bytes = out.toByteArray()
        File(outputDir, "image-01-bitmap.pdf").writeBytes(bytes)

        val result = PdfValidator.validate(bytes)
        assertTrue(result.valid) { "Image PDF invalid:\n$result" }

        val content = String(bytes, Charsets.ISO_8859_1)
        assertTrue(content.contains("/Subtype /Image")) { "Should contain Image XObject" }
        assertTrue(content.contains("/FlateDecode")) { "Should use FlateDecode compression" }
    }

    @Test
    fun `embed image with layout API`() {
        setup()
        val bitmap = createTestBitmap(200, 150)
        val pdfImage = PdfImage.fromBitmap(bitmap)

        val out = ByteArrayOutputStream()
        val pdfDoc = PdfDocument(out)
        val doc = com.droidpdf.layout.Document(pdfDoc)

        doc.add(
            com.droidpdf.layout.Paragraph("Document with Image")
                .setFont(PdfFont.helveticaBold())
                .setFontSize(18f),
        )
        doc.add(
            com.droidpdf.layout.Image(pdfImage).scaleToFit(300f),
        )
        doc.add(
            com.droidpdf.layout.Paragraph("Caption below the image"),
        )

        doc.close()

        val bytes = out.toByteArray()
        File(outputDir, "image-02-layout.pdf").writeBytes(bytes)

        val result = PdfValidator.validate(bytes)
        assertTrue(result.valid) { "Image layout PDF invalid:\n$result" }
    }

    @Test
    fun `embed JPEG from stream`() {
        setup()
        // Create a minimal valid JPEG in memory
        val bitmap = createTestBitmap(50, 50)
        val jpegStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, jpegStream)
        val jpegBytes = jpegStream.toByteArray()

        val image = PdfImage.fromJpeg(java.io.ByteArrayInputStream(jpegBytes))
        assertNotNull(image)

        val out = ByteArrayOutputStream()
        val doc = PdfDocument(out)
        val page = doc.addNewPage()
        val canvas = PdfCanvas(page)
        image.drawOn(canvas, page, 72f, 600f, 150f, 150f)
        canvas.flush()
        doc.close()

        val bytes = out.toByteArray()
        File(outputDir, "image-03-jpeg.pdf").writeBytes(bytes)

        val result = PdfValidator.validate(bytes)
        assertTrue(result.valid) { "JPEG PDF invalid:\n$result" }

        val content = String(bytes, Charsets.ISO_8859_1)
        assertTrue(content.contains("/DCTDecode")) { "JPEG should use DCTDecode" }
    }

    @Test
    fun `embed image with transparency`() {
        setup()
        val bitmap = Bitmap.createBitmap(60, 60, Bitmap.Config.ARGB_8888)
        for (y in 0 until 60) {
            for (x in 0 until 60) {
                val alpha = if ((x + y) % 2 == 0) 255 else 128
                bitmap.setPixel(x, y, Color.argb(alpha, 255, 0, 0))
            }
        }

        val image = PdfImage.fromBitmap(bitmap)

        val out = ByteArrayOutputStream()
        val doc = PdfDocument(out)
        val page = doc.addNewPage()
        val canvas = PdfCanvas(page)
        image.drawOn(canvas, page, 72f, 700f, 100f, 100f)
        canvas.flush()
        doc.close()

        val bytes = out.toByteArray()
        File(outputDir, "image-04-alpha.pdf").writeBytes(bytes)

        val result = PdfValidator.validate(bytes)
        assertTrue(result.valid) { "Alpha image PDF invalid:\n$result" }

        val content = String(bytes, Charsets.ISO_8859_1)
        assertTrue(content.contains("/SMask")) { "Should have soft mask for transparency" }
    }
}
