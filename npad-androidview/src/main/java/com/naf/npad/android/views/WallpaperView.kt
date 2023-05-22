package com.naf.npad.android.views

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.graphics.*
import androidx.preference.PreferenceManager
import android.util.AttributeSet

import com.naf.npad.R

class WallpaperView : DimmedImageView, SharedPreferences.OnSharedPreferenceChangeListener {

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

    private fun init() {

        PreferenceManager.getDefaultSharedPreferences(context)
                .registerOnSharedPreferenceChangeListener(this)

        // Precompute blur, cache it and animate the opacity.
        // We need to figure out when to update the blur cache though.
        // Change on image change and on layout change.


    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!isInEditMode) {
            this.applyWallpaperDimmerFromPreferences()
            this.applyScalingFromPreferences()
            this.forceWallpaperBehindKeyboard()
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {

        val dimmerIntensity_key = resources.getString(R.string.pref_key_dimmer_intensity)
        val scaling_key = resources.getString(R.string.pref_key_scaling)

        when (key) {
            dimmerIntensity_key -> {
                applyWallpaperDimmerFromPreferences()
            }
            scaling_key -> {
                applyScalingFromPreferences()
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun forceWallpaperBehindKeyboard() {
        /* Instead of Match Parent, which shrinks the wallpaper when the soft keyboard is active,
        set height to window height. */
        val screenSize = Point()

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            (context as Activity).display!!.getRealSize(screenSize)
        } else {
            (context as Activity).windowManager.defaultDisplay.getRealSize(screenSize)
        }

        val params = layoutParams
        params.height = screenSize.y
        layoutParams = params
    }


    fun applyScalingFromPreferences() {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val scalingKey = context.getString(R.string.pref_key_scaling)
        val scalingDefault = context.getString(R.string.pref_default_scaling)
        val scaling = preferences.getString(scalingKey, scalingDefault)
        applyScaling(scaling!!)
    }

    fun applyScaling(scaling: String) {
        val TO_WIDTH = context.getString(R.string.to_width)
        val TO_HEIGHT = context.getString(R.string.to_height)
        val STRETCH = context.getString(R.string.stretch)

        if (scaling == TO_WIDTH) {
            scaleType = ScaleType.FIT_CENTER
        } else if (scaling == TO_HEIGHT) {
            scaleType = ScaleType.CENTER_CROP
        } else if (scaling == STRETCH) {
            scaleType = ScaleType.FIT_XY
        }
    }
}
