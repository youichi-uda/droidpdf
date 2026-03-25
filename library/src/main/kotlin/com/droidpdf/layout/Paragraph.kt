package com.droidpdf.layout

import com.droidpdf.content.PdfCanvas
import com.droidpdf.content.PdfFont

/**
 * A text paragraph layout element.
 *
 * Supports font, size, color, alignment, and automatic line wrapping.
 */
class Paragraph(private val text: String) : LayoutElement {
    private var font: PdfFont = PdfFont.helvetica()
    private var fontSize: Float = 12f
    private var color: Color = Color.BLACK
    private var alignment: Alignment = Alignment.LEFT
    private var lineSpacingMultiplier: Float = 1.2f
    private var marginBottom: Float = 4f

    fun setFont(font: PdfFont): Paragraph {
        this.font = font
        return this
    }

    fun setFontSize(size: Float): Paragraph {
        this.fontSize = size
        return this
    }

    fun setColor(color: Color): Paragraph {
        this.color = color
        return this
    }

    fun setAlignment(alignment: Alignment): Paragraph {
        this.alignment = alignment
        return this
    }

    fun setLineSpacing(multiplier: Float): Paragraph {
        this.lineSpacingMultiplier = multiplier
        return this
    }

    fun setMarginBottom(margin: Float): Paragraph {
        this.marginBottom = margin
        return this
    }

    override fun render(context: RenderContext): Float {
        val lines = wrapText(text, context.availableWidth)
        val lineHeight = fontSize * lineSpacingMultiplier
        val totalHeight = lines.size * lineHeight + marginBottom

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
        val canvas = PdfCanvas(page)

        canvas.beginText()
        canvas.setFillColor(color.r, color.g, color.b)
        canvas.setFont(fontName, fontSize)
        canvas.setLeading(lineHeight)

        lines.forEachIndexed { index, line ->
            val xPos = calculateXPosition(line, context.x, context.availableWidth)
            val yPos = context.y - (index + 1) * lineHeight + (lineHeight - fontSize)

            if (index == 0) {
                canvas.moveText(xPos, yPos)
            } else {
                canvas.newLine()
                // Adjust x if alignment changes position per line
                val prevX = calculateXPosition(lines[index - 1], context.x, context.availableWidth)
                if (xPos != prevX) {
                    canvas.moveText(xPos - prevX, 0f)
                }
            }
            canvas.showText(line)
        }

        canvas.endText()
        canvas.flush()

        return totalHeight
    }

    private fun wrapText(
        text: String,
        maxWidth: Float,
    ): List<String> {
        if (text.isEmpty()) return listOf("")

        val lines = mutableListOf<String>()
        for (paragraph in text.split('\n')) {
            if (paragraph.isEmpty()) {
                lines.add("")
                continue
            }

            val words = paragraph.split(' ')
            val currentLine = StringBuilder()

            for (word in words) {
                val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                val testWidth = font.getTextWidth(testLine, fontSize)

                if (testWidth > maxWidth && currentLine.isNotEmpty()) {
                    lines.add(currentLine.toString())
                    currentLine.clear()
                    currentLine.append(word)
                } else {
                    currentLine.clear()
                    currentLine.append(testLine)
                }
            }
            if (currentLine.isNotEmpty()) {
                lines.add(currentLine.toString())
            }
        }

        return lines.ifEmpty { listOf("") }
    }

    private fun calculateXPosition(
        line: String,
        baseX: Float,
        availableWidth: Float,
    ): Float {
        return when (alignment) {
            Alignment.LEFT -> baseX
            Alignment.CENTER -> baseX + (availableWidth - font.getTextWidth(line, fontSize)) / 2
            Alignment.RIGHT -> baseX + availableWidth - font.getTextWidth(line, fontSize)
        }
    }

    enum class Alignment {
        LEFT,
        CENTER,
        RIGHT,
    }
}
