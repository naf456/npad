package com.naf.npad.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AppCompatDialogFragment

class WarnSaveDialog : AppCompatDialogFragment() {
    var onDialogFinished: (()->Unit)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireActivity())
        builder.setMessage("All unsaved changes will be lost.")
        builder.setPositiveButton("Okay") {_,_ ->
                   onDialogFinished?.invoke()
        }
        builder.setNeutralButton("Cancel") {dialog,_ ->
            dialog.cancel()
        }
        return builder.create()
    }
}