package com.naf.npad.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AppCompatDialogFragment
import com.naf.npad.R

class WarnExitDialog : AppCompatDialogFragment() {
    var onDialogFinished: (()->Unit)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireActivity())
        val appName = resources.getString(R.string.app_name)
        builder.setMessage("Exit $appName?")
        builder.setPositiveButton("Yes") {_,_ ->
                   onDialogFinished?.invoke()
        }
        builder.setNegativeButton("No") {dialog,_ ->
            dialog.cancel()
        }
        return builder.create()
    }
}