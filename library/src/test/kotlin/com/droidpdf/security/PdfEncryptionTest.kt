package com.droidpdf.security

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PdfEncryptionTest {
    @Test
    fun `builds encryption with default settings`() {
        val enc =
            PdfEncryption.Builder()
                .setUserPassword("user123")
                .build()

        assertNotNull(enc)
    }

    @Test
    fun `generates encryption key`() {
        val enc =
            PdfEncryption.Builder()
                .setUserPassword("test")
                .setOwnerPassword("owner")
                .build()

        val key = enc.generateEncryptionKey()
        assertEquals(16, key.size) // 128-bit = 16 bytes
    }

    @Test
    fun `generates 256-bit key`() {
        val enc =
            PdfEncryption.Builder()
                .setUserPassword("test")
                .setKeyLength(256)
                .build()

        val key = enc.generateEncryptionKey()
        assertEquals(32, key.size) // 256-bit = 32 bytes
    }

    @Test
    fun `encrypt and decrypt round-trip`() {
        val enc =
            PdfEncryption.Builder()
                .setUserPassword("secret")
                .build()

        val key = enc.generateEncryptionKey()
        val original = "Hello, PDF encryption!".toByteArray()
        val encrypted = enc.encrypt(original, key)
        val decrypted = enc.decrypt(encrypted, key)

        assertArrayEquals(original, decrypted)
    }

    @Test
    fun `encrypted data differs from original`() {
        val enc =
            PdfEncryption.Builder()
                .setUserPassword("secret")
                .build()

        val key = enc.generateEncryptionKey()
        val original = "Hello, PDF encryption!".toByteArray()
        val encrypted = enc.encrypt(original, key)

        assertNotEquals(original.toList(), encrypted.toList())
    }

    @Test
    fun `verifies passwords`() {
        val enc =
            PdfEncryption.Builder()
                .setUserPassword("user")
                .setOwnerPassword("owner")
                .build()

        assertTrue(enc.verifyUserPassword("user"))
        assertFalse(enc.verifyUserPassword("wrong"))
        assertTrue(enc.verifyOwnerPassword("owner"))
        assertFalse(enc.verifyOwnerPassword("wrong"))
    }

    @Test
    fun `builds encrypt dictionary for AES-128`() {
        val enc =
            PdfEncryption.Builder()
                .setUserPassword("test")
                .setKeyLength(128)
                .build()

        val dict = enc.buildEncryptDictionary()
        assertEquals("Standard", dict.getAsName("Filter")?.value)
        assertEquals(4L, dict.getAsInteger("V")?.value)
        assertEquals(128L, dict.getAsInteger("Length")?.value)
    }

    @Test
    fun `builds encrypt dictionary for AES-256`() {
        val enc =
            PdfEncryption.Builder()
                .setUserPassword("test")
                .setKeyLength(256)
                .build()

        val dict = enc.buildEncryptDictionary()
        assertEquals(5L, dict.getAsInteger("V")?.value)
        assertEquals(256L, dict.getAsInteger("Length")?.value)
    }

    @Test
    fun `permission flags work correctly`() {
        val enc =
            PdfEncryption.Builder()
                .setUserPassword("test")
                .allowPrinting(false)
                .allowCopying(false)
                .allowModifying(true)
                .build()

        val dict = enc.buildEncryptDictionary()
        val p = dict.getAsInteger("P")?.value?.toInt() ?: 0
        // Print bit should be off
        assertEquals(0, p and PRINT)
        // Copy bit should be off
        assertEquals(0, p and COPY)
        // Modify bit should be on
        assertNotEquals(0, p and MODIFY)
    }

    @Test
    fun `different passwords produce different keys`() {
        val enc1 = PdfEncryption.Builder().setUserPassword("pass1").build()
        val enc2 = PdfEncryption.Builder().setUserPassword("pass2").build()

        val key1 = enc1.generateEncryptionKey()
        val key2 = enc2.generateEncryptionKey()

        assertNotEquals(key1.toList(), key2.toList())
    }

    @Test
    fun `owner password defaults to user password`() {
        val enc =
            PdfEncryption.Builder()
                .setUserPassword("same")
                .build()

        assertTrue(enc.verifyOwnerPassword("same"))
    }
}
