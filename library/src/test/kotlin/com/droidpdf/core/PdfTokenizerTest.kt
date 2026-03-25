package com.droidpdf.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class PdfTokenizerTest {
    private fun tokenizer(s: String) = PdfTokenizer(s.toByteArray(Charsets.US_ASCII))

    @Test
    fun `reads simple tokens`() {
        val t = tokenizer("hello world")
        assertEquals("hello", t.readToken())
        assertEquals("world", t.readToken())
        assertNull(t.readToken())
    }

    @Test
    fun `skips whitespace and comments`() {
        val t = tokenizer("  % this is a comment\nhello")
        assertEquals("hello", t.readToken())
    }

    @Test
    fun `reads delimiter tokens`() {
        val t = tokenizer("<< >>")
        assertEquals("<<", t.readToken())
        assertEquals(">>", t.readToken())
    }

    @Test
    fun `reads literal string`() {
        val t = tokenizer("(Hello World)")
        val str = t.readString()
        assertEquals("Hello World", str?.value)
        assertEquals(PdfString.Encoding.LITERAL, str?.encoding)
    }

    @Test
    fun `reads literal string with escapes`() {
        val t = tokenizer("(A\\(B\\)C)")
        val str = t.readString()
        assertEquals("A(B)C", str?.value)
    }

    @Test
    fun `reads nested parentheses in string`() {
        val t = tokenizer("(A(B)C)")
        val str = t.readString()
        assertEquals("A(B)C", str?.value)
    }

    @Test
    fun `reads hex string`() {
        val t = tokenizer("<48656C6C6F>")
        val str = t.readString()
        assertEquals("Hello", str?.value)
    }

    @Test
    fun `reads integer`() {
        val t = tokenizer("42")
        assertEquals(42, t.readInt())
    }

    @Test
    fun `reads line`() {
        val t = tokenizer("first line\nsecond line\r\nthird")
        assertEquals("first line", t.readLine())
        assertEquals("second line", t.readLine())
        assertEquals("third", t.readLine())
    }

    @Test
    fun `findLastOccurrence finds keyword`() {
        val t = tokenizer("hello world hello end")
        val pos = t.findLastOccurrence("hello")
        assertEquals(12, pos)
    }

    @Test
    fun `findLastOccurrence returns -1 for missing keyword`() {
        val t = tokenizer("hello world")
        assertEquals(-1, t.findLastOccurrence("missing"))
    }
}
