package com.droidpdf.core

/**
 * A rectangle in PDF coordinate space (lower-left origin).
 * Used for annotation bounds, page areas, etc.
 */
data class Rectangle(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
) {
    val left: Float get() = x
    val bottom: Float get() = y
    val right: Float get() = x + width
    val top: Float get() = y + height

    fun toPdfArray(): PdfArray =
        PdfArray(
            PdfReal(left),
            PdfReal(bottom),
            PdfReal(right),
            PdfReal(top),
        )
}
