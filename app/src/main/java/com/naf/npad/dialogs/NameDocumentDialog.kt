package com.naf.npad.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AppCompatDialogFragment
import com.naf.npad.R

class NameDocumentDialog : AppCompatDialogFragment() {
    var onDialogFinished: ((documentName: String?)->Unit)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialogView = requireActivity().layoutInflater.inflate(R.layout.dialog_name_document, null)
        val textBox = dialogView.findViewById<EditText>(R.id.dialog_name_document_name)
        val builder = AlertDialog.Builder(requireActivity())
        builder.setMessage("Name your document")
        builder.setView(dialogView)
        builder.setPositiveButton("Done") {_,_ ->
                   onDialogFinished?.invoke(textBox.text.toString())
        }
        builder.setNegativeButton("Cancel") {dialog,_ ->
            dialog.cancel()
        }
        return builder.create()
    }
}