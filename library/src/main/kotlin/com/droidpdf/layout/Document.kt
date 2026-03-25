package com.droidpdf.layout

import com.droidpdf.core.PageSize
import com.droidpdf.core.PdfDocument
import com.droidpdf.core.PdfPage

/**
 * High-level document layout API.
 *
 * Manages page flow, margins, and element placement.
 * Elements are added sequentially and automatically flow to new pages.
 */
class Document(
    private val pdfDocument: PdfDocument,
    private val pageSize: PageSize = PageSize.A4,
) {
    private var marginTop: Float = 36f
    private var marginBottom: Float = 36f
    private var marginLeft: Float = 36f
    private var marginRight: Float = 36f

    internal var currentY: Float = 0f
        private set
    private var pageInitialized = false

    /**
     * The content area height (page height minus margins).
     */
    val pageContentHeight: Float
        get() = pageSize.height - marginTop - marginBottom

    /**
     * The content area width (page width minus margins).
     */
    val pageContentWidth: Float
        get() = pageSize.width - marginLeft - marginRight

    /**
     * Remaining height on current page.
     */
    val remainingHeight: Float
        get() = currentY - marginBottom

    /**
     * The current page being written to.
     */
    val currentPage: PdfPage
        get() {
            ensurePageInitialized()
            return pdfDocument.getPage(pdfDocument.numberOfPages - 1)
        }

    fun setMargins(
        top: Float,
        right: Float,
        bottom: Float,
        left: Float,
    ): Document {
        this.marginTop = top
        this.marginRight = right
        this.marginBottom = bottom
        this.marginLeft = left
        return this
    }

    fun setMargins(margin: Float): Document = setMargins(margin, margin, margin, margin)

    /**
     * Add a layout element (Paragraph, Table, Image, etc.) to the document.
     */
    fun add(element: LayoutElement): Document {
        ensurePageInitialized()
        val context =
            RenderContext(
                document = this,
                x = marginLeft,
                y = currentY,
                availableWidth = pageContentWidth,
                availableHeight = remainingHeight,
            )
        val consumed = element.render(context)
        currentY -= consumed
        return this
    }

    /**
     * Add a new page and reset the cursor.
     */
    fun addNewLayoutPage(): PdfPage {
        val page = pdfDocument.addNewPage(pageSize)
        currentY = pageSize.height - marginTop
        pageInitialized = true
        return page
    }

    /**
     * Close the document.
     */
    fun close() {
        pdfDocument.close()
    }

    private fun ensurePageInitialized() {
        if (!pageInitialized) {
            addNewLayoutPage()
        }
    }
}
