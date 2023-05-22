package com.naf.npad.android.util

import android.content.Context
import android.provider.MediaStore

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
