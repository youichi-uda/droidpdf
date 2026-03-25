package com.droidpdf.manipulation

import com.droidpdf.core.PdfDocument
import com.droidpdf.core.PdfReader
import java.io.InputStream
import java.io.OutputStream

/**
 * Splits a PDF document into multiple documents.
 *
 * Usage:
 * ```kotlin
 * val splitter = PdfSplitter(inputStream)
 * splitter.extractPages(1, 3, outputStream1)   // pages 1-3
 * splitter.extractPages(4, 4, outputStream2)   // page 4 only
 * splitter.splitByPage(outputStreams)           // one PDF per page
 * ```
 */
class PdfSplitter(inputStream: InputStream) {
    private val reader = PdfReader(inputStream)
    private val sourceDoc = PdfDocument(reader)

    /**
     * Total number of pages in the source document.
     */
    val numberOfPages: Int get() = sourceDoc.numberOfPages

    /**
     * Extract a range of pages into a new PDF.
     * Page numbers are 1-based.
     */
    fun extractPages(
        fromPage: Int,
        toPage: Int,
        outputStream: OutputStream,
    ) {
        require(fromPage >= 1) { "fromPage must be >= 1" }
        require(toPage <= numberOfPages) { "toPage exceeds page count" }
        require(fromPage <= toPage) { "fromPage must be <= toPage" }

        val merger = PdfMerger(outputStream)
        merger.merge(sourceDoc, fromPage, toPage)
        merger.close()
    }

    /**
     * Extract specific pages (by 1-based page numbers) into a new PDF.
     */
    fun extractPages(
        pageNumbers: List<Int>,
        outputStream: OutputStream,
    ) {
        require(pageNumbers.isNotEmpty()) { "pageNumbers must not be empty" }
        pageNumbers.forEach { p ->
            require(p in 1..numberOfPages) { "Page number $p out of range (1..$numberOfPages)" }
        }

        val outputDoc = PdfDocument(outputStream)
        for (pageNum in pageNumbers) {
            val sourcePage = sourceDoc.getPage(pageNum - 1)
            val newPage = outputDoc.addNewPage(sourcePage.getPageSize())
            newPage.setRotation(sourcePage.getRotation())
            newPage.sourceDictionary = sourcePage.sourceDictionary
        }
        outputDoc.close()
    }

    /**
     * Split into individual single-page PDFs.
     * The outputProvider function receives the 1-based page number
     * and should return an OutputStream for that page's PDF.
     */
    fun splitByPage(outputProvider: (pageNumber: Int) -> OutputStream) {
        for (i in 1..numberOfPages) {
            val output = outputProvider(i)
            extractPages(i, i, output)
        }
    }
}
