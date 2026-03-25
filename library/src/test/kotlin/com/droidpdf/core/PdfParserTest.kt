package com.droidpdf.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PdfParserTest {
    @BeforeEach
    fun setup() {
        PdfLog.logger = PdfLog.NoOpLogger()
    }

    private fun parse(s: String): PdfObject? {
        val tokenizer = PdfTokenizer(s.toByteArray(Charsets.US_ASCII))
        val parser = PdfParser(tokenizer)
        return parser.parseObject()
    }

    @Test
    fun `parses boolean`() {
        assertEquals(PdfBoolean(true), parse("true"))
        assertEquals(PdfBoolean(false), parse("false"))
    }

    @Test
    fun `parses null`() {
        assertEquals(PdfNull, parse("null"))
    }

    @Test
    fun `parses integer`() {
        assertEquals(PdfInteger(42), parse("42"))
        assertEquals(PdfInteger(-3), parse("-3"))
    }

    @Test
    fun `parses real`() {
        val result = parse("3.14")
        assertTrue(result is PdfReal)
        assertEquals(3.14, (result as PdfReal).value, 0.001)
    }

    @Test
    fun `parses name`() {
        assertEquals(PdfName("Type"), parse("/Type"))
        assertEquals(PdfName("Catalog"), parse("/Catalog"))
    }

    @Test
    fun `parses string`() {
        val result = parse("(Hello)")
        assertTrue(result is PdfString)
        assertEquals("Hello", (result as PdfString).value)
    }

    @Test
    fun `parses array`() {
        val result = parse("[1 2 3]")
        assertTrue(result is PdfArray)
        val array = result as PdfArray
        assertEquals(3, array.size())
        assertEquals(PdfInteger(1), array[0])
    }

    @Test
    fun `parses dictionary`() {
        val result = parse("<< /Type /Catalog /Pages 1 0 R >>")
        assertTrue(result is PdfDictionary)
        val dict = result as PdfDictionary
        assertEquals(PdfName.CATALOG, dict.getAsName("Type"))
    }

    @Test
    fun `parses indirect reference`() {
        val result = parse("1 0 R")
        assertTrue(result is PdfIndirectReference)
        val ref = result as PdfIndirectReference
        assertEquals(1, ref.objectNumber)
        assertEquals(0, ref.generation)
    }

    @Test
    fun `parses nested structures`() {
        val result = parse("<< /Kids [1 0 R 2 0 R] /Count 2 >>")
        assertTrue(result is PdfDictionary)
        val dict = result as PdfDictionary
        assertEquals(PdfInteger(2), dict.getAsInteger("Count"))
        val kids = dict.getAsArray("Kids")!!
        assertEquals(2, kids.size())
    }
}
