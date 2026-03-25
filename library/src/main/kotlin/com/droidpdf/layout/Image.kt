package com.droidpdf.layout

import com.droidpdf.content.PdfCanvas
import com.droidpdf.content.PdfImage

/**
 * An image layout element.
 *
 * Wraps a PdfImage and adds layout capabilities (sizing, alignment).
 */
class Image(private val pdfImage: PdfImage) : LayoutElement {
    private var drawWidth: Float? = null
    private var drawHeight: Float? = null
    private var alignment: Paragraph.Alignment = Paragraph.Alignment.LEFT
    private var marginBottom: Float = 8f

    /**
     * Set explicit width. Height will scale proportionally if not set.
     */
    fun setWidth(width: Float): Image {
        this.drawWidth = width
        return this
    }

    /**
     * Set explicit height. Width will scale proportionally if not set.
     */
    fun setHeight(height: Float): Image {
        this.drawHeight = height
        return this
    }

    /**
     * Scale to fit within the given width, maintaining aspect ratio.
     */
    fun scaleToFit(maxWidth: Float): Image {
        val ratio = maxWidth / pdfImage.width.toFloat()
        this.drawWidth = maxWidth
        this.drawHeight = pdfImage.height * ratio
        return this
    }

    fun setAlignment(alignment: Paragraph.Alignment): Image {
        this.alignment = alignment
        return this
    }

    fun setMarginBottom(margin: Float): Image {
        this.marginBottom = margin
        return this
    }

    override fun render(context: RenderContext): Float {
        val w = resolveWidth(context.availableWidth)
        val h = resolveHeight(w)
        val totalHeight = h + marginBottom

        // Check if we need a new page
        if (totalHeight > context.availableHeight && context.availableHeight < context.document.pageContentHeight) {
            context.document.addNewLayoutPage()
            return render(
                context.copy(
                    y = context.document.currentY,
                    availableHeight = context.document.remainingHeight,
                ),
            )
        }

        val page = context.document.currentPage
        val canvas = PdfCanvas(page)

        val xPos =
            when (alignment) {
                Paragraph.Alignment.LEFT -> context.x
                Paragraph.Alignment.CENTER -> context.x + (context.availableWidth - w) / 2
                Paragraph.Alignment.RIGHT -> context.x + context.availableWidth - w
            }

        // PDF images are drawn from bottom-left corner
        val yPos = context.y - h

        pdfImage.drawOn(canvas, page, xPos, yPos, w, h)
        canvas.flush()

        return totalHeight
    }

    private fun resolveWidth(availableWidth: Float): Float {
        if (drawWidth != null) return drawWidth!!.coerceAtMost(availableWidth)
        if (drawHeight != null) {
            val ratio = drawHeight!! / pdfImage.height.toFloat()
            return (pdfImage.width * ratio).coerceAtMost(availableWidth)
        }
        return pdfImage.width.toFloat().coerceAtMost(availableWidth)
    }

    private fun resolveHeight(resolvedWidth: Float): Float {
        if (drawHeight != null) return drawHeight!!
        val ratio = resolvedWidth / pdfImage.width.toFloat()
        return pdfImage.height * ratio
    }
}
