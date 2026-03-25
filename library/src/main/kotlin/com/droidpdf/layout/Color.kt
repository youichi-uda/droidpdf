package com.droidpdf.layout

/**
 * RGB color representation for use in layout elements.
 */
data class Color(val r: Float, val g: Float, val b: Float) {
    companion object {
        val BLACK = Color(0f, 0f, 0f)
        val WHITE = Color(1f, 1f, 1f)
        val RED = Color(1f, 0f, 0f)
        val GREEN = Color(0f, 1f, 0f)
        val BLUE = Color(0f, 0f, 1f)
        val GRAY = Color(0.5f, 0.5f, 0.5f)
        val LIGHT_GRAY = Color(0.75f, 0.75f, 0.75f)
        val DARK_GRAY = Color(0.25f, 0.25f, 0.25f)
        val YELLOW = Color(1f, 1f, 0f)
        val CYAN = Color(0f, 1f, 1f)
        val MAGENTA = Color(1f, 0f, 1f)
    }
}
