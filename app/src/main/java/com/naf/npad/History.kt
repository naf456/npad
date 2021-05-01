package com.naf.npad

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText

class History {

    private var watchedText : EditText? = null

    fun watch(editText: EditText) {
        editText.addTextChangedListener(Watcher())
    }

    class action {
        fun undo(){}
        fun redo(){}
    }

    class Watcher : TextWatcher {

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            TODO("Not yet implemented")
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            TODO("Not yet implemented")
        }

        override fun afterTextChanged(s: Editable?) {}

    }
}