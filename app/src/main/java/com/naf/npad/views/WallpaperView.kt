package com.naf.npad.views

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.graphics.*
import android.graphics.drawable.Drawable
import androidx.preference.PreferenceManager
import android.util.AttributeSet
import android.view.View
import android.widget.Toast
import com.google.android.renderscript.Toolkit

import com.naf.npad.R
import com.naf.npad.repository.WallpaperManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking

import java.io.IOException

import pl.droidsonroids.gif.GifDrawable
import pl.droidsonroids.gif.GifImageView
import kotlin.math.roundToInt

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

        // Precompute blur, cache it and animate the opacity.
        // We need to figure out when to update the blur cache though.
        // Change on image change and on layout change.


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

        when (key) {
            wallpaper_key -> {
                applyWallpaperFromPreferences()
            }
            dimmerIntensity_key -> {
                applyWallpaperDimmerFromPreferences()
            }
            scaling_key -> {
                applyScalingFromPreferences()
            }
        }
    }

    fun applyWallpaperFromPreferences() {
        try {
            val wallpaperFile = wallpaperManager!!.internalizedWallpaper

            this.clearWallpaper()

            if (wallpaperFile.name.endsWith(".gif")) {
                val drawable = GifDrawable(wallpaperFile)
                this.setImageDrawable(drawable)
            } else {
                val drawable = Drawable.createFromPath(wallpaperFile.path)
                setImageDrawable(drawable)
            }
        } catch (e: WallpaperManager.NoInternalWallpaperException) {
            this.clearWallpaper()
        } catch (e: IOException) {
            this.clearWallpaper()
            Toast.makeText(context, "Can't copy wallpaper", Toast.LENGTH_LONG).show()
        }
    }

    private fun clearWallpaper() {
        setImageDrawable(null)
    }

    fun applyWallpaperDimmerFromPreferences() {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val dimmerIntensityKey = context.getString(R.string.pref_key_dimmer_intensity)
        val dimmerIntensityDefault = context.resources.getInteger(R.integer.pref_default_shade_intensity)
        val dimmerIntensity = preferences.getInt(dimmerIntensityKey, dimmerIntensityDefault)
        applyWallpaperDimmer(dimmerIntensity)
    }

    fun applyWallpaperDimmer(dimmingIntensity: Int) {
        dimmerPaint!!.setARGB(percentageToHex(dimmingIntensity),0,0, 0)
        invalidate()
    }

    fun percentageToHex(percent: Int) : Int{
        return (2.55 * percent).roundToInt()
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
