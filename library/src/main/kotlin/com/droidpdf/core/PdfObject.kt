package com.droidpdf.core

import java.io.OutputStream

/**
 * Base class for all PDF objects (ISO 32000-1, Section 7.3).
 */
sealed class PdfObject {
    abstract fun writeTo(output: OutputStream)
}

/**
 * PDF Boolean object (ISO 32000-1, 7.3.2).
 */
data class PdfBoolean(val value: Boolean) : PdfObject() {
    override fun writeTo(output: OutputStream) {
        output.write(if (value) "true" else "false")
    }
}

/**
 * PDF Numeric objects (ISO 32000-1, 7.3.3).
 */
data class PdfInteger(val value: Long) : PdfObject() {
    constructor(value: Int) : this(value.toLong())

    override fun writeTo(output: OutputStream) {
        output.write(value.toString())
    }
}

data class PdfReal(val value: Double) : PdfObject() {
    constructor(value: Float) : this(value.toDouble())

    override fun writeTo(output: OutputStream) {
        // Avoid trailing zeros and scientific notation
        val formatted =
            if (value == value.toLong().toDouble()) {
                value.toLong().toString() + ".0"
            } else {
                "%.4f".format(value).trimEnd('0')
            }
        output.write(formatted)
    }
}

/**
 * PDF String objects (ISO 32000-1, 7.3.4).
 * Supports both literal strings (parentheses) and hexadecimal strings.
 */
data class PdfString(
    val value: String,
    val encoding: Encoding = Encoding.LITERAL,
) : PdfObject() {
    enum class Encoding { LITERAL, HEX }

    override fun writeTo(output: OutputStream) {
        when (encoding) {
            Encoding.LITERAL -> {
                output.write("(")
                output.write(escapeLiteral(value))
                output.write(")")
            }
            Encoding.HEX -> {
                output.write("<")
                value.toByteArray(Charsets.ISO_8859_1).forEach { b ->
                    output.write("%02X".format(b))
                }
                output.write(">")
            }
        }
    }

    companion object {
        fun escapeLiteral(s: String): String =
            buildString {
                for (c in s) {
                    when (c) {
                        '(' -> append("\\(")
                        ')' -> append("\\)")
                        '\\' -> append("\\\\")
                        '\n' -> append("\\n")
                        '\r' -> append("\\r")
                        '\t' -> append("\\t")
                        '\b' -> append("\\b")
                        else -> append(c)
                    }
                }
            }
    }
}

/**
 * PDF Name object (ISO 32000-1, 7.3.5).
 */
data class PdfName(val value: String) : PdfObject() {
    override fun writeTo(output: OutputStream) {
        output.write("/")
        for (c in value) {
            if (c.code in 33..126 && c != '#') {
                output.write(c.toString())
            } else {
                output.write("#%02X".format(c.code))
            }
        }
    }

    companion object {
        // Commonly used PDF names
        val TYPE = PdfName("Type")
        val SUBTYPE = PdfName("Subtype")
        val CATALOG = PdfName("Catalog")
        val PAGES = PdfName("Pages")
        val PAGE = PdfName("Page")
        val COUNT = PdfName("Count")
        val KIDS = PdfName("Kids")
        val PARENT = PdfName("Parent")
        val MEDIABOX = PdfName("MediaBox")
        val RESOURCES = PdfName("Resources")
        val CONTENTS = PdfName("Contents")
        val FONT = PdfName("Font")
        val LENGTH = PdfName("Length")
        val FILTER = PdfName("Filter")
        val FLATE_DECODE = PdfName("FlateDecode")
        val PROCSET = PdfName("ProcSet")
        val PDF = PdfName("PDF")
        val TEXT = PdfName("Text")
        val IMAGE_B = PdfName("ImageB")
        val IMAGE_C = PdfName("ImageC")
    }
}

/**
 * PDF Array object (ISO 32000-1, 7.3.6).
 */
data class PdfArray(
    val elements: MutableList<PdfObject> = mutableListOf(),
) : PdfObject() {
    constructor(vararg items: PdfObject) : this(items.toMutableList())

    fun add(obj: PdfObject): PdfArray {
        elements.add(obj)
        return this
    }

    fun size(): Int = elements.size

    operator fun get(index: Int): PdfObject = elements[index]

    override fun writeTo(output: OutputStream) {
        output.write("[")
        elements.forEachIndexed { index, obj ->
            if (index > 0) output.write(" ")
            obj.writeTo(output)
        }
        output.write("]")
    }
}

/**
 * PDF Dictionary object (ISO 32000-1, 7.3.7).
 */
data class PdfDictionary(
    val entries: MutableMap<PdfName, PdfObject> = mutableMapOf(),
) : PdfObject() {
    fun put(
        key: PdfName,
        value: PdfObject,
    ): PdfDictionary {
        entries[key] = value
        return this
    }

    fun put(
        key: String,
        value: PdfObject,
    ): PdfDictionary = put(PdfName(key), value)

    operator fun get(key: PdfName): PdfObject? = entries[key]

    operator fun get(key: String): PdfObject? = entries[PdfName(key)]

    fun getAsName(key: String): PdfName? = entries[PdfName(key)] as? PdfName

    fun getAsInteger(key: String): PdfInteger? = entries[PdfName(key)] as? PdfInteger

    fun getAsArray(key: String): PdfArray? = entries[PdfName(key)] as? PdfArray

    fun getAsDictionary(key: String): PdfDictionary? = entries[PdfName(key)] as? PdfDictionary

    fun getAsStream(key: String): PdfStream? = entries[PdfName(key)] as? PdfStream

    fun containsKey(key: String): Boolean = entries.containsKey(PdfName(key))

    override fun writeTo(output: OutputStream) {
        output.write("<<")
        entries.forEach { (key, value) ->
            output.write("\n")
            key.writeTo(output)
            output.write(" ")
            value.writeTo(output)
        }
        output.write("\n>>")
    }
}

/**
 * PDF Stream object (ISO 32000-1, 7.3.8).
 */
class PdfStream(
    val dictionary: PdfDictionary = PdfDictionary(),
    var data: ByteArray = ByteArray(0),
) : PdfObject() {
    override fun writeTo(output: OutputStream) {
        dictionary.put(PdfName.LENGTH, PdfInteger(data.size))
        dictionary.writeTo(output)
        output.write("\nstream\n")
        output.writeBytes(data)
        output.write("\nendstream")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PdfStream) return false
        return dictionary == other.dictionary && data.contentEquals(other.data)
    }

    override fun hashCode(): Int = 31 * dictionary.hashCode() + data.contentHashCode()
}

/**
 * PDF Null object (ISO 32000-1, 7.3.9).
 */
data object PdfNull : PdfObject() {
    override fun writeTo(output: OutputStream) {
        output.write("null")
    }
}

/**
 * PDF Indirect Reference (ISO 32000-1, 7.3.10).
 */
data class PdfIndirectReference(
    val objectNumber: Int,
    val generation: Int = 0,
) : PdfObject() {
    override fun writeTo(output: OutputStream) {
        output.write("$objectNumber $generation R")
    }
}

// Extension to write String to OutputStream as ASCII bytes
internal fun OutputStream.write(s: String) {
    write(s.toByteArray(Charsets.US_ASCII))
}

internal fun OutputStream.writeBytes(data: ByteArray) {
    write(data)
}
