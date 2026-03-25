package com.droidpdf.core

import java.io.ByteArrayOutputStream
import java.util.zip.InflaterOutputStream

/**
 * Extracts text content from PDF pages.
 *
 * Parses content streams and extracts text from PDF text operators
 * (Tj, TJ, ', "). Supports FlateDecode decompression.
 */
class TextExtractor(private val reader: PdfReader) {
    /**
     * Extract all text from all pages.
     */
    fun extractAll(): String {
        val sb = StringBuilder()
        val doc = PdfDocument(reader)
        for (i in 0 until doc.numberOfPages) {
            if (i > 0) sb.append('\n')
            sb.append(extractFromPage(i))
        }
        return sb.toString()
    }

    /**
     * Extract text from a specific page (0-based index).
     */
    fun extractFromPage(pageIndex: Int): String {
        val doc = PdfDocument(reader)
        if (pageIndex >= doc.numberOfPages) return ""
        val page = doc.getPage(pageIndex)
        val dict = page.sourceDictionary ?: return ""

        val contentsObj = dict[PdfName.CONTENTS] ?: return ""
        val contentData = resolveContentData(contentsObj)
        return parseTextFromContentStream(contentData)
    }

    /**
     * Search for text and return matches with page index.
     */
    fun search(query: String): List<SearchResult> {
        val results = mutableListOf<SearchResult>()
        val doc = PdfDocument(reader)
        for (i in 0 until doc.numberOfPages) {
            val pageText = extractFromPage(i)
            var startIndex = 0
            while (true) {
                val idx = pageText.indexOf(query, startIndex, ignoreCase = true)
                if (idx < 0) break
                results.add(SearchResult(pageIndex = i, text = query, charOffset = idx))
                startIndex = idx + 1
            }
        }
        return results
    }

    private fun resolveContentData(obj: PdfObject): ByteArray {
        return when (val resolved = reader.resolve(obj) ?: obj) {
            is PdfStream -> decompressStream(resolved)
            is PdfArray -> {
                // Multiple content streams concatenated
                val combined = ByteArrayOutputStream()
                for (element in resolved.elements) {
                    val stream = reader.resolve(element) as? PdfStream ?: continue
                    combined.write(decompressStream(stream))
                    combined.write(' '.code)
                }
                combined.toByteArray()
            }
            else -> ByteArray(0)
        }
    }

    private fun decompressStream(stream: PdfStream): ByteArray {
        val filter = stream.dictionary.getAsName("Filter")
        return if (filter?.value == "FlateDecode") {
            try {
                val output = ByteArrayOutputStream()
                InflaterOutputStream(output).use { it.write(stream.data) }
                output.toByteArray()
            } catch (e: Exception) {
                PdfLog.w("Failed to decompress stream: ${e.message}")
                stream.data
            }
        } else {
            stream.data
        }
    }

    private fun parseTextFromContentStream(data: ByteArray): String {
        val content = String(data, Charsets.ISO_8859_1)
        val sb = StringBuilder()
        val tokenizer = PdfTokenizer(data)

        var inTextBlock = false
        val operandStack = mutableListOf<String>()

        while (!tokenizer.isEof()) {
            tokenizer.skipWhitespaceAndComments()
            if (tokenizer.isEof()) break

            val byte = tokenizer.peek() ?: break

            when {
                byte == '('.code.toByte() -> {
                    val str = tokenizer.readString()
                    if (str != null) operandStack.add(str.value)
                }
                byte == '<'.code.toByte() -> {
                    val nextByte = if (tokenizer.position + 1 < data.size) data[tokenizer.position + 1] else 0
                    if (nextByte != '<'.code.toByte()) {
                        val str = tokenizer.readString()
                        if (str != null) operandStack.add(str.value)
                    } else {
                        // Skip dictionary
                        tokenizer.readToken()
                        operandStack.clear()
                    }
                }
                byte == '['.code.toByte() -> {
                    // TJ array - parse elements
                    tokenizer.readByte() // consume '['
                    val texts = mutableListOf<String>()
                    while (!tokenizer.isEof()) {
                        tokenizer.skipWhitespaceAndComments()
                        val b = tokenizer.peek() ?: break
                        when {
                            b == ']'.code.toByte() -> {
                                tokenizer.readByte()
                                break
                            }
                            b == '('.code.toByte() -> {
                                val str = tokenizer.readString()
                                if (str != null) texts.add(str.value)
                            }
                            b == '<'.code.toByte() -> {
                                val str = tokenizer.readString()
                                if (str != null) texts.add(str.value)
                            }
                            else -> {
                                // Numeric spacing adjustment - skip
                                tokenizer.readToken()
                            }
                        }
                    }
                    operandStack.add(texts.joinToString(""))
                }
                else -> {
                    val token = tokenizer.readToken() ?: break
                    when (token) {
                        "BT" -> {
                            inTextBlock = true
                            operandStack.clear()
                        }
                        "ET" -> {
                            inTextBlock = false
                            operandStack.clear()
                        }
                        "Tj", "'" -> {
                            if (operandStack.isNotEmpty()) {
                                sb.append(operandStack.last())
                            }
                            operandStack.clear()
                        }
                        "TJ" -> {
                            if (operandStack.isNotEmpty()) {
                                sb.append(operandStack.last())
                            }
                            operandStack.clear()
                        }
                        "\"" -> {
                            // Move to next line, set word/char spacing, show text
                            if (operandStack.isNotEmpty()) {
                                sb.append(operandStack.last())
                            }
                            operandStack.clear()
                        }
                        "Td", "TD", "T*" -> {
                            // Text position operators - add space/newline
                            if (sb.isNotEmpty() && sb.last() != '\n' && sb.last() != ' ') {
                                sb.append(' ')
                            }
                            operandStack.clear()
                        }
                        "Tm" -> {
                            operandStack.clear()
                        }
                        "Tf", "Tc", "Tw", "Tz", "TL", "Ts", "Tr" -> {
                            operandStack.clear()
                        }
                        else -> {
                            if (!inTextBlock) {
                                operandStack.clear()
                            }
                        }
                    }
                }
            }
        }

        return sb.toString().trim()
    }

    /**
     * Result of a text search.
     */
    data class SearchResult(
        val pageIndex: Int,
        val text: String,
        val charOffset: Int,
    )
}
