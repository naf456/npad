package com.naf.npad

import android.content.Context
import android.provider.MediaStore
import android.view.Gravity
import android.widget.Toast

object Utls {

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

    internal object Uri {
        fun getDisplayName(context: Context, uri: android.net.Uri): String? {
            val fileDisplayNameColumn = arrayOf(MediaStore.Files.FileColumns.DISPLAY_NAME)

            val cursor = context.contentResolver.query(uri, fileDisplayNameColumn, null, null, null)
                    ?: return null

            if (cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndex(fileDisplayNameColumn[0])
                return cursor.getString(columnIndex)
            }
            cursor.close()
            return null
        }

        fun getExtension(context: Context, uri: android.net.Uri): String {
            val displayName = getDisplayName(context, uri) ?: return ""
            return displayName.substring(displayName.lastIndexOf("."))
        }
    }
}
