package com.droidpdf.sample

import android.app.Activity
import android.os.Bundle
import com.droidpdf.DroidPDF

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DroidPDF.initialize(this)
    }
}
