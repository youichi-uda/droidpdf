package com.droidpdf.content

import com.droidpdf.core.PdfArray
import com.droidpdf.core.PdfDictionary
import com.droidpdf.core.PdfInteger
import com.droidpdf.core.PdfName
import com.droidpdf.core.PdfPage
import com.droidpdf.core.PdfStream
import org.apache.fontbox.ttf.CmapLookup
import org.apache.fontbox.ttf.OTFParser
import org.apache.fontbox.ttf.TTFParser
import org.apache.fontbox.ttf.TrueTypeFont
import org.apache.pdfbox.io.RandomAccessReadBuffer
import java.io.ByteArrayOutputStream
import java.io.InputStream

/**
 * PDF Font management.
 *
 * Supports:
 * - Standard 14 fonts (no embedding needed)
 * - TrueType fonts (TTF/OTF embedding via FontBox)
 */
class PdfFont private constructor(
    private val fontDict: PdfDictionary,
    private val fontName: String,
    private val ttf: TrueTypeFont? = null,
    private val cmapLookup: CmapLookup? = null,
    private val unitsPerEm: Float = 1000f,
    private val isStandard: Boolean = false,
) {
    private var registeredName: String? = null

    /**
     * Register this font on a page and return the resource name (e.g., "F1").
     */
    fun registerOn(page: PdfPage): String {
        if (registeredName == null) {
            registeredName = page.addFont(fontDict)
        }
        return registeredName!!
    }

    /**
     * Get the width of a text string in points at the given font size.
     */
    fun getTextWidth(
        text: String,
        fontSize: Float,
    ): Float {
        if (ttf != null) {
            return getTtfTextWidth(text, fontSize)
        }
        // Approximate width for standard fonts (average char width ~0.5 of font size)
        return text.length * fontSize * 0.5f
    }

    private fun getTtfTextWidth(
        text: String,
        fontSize: Float,
    ): Float {
        var width = 0f
        for (c in text) {
            val gid = cmapLookup?.getGlyphId(c.code) ?: 0
            val glyphWidth =
                try {
                    ttf!!.getHorizontalMetrics()?.getAdvanceWidth(gid) ?: 0
                } catch (_: Exception) {
                    0
                }
            width += glyphWidth.toFloat() / unitsPerEm * fontSize
        }
        return width
    }

    /**
     * Get the font ascent in points at the given font size.
     */
    fun getAscent(fontSize: Float): Float {
        if (ttf != null) {
            try {
                val ascender = ttf.getOS2Windows()?.typoAscender
                if (ascender != null) return ascender.toFloat() / unitsPerEm * fontSize
            } catch (_: Exception) {
            }
        }
        return fontSize * 0.8f
    }

    /**
     * Get the font descent in points at the given font size (negative value).
     */
    fun getDescent(fontSize: Float): Float {
        if (ttf != null) {
            try {
                val descender = ttf.getOS2Windows()?.typoDescender
                if (descender != null) return descender.toFloat() / unitsPerEm * fontSize
            } catch (_: Exception) {
            }
        }
        return fontSize * -0.2f
    }

    companion object {
        // ISO 32000-1, 9.6.2.2 - Standard Type 1 Fonts (Standard 14)
        private val STANDARD_FONTS =
            setOf(
                "Helvetica",
                "Helvetica-Bold",
                "Helvetica-Oblique",
                "Helvetica-BoldOblique",
                "Times-Roman",
                "Times-Bold",
                "Times-Italic",
                "Times-BoldItalic",
                "Courier",
                "Courier-Bold",
                "Courier-Oblique",
                "Courier-BoldOblique",
                "Symbol",
                "ZapfDingbats",
            )

        fun createStandard(name: String): PdfFont {
            require(name in STANDARD_FONTS) {
                "Unknown standard font: $name. Available: $STANDARD_FONTS"
            }
            val dict = PdfDictionary()
            dict.put(PdfName.TYPE, PdfName.FONT)
            dict.put(PdfName.SUBTYPE, PdfName("Type1"))
            dict.put("BaseFont", PdfName(name))
            return PdfFont(dict, name, isStandard = true)
        }

        fun helvetica(): PdfFont = createStandard("Helvetica")

        fun helveticaBold(): PdfFont = createStandard("Helvetica-Bold")

        fun timesRoman(): PdfFont = createStandard("Times-Roman")

        fun timesBold(): PdfFont = createStandard("Times-Bold")

        fun courier(): PdfFont = createStandard("Courier")

        fun courierBold(): PdfFont = createStandard("Courier-Bold")

        fun createFromTtf(inputStream: InputStream): PdfFont {
            val parser = TTFParser()
            val ttf = parser.parseEmbedded(inputStream)
            return createFromTrueTypeFont(ttf)
        }

        fun createFromOtf(inputStream: InputStream): PdfFont {
            val parser = OTFParser()
            val rar = RandomAccessReadBuffer(inputStream)
            val ttf = parser.parse(rar)
            return createFromTrueTypeFont(ttf)
        }

        private fun createFromTrueTypeFont(ttf: TrueTypeFont): PdfFont {
            val fontName =
                try {
                    ttf.getName()
                } catch (_: Exception) {
                    "CustomFont"
                } ?: "CustomFont"
            val header = ttf.getHeader()
            val os2 =
                try {
                    ttf.getOS2Windows()
                } catch (_: Exception) {
                    null
                }
            val postScript =
                try {
                    ttf.getPostScript()
                } catch (_: Exception) {
                    null
                }
            val cmap =
                try {
                    ttf.getUnicodeCmapLookup()
                } catch (_: Exception) {
                    null
                }
            val hMetrics =
                try {
                    ttf.getHorizontalMetrics()
                } catch (_: Exception) {
                    null
                }

            val unitsPerEm = header.unitsPerEm
            val scale = 1000f / unitsPerEm

            // Embed the font program
            val fontStream = PdfStream()
            val fontBytes = ByteArrayOutputStream()
            ttf.getOriginalData()?.use { data -> data.copyTo(fontBytes) }
            fontStream.data = fontBytes.toByteArray()
            fontStream.dictionary.put(PdfName("Length1"), PdfInteger(fontStream.data.size))

            // Font descriptor
            val descriptor = PdfDictionary()
            descriptor.put(PdfName.TYPE, PdfName("FontDescriptor"))
            descriptor.put("FontName", PdfName(fontName))
            descriptor.put("Flags", PdfInteger(32)) // Nonsymbolic
            descriptor.put(
                "FontBBox",
                PdfArray(
                    PdfInteger((header.xMin * scale).toInt()),
                    PdfInteger((header.yMin * scale).toInt()),
                    PdfInteger((header.xMax * scale).toInt()),
                    PdfInteger((header.yMax * scale).toInt()),
                ),
            )
            descriptor.put("ItalicAngle", PdfInteger(postScript?.italicAngle?.toInt() ?: 0))
            descriptor.put("Ascent", PdfInteger(os2?.typoAscender?.let { (it * scale).toInt() } ?: 800))
            descriptor.put("Descent", PdfInteger(os2?.typoDescender?.let { (it * scale).toInt() } ?: -200))
            descriptor.put("CapHeight", PdfInteger(os2?.capHeight?.let { (it * scale).toInt() } ?: 700))
            descriptor.put("StemV", PdfInteger(80))
            descriptor.put("FontFile2", fontStream)

            // Font dictionary
            val dict = PdfDictionary()
            dict.put(PdfName.TYPE, PdfName.FONT)
            dict.put(PdfName.SUBTYPE, PdfName("TrueType"))
            dict.put("BaseFont", PdfName(fontName))
            dict.put("Encoding", PdfName("WinAnsiEncoding"))
            dict.put("FontDescriptor", descriptor)

            // Widths array (first 256 characters for WinAnsi)
            val firstChar = 32
            val lastChar = 255
            val widths = PdfArray()
            for (charCode in firstChar..lastChar) {
                val gid = cmap?.getGlyphId(charCode) ?: 0
                val w = hMetrics?.getAdvanceWidth(gid) ?: 0
                widths.add(PdfInteger((w * scale).toInt()))
            }
            dict.put("FirstChar", PdfInteger(firstChar))
            dict.put("LastChar", PdfInteger(lastChar))
            dict.put("Widths", widths)

            return PdfFont(dict, fontName, ttf, cmap, unitsPerEm.toFloat())
        }
    }
}
