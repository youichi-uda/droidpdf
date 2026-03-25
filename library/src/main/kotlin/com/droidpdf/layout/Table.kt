package com.droidpdf.layout

import com.droidpdf.content.PdfCanvas
import com.droidpdf.content.PdfFont

/**
 * A table layout element.
 *
 * Supports configurable column count, cell text, borders, and padding.
 */
class Table(private val columnCount: Int) : LayoutElement {
    private val cells = mutableListOf<Cell>()
    private var font: PdfFont = PdfFont.helvetica()
    private var fontSize: Float = 10f
    private var cellPadding: Float = 4f
    private var borderWidth: Float = 0.5f
    private var borderColor: Color = Color.BLACK
    private var headerRows: Int = 0
    private var headerFont: PdfFont? = null
    private var marginBottom: Float = 8f

    fun addCell(text: String): Table {
        cells.add(Cell(text))
        return this
    }

    fun addCell(cell: Cell): Table {
        cells.add(cell)
        return this
    }

    fun setFont(font: PdfFont): Table {
        this.font = font
        return this
    }

    fun setFontSize(size: Float): Table {
        this.fontSize = size
        return this
    }

    fun setCellPadding(padding: Float): Table {
        this.cellPadding = padding
        return this
    }

    fun setBorderWidth(width: Float): Table {
        this.borderWidth = width
        return this
    }

    fun setBorderColor(color: Color): Table {
        this.borderColor = color
        return this
    }

    fun setHeaderRows(rows: Int): Table {
        this.headerRows = rows
        return this
    }

    fun setHeaderFont(font: PdfFont): Table {
        this.headerFont = font
        return this
    }

    fun setMarginBottom(margin: Float): Table {
        this.marginBottom = margin
        return this
    }

    override fun render(context: RenderContext): Float {
        val colWidth = context.availableWidth / columnCount
        val rowCount = (cells.size + columnCount - 1) / columnCount
        val cellHeight = fontSize + cellPadding * 2
        val totalHeight = rowCount * cellHeight + marginBottom

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
        val fontName = font.registerOn(page)
        val headerFontName = headerFont?.registerOn(page) ?: fontName
        val canvas = PdfCanvas(page)

        // Draw borders and cell contents
        canvas.setStrokeColor(borderColor.r, borderColor.g, borderColor.b)
        canvas.setLineWidth(borderWidth)

        for (row in 0 until rowCount) {
            val rowY = context.y - (row + 1) * cellHeight

            for (col in 0 until columnCount) {
                val cellIndex = row * columnCount + col
                val cellX = context.x + col * colWidth

                // Draw cell border
                canvas.rectangle(cellX, rowY, colWidth, cellHeight)
                canvas.stroke()

                // Draw cell content
                if (cellIndex < cells.size) {
                    val cell = cells[cellIndex]
                    val cellText = cell.text
                    val isHeader = row < headerRows
                    val activeFontName = if (isHeader) headerFontName else fontName
                    val activeFont = if (isHeader && headerFont != null) headerFont!! else font

                    canvas.beginText()

                    if (cell.backgroundColor != null) {
                        canvas.endText()
                        canvas.saveState()
                        canvas.setFillColor(
                            cell.backgroundColor.r,
                            cell.backgroundColor.g,
                            cell.backgroundColor.b,
                        )
                        canvas.rectangle(cellX, rowY, colWidth, cellHeight)
                        canvas.fill()
                        canvas.restoreState()
                        canvas.beginText()
                    }

                    val textColor = cell.textColor ?: Color.BLACK
                    canvas.setFillColor(textColor.r, textColor.g, textColor.b)
                    canvas.setFont(activeFontName, fontSize)

                    val textX = cellX + cellPadding
                    val textY = rowY + cellPadding
                    canvas.moveText(textX, textY)
                    canvas.showText(cellText)
                    canvas.endText()
                }
            }
        }

        canvas.flush()
        return totalHeight
    }

    /**
     * A table cell with optional styling.
     */
    data class Cell(
        val text: String,
        val textColor: Color? = null,
        val backgroundColor: Color? = null,
    )
}
