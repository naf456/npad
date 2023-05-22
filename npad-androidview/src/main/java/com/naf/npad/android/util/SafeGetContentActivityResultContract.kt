package com.naf.npad.android.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract

class SafeGetContentActivityResultContract : ActivityResultContract<String, Uri?>() {

    override fun createIntent(context: Context, input: String): Intent {
        val i = Intent()
        i.action = Intent.ACTION_GET_CONTENT
        i.addCategory(Intent.CATEGORY_OPENABLE)
        i.type = input

        return i
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        intent?.let {
            return it.data
        }
        return null
    }
}