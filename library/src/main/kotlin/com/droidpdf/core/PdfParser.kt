package com.droidpdf.core

/**
 * Parses PDF objects from tokenized input (ISO 32000-1, 7.3).
 *
 * Follows Lenient policy: parse errors are logged as warnings
 * and problematic objects are skipped where possible.
 */
class PdfParser(private val tokenizer: PdfTokenizer) {
    /**
     * Parse the next PDF object from the token stream.
     */
    fun parseObject(): PdfObject? {
        tokenizer.skipWhitespaceAndComments()
        if (tokenizer.isEof()) return null

        val byte = tokenizer.peek() ?: return null
        val c = byte.toInt().toChar()

        return when {
            c == '(' -> tokenizer.readString()
            c == '<' -> parseAngleBracket()
            c == '[' -> parseArray()
            c == '/' -> parseName()
            c == '+' || c == '-' || c == '.' || c.isDigit() -> parseNumberOrReference()
            else -> parseKeyword()
        }
    }

    private fun parseAngleBracket(): PdfObject? {
        val pos = tokenizer.position
        val token = tokenizer.readToken() ?: return null
        return when (token) {
            "<<" -> parseDictionary()
            "<" -> {
                // Hex string - back up and read as string
                tokenizer.setPosition(pos)
                tokenizer.readString()
            }
            else -> {
                PdfLog.w("Unexpected token after '<': $token at position $pos")
                null
            }
        }
    }

    private fun parseName(): PdfName {
        tokenizer.readByte() // consume '/'
        val sb = StringBuilder()
        while (!tokenizer.isEof()) {
            val b = tokenizer.peek() ?: break
            if (PdfTokenizer.isWhitespace(b) || PdfTokenizer.isDelimiter(b)) break
            tokenizer.readByte()
            if (b == '#'.code.toByte()) {
                // Hex-encoded character
                val hi = tokenizer.readByte()
                val lo = tokenizer.readByte()
                if (hi != null && lo != null) {
                    val hex = "${hi.toInt().toChar()}${lo.toInt().toChar()}"
                    val code = hex.toIntOrNull(16)
                    if (code != null) {
                        sb.append(code.toChar())
                    } else {
                        PdfLog.w("Invalid hex escape in name: #$hex")
                        sb.append('#').append(hex)
                    }
                }
            } else {
                sb.append(b.toInt().toChar())
            }
        }
        return PdfName(sb.toString())
    }

    private fun parseNumberOrReference(): PdfObject {
        val startPos = tokenizer.position
        val token1 = tokenizer.readToken() ?: return PdfInteger(0)

        // Check if this might be an indirect reference: "X Y R"
        val num1 = token1.toLongOrNull()
        if (num1 != null) {
            val posAfterFirst = tokenizer.position
            tokenizer.skipWhitespaceAndComments()
            val token2 = tokenizer.readToken()
            if (token2 != null) {
                val num2 = token2.toIntOrNull()
                if (num2 != null) {
                    tokenizer.skipWhitespaceAndComments()
                    val token3 = tokenizer.readToken()
                    if (token3 == "R") {
                        return PdfIndirectReference(num1.toInt(), num2)
                    }
                }
            }
            // Not a reference, restore position
            tokenizer.setPosition(posAfterFirst)
        }

        // Parse as number
        return if (token1.contains('.')) {
            token1.toDoubleOrNull()?.let { PdfReal(it) } ?: run {
                PdfLog.w("Invalid real number: $token1 at position $startPos")
                PdfInteger(0)
            }
        } else {
            num1?.let { PdfInteger(it) } ?: run {
                PdfLog.w("Invalid integer: $token1 at position $startPos")
                PdfInteger(0)
            }
        }
    }

    private fun parseArray(): PdfArray {
        tokenizer.readToken() // consume '['
        val array = PdfArray()
        while (!tokenizer.isEof()) {
            tokenizer.skipWhitespaceAndComments()
            val byte = tokenizer.peek() ?: break
            if (byte == ']'.code.toByte()) {
                tokenizer.readByte()
                break
            }
            val obj = parseObject()
            if (obj != null) {
                array.add(obj)
            } else {
                // Lenient: skip unrecognized byte
                PdfLog.w("Skipping unrecognized byte in array at position ${tokenizer.position}")
                tokenizer.readByte()
            }
        }
        return array
    }

    private fun parseDictionary(): PdfObject {
        val dict = PdfDictionary()
        while (!tokenizer.isEof()) {
            tokenizer.skipWhitespaceAndComments()
            val byte = tokenizer.peek() ?: break

            // Check for ">>"
            if (byte == '>'.code.toByte()) {
                tokenizer.readToken() // consume ">>"
                break
            }

            // Key must be a name
            if (byte != '/'.code.toByte()) {
                PdfLog.w("Expected name in dictionary at position ${tokenizer.position}, skipping")
                tokenizer.readByte()
                continue
            }

            val key = parseName()
            val value = parseObject()
            if (value != null) {
                dict.put(key, value)
            } else {
                PdfLog.w("Null value for key /${key.value} at position ${tokenizer.position}")
            }
        }

        // Check if this dictionary is followed by "stream"
        val posBeforeStream = tokenizer.position
        tokenizer.skipWhitespaceAndComments()
        val nextToken = tokenizer.readToken()
        if (nextToken == "stream") {
            return parseStream(dict)
        }
        // Not a stream, restore position
        tokenizer.setPosition(posBeforeStream)
        return dict
    }

    private fun parseStream(dict: PdfDictionary): PdfStream {
        // Skip the single newline after "stream" keyword (ISO 32000-1, 7.3.8.1)
        if (!tokenizer.isEof() && tokenizer.peek() == '\r'.code.toByte()) tokenizer.readByte()
        if (!tokenizer.isEof() && tokenizer.peek() == '\n'.code.toByte()) tokenizer.readByte()

        val length = dict.getAsInteger("Length")?.value?.toInt() ?: 0
        val streamData = ByteArray(length)
        val startPos = tokenizer.position
        for (i in 0 until length) {
            val b = tokenizer.readByte()
            if (b != null) {
                streamData[i] = b
            } else {
                PdfLog.w("Unexpected end of stream data at position ${tokenizer.position}")
                break
            }
        }

        // Skip "endstream" keyword
        tokenizer.skipWhitespaceAndComments()
        val endToken = tokenizer.readToken()
        if (endToken != "endstream") {
            PdfLog.w("Expected 'endstream', got '$endToken' at position ${tokenizer.position}")
        }

        return PdfStream(dict, streamData)
    }

    private fun parseKeyword(): PdfObject? {
        val token = tokenizer.readToken() ?: return null
        return when (token) {
            "true" -> PdfBoolean(true)
            "false" -> PdfBoolean(false)
            "null" -> PdfNull
            else -> {
                PdfLog.w("Unknown keyword: $token at position ${tokenizer.position}")
                null
            }
        }
    }
}
