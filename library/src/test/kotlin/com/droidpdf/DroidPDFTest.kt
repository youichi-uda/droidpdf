package com.droidpdf

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DroidPDFTest {
    @Test
    fun `license key is null by default`() {
        assertNull(DroidPDF.licenseKey)
    }

    @Test
    fun `validates correct Gumroad license key`() {
        assertTrue(DroidPDF.validateKeyFormat("6DE81129-17CD4B63-84B5F942-4E36453C"))
    }

    @Test
    fun `validates lowercase key`() {
        assertTrue(DroidPDF.validateKeyFormat("6de81129-17cd4b63-84b5f942-4e36453c"))
    }

    @Test
    fun `validates mixed case key`() {
        assertTrue(DroidPDF.validateKeyFormat("6De81129-17cD4B63-84b5F942-4e36453C"))
    }

    @Test
    fun `rejects empty string`() {
        assertFalse(DroidPDF.validateKeyFormat(""))
    }

    @Test
    fun `rejects key with wrong segment count`() {
        assertFalse(DroidPDF.validateKeyFormat("6DE81129-17CD4B63-84B5F942"))
    }

    @Test
    fun `rejects key with wrong segment length`() {
        assertFalse(DroidPDF.validateKeyFormat("6DE8112-17CD4B63-84B5F942-4E36453C"))
    }

    @Test
    fun `rejects key with non-hex characters`() {
        assertFalse(DroidPDF.validateKeyFormat("ZZZZZZZZ-17CD4B63-84B5F942-4E36453C"))
    }

    @Test
    fun `rejects key without dashes`() {
        assertFalse(DroidPDF.validateKeyFormat("6DE8112917CD4B6384B5F9424E36453C"))
    }

    @Test
    fun `rejects random string`() {
        assertFalse(DroidPDF.validateKeyFormat("not-a-valid-key"))
    }
}
