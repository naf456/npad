package com.naf.npad.android.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import androidx.preference.PreferenceManager
import com.naf.npad.R
import pl.droidsonroids.gif.GifImageView
import kotlin.math.roundToInt

open class DimmedImageView : GifImageView {

    private var dimmerPaint = Paint()

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        if (!isInEditMode) {
            init()
        }
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        if (!isInEditMode) {
            init()
        }
    }

    constructor(context: Context) : super(context) {
        if (!isInEditMode) {
            init()
        }
    }

    private fun init(){
        this.dimmerPaint = Paint()
        applyWallpaperDimmer(27)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), dimmerPaint)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!isInEditMode) {
            this.applyWallpaperDimmerFromPreferences()
        }
    }

    protected fun applyWallpaperDimmerFromPreferences() {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val dimmerIntensityKey = context.getString(R.string.pref_key_dimmer_intensity)
        val dimmerIntensityDefault = context.resources.getInteger(R.integer.pref_default_shade_intensity)
        val dimmerIntensity = preferences.getInt(dimmerIntensityKey, dimmerIntensityDefault)
        applyWallpaperDimmer(dimmerIntensity)
    }

    private fun applyWallpaperDimmer(dimmingIntensity: Int) {
        dimmerPaint.setARGB(percentageToHex(dimmingIntensity),0,0, 0)
        invalidate()
    }

    private fun percentageToHex(percent: Int) : Int{
        return (2.55 * percent).roundToInt()
    }

}