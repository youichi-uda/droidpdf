package com.droidpdf.security

import com.droidpdf.core.PdfDictionary
import com.droidpdf.core.PdfInteger
import com.droidpdf.core.PdfName
import com.droidpdf.core.PdfString
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * PDF encryption support (ISO 32000-1, 7.6).
 *
 * Supports:
 * - AES-128 encryption (PDF 1.6+)
 * - AES-256 encryption (PDF 2.0 / Extension Level 5)
 * - User and owner passwords
 * - Permission flags (print, copy, modify, etc.)
 */
class PdfEncryption private constructor(
    private val userPassword: String,
    private val ownerPassword: String,
    private val permissions: Int,
    private val keyLength: Int,
) {
    private val random = SecureRandom()

    /**
     * Encrypt data with AES.
     */
    fun encrypt(
        data: ByteArray,
        key: ByteArray,
    ): ByteArray {
        val iv = ByteArray(16)
        random.nextBytes(iv)
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
        val encrypted = cipher.doFinal(data)
        return iv + encrypted
    }

    /**
     * Decrypt data with AES.
     */
    fun decrypt(
        data: ByteArray,
        key: ByteArray,
    ): ByteArray {
        if (data.size < 16) return data
        val iv = data.copyOfRange(0, 16)
        val encrypted = data.copyOfRange(16, data.size)
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
        return cipher.doFinal(encrypted)
    }

    /**
     * Generate encryption key from password.
     */
    fun generateEncryptionKey(): ByteArray {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(userPassword.toByteArray(Charsets.UTF_8))
        md.update(ownerPassword.toByteArray(Charsets.UTF_8))
        md.update(
            byteArrayOf(
                (permissions and 0xFF).toByte(),
                ((permissions shr 8) and 0xFF).toByte(),
                ((permissions shr 16) and 0xFF).toByte(),
                ((permissions shr 24) and 0xFF).toByte(),
            ),
        )
        val digest = md.digest()
        return digest.copyOf(keyLength / 8)
    }

    /**
     * Build the Encrypt dictionary for the PDF trailer.
     */
    fun buildEncryptDictionary(): PdfDictionary {
        val dict = PdfDictionary()
        dict.put(PdfName.FILTER, PdfName("Standard"))

        if (keyLength == 256) {
            // AES-256 (Extension Level 5)
            dict.put("V", PdfInteger(5))
            dict.put("R", PdfInteger(5))
            dict.put("Length", PdfInteger(256))
        } else {
            // AES-128
            dict.put("V", PdfInteger(4))
            dict.put("R", PdfInteger(4))
            dict.put("Length", PdfInteger(128))
        }

        dict.put("P", PdfInteger(permissions))

        // Stream and string encryption filters
        val cf = PdfDictionary()
        val stdCf = PdfDictionary()
        stdCf.put("CFM", PdfName("AESV2"))
        stdCf.put("AuthEvent", PdfName("DocOpen"))
        stdCf.put("Length", PdfInteger(keyLength / 8))
        cf.put("StdCF", stdCf)
        dict.put("CF", cf)
        dict.put("StmF", PdfName("StdCF"))
        dict.put("StrF", PdfName("StdCF"))

        // Generate password hashes
        val key = generateEncryptionKey()
        val uHash = computeUserPasswordHash(key)
        val oHash = computeOwnerPasswordHash(key)
        dict.put("U", PdfString(String(uHash, Charsets.ISO_8859_1), PdfString.Encoding.HEX))
        dict.put("O", PdfString(String(oHash, Charsets.ISO_8859_1), PdfString.Encoding.HEX))

        return dict
    }

    private fun computeUserPasswordHash(key: ByteArray): ByteArray {
        val md = MessageDigest.getInstance("MD5")
        md.update(PADDING)
        return md.digest().copyOf(32)
    }

    private fun computeOwnerPasswordHash(key: ByteArray): ByteArray {
        val md = MessageDigest.getInstance("MD5")
        md.update(ownerPassword.toByteArray(Charsets.UTF_8))
        md.update(PADDING)
        return md.digest().copyOf(32)
    }

    /**
     * Verify a user password against this encryption.
     */
    fun verifyUserPassword(password: String): Boolean = password == userPassword

    /**
     * Verify an owner password against this encryption.
     */
    fun verifyOwnerPassword(password: String): Boolean = password == ownerPassword

    companion object {
        // ISO 32000-1, Table 3 - PDF standard padding
        @Suppress("ktlint:standard:max-line-length")
        private val PADDING =
            intArrayOf(
                0x28, 0xBF, 0x4E, 0x5E, 0x4E, 0x75, 0x8A, 0x41,
                0x64, 0x00, 0x4E, 0x56, 0xFF, 0xFA, 0x01, 0x08,
                0x2E, 0x2E, 0x00, 0xB6, 0xD0, 0x68, 0x3E, 0x80,
                0x2F, 0x0C, 0xA9, 0xFE, 0x64, 0x53, 0x69, 0x7A,
            ).map { it.toByte() }.toByteArray()
    }

    /**
     * Builder for PdfEncryption configuration.
     */
    class Builder {
        private var userPassword: String = ""
        private var ownerPassword: String = ""
        private var permissions: Int = DEFAULT_PERMISSIONS
        private var keyLength: Int = 128

        fun setUserPassword(password: String): Builder {
            this.userPassword = password
            return this
        }

        fun setOwnerPassword(password: String): Builder {
            this.ownerPassword = password
            return this
        }

        fun setKeyLength(bits: Int): Builder {
            require(bits == 128 || bits == 256) { "Key length must be 128 or 256" }
            this.keyLength = bits
            return this
        }

        fun allowPrinting(allow: Boolean): Builder {
            permissions = if (allow) permissions or PRINT else permissions and PRINT.inv()
            return this
        }

        fun allowCopying(allow: Boolean): Builder {
            permissions = if (allow) permissions or COPY else permissions and COPY.inv()
            return this
        }

        fun allowModifying(allow: Boolean): Builder {
            permissions = if (allow) permissions or MODIFY else permissions and MODIFY.inv()
            return this
        }

        fun allowAnnotations(allow: Boolean): Builder {
            permissions = if (allow) permissions or ANNOT else permissions and ANNOT.inv()
            return this
        }

        fun allowFilling(allow: Boolean): Builder {
            permissions = if (allow) permissions or FILL else permissions and FILL.inv()
            return this
        }

        fun build(): PdfEncryption {
            if (ownerPassword.isEmpty()) ownerPassword = userPassword
            return PdfEncryption(userPassword, ownerPassword, permissions, keyLength)
        }
    }
}

// PDF permission flags (ISO 32000-1, Table 22)
const val PRINT = 1 shl 2
const val MODIFY = 1 shl 3
const val COPY = 1 shl 4
const val ANNOT = 1 shl 5
const val FILL = 1 shl 8
const val ACCESSIBILITY = 1 shl 9
const val ASSEMBLE = 1 shl 10
const val PRINT_HQ = 1 shl 11
const val DEFAULT_PERMISSIONS = -4
