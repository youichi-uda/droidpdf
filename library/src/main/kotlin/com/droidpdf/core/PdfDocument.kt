package com.droidpdf.core

import com.droidpdf.DroidPDF
import java.io.OutputStream

/**
 * Represents a PDF document.
 *
 * Can be created in two modes:
 * - Write mode: new document for writing (PdfWriter provided)
 * - Read mode: existing document for reading (PdfReader provided)
 * - Read-Write mode: existing document for modification (both provided)
 */
class PdfDocument private constructor(
    private val reader: PdfReader?,
    private val writer: PdfWriter?,
    private val outputStream: OutputStream?,
) {
    private val pages = mutableListOf<PdfPage>()
    private var closed = false
    private var hasWarnedLicense = false

    /**
     * Create a new PDF document for writing.
     */
    constructor(outputStream: OutputStream) : this(null, PdfWriter(outputStream), outputStream) {
        warnIfUnlicensed()
    }

    /**
     * Open an existing PDF for reading.
     */
    constructor(reader: PdfReader) : this(reader, null, null) {
        warnIfUnlicensed()
        loadPagesFromReader()
    }

    /**
     * Open an existing PDF for modification.
     */
    constructor(
        reader: PdfReader,
        outputStream: OutputStream,
    ) : this(reader, PdfWriter(outputStream), outputStream) {
        warnIfUnlicensed()
        loadPagesFromReader()
    }

    /**
     * Add a new page with the given size.
     * Returns the new page.
     */
    fun addNewPage(pageSize: PageSize = PageSize.A4): PdfPage {
        check(!closed) { "Document is closed" }
        val page = PdfPage(this, pageSize)
        pages.add(page)
        return page
    }

    /**
     * Get a page by index (0-based).
     */
    fun getPage(index: Int): PdfPage {
        require(index in pages.indices) { "Page index $index out of range (0..${pages.size - 1})" }
        return pages[index]
    }

    /**
     * Get the first page.
     */
    val firstPage: PdfPage
        get() {
            require(pages.isNotEmpty()) { "Document has no pages" }
            return pages[0]
        }

    /**
     * Get the number of pages.
     */
    val numberOfPages: Int get() = pages.size

    /**
     * Close the document and write to output if in write mode.
     */
    fun close() {
        if (closed) return
        closed = true

        if (writer != null) {
            writePdf()
        }
    }

    private fun writePdf() {
        val writer = this.writer ?: return

        // Build page content objects first
        val pageContentRefs =
            pages.map { page ->
                val contentStream = page.buildContentStream()
                if (contentStream != null) {
                    writer.addObject(contentStream)
                } else {
                    null
                }
            }

        // Create Pages tree
        val pagesDict = PdfDictionary()
        pagesDict.put(PdfName.TYPE, PdfName.PAGES)
        pagesDict.put(PdfName.COUNT, PdfInteger(pages.size))
        val pagesRef = writer.addObject(pagesDict) // placeholder, will be updated

        // Create individual Page objects
        val pageRefs =
            pages.mapIndexed { index, page ->
                val pageDict = page.buildPageDictionary(pagesRef, pageContentRefs[index])
                writer.addObject(pageDict)
            }

        // Update Pages Kids array
        val kidsArray = PdfArray()
        pageRefs.forEach { kidsArray.add(it) }
        pagesDict.put(PdfName.KIDS, kidsArray)

        // Create Catalog
        val catalog = PdfDictionary()
        catalog.put(PdfName.TYPE, PdfName.CATALOG)
        catalog.put(PdfName.PAGES, pagesRef)
        val catalogRef = writer.addObject(catalog)

        // Write the PDF
        val trailerExtras = PdfDictionary()
        trailerExtras.put("Root", catalogRef)
        writer.write(trailerExtras)
    }

    private fun loadPagesFromReader() {
        val reader = this.reader ?: return
        val catalog =
            reader.getCatalog() ?: run {
                PdfLog.w("Could not find document catalog")
                return
            }

        val pagesRef = catalog[PdfName.PAGES]
        val pagesDict =
            reader.resolve(pagesRef) as? PdfDictionary ?: run {
                PdfLog.w("Could not find Pages dictionary")
                return
            }

        loadPagesFromNode(pagesDict)
    }

    private fun loadPagesFromNode(node: PdfDictionary) {
        val reader = this.reader ?: return
        val type = node.getAsName("Type")

        when (type) {
            PdfName.PAGES -> {
                val kids = node.getAsArray("Kids") ?: return
                for (kid in kids.elements) {
                    val kidDict = reader.resolve(kid) as? PdfDictionary ?: continue
                    loadPagesFromNode(kidDict)
                }
            }
            PdfName.PAGE -> {
                val page = PdfPage.fromDictionary(this, node)
                pages.add(page)
            }
            else -> PdfLog.w("Unknown page tree node type: ${type?.value}")
        }
    }

    private fun warnIfUnlicensed() {
        if (!hasWarnedLicense) {
            hasWarnedLicense = true
            DroidPDF.warnIfUnlicensed()
        }
    }
}
