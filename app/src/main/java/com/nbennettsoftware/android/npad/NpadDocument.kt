package com.nbennettsoftware.android.npad

import android.net.Uri

internal class NpadDocument(val uri: Uri?, documentType: String?) {
    val type: String

    init {
        type = TYPE_NPAD_ML
    }

    companion object {

        val TYPE_PLAIN_TEXT = ".txt"
        val TYPE_NPAD_ML = ".npml"
    }
}
