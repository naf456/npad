package com.naf.npad.widgets

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.graphics.drawable.Drawable
import androidx.preference.PreferenceManager
import androidx.core.content.ContextCompat
import android.util.AttributeSet
import android.widget.Toast

import com.naf.npad.R
import com.naf.npad.storage.WallpaperManager

import java.io.IOException

import pl.droidsonroids.gif.GifDrawable
import pl.droidsonroids.gif.GifImageView

class WallpaperView : GifImageView, SharedPreferences.OnSharedPreferenceChangeListener {

    private var wallpaperManager: WallpaperManager? = null
    private var dimmerPaint: Paint? = null

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
        wallpaperManager = WallpaperManager(context)

        this.dimmerPaint = Paint()
        this.dimmerPaint!!.color = Color.BLACK
        this.dimmerPaint!!.alpha = 0


        PreferenceManager.getDefaultSharedPreferences(context)
                .registerOnSharedPreferenceChangeListener(this)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), dimmerPaint!!)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!isInEditMode) {
            this.applyWallpaperFromPreferences()
            this.applyWallpaperDimmerFromPreferences()
            this.applyScalingFromPreferences()
            this.forceWallpaperBehindKeyboard()
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {

        val wallpaper_key = resources.getString(R.string.pref_key_internal_wallpaper_name)
        val dimmerIntensity_key = resources.getString(R.string.pref_key_dimmer_intensity)
        val scaling_key = resources.getString(R.string.pref_key_scaling)

        if (key == wallpaper_key) {
            applyWallpaperFromPreferences()
        } else if (key == dimmerIntensity_key) {
            applyWallpaperDimmerFromPreferences()
        } else if (key == scaling_key) {
            applyScalingFromPreferences()
        }
    }

    fun applyWallpaperFromPreferences() {
        try {
            val wallpaperFile = wallpaperManager!!.internalizedWallpaper

            this.setDefaultWallpaper()

            if (wallpaperFile.name.endsWith(".gif")) {
                val drawable = GifDrawable(wallpaperFile)
                this.setImageDrawable(drawable)
            } else {
                val drawable = Drawable.createFromPath(wallpaperFile.path)
                setImageDrawable(drawable)
            }
        } catch (e: WallpaperManager.NoInternalWallpaperException) {
            this.setDefaultWallpaper()
        } catch (e: IOException) {
            this.setDefaultWallpaper()
            Toast.makeText(context, "Can't copy wallpaper", Toast.LENGTH_LONG).show()
        }

    }

    fun setDefaultWallpaper() {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)

        val drawDefaultBackground_key = this.resources.getString(R.string.pref_key_draw_default_background)
        val drawDefaultBackground_default = this.resources.getBoolean(R.bool.pref_default_draw_default_background)

        val drawDefaultBackground = preferences.getBoolean(drawDefaultBackground_key, drawDefaultBackground_default)

        if (drawDefaultBackground) {
            val defaultWallpaperResource = R.mipmap.beautiful_background
            this.setImageResource(defaultWallpaperResource)
        } else {
            this.setImageDrawable(null)
        }

    }

    fun applyWallpaperDimmerFromPreferences() {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val dimmerIntensityKey = context.getString(R.string.pref_key_dimmer_intensity)
        val dimmerIntensityDefault = context.getString(R.string.pref_default_shade_intensity)
        val dimmerIntensity = preferences.getString(dimmerIntensityKey, dimmerIntensityDefault)
        applyWallpaperDimmer(dimmerIntensity!!)
    }

    fun applyWallpaperDimmer(dimmingIntensity: String) {
        val DIMMER_OFF = context.getString(R.string.dimmer_off)
        val DIMMER_SUBTLE = context.getString(R.string.dimmer_subtle)
        val DIMMER_MODERATE = context.getString(R.string.dimmer_moderate)
        val DIMMER_INTENSE = context.getString(R.string.dimmer_intense)

        if (dimmingIntensity == DIMMER_OFF) {
            dimmerPaint!!.setARGB(0, 0, 0, 0) //transparent
        } else if (dimmingIntensity == DIMMER_SUBTLE) {
            dimmerPaint!!.color = ContextCompat.getColor(context, R.color.dimmerSubtle)
        } else if (dimmingIntensity == DIMMER_MODERATE) {
            dimmerPaint!!.color = ContextCompat.getColor(context, R.color.dimmerModerate)
        } else if (dimmingIntensity == DIMMER_INTENSE) {
            dimmerPaint!!.color = ContextCompat.getColor(context, R.color.dimmerIntense)
        }
        invalidate()
    }

    private fun forceWallpaperBehindKeyboard() {
        /* Instead of Match Parent, which shrinks the wallpaper when the soft keyboard is active,
        set height to window height. */
        val screenSize = Point()

        (context as Activity).windowManager.defaultDisplay.getRealSize(screenSize)

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