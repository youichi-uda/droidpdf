package com.droidpdf.core

import java.io.ByteArrayOutputStream
import java.io.OutputStream

/**
 * Writes PDF objects to an output stream, producing a valid PDF file.
 *
 * Handles the PDF file structure (ISO 32000-1, 7.5):
 * - Header
 * - Body (indirect objects)
 * - Cross-reference table
 * - Trailer
 */
class PdfWriter(private val output: OutputStream) {
    private val objects = mutableListOf<PdfObject>()
    private val offsets = mutableListOf<Long>()
    private var byteCount = 0L
    private var closed = false

    /**
     * Add an object and return its indirect reference.
     * Object number starts at 1 (object 0 is reserved for the free entry).
     */
    fun addObject(obj: PdfObject): PdfIndirectReference {
        val objectNumber = objects.size + 1
        objects.add(obj)
        return PdfIndirectReference(objectNumber, 0)
    }

    /**
     * Write the complete PDF file.
     * Must be called after all objects have been added.
     */
    fun write(trailerExtras: PdfDictionary = PdfDictionary()) {
        check(!closed) { "PdfWriter is already closed" }
        closed = true

        writeHeader()
        writeBody()
        val xrefOffset = byteCount
        writeXrefTable()
        writeTrailer(trailerExtras, xrefOffset)
    }

    private fun writeHeader() {
        // ISO 32000-1, 7.5.2 - File Header
        writeLine("%PDF-1.7")
        // Binary comment to indicate binary content (ISO 32000-1, 7.5.2)
        val binaryComment =
            byteArrayOf(
                '%'.code.toByte(),
                0xE2.toByte(),
                0xE3.toByte(),
                0xCF.toByte(),
                0xD3.toByte(),
                '\n'.code.toByte(),
            )
        writeRaw(binaryComment)
    }

    private fun writeBody() {
        // ISO 32000-1, 7.5.3 - Body
        objects.forEachIndexed { index, obj ->
            offsets.add(byteCount)
            val objectNumber = index + 1
            writeLine("$objectNumber 0 obj")
            val objBytes = objectToBytes(obj)
            writeRaw(objBytes)
            writeLine("")
            writeLine("endobj")
        }
    }

    private fun writeXrefTable() {
        // ISO 32000-1, 7.5.4 - Cross-Reference Table
        writeLine("xref")
        writeLine("0 ${objects.size + 1}")
        // Entry for object 0 (free entry, head of free list)
        writeLine("0000000000 65535 f ")
        offsets.forEach { offset ->
            writeLine("%010d 00000 n ".format(offset))
        }
    }

    private fun writeTrailer(
        extras: PdfDictionary,
        xrefOffset: Long,
    ) {
        // ISO 32000-1, 7.5.5 - File Trailer
        val trailer = PdfDictionary()
        trailer.put("Size", PdfInteger(objects.size + 1))
        extras.entries.forEach { (key, value) ->
            trailer.put(key, value)
        }

        writeLine("trailer")
        val trailerBytes = objectToBytes(trailer)
        writeRaw(trailerBytes)
        writeLine("")
        writeLine("startxref")
        writeLine(xrefOffset.toString())
        writeRaw("%%EOF".toByteArray(Charsets.US_ASCII))
    }

    private fun objectToBytes(obj: PdfObject): ByteArray {
        val buffer = ByteArrayOutputStream()
        obj.writeTo(buffer)
        return buffer.toByteArray()
    }

    private fun writeLine(line: String) {
        val bytes = "$line\n".toByteArray(Charsets.US_ASCII)
        output.write(bytes)
        byteCount += bytes.size
    }

    private fun writeRaw(bytes: ByteArray) {
        output.write(bytes)
        byteCount += bytes.size
    }
}
