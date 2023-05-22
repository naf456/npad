package com.naf.npad.android.util

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import com.naf.npad.android.MainViewModel
import java.util.*

class NPMLImporter(val activity: ComponentActivity) {

    private val mainViewModel : MainViewModel = ViewModelProvider(activity)[MainViewModel::class.java]

    private var importDocumentLauncher = activity.registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        try {
            uri?: return@registerForActivityResult
            val stream = activity.contentResolver.openInputStream(uri)
            val content : String
            Scanner(stream).let {
                it.useDelimiter("\\A")
                content = if(it.hasNext()) it.next() else ""
            }
            val name = tryToGetNameFromUri(uri)
            mainViewModel.newPage(title = name, content = content)

        } catch (e: Exception) {
            e.printStackTrace()
            toast(activity, "Cannot Important Document")
        }
    }

    private fun tryToGetNameFromUri(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = activity.contentResolver.query(uri, null, null, null, null) ?: return null
            cursor.use {
                if (it.moveToFirst()) {
                    val colIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    result = cursor.getString(colIndex)
                }
            }
        }
        if (result == null) {
            result = uri.path
            result?.let {
                val cut = it.lastIndexOf('/')
                if (cut != -1) {
                    result = it.substring(cut + 1)
                }
            }
        }
        return result
    }

    fun importDocument() {
        importDocumentLauncher.launch(arrayOf())
    }
}