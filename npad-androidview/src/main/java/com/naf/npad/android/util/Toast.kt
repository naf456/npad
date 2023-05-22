package com.naf.npad.android.util

import android.content.Context
import android.view.Gravity
import android.widget.Toast

internal fun toast(context: Context, msg: String) {
    val toast = Toast.makeText(context, msg, Toast.LENGTH_SHORT)
    toast.setGravity(Gravity.TOP or Gravity.CENTER, 0, 0)
    toast.show()
}

internal fun toastLong(context: Context, msg: String) {
    val toast = Toast.makeText(context, msg, Toast.LENGTH_LONG)
    toast.setGravity(Gravity.TOP or Gravity.CENTER, 0, 0)
    toast.show()
}