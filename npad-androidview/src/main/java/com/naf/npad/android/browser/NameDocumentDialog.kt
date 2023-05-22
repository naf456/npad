package com.naf.npad.android.browser

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
import android.widget.EditText
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatDialogFragment
import com.naf.npad.R

class NameDocumentDialog : AppCompatDialogFragment() {
    var onDialogFinished: ((documentName: String?)->Unit)? = null

    private lateinit var textBox: EditText

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialogContent = requireActivity().layoutInflater.inflate(R.layout.editor_dialog_name_document, null)
        textBox = dialogContent.findViewById(R.id.dialog_name_document_name)
        textBox.setOnKeyListener { _, keyCode, keyEvent ->
            if(keyCode ==  KeyEvent.KEYCODE_ENTER && keyEvent.action == KeyEvent.ACTION_UP) {
                finish()
                return@setOnKeyListener true
            }
            return@setOnKeyListener false
        }
        val builder = AlertDialog.Builder(requireActivity())
        builder.setTitle(R.string.pagemanger_new_document_dialog_title)
        builder.setView(dialogContent)
        builder.setPositiveButton("Confirm") {_,_ ->
            finish()
        }
        val dialog = builder.create()
        dialog.window?.setSoftInputMode(SOFT_INPUT_STATE_VISIBLE)
        return dialog
    }

    private val backPressCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            dismiss()
        }
    }

    private fun finish(){
        onDialogFinished?.invoke(textBox.text.toString())
        dismiss()
    }


}