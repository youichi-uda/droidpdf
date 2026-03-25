package com.droidpdf.manipulation

import com.droidpdf.core.PdfDocument
import com.droidpdf.core.PdfReader
import java.io.InputStream
import java.io.OutputStream

/**
 * Utility functions for page-level operations on PDFs.
 *
 * Provides a fluent API for rotate, reorder, remove, and insert operations.
 */
class PageOperations(inputStream: InputStream) {
    private val reader = PdfReader(inputStream)
    private val sourceDoc = PdfDocument(reader)

    val numberOfPages: Int get() = sourceDoc.numberOfPages

    /**
     * Rotate specific pages by the given degrees (must be multiple of 90).
     * Page numbers are 1-based.
     */
    fun rotatePages(
        pageNumbers: List<Int>,
        degrees: Int,
    ): PageOperations {
        pageNumbers.forEach { p ->
            require(p in 1..numberOfPages) { "Page $p out of range" }
            val page = sourceDoc.getPage(p - 1)
            val current = page.getRotation()
            page.setRotation(current + degrees)
        }
        return this
    }

    /**
     * Rotate all pages by the given degrees.
     */
    fun rotateAllPages(degrees: Int): PageOperations {
        return rotatePages((1..numberOfPages).toList(), degrees)
    }

    /**
     * Remove pages by 1-based page numbers.
     * Pages are removed in reverse order to maintain correct indices.
     */
    fun removePages(pageNumbers: List<Int>): PageOperations {
        pageNumbers.forEach { p ->
            require(p in 1..numberOfPages) { "Page $p out of range" }
        }
        pageNumbers.sortedDescending().forEach { p ->
            sourceDoc.removePage(p - 1)
        }
        return this
    }

    /**
     * Reorder pages according to the given order.
     * The list contains 1-based page numbers in the desired order.
     * Example: [3, 1, 2] puts page 3 first, then page 1, then page 2.
     */
    fun reorderPages(newOrder: List<Int>): PageOperations {
        newOrder.forEach { p ->
            require(p in 1..numberOfPages) { "Page $p out of range" }
        }
        val originalPages = sourceDoc.getPages()
        // Remove all pages, then re-add in new order
        repeat(sourceDoc.numberOfPages) { sourceDoc.removePage(0) }
        newOrder.forEach { p ->
            sourceDoc.addPage(originalPages[p - 1])
        }
        return this
    }

    /**
     * Write the modified document to the output stream.
     */
    fun writeTo(outputStream: OutputStream) {
        // Create a new document and copy pages
        val outputDoc = PdfDocument(outputStream)
        for (i in 0 until sourceDoc.numberOfPages) {
            val page = sourceDoc.getPage(i)
            val newPage = outputDoc.addNewPage(page.getPageSize())
            newPage.setRotation(page.getRotation())
            newPage.sourceDictionary = page.sourceDictionary
        }
        outputDoc.close()
    }
}
