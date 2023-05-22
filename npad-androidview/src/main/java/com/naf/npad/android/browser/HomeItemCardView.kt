package com.naf.npad.android.browser

import android.content.Context
import android.util.AttributeSet
import androidx.cardview.widget.CardView
import kotlin.math.roundToInt

class HomeItemCardView : CardView {
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context) : super(context)

    private var ratio : Float = 1.0F

    init {
        val dm = context.resources.displayMetrics
        val w = dm.widthPixels
        val h = dm.heightPixels
        ratio = (h / w).toFloat()
    }


    //For Vertical Lists
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(
            widthMeasureSpec,
            MeasureSpec.makeMeasureSpec(
                ((MeasureSpec.getSize(widthMeasureSpec) * 1.0) / 1).roundToInt(),
                //MeasureSpec.getSize(widthMeasureSpec),
                MeasureSpec.getMode(widthMeasureSpec)))
    }

    //For Horizontal Lists
    /*override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(
            MeasureSpec.makeMeasureSpec(
                ((MeasureSpec.getSize(heightMeasureSpec) * 0.5625) / 1).roundToInt(),
                MeasureSpec.getMode(heightMeasureSpec)),
        heightMeasureSpec)
    }*/
}