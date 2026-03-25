package com.droidpdf.annotations

import com.droidpdf.core.PdfArray
import com.droidpdf.core.PdfDictionary
import com.droidpdf.core.PdfInteger
import com.droidpdf.core.PdfName
import com.droidpdf.core.PdfPage
import com.droidpdf.core.PdfReal
import com.droidpdf.core.PdfString
import com.droidpdf.core.Rectangle
import com.droidpdf.layout.Color

/**
 * Base class for PDF annotations (ISO 32000-1, 12.5).
 */
sealed class PdfAnnotation(
    protected val rect: Rectangle,
    protected val subtype: String,
) {
    protected var contents: String? = null
    protected var color: Color? = null
    protected var opacity: Float = 1f

    fun setContents(text: String): PdfAnnotation {
        this.contents = text
        return this
    }

    fun setColor(color: Color): PdfAnnotation {
        this.color = color
        return this
    }

    fun setOpacity(opacity: Float): PdfAnnotation {
        this.opacity = opacity.coerceIn(0f, 1f)
        return this
    }

    /**
     * Add this annotation to a page.
     */
    fun addTo(page: PdfPage) {
        page.addAnnotation(buildDictionary())
    }

    protected open fun buildDictionary(): PdfDictionary {
        val dict = PdfDictionary()
        dict.put(PdfName.TYPE, PdfName("Annot"))
        dict.put(PdfName.SUBTYPE, PdfName(subtype))
        dict.put("Rect", rect.toPdfArray())

        contents?.let { dict.put("Contents", PdfString(it)) }

        color?.let {
            dict.put("C", PdfArray(PdfReal(it.r), PdfReal(it.g), PdfReal(it.b)))
        }

        if (opacity < 1f) {
            dict.put("CA", PdfReal(opacity))
        }

        return dict
    }
}

// --- Text Markup Annotations (ISO 32000-1, 12.5.6.10) ---

/**
 * Text highlight annotation.
 */
class PdfHighlightAnnotation(rect: Rectangle) : PdfAnnotation(rect, "Highlight") {
    override fun buildDictionary(): PdfDictionary {
        val dict = super.buildDictionary()
        dict.put("QuadPoints", buildQuadPoints())
        return dict
    }

    private fun buildQuadPoints(): PdfArray =
        PdfArray(
            PdfReal(rect.left),
            PdfReal(rect.top),
            PdfReal(rect.right),
            PdfReal(rect.top),
            PdfReal(rect.left),
            PdfReal(rect.bottom),
            PdfReal(rect.right),
            PdfReal(rect.bottom),
        )
}

/**
 * Text underline annotation.
 */
class PdfUnderlineAnnotation(rect: Rectangle) : PdfAnnotation(rect, "Underline") {
    override fun buildDictionary(): PdfDictionary {
        val dict = super.buildDictionary()
        dict.put("QuadPoints", buildQuadPoints())
        return dict
    }

    private fun buildQuadPoints(): PdfArray =
        PdfArray(
            PdfReal(rect.left),
            PdfReal(rect.top),
            PdfReal(rect.right),
            PdfReal(rect.top),
            PdfReal(rect.left),
            PdfReal(rect.bottom),
            PdfReal(rect.right),
            PdfReal(rect.bottom),
        )
}

/**
 * Strikeout annotation.
 */
class PdfStrikeoutAnnotation(rect: Rectangle) : PdfAnnotation(rect, "StrikeOut") {
    override fun buildDictionary(): PdfDictionary {
        val dict = super.buildDictionary()
        dict.put("QuadPoints", buildQuadPoints())
        return dict
    }

    private fun buildQuadPoints(): PdfArray =
        PdfArray(
            PdfReal(rect.left),
            PdfReal(rect.top),
            PdfReal(rect.right),
            PdfReal(rect.top),
            PdfReal(rect.left),
            PdfReal(rect.bottom),
            PdfReal(rect.right),
            PdfReal(rect.bottom),
        )
}

