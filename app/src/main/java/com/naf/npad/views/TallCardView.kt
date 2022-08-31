package com.naf.npad.views

import android.content.Context
import android.content.ContextWrapper
import android.util.AttributeSet
import android.view.WindowMetrics
import androidx.cardview.widget.CardView
import kotlin.math.roundToInt

class TallCardView : CardView {
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context) : super(context)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(
            widthMeasureSpec,
            MeasureSpec.makeMeasureSpec(
                ((MeasureSpec.getSize(widthMeasureSpec) * 1.7777) / 1).roundToInt(),
                MeasureSpec.getMode(widthMeasureSpec)))
    }
}