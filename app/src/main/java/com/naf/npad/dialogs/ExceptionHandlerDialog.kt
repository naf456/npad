package com.naf.npad.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AppCompatDialogFragment
import com.naf.npad.R

class ExceptionHandlerDialog(val exception: Throwable) : AppCompatDialogFragment() {
    var onDialogFinished: (()->Unit)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireActivity())
        builder.setTitle("Exception!")
        builder.setMessage(exception.message)
        builder.setPositiveButton("Okay") {_,_ ->
                   onDialogFinished?.invoke()
        }
        return builder.create()
    }
}