// --- Free Text Annotation (ISO 32000-1, 12.5.6.6) ---

/**
 * Free text annotation (text box directly on page).
 */
class PdfFreeTextAnnotation(rect: Rectangle) : PdfAnnotation(rect, "FreeText") {
    private var defaultAppearance: String = "/Helv 12 Tf 0 g"
    private var fontSize: Float = 12f

    fun setFontSize(size: Float): PdfFreeTextAnnotation {
        this.fontSize = size
        this.defaultAppearance = "/Helv ${"%.0f".format(size)} Tf 0 g"
        return this
    }

    override fun buildDictionary(): PdfDictionary {
        val dict = super.buildDictionary()
        dict.put("DA", PdfString(defaultAppearance))
        return dict
    }
}

// --- Stamp Annotation (ISO 32000-1, 12.5.6.12) ---

/**
 * Stamp annotation (e.g., "Approved", "Draft", "Confidential").
 */
class PdfStampAnnotation(
    rect: Rectangle,
    private val stampName: String = "Draft",
) : PdfAnnotation(rect, "Stamp") {
    override fun buildDictionary(): PdfDictionary {
        val dict = super.buildDictionary()
        dict.put("Name", PdfName(stampName))
        return dict
    }

    companion object {
        // ISO 32000-1, Table 181 - Predefined stamp names
        const val APPROVED = "Approved"
        const val EXPERIMENTAL = "Experimental"
        const val NOT_APPROVED = "NotApproved"
        const val AS_IS = "AsIs"
        const val EXPIRED = "Expired"
        const val NOT_FOR_PUBLIC_RELEASE = "NotForPublicRelease"
        const val CONFIDENTIAL = "Confidential"
        const val FINAL = "Final"
        const val SOLD = "Sold"
        const val DEPARTMENT_SAMPLE = "Departmental"
        const val FOR_COMMENT = "ForComment"
        const val DRAFT = "Draft"
    }
}

// --- Shape Annotations (ISO 32000-1, 12.5.6.8) ---

/**
 * Rectangle annotation.
 */
class PdfSquareAnnotation(rect: Rectangle) : PdfAnnotation(rect, "Square") {
    private var borderWidth: Float = 1f
    private var interiorColor: Color? = null

    fun setBorderWidth(width: Float): PdfSquareAnnotation {
        this.borderWidth = width
        return this
    }

    fun setInteriorColor(color: Color): PdfSquareAnnotation {
        this.interiorColor = color
        return this
    }

    override fun buildDictionary(): PdfDictionary {
        val dict = super.buildDictionary()
        dict.put("BS", buildBorderStyle())
        interiorColor?.let {
            dict.put("IC", PdfArray(PdfReal(it.r), PdfReal(it.g), PdfReal(it.b)))
        }
        return dict
    }

    private fun buildBorderStyle(): PdfDictionary {
        val bs = PdfDictionary()
        bs.put("W", PdfReal(borderWidth))
        bs.put("S", PdfName("S")) // Solid
        return bs
    }
}

/**
 * Circle annotation.
 */
class PdfCircleAnnotation(rect: Rectangle) : PdfAnnotation(rect, "Circle") {
    private var borderWidth: Float = 1f
    private var interiorColor: Color? = null

    fun setBorderWidth(width: Float): PdfCircleAnnotation {
        this.borderWidth = width
        return this
    }

    fun setInteriorColor(color: Color): PdfCircleAnnotation {
        this.interiorColor = color
        return this
    }

    override fun buildDictionary(): PdfDictionary {
        val dict = super.buildDictionary()
        dict.put("BS", buildBorderStyle())
        interiorColor?.let {
            dict.put("IC", PdfArray(PdfReal(it.r), PdfReal(it.g), PdfReal(it.b)))
        }
        return dict
    }

    private fun buildBorderStyle(): PdfDictionary {
        val bs = PdfDictionary()
        bs.put("W", PdfReal(borderWidth))
        bs.put("S", PdfName("S"))
        return bs
    }
}

