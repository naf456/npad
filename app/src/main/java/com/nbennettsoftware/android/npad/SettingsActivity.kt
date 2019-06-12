package com.nbennettsoftware.android.npad


import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity

import com.nbennettsoftware.android.npad.widgets.WallpaperView

class SettingsActivity : AppCompatActivity(), SettingsFragment.OnWallpaperChangedListener, SettingsFragment.OnShadeChangedListener, SettingsFragment.OnScalingChangedListener {

    private var wallpaperView: WallpaperView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)


        val settingsFragment = SettingsFragment()
        settingsFragment.setOnWallpaperChangedListener(this)
        settingsFragment.setOnShadeChangedListener(this)
        settingsFragment.setOnScalingChangedListener(this)

        supportFragmentManager.beginTransaction()
                .replace(R.id.settings_fragment_placeholder, settingsFragment as Fragment)
                .commit()


        wallpaperView = findViewById(R.id.settings_wallpaperImageView)

        wallpaperView?.applyWallpaperFromPreferences()
        wallpaperView?.applyWallpaperDimmerFromPreferences()
        wallpaperView?.applyScalingFromPreferences()

    }

    override fun OnWallpaperChanged() {
        wallpaperView?.applyWallpaperFromPreferences()
    }

    override fun OnDimmerChanged(intensity: String) {
        wallpaperView?.applyWallpaperDimmer(intensity)
    }

    override fun OnScalingChanged(scaling: String) {
        wallpaperView?.applyScaling(scaling)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

}
