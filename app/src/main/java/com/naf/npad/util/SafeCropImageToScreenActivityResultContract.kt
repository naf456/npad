package com.naf.npad.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.content.ContextCompat
import com.naf.npad.R
import com.yalantis.ucrop.UCrop
import java.io.File

class SafeCropImageToScreenActivityResultContract : ActivityResultContract<Uri, Uri?>() {

    override fun createIntent(context: Context, input: Uri): Intent {
        val w = context.resources.displayMetrics.widthPixels.toFloat()
        val h = context.resources.displayMetrics.heightPixels.toFloat()

        val primary = ContextCompat.getColor(context, R.color.colorPrimary)
        val dark = ContextCompat.getColor(context, R.color.colorPrimaryDark)
        val accent = ContextCompat.getColor(context, R.color.colorAccent)

        val o = UCrop.Options()
        o.setCropFrameColor(Color.WHITE)
        o.setCropGridColor(Color.WHITE)
        o.setToolbarColor(Color.BLACK)
        o.setToolbarWidgetColor(Color.WHITE)
        o.setStatusBarColor(Color.BLACK)
        o.setHideBottomControls(true)
        o.setToolbarTitle("Positioning")

        return UCrop.of(input, Uri.fromFile(File(context.cacheDir, "temp.png")))
            .withAspectRatio(1f, h/w)
            .withOptions(o)
            .getIntent(context)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        return intent?.let { UCrop.getOutput(it) }
    }
}