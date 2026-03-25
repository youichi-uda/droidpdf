package com.droidpdf.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream

class PdfObjectTest {
    private fun PdfObject.toByteString(): String {
        val out = ByteArrayOutputStream()
        writeTo(out)
        return out.toString("US-ASCII")
    }

    @Test
    fun `boolean writes correctly`() {
        assertEquals("true", PdfBoolean(true).toByteString())
        assertEquals("false", PdfBoolean(false).toByteString())
    }

    @Test
    fun `integer writes correctly`() {
        assertEquals("42", PdfInteger(42).toByteString())
        assertEquals("-7", PdfInteger(-7).toByteString())
        assertEquals("0", PdfInteger(0).toByteString())
    }

    @Test
    fun `real writes correctly`() {
        assertEquals("3.14", PdfReal(3.14).toByteString().trimEnd('0'))
        assertEquals("0.0", PdfReal(0.0).toByteString())
    }

    @Test
    fun `literal string writes correctly`() {
        assertEquals("(Hello)", PdfString("Hello").toByteString())
        assertEquals("(A\\(B\\)C)", PdfString("A(B)C").toByteString())
        assertEquals("(Line\\nBreak)", PdfString("Line\nBreak").toByteString())
    }

    @Test
    fun `hex string writes correctly`() {
        assertEquals("<48656C6C6F>", PdfString("Hello", PdfString.Encoding.HEX).toByteString())
    }

    @Test
    fun `name writes correctly`() {
        assertEquals("/Type", PdfName("Type").toByteString())
        assertEquals("/Catalog", PdfName.CATALOG.toByteString())
    }

    @Test
    fun `null writes correctly`() {
        assertEquals("null", PdfNull.toByteString())
    }

    @Test
    fun `indirect reference writes correctly`() {
        assertEquals("1 0 R", PdfIndirectReference(1, 0).toByteString())
        assertEquals("10 2 R", PdfIndirectReference(10, 2).toByteString())
    }

    @Test
    fun `array writes correctly`() {
        val array = PdfArray(PdfInteger(1), PdfInteger(2), PdfInteger(3))
        assertEquals("[1 2 3]", array.toByteString())
    }

    @Test
    fun `empty array writes correctly`() {
        assertEquals("[]", PdfArray().toByteString())
    }

    @Test
    fun `dictionary writes correctly`() {
        val dict = PdfDictionary()
        dict.put(PdfName.TYPE, PdfName.CATALOG)
        val result = dict.toByteString()
        assert(result.contains("/Type")) { "Should contain /Type" }
        assert(result.contains("/Catalog")) { "Should contain /Catalog" }
        assert(result.startsWith("<<")) { "Should start with <<" }
        assert(result.endsWith(">>")) { "Should end with >>" }
    }

    @Test
    fun `stream writes correctly`() {
        val stream = PdfStream()
        stream.data = "BT /F1 12 Tf (Hello) Tj ET".toByteArray(Charsets.US_ASCII)
        val result = stream.toByteString()
        assert(result.contains("/Length 26")) { "Should contain length" }
        assert(result.contains("stream")) { "Should contain stream keyword" }
        assert(result.contains("endstream")) { "Should contain endstream keyword" }
        assert(result.contains("Hello")) { "Should contain stream data" }
    }

    @Test
    fun `dictionary typed getters work`() {
        val dict = PdfDictionary()
        dict.put("Type", PdfName.CATALOG)
        dict.put("Count", PdfInteger(5))
        dict.put("Kids", PdfArray())

        assertEquals(PdfName.CATALOG, dict.getAsName("Type"))
        assertEquals(PdfInteger(5), dict.getAsInteger("Count"))
        assertEquals(PdfArray(), dict.getAsArray("Kids"))
        assertEquals(null, dict.getAsName("Missing"))
    }
}
