package com.naf.npad.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
import android.widget.EditText
import androidx.appcompat.app.AppCompatDialogFragment
import com.naf.npad.R

class NameDocumentDialog : AppCompatDialogFragment() {
    var onDialogFinished: ((documentName: String?)->Unit)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialogContent = requireActivity().layoutInflater.inflate(R.layout.dialog_name_document, null)
        val textBox = dialogContent.findViewById<EditText>(R.id.dialog_name_document_name)
        textBox.setOnKeyListener { v, keyCode, _ ->
            if(keyCode ==  KeyEvent.KEYCODE_ENTER){
                onDialogFinished?.invoke(textBox.text.toString())
                dismiss()
                return@setOnKeyListener true
            }
            return@setOnKeyListener false
        }
        val builder = AlertDialog.Builder(requireActivity())
        builder.setTitle(R.string.pagemanger_new_document_dialog_title)
        builder.setView(dialogContent)
        builder.setNegativeButton("Cancel") {dialog,_ ->
            dialog.cancel()
        }
        val dialog = builder.create()
        dialog.window?.setSoftInputMode(SOFT_INPUT_STATE_VISIBLE)
        return dialog
    }



}