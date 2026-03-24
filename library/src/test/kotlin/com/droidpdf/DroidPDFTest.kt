package com.droidpdf

import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class DroidPDFTest {
    @Test
    fun `license key is null by default`() {
        assertNull(DroidPDF.licenseKey)
    }
}
