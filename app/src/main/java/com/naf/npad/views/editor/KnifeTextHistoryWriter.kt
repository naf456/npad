package com.naf.npad.views.editor

import android.text.Editable
import android.text.TextWatcher
import com.naf.npad.History
import io.github.mthli.knife.KnifeText

class KnifeTextHistoryWriter (private val knifeText: KnifeText, private val history: History) : TextWatcher {

    init {
        knifeText.addTextChangedListener(this)
    }

    class BeforeTextChangedData (
            val start : Int,
            val origEnd : Int,
            val origText : CharSequence
            )

    class AfterTextChangedData(
            val newEnd: Int,
            val newText: CharSequence
            )

    private var beforeData : BeforeTextChangedData? = null
    private var afterData : AfterTextChangedData? = null

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        if(!history.recording) return
        s?: return

        beforeData = BeforeTextChangedData(
                start,
                start + count,
                s.subSequence(start, start + count)
        )
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        if(!history.recording) return

        val newText = s!!.subSequence(start, start + count)

        afterData = AfterTextChangedData(
                start + count,
                newText
        )
    }

    override fun afterTextChanged(s: Editable?) {
        if(!history.recording) return

        beforeData?: return
        afterData?: return

        val xdo = KnifeTextEditXdo(knifeText,
                beforeData!!.start,
                beforeData!!.origEnd,
                beforeData!!.origText,
                afterData!!.newEnd,
                afterData!!.newText
        )
        history.add(xdo)

        beforeData = null
        afterData = null
    }

    private class KnifeTextEditXdo(
            val knifeText: KnifeText,
            val start: Int,
            val origEnd: Int,
            val origText: CharSequence,
            val newEnd: Int,
            val newText: CharSequence
            ) : History.Xdo
    {
        override fun undo() {
            knifeText.text.replace(start, newEnd, origText)
        }

        override fun redo() {
            knifeText.text.replace(start, origEnd, newText)
        }
    }
}