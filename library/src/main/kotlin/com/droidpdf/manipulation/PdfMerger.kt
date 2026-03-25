package com.droidpdf.manipulation

import com.droidpdf.core.PdfDocument
import com.droidpdf.core.PdfReader
import java.io.InputStream
import java.io.OutputStream

/**
 * Merges multiple PDF documents into a single output PDF.
 *
 * Usage:
 * ```kotlin
 * val merger = PdfMerger(outputStream)
 * merger.merge(inputStream1)              // all pages
 * merger.merge(inputStream2, 1, 3)        // pages 1-3 only
 * merger.close()
 * ```
 */
class PdfMerger(outputStream: OutputStream) {
    private val outputDoc = PdfDocument(outputStream)

    /**
     * Merge all pages from the given PDF input stream.
     */
    fun merge(inputStream: InputStream): PdfMerger {
        val reader = PdfReader(inputStream)
        val sourceDoc = PdfDocument(reader)
        return mergePages(sourceDoc, 1, sourceDoc.numberOfPages)
    }

    /**
     * Merge pages from the given PDF input stream.
     * Page numbers are 1-based (first page is 1).
     */
    fun merge(
        inputStream: InputStream,
        fromPage: Int,
        toPage: Int,
    ): PdfMerger {
        val reader = PdfReader(inputStream)
        val sourceDoc = PdfDocument(reader)
        return mergePages(sourceDoc, fromPage, toPage)
    }

    /**
     * Merge pages from an already-opened PdfDocument.
     * Page numbers are 1-based.
     */
    fun merge(
        sourceDoc: PdfDocument,
        fromPage: Int = 1,
        toPage: Int = sourceDoc.numberOfPages,
    ): PdfMerger {
        return mergePages(sourceDoc, fromPage, toPage)
    }

    private fun mergePages(
        sourceDoc: PdfDocument,
        fromPage: Int,
        toPage: Int,
    ): PdfMerger {
        require(fromPage >= 1) { "fromPage must be >= 1, got $fromPage" }
        require(toPage <= sourceDoc.numberOfPages) {
            "toPage $toPage exceeds document page count ${sourceDoc.numberOfPages}"
        }
        require(fromPage <= toPage) { "fromPage ($fromPage) must be <= toPage ($toPage)" }

        for (i in (fromPage - 1) until toPage) {
            val sourcePage = sourceDoc.getPage(i)
            val newPage = outputDoc.addNewPage(sourcePage.getPageSize())
            newPage.setRotation(sourcePage.getRotation())
            newPage.sourceDictionary = sourcePage.sourceDictionary
        }

        return this
    }

    /**
     * Get the number of pages currently in the merged document.
     */
    val numberOfPages: Int get() = outputDoc.numberOfPages

    /**
     * Close the merger and write the merged PDF.
     */
    fun close() {
        outputDoc.close()
    }
}
