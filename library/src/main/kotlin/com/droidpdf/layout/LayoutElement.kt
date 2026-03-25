package com.droidpdf.layout

/**
 * Base interface for elements that can be added to a Document.
 */
interface LayoutElement {
    /**
     * Render this element and return the vertical space consumed.
     *
     * @param context the rendering context
     * @return the height consumed in points
     */
    fun render(context: RenderContext): Float
}

/**
 * Context passed to layout elements during rendering.
 */
data class RenderContext(
    val document: Document,
    val x: Float,
    val y: Float,
    val availableWidth: Float,
    val availableHeight: Float,
)
