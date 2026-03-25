package com.droidpdf.content

import com.droidpdf.core.PdfPage

/**
 * Low-level PDF content stream builder (ISO 32000-1, Section 8 & 9).
 *
 * Provides methods to emit PDF drawing operators for text, graphics,
 * and images onto a page.
 */
class PdfCanvas(private val page: PdfPage) {
    private val sb = StringBuilder()

    // --- Text operations (ISO 32000-1, 9.4) ---

    /**
     * Begin a text object.
     */
    fun beginText(): PdfCanvas {
        sb.appendLine("BT")
        return this
    }

    /**
     * End a text object.
     */
    fun endText(): PdfCanvas {
        sb.appendLine("ET")
        return this
    }

    /**
     * Set the font and size (Tf operator).
     */
    fun setFont(
        fontName: String,
        size: Float,
    ): PdfCanvas {
        sb.appendLine("/$fontName ${formatFloat(size)} Tf")
        return this
    }

    /**
     * Move text position (Td operator).
     */
    fun moveText(
        x: Float,
        y: Float,
    ): PdfCanvas {
        sb.appendLine("${formatFloat(x)} ${formatFloat(y)} Td")
        return this
    }

    /**
     * Set the text matrix (Tm operator).
     */
    fun setTextMatrix(
        a: Float,
        b: Float,
        c: Float,
        d: Float,
        e: Float,
        f: Float,
    ): PdfCanvas {
        sb.appendLine(
            "${formatFloat(a)} ${formatFloat(b)} ${formatFloat(c)} " +
                "${formatFloat(d)} ${formatFloat(e)} ${formatFloat(f)} Tm",
        )
        return this
    }

    /**
     * Show a text string (Tj operator).
     */
    fun showText(text: String): PdfCanvas {
        sb.appendLine("(${escapeString(text)}) Tj")
        return this
    }

    /**
     * Move to next line and show text (single quote operator).
     */
    fun newlineShowText(text: String): PdfCanvas {
        sb.appendLine("(${escapeString(text)}) '")
        return this
    }

    /**
     * Set the text leading (TL operator).
     */
    fun setLeading(leading: Float): PdfCanvas {
        sb.appendLine("${formatFloat(leading)} TL")
        return this
    }

    /**
     * Move to start of next line (T* operator).
     */
    fun newLine(): PdfCanvas {
        sb.appendLine("T*")
        return this
    }

    /**
     * Set character spacing (Tc operator).
     */
    fun setCharacterSpacing(spacing: Float): PdfCanvas {
        sb.appendLine("${formatFloat(spacing)} Tc")
        return this
    }

    /**
     * Set word spacing (Tw operator).
     */
    fun setWordSpacing(spacing: Float): PdfCanvas {
        sb.appendLine("${formatFloat(spacing)} Tw")
        return this
    }

    /**
     * Set text rendering mode (Tr operator).
     * 0=fill, 1=stroke, 2=fill+stroke, 3=invisible
     */
    fun setTextRenderingMode(mode: Int): PdfCanvas {
        sb.appendLine("$mode Tr")
        return this
    }

    // --- Color operations (ISO 32000-1, 8.6) ---

    /**
     * Set fill color in RGB (rg operator).
     */
    fun setFillColor(
        r: Float,
        g: Float,
        b: Float,
    ): PdfCanvas {
        sb.appendLine("${formatFloat(r)} ${formatFloat(g)} ${formatFloat(b)} rg")
        return this
    }

    /**
     * Set stroke color in RGB (RG operator).
     */
    fun setStrokeColor(
        r: Float,
        g: Float,
        b: Float,
    ): PdfCanvas {
        sb.appendLine("${formatFloat(r)} ${formatFloat(g)} ${formatFloat(b)} RG")
        return this
    }

    /**
     * Set fill color in grayscale (g operator).
     */
    fun setFillGray(gray: Float): PdfCanvas {
        sb.appendLine("${formatFloat(gray)} g")
        return this
    }

