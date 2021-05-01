package com.naf.npad


import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

import com.naf.npad.widgets.WallpaperView

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
                .replace(R.id.settings_fragment_placeholder, settingsFragment)
                .commit()


        wallpaperView = findViewById(R.id.settings_wallpaperImageView)

        wallpaperView?.applyWallpaperFromPreferences()
        wallpaperView?.applyWallpaperDimmerFromPreferences()
        wallpaperView?.applyScalingFromPreferences()

    }

    override fun onWallpaperChanged() {
        wallpaperView?.applyWallpaperFromPreferences()
    }

    override fun onDimmerChanged(shadeIntensity: String) {
        wallpaperView?.applyWallpaperDimmer(shadeIntensity)
    }

    override fun onScalingChanged(scaling: String) {
        wallpaperView?.applyScaling(scaling)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

}
