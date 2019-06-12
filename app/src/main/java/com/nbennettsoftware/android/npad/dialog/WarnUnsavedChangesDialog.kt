package com.nbennettsoftware.android.npad.dialog

import android.app.Dialog
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatDialogFragment

class WarnUnsavedChangesDialog : AppCompatDialogFragment() {

    var onWarningFinished: (()->Unit)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(activity!!)
        builder.setMessage("Any unsaved changes to the document will be lost.")
        builder.setPositiveButton("Ok") { _, _ ->
            onWarningFinished?.invoke()
        }
        builder.setNeutralButton("Cancel") { dialog, _ -> dialog.cancel() }
        isCancelable = false
        return builder.create()
    }

}
