package com.droidpdf.content

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.droidpdf.core.PdfInteger
import com.droidpdf.core.PdfName
import com.droidpdf.core.PdfPage
import com.droidpdf.core.PdfStream
import java.io.ByteArrayOutputStream
import java.io.InputStream

/**
 * PDF Image embedding.
 *
 * Supports JPEG, PNG, and WebP images via Android BitmapFactory.
 * Images are embedded as XObject Image resources.
 */
class PdfImage private constructor(
    private val imageStream: PdfStream,
    val width: Int,
    val height: Int,
) {
    private var registeredName: String? = null

    /**
     * Register this image on a page as an XObject resource.
     * Returns the resource name (e.g., "Im1").
     */
    fun registerOn(page: PdfPage): String {
        if (registeredName == null) {
            registeredName = page.addXObject(imageStream)
        }
        return registeredName!!
    }

    /**
     * Draw this image on a canvas at the specified position and size.
     */
    fun drawOn(
        canvas: PdfCanvas,
        page: PdfPage,
        x: Float,
        y: Float,
        drawWidth: Float,
        drawHeight: Float,
    ) {
        val name = registerOn(page)
        canvas.saveState()
        canvas.concatMatrix(drawWidth, 0f, 0f, drawHeight, x, y)
        canvas.addXObject(name)
        canvas.restoreState()
    }

    companion object {
        /**
         * Create a PdfImage from a JPEG input stream.
         * JPEG data is embedded directly (DCTDecode) without re-encoding.
         */
        fun fromJpeg(inputStream: InputStream): PdfImage {
            val jpegBytes = inputStream.readBytes()

            // Decode just enough to get dimensions
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size, options)

            val stream = PdfStream()
            stream.data = jpegBytes
            stream.dictionary.put(PdfName.TYPE, PdfName("XObject"))
            stream.dictionary.put(PdfName.SUBTYPE, PdfName("Image"))
            stream.dictionary.put("Width", PdfInteger(options.outWidth))
            stream.dictionary.put("Height", PdfInteger(options.outHeight))
            stream.dictionary.put("ColorSpace", PdfName("DeviceRGB"))
            stream.dictionary.put("BitsPerComponent", PdfInteger(8))
            stream.dictionary.put(PdfName.FILTER, PdfName("DCTDecode"))

            return PdfImage(stream, options.outWidth, options.outHeight)
        }

        /**
         * Create a PdfImage from a PNG or other bitmap-format input stream.
         * The image is decoded and stored as raw RGB data with FlateDecode compression.
         */
        fun fromBitmap(inputStream: InputStream): PdfImage {
            val bitmap =
                BitmapFactory.decodeStream(inputStream)
                    ?: throw IllegalArgumentException("Could not decode image")
            return fromBitmap(bitmap)
        }

        /**
         * Create a PdfImage from an Android Bitmap.
         */
        fun fromBitmap(bitmap: Bitmap): PdfImage {
            val width = bitmap.width
            val height = bitmap.height
            val hasAlpha = bitmap.hasAlpha()

            // Extract raw RGB(A) pixel data
            val rgbData = ByteArrayOutputStream()
            val alphaData = if (hasAlpha) ByteArrayOutputStream() else null

            for (y in 0 until height) {
                for (x in 0 until width) {
                    val pixel = bitmap.getPixel(x, y)
                    rgbData.write((pixel shr 16) and 0xFF) // R
                    rgbData.write((pixel shr 8) and 0xFF) // G
                    rgbData.write(pixel and 0xFF) // B
                    alphaData?.write((pixel shr 24) and 0xFF) // A
                }
            }

            // Compress with Flate
            val compressedRgb = flateCompress(rgbData.toByteArray())

            val stream = PdfStream()
            stream.data = compressedRgb
            stream.dictionary.put(PdfName.TYPE, PdfName("XObject"))
            stream.dictionary.put(PdfName.SUBTYPE, PdfName("Image"))
            stream.dictionary.put("Width", PdfInteger(width))
            stream.dictionary.put("Height", PdfInteger(height))
            stream.dictionary.put("ColorSpace", PdfName("DeviceRGB"))
            stream.dictionary.put("BitsPerComponent", PdfInteger(8))
            stream.dictionary.put(PdfName.FILTER, PdfName.FLATE_DECODE)

            // Add soft mask for transparency
            if (hasAlpha && alphaData != null) {
                val compressedAlpha = flateCompress(alphaData.toByteArray())
                val maskStream = PdfStream()
                maskStream.data = compressedAlpha
                maskStream.dictionary.put(PdfName.TYPE, PdfName("XObject"))
                maskStream.dictionary.put(PdfName.SUBTYPE, PdfName("Image"))
                maskStream.dictionary.put("Width", PdfInteger(width))
                maskStream.dictionary.put("Height", PdfInteger(height))
                maskStream.dictionary.put("ColorSpace", PdfName("DeviceGray"))
                maskStream.dictionary.put("BitsPerComponent", PdfInteger(8))
                maskStream.dictionary.put(PdfName.FILTER, PdfName.FLATE_DECODE)
                stream.dictionary.put("SMask", maskStream)
            }

            return PdfImage(stream, width, height)
        }

        /**
         * Create a PdfImage from raw RGB byte data (no Android dependency).
         * Data must be width*height*3 bytes in R,G,B order, top-to-bottom.
         */
        fun fromRgbBytes(
            rgbData: ByteArray,
            width: Int,
            height: Int,
        ): PdfImage {
            require(rgbData.size == width * height * 3) {
                "Expected ${width * height * 3} bytes, got ${rgbData.size}"
            }
            val compressed = flateCompress(rgbData)
            val stream = PdfStream()
            stream.data = compressed
            stream.dictionary.put(PdfName.TYPE, PdfName("XObject"))
            stream.dictionary.put(PdfName.SUBTYPE, PdfName("Image"))
            stream.dictionary.put("Width", PdfInteger(width))
            stream.dictionary.put("Height", PdfInteger(height))
            stream.dictionary.put("ColorSpace", PdfName("DeviceRGB"))
            stream.dictionary.put("BitsPerComponent", PdfInteger(8))
            stream.dictionary.put(PdfName.FILTER, PdfName.FLATE_DECODE)
            return PdfImage(stream, width, height)
        }

        private fun flateCompress(data: ByteArray): ByteArray {
            val output = ByteArrayOutputStream()
            java.util.zip.DeflaterOutputStream(output).use { it.write(data) }
            return output.toByteArray()
        }
    }
}