/**
 * Line annotation.
 */
class PdfLineAnnotation(
    rect: Rectangle,
    private val startX: Float,
    private val startY: Float,
    private val endX: Float,
    private val endY: Float,
) : PdfAnnotation(rect, "Line") {
    private var borderWidth: Float = 1f

    fun setBorderWidth(width: Float): PdfLineAnnotation {
        this.borderWidth = width
        return this
    }

    override fun buildDictionary(): PdfDictionary {
        val dict = super.buildDictionary()
        dict.put(
            "L",
            PdfArray(PdfReal(startX), PdfReal(startY), PdfReal(endX), PdfReal(endY)),
        )
        dict.put("BS", buildBorderStyle())
        return dict
    }

    private fun buildBorderStyle(): PdfDictionary {
        val bs = PdfDictionary()
        bs.put("W", PdfReal(borderWidth))
        bs.put("S", PdfName("S"))
        return bs
    }
}

// --- Ink Annotation (ISO 32000-1, 12.5.6.13) ---

/**
 * Ink (freehand drawing) annotation.
 */
class PdfInkAnnotation(rect: Rectangle) : PdfAnnotation(rect, "Ink") {
    private val paths = mutableListOf<List<Pair<Float, Float>>>()

    fun addPath(points: List<Pair<Float, Float>>): PdfInkAnnotation {
        paths.add(points)
        return this
    }

    override fun buildDictionary(): PdfDictionary {
        val dict = super.buildDictionary()
        val inkList = PdfArray()
        for (path in paths) {
            val pathArray = PdfArray()
            for ((x, y) in path) {
                pathArray.add(PdfReal(x))
                pathArray.add(PdfReal(y))
            }
            inkList.add(pathArray)
        }
        dict.put("InkList", inkList)
        return dict
    }
}

// --- Link Annotation (ISO 32000-1, 12.5.6.5) ---

/**
 * Link annotation (hyperlink).
 */
class PdfLinkAnnotation(rect: Rectangle) : PdfAnnotation(rect, "Link") {
    private var uri: String? = null
    private var destPage: Int? = null

    fun setUri(uri: String): PdfLinkAnnotation {
        this.uri = uri
        this.destPage = null
        return this
    }

    fun setDestinationPage(pageIndex: Int): PdfLinkAnnotation {
        this.destPage = pageIndex
        this.uri = null
        return this
    }

    override fun buildDictionary(): PdfDictionary {
        val dict = super.buildDictionary()
        dict.put("Border", PdfArray(PdfInteger(0), PdfInteger(0), PdfInteger(0)))

        uri?.let {
            val action = PdfDictionary()
            action.put("S", PdfName("URI"))
            action.put("URI", PdfString(it))
            dict.put("A", action)
        }

        destPage?.let {
            dict.put("Dest", PdfArray(PdfInteger(it), PdfName("Fit")))
        }

        return dict
    }
}

// --- Text Annotation (ISO 32000-1, 12.5.6.4) ---

/**
 * Sticky note / text annotation.
 */
class PdfTextAnnotation(rect: Rectangle) : PdfAnnotation(rect, "Text") {
    private var open: Boolean = false
    private var iconName: String = "Note"

    fun setOpen(open: Boolean): PdfTextAnnotation {
        this.open = open
        return this
    }

    fun setIcon(name: String): PdfTextAnnotation {
        this.iconName = name
        return this
    }

    override fun buildDictionary(): PdfDictionary {
        val dict = super.buildDictionary()
        dict.put("Open", com.droidpdf.core.PdfBoolean(open))
        dict.put("Name", PdfName(iconName))
        return dict
    }

    companion object {
        const val ICON_NOTE = "Note"
        const val ICON_COMMENT = "Comment"
        const val ICON_KEY = "Key"
        const val ICON_HELP = "Help"
        const val ICON_PARAGRAPH = "Paragraph"
    }
}
