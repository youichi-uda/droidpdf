package com.droidpdf

import android.content.Context
import android.util.Log

/**
 * DroidPDF library entry point.
 */
object DroidPDF {
    private const val TAG = "DroidPDF"
    internal var licenseKey: String? = null
    internal var isInitialized = false

    /**
     * Initialize DroidPDF with an optional license key.
     * Commercial use requires a valid license key.
     */
    fun initialize(
        context: Context,
        licenseKey: String? = null,
    ) {
        this.licenseKey = licenseKey
        this.isInitialized = true
    }

    internal fun warnIfUnlicensed() {
        if (licenseKey == null) {
            Log.w(TAG, "No license key set. Commercial use requires a license.")
            Log.w(TAG, "Purchase at https://xxxxx.gumroad.com/l/droidpdf")
        }
    }
}
