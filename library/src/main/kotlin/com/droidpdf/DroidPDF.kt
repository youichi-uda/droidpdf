package com.droidpdf

import android.content.Context
import com.droidpdf.core.PdfLog

/**
 * DroidPDF library entry point.
 */
object DroidPDF {
    internal var licenseKey: String? = null
    internal var isInitialized = false
    internal var isLicenseValid = false

    // Gumroad license key format: 8 hex chars separated by hyphens, 4 groups
    private val LICENSE_KEY_PATTERN =
        Regex("^[0-9A-Fa-f]{8}-[0-9A-Fa-f]{8}-[0-9A-Fa-f]{8}-[0-9A-Fa-f]{8}$")

    /**
     * Initialize DroidPDF with an optional license key.
     * Commercial use requires a valid license key.
     */
    fun initialize(
        context: Context,
        licenseKey: String? = null,
    ) {
        this.licenseKey = licenseKey
        this.isLicenseValid = licenseKey != null && validateKeyFormat(licenseKey)
        this.isInitialized = true

        if (licenseKey != null && !isLicenseValid) {
            PdfLog.w("Invalid license key format.")
        }
    }

    /**
     * Check if the provided license key has a valid format.
     */
    fun validateKeyFormat(key: String): Boolean = LICENSE_KEY_PATTERN.matches(key)

    internal fun warnIfUnlicensed() {
        if (!isLicenseValid) {
            PdfLog.w("No license key set. Commercial use requires a license.")
            PdfLog.w("Purchase at https://y1uda.gumroad.com/l/DroidPDF")
        }
    }
}
