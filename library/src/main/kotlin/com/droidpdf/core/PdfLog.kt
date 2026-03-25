package com.droidpdf.core

import android.util.Log

/**
 * Internal logging wrapper for DroidPDF.
 * Delegates to android.util.Log but can be overridden for testing.
 */
internal object PdfLog {
    private const val TAG = "DroidPDF"

    var logger: Logger = AndroidLogger()

    fun w(message: String) {
        logger.w(TAG, message)
    }

    fun e(message: String) {
        logger.e(TAG, message)
    }

    interface Logger {
        fun w(
            tag: String,
            message: String,
        )

        fun e(
            tag: String,
            message: String,
        )
    }

    class AndroidLogger : Logger {
        override fun w(
            tag: String,
            message: String,
        ) {
            Log.w(tag, message)
        }

        override fun e(
            tag: String,
            message: String,
        ) {
            Log.e(tag, message)
        }
    }

    class NoOpLogger : Logger {
        override fun w(
            tag: String,
            message: String,
        ) {}

        override fun e(
            tag: String,
            message: String,
        ) {}
    }
}
