package com.droidpdf.core

/**
 * Tokenizer for PDF content (ISO 32000-1, 7.2).
 *
 * Reads a byte array and produces tokens for the parser.
 * Follows Lenient policy: invalid bytes are skipped with a warning.
 */
class PdfTokenizer(private val data: ByteArray) {
    private var pos = 0

    val position: Int get() = pos

    fun setPosition(newPos: Int) {
        pos = newPos.coerceIn(0, data.size)
    }

    fun isEof(): Boolean = pos >= data.size

    fun peek(): Byte? = if (pos < data.size) data[pos] else null

    fun readByte(): Byte? = if (pos < data.size) data[pos++] else null

    fun skipWhitespaceAndComments() {
        while (pos < data.size) {
            val b = data[pos]
            when {
                isWhitespace(b) -> pos++
                b == '%'.code.toByte() -> skipComment()
                else -> return
            }
        }
    }

    /**
     * Read the next token, returning its string representation.
     */
    fun readToken(): String? {
        skipWhitespaceAndComments()
        if (isEof()) return null

        val b = data[pos]
        return when {
            isDelimiter(b) -> readDelimiterToken()
            else -> readRegularToken()
        }
    }

    /**
     * Read a line (up to \n or \r\n).
     */
    fun readLine(): String? {
        if (isEof()) return null
        val start = pos
        while (pos < data.size && data[pos] != '\n'.code.toByte() && data[pos] != '\r'.code.toByte()) {
            pos++
        }
        val line = String(data, start, pos - start, Charsets.US_ASCII)
        // Consume line ending
        if (pos < data.size && data[pos] == '\r'.code.toByte()) pos++
        if (pos < data.size && data[pos] == '\n'.code.toByte()) pos++
        return line
    }

    /**
     * Read an integer at the current position.
     */
    fun readInt(): Int? {
        val token = readToken() ?: return null
        return token.toIntOrNull()
    }

    /**
     * Read a PDF string (literal or hex).
     */
    fun readString(): PdfString? {
        skipWhitespaceAndComments()
        if (isEof()) return null

        return when (data[pos].toInt().toChar()) {
            '(' -> readLiteralString()
            '<' -> {
                if (pos + 1 < data.size && data[pos + 1] == '<'.code.toByte()) {
                    null // This is a dictionary, not a hex string
                } else {
                    readHexString()
                }
            }
            else -> null
        }
    }

    /**
     * Search backwards from the end for a keyword.
     */
    fun findLastOccurrence(keyword: String): Int {
        val keyBytes = keyword.toByteArray(Charsets.US_ASCII)
        for (i in data.size - keyBytes.size downTo 0) {
            var found = true
            for (j in keyBytes.indices) {
                if (data[i + j] != keyBytes[j]) {
                    found = false
                    break
                }
            }
            if (found) return i
        }
        return -1
    }

    private fun readLiteralString(): PdfString {
        pos++ // skip opening '('
        val sb = StringBuilder()
        var depth = 1
        while (pos < data.size && depth > 0) {
            val c = data[pos++].toInt().toChar()
            when (c) {
                '(' -> {
                    depth++
                    sb.append(c)
                }
                ')' -> {
                    depth--
                    if (depth > 0) sb.append(c)
                }
                '\\' -> sb.append(readEscapeSequence())
                else -> sb.append(c)
            }
        }
        return PdfString(sb.toString(), PdfString.Encoding.LITERAL)
    }

    private fun readEscapeSequence(): Char {
        if (isEof()) return '\\'
        val c = data[pos++].toInt().toChar()
        return when (c) {
            'n' -> '\n'
            'r' -> '\r'
            't' -> '\t'
            'b' -> '\b'
            'f' -> '\u000C'
            '(' -> '('
            ')' -> ')'
            '\\' -> '\\'
            in '0'..'7' -> {
                // Octal character code
                var octal = c.toString()
                repeat(2) {
                    if (pos < data.size && data[pos].toInt().toChar() in '0'..'7') {
                        octal += data[pos++].toInt().toChar()
                    }
                }
                octal.toInt(8).toChar()
            }
            '\r' -> {
                // Line continuation: \r or \r\n
                if (pos < data.size && data[pos] == '\n'.code.toByte()) pos++
                ' ' // Return space as placeholder, will be ignored
            }
            '\n' -> ' ' // Line continuation
            else -> c
        }
    }

    private fun readHexString(): PdfString {
        pos++ // skip opening '<'
        val sb = StringBuilder()
        while (pos < data.size) {
            val c = data[pos++].toInt().toChar()
            when {
                c == '>' -> break
                c.isWhitespace() -> continue
                else -> sb.append(c)
            }
        }
        // If odd number of hex digits, append 0 (ISO 32000-1, 7.3.4.3)
        val hex = if (sb.length % 2 != 0) sb.append('0').toString() else sb.toString()
        val bytes = hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        return PdfString(String(bytes, Charsets.ISO_8859_1), PdfString.Encoding.HEX)
    }

    private fun readDelimiterToken(): String {
        val c = data[pos++].toInt().toChar()
        return when (c) {
            '<' -> {
                if (pos < data.size && data[pos] == '<'.code.toByte()) {
                    pos++
                    "<<"
                } else {
                    "<"
                }
            }
            '>' -> {
                if (pos < data.size && data[pos] == '>'.code.toByte()) {
                    pos++
                    ">>"
                } else {
                    ">"
                }
            }
            else -> c.toString()
        }
    }

    private fun readRegularToken(): String {
        val start = pos
        while (pos < data.size && !isWhitespace(data[pos]) && !isDelimiter(data[pos])) {
            pos++
        }
        return String(data, start, pos - start, Charsets.US_ASCII)
    }

    private fun skipComment() {
        while (pos < data.size && data[pos] != '\n'.code.toByte() && data[pos] != '\r'.code.toByte()) {
            pos++
        }
    }

    companion object {
        private const val TAG = "DroidPDF"

        // ISO 32000-1, Table 1 - White-space characters
        fun isWhitespace(b: Byte): Boolean =
            when (b.toInt()) {
                0, 9, 10, 12, 13, 32 -> true
                else -> false
            }

        // ISO 32000-1, Table 2 - Delimiter characters
        fun isDelimiter(b: Byte): Boolean =
            when (b.toInt().toChar()) {
                '(', ')', '<', '>', '[', ']', '{', '}', '/', '%' -> true
                else -> false
            }
    }
}
