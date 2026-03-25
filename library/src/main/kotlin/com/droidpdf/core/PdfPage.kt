package com.droidpdf.core

/**
 * Represents a single page in a PDF document.
 */
class PdfPage internal constructor(
    private val document: PdfDocument,
    private val pageSize: PageSize,
) {
    private val contentOperations = mutableListOf<String>()
    private val fonts = mutableMapOf<String, PdfDictionary>()
    private val xObjects = mutableMapOf<String, PdfObject>()
    private var sourceDictionary: PdfDictionary? = null

    /**
     * Get the page width in points.
     */
    val width: Float get() = pageSize.width

    /**
     * Get the page height in points.
     */
    val height: Float get() = pageSize.height

    /**
     * Add raw content stream operations.
     * Used internally by higher-level APIs (PdfCanvas, layout engine).
     */
    fun addContent(operations: String) {
        contentOperations.add(operations)
    }

    /**
     * Register a font for use on this page.
     * Returns the font resource name (e.g., "F1").
     */
    fun addFont(fontDict: PdfDictionary): String {
        val name = "F${fonts.size + 1}"
        fonts[name] = fontDict
        return name
    }

    /**
     * Register an XObject (image) for use on this page.
     * Returns the XObject resource name (e.g., "Im1").
     */
    fun addXObject(xObject: PdfObject): String {
        val name = "Im${xObjects.size + 1}"
        xObjects[name] = xObject
        return name
    }

    internal fun buildContentStream(): PdfStream? {
        if (contentOperations.isEmpty()) return null
        val content = contentOperations.joinToString("\n")
        val stream = PdfStream()
        stream.data = content.toByteArray(Charsets.US_ASCII)
        return stream
    }

    internal fun buildPageDictionary(
        pagesRef: PdfIndirectReference,
        contentRef: PdfIndirectReference?,
    ): PdfDictionary {
        val dict = PdfDictionary()
        dict.put(PdfName.TYPE, PdfName.PAGE)
        dict.put(PdfName.PARENT, pagesRef)
        dict.put(
            PdfName.MEDIABOX,
            PdfArray(
                PdfInteger(0),
                PdfInteger(0),
                PdfReal(pageSize.width),
                PdfReal(pageSize.height),
            ),
        )

        // Resources
        val resources = PdfDictionary()
        if (fonts.isNotEmpty()) {
            val fontDict = PdfDictionary()
            fonts.forEach { (name, font) ->
                fontDict.put(name, font)
            }
            resources.put(PdfName.FONT, fontDict)
        }
        if (xObjects.isNotEmpty()) {
            val xObjDict = PdfDictionary()
            xObjects.forEach { (name, xObj) ->
                xObjDict.put(name, xObj)
            }
            resources.put("XObject", xObjDict)
        }
        // ProcSet (recommended for compatibility)
        val procSet = PdfArray(PdfName.PDF, PdfName.TEXT)
        if (xObjects.isNotEmpty()) {
            procSet.add(PdfName.IMAGE_C)
            procSet.add(PdfName.IMAGE_B)
        }
        resources.put(PdfName.PROCSET, procSet)
        dict.put(PdfName.RESOURCES, resources)

        if (contentRef != null) {
            dict.put(PdfName.CONTENTS, contentRef)
        }

        return dict
    }

    companion object {
        /**
         * Create a PdfPage from an existing page dictionary (read mode).
         */
        internal fun fromDictionary(
            document: PdfDocument,
            dict: PdfDictionary,
        ): PdfPage {
            val mediaBox = dict.getAsArray("MediaBox")
            val pageSize =
                if (mediaBox != null && mediaBox.size() >= 4) {
                    val width =
                        (mediaBox[2] as? PdfReal)?.value?.toFloat()
                            ?: (mediaBox[2] as? PdfInteger)?.value?.toFloat()
                            ?: PageSize.A4.width
                    val height =
                        (mediaBox[3] as? PdfReal)?.value?.toFloat()
                            ?: (mediaBox[3] as? PdfInteger)?.value?.toFloat()
                            ?: PageSize.A4.height
                    PageSize(width, height)
                } else {
                    PageSize.A4
                }

            val page = PdfPage(document, pageSize)
            page.sourceDictionary = dict
            return page
        }
    }
}

/**
 * Standard page sizes in points (1 point = 1/72 inch).
 */
data class PageSize(val width: Float, val height: Float) {
    companion object {
        // ISO 32000-1, no specific section - common page sizes
        val A4 = PageSize(595f, 842f)
        val A3 = PageSize(842f, 1191f)
        val A5 = PageSize(420f, 595f)
        val LETTER = PageSize(612f, 792f)
        val LEGAL = PageSize(612f, 1008f)
    }
}