    /**
     * Set stroke color in grayscale (G operator).
     */
    fun setStrokeGray(gray: Float): PdfCanvas {
        sb.appendLine("${formatFloat(gray)} G")
        return this
    }

    // --- Graphics state (ISO 32000-1, 8.4) ---

    /**
     * Save graphics state (q operator).
     */
    fun saveState(): PdfCanvas {
        sb.appendLine("q")
        return this
    }

    /**
     * Restore graphics state (Q operator).
     */
    fun restoreState(): PdfCanvas {
        sb.appendLine("Q")
        return this
    }

    /**
     * Set line width (w operator).
     */
    fun setLineWidth(width: Float): PdfCanvas {
        sb.appendLine("${formatFloat(width)} w")
        return this
    }

    /**
     * Concatenate matrix to CTM (cm operator).
     */
    fun concatMatrix(
        a: Float,
        b: Float,
        c: Float,
        d: Float,
        e: Float,
        f: Float,
    ): PdfCanvas {
        sb.appendLine(
            "${formatFloat(a)} ${formatFloat(b)} ${formatFloat(c)} " +
                "${formatFloat(d)} ${formatFloat(e)} ${formatFloat(f)} cm",
        )
        return this
    }

    // --- Path construction (ISO 32000-1, 8.5.2) ---

    /**
     * Move to point (m operator).
     */
    fun moveTo(
        x: Float,
        y: Float,
    ): PdfCanvas {
        sb.appendLine("${formatFloat(x)} ${formatFloat(y)} m")
        return this
    }

    /**
     * Line to point (l operator).
     */
    fun lineTo(
        x: Float,
        y: Float,
    ): PdfCanvas {
        sb.appendLine("${formatFloat(x)} ${formatFloat(y)} l")
        return this
    }

    /**
     * Append rectangle (re operator).
     */
    fun rectangle(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
    ): PdfCanvas {
        sb.appendLine("${formatFloat(x)} ${formatFloat(y)} ${formatFloat(width)} ${formatFloat(height)} re")
        return this
    }

    // --- Path painting (ISO 32000-1, 8.5.3) ---

    /**
     * Stroke the path (S operator).
     */
    fun stroke(): PdfCanvas {
        sb.appendLine("S")
        return this
    }

    /**
     * Fill the path using non-zero winding rule (f operator).
     */
    fun fill(): PdfCanvas {
        sb.appendLine("f")
        return this
    }

    /**
     * Fill and stroke (B operator).
     */
    fun fillStroke(): PdfCanvas {
        sb.appendLine("B")
        return this
    }

    /**
     * Close and stroke (s operator).
     */
    fun closeStroke(): PdfCanvas {
        sb.appendLine("s")
        return this
    }

    /**
     * Close path (h operator).
     */
    fun closePath(): PdfCanvas {
        sb.appendLine("h")
        return this
    }

    // --- XObject / Image (ISO 32000-1, 8.8) ---

    /**
     * Paint an XObject (Do operator).
     */
    fun addXObject(name: String): PdfCanvas {
        sb.appendLine("/$name Do")
        return this
    }

    /**
     * Flush all accumulated operations to the page's content stream.
     */
    fun flush() {
        if (sb.isNotEmpty()) {
            page.addContent(sb.toString().trimEnd())
            sb.clear()
        }
    }

    companion object {
        internal fun formatFloat(f: Float): String {
            return if (f == f.toLong().toFloat()) {
                f.toLong().toString()
            } else {
                "%.4f".format(f).trimEnd('0').trimEnd('.')
            }
        }

        internal fun escapeString(s: String): String =
            buildString {
                for (c in s) {
                    when (c) {
                        '(' -> append("\\(")
                        ')' -> append("\\)")
                        '\\' -> append("\\\\")
                        else -> append(c)
                    }
                }
            }
    }
}
