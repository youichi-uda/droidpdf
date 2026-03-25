package com.droidpdf.core

import java.io.InputStream

/**
 * Reads and parses an existing PDF file (ISO 32000-1, 7.5).
 *
 * Follows Lenient policy: structural errors are logged as warnings
 * and the reader attempts to recover where possible.
 */
class PdfReader(inputStream: InputStream) {
    private val data: ByteArray = inputStream.readBytes()
    private val tokenizer = PdfTokenizer(data)
    private val parser = PdfParser(tokenizer)
    private val xrefEntries = mutableMapOf<Int, XrefEntry>()

    val objects = mutableMapOf<Int, PdfObject>()
    var trailer: PdfDictionary = PdfDictionary()
        private set

    init {
        parse()
    }

    /**
     * Get an object by its object number, resolving indirect references.
     */
    fun getObject(objectNumber: Int): PdfObject? = objects[objectNumber]

    /**
     * Resolve an indirect reference to the actual object.
     */
    fun resolve(obj: PdfObject?): PdfObject? {
        if (obj is PdfIndirectReference) {
            return objects[obj.objectNumber]
        }
        return obj
    }

    /**
     * Get the document catalog (root object).
     */
    fun getCatalog(): PdfDictionary? {
        val rootRef = trailer["Root"] ?: return null
        return resolve(rootRef) as? PdfDictionary
    }

    /**
     * Get the total number of parsed objects.
     */
    fun objectCount(): Int = objects.size

    private fun parse() {
        try {
            verifyHeader()
            val xrefOffset = findXrefOffset()
            if (xrefOffset >= 0) {
                parseXrefTable(xrefOffset)
                parseAllObjects()
            } else {
                PdfLog.w("Could not find xref offset, attempting sequential parse")
                parseSequential()
            }
        } catch (e: Exception) {
            PdfLog.w("Error during PDF parse, attempting recovery: ${e.message}")
            parseSequential()
        }
    }

    private fun verifyHeader() {
        tokenizer.setPosition(0)
        val line = tokenizer.readLine() ?: ""
        if (!line.startsWith("%PDF-")) {
            PdfLog.w("Missing PDF header, got: $line")
        }
    }

    private fun findXrefOffset(): Int {
        // Find "startxref" near the end of the file
        val startxrefPos = tokenizer.findLastOccurrence("startxref")
        if (startxrefPos < 0) {
            PdfLog.w("Could not find 'startxref' keyword")
            return -1
        }

        tokenizer.setPosition(startxrefPos)
        tokenizer.readToken() // consume "startxref"
        tokenizer.skipWhitespaceAndComments()
        return tokenizer.readInt() ?: -1
    }

    private fun parseXrefTable(offset: Int) {
        tokenizer.setPosition(offset)
        val keyword = tokenizer.readToken()
        if (keyword != "xref") {
            PdfLog.w("Expected 'xref', got '$keyword' at offset $offset")
            return
        }

        // Read xref subsections
        while (!tokenizer.isEof()) {
            tokenizer.skipWhitespaceAndComments()
            val byte = tokenizer.peek() ?: break
            // "trailer" keyword signals end of xref
            if (byte == 't'.code.toByte()) break

            val startObj = tokenizer.readInt() ?: break
            val count = tokenizer.readInt() ?: break
            // Consume the rest of the subsection header line
            tokenizer.readLine()

            for (i in 0 until count) {
                val line = tokenizer.readLine()?.trim() ?: break
                val parts = line.split("\\s+".toRegex())
                if (parts.size >= 3) {
                    val entryOffset = parts[0].toLongOrNull() ?: 0L
                    val gen = parts[1].toIntOrNull() ?: 0
                    val inUse = parts[2] == "n"
                    if (inUse) {
                        xrefEntries[startObj + i] = XrefEntry(entryOffset, gen)
                    }
                }
            }
        }

        // Parse trailer dictionary
        tokenizer.skipWhitespaceAndComments()
        val trailerKeyword = tokenizer.readToken()
        if (trailerKeyword == "trailer") {
            tokenizer.skipWhitespaceAndComments()
            val obj = parser.parseObject()
            if (obj is PdfDictionary) {
                trailer = obj
            }
        }
    }

    private fun parseAllObjects() {
        for ((objNum, entry) in xrefEntries) {
            try {
                tokenizer.setPosition(entry.offset.toInt())
                val obj = parseIndirectObject(objNum)
                if (obj != null) {
                    objects[objNum] = obj
                }
            } catch (e: Exception) {
                PdfLog.w("Failed to parse object $objNum at offset ${entry.offset}: ${e.message}")
            }
        }
    }

    private fun parseIndirectObject(expectedObjNum: Int): PdfObject? {
        val objNum = tokenizer.readInt() ?: return null
        val gen = tokenizer.readInt() ?: return null
        val keyword = tokenizer.readToken() ?: return null

        if (keyword != "obj") {
            PdfLog.w("Expected 'obj', got '$keyword' for object $expectedObjNum")
            return null
        }

        val obj = parser.parseObject() ?: return null

        // Consume "endobj"
        tokenizer.skipWhitespaceAndComments()
        val endKeyword = tokenizer.readToken()
        if (endKeyword != "endobj") {
            PdfLog.w("Expected 'endobj', got '$endKeyword' for object $objNum")
        }

        return obj
    }

    /**
     * Sequential parse fallback for damaged PDFs.
     * Scans for "X Y obj" patterns throughout the file.
     */
    private fun parseSequential() {
        tokenizer.setPosition(0)
        val pattern = "\\d+ \\d+ obj".toRegex()
        val content = String(data, Charsets.US_ASCII)

        for (match in pattern.findAll(content)) {
            try {
                val parts = match.value.split(" ")
                val objNum = parts[0].toInt()
                tokenizer.setPosition(match.range.last + 1)
                tokenizer.skipWhitespaceAndComments()
                val obj = parser.parseObject()
                if (obj != null) {
                    objects[objNum] = obj
                }
            } catch (e: Exception) {
                PdfLog.w("Sequential parse error at offset ${match.range.first}: ${e.message}")
            }
        }

        // Try to find trailer
        if (trailer.entries.isEmpty()) {
            val trailerPos = tokenizer.findLastOccurrence("trailer")
            if (trailerPos >= 0) {
                tokenizer.setPosition(trailerPos)
                tokenizer.readToken() // consume "trailer"
                tokenizer.skipWhitespaceAndComments()
                val obj = parser.parseObject()
                if (obj is PdfDictionary) {
                    trailer = obj
                }
            }
        }
    }

    private data class XrefEntry(val offset: Long, val generation: Int)
}
