package com.nbennettsoftware.android.npad

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.preference.ListPreference
import android.preference.Preference
import android.preference.PreferenceFragment
import android.preference.PreferenceManager
import android.widget.Toast

import com.nbennettsoftware.android.npad.storage.WallpaperManager

class SettingsFragment : PreferenceFragment() {

    private lateinit var wallpaperManager: WallpaperManager
    private val pickWallpaperIntentId = 0
    var onWallpaperChangedListener: OnWallpaperChangedListener? = null
    var onScalingChangedListener: OnScalingChangedListener? = null
    var onShadeChangedListener: OnShadeChangedListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        wallpaperManager = WallpaperManager(activity)

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences)
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_key_font_size)))
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_key_dimmer_intensity)))
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_key_scaling)))

        //We let bindPreferenceSummaryToValue set are summary for us.
        findPreference(getString(R.string.pref_key_scaling)).onPreferenceChangeListener = OnScalingChangeListener()
        findPreference(getString(R.string.pref_key_dimmer_intensity)).onPreferenceChangeListener = OnShadeIntensityChangeListener()

        findPreference(getString(R.string.pref_key_pick_wallpaper)).onPreferenceClickListener = OnPickWallpaperClickListener()
        findPreference(getString(R.string.pref_key_clear_wallpaper)).onPreferenceClickListener = OnClearWallpaperClickListener()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == pickWallpaperIntentId && resultCode == Activity.RESULT_OK) {
            val uri = data.data ?: return
            try {
                wallpaperManager.replaceInternalizeWallpaper(uri)
            } catch (e: WallpaperManager.ReplaceInternalWallpaperException) {
                e.printStackTrace()
                Toast.makeText(activity, "Cannot replace internal wallpaper", Toast.LENGTH_LONG).show()
            }

            if (onWallpaperChangedListener != null) {
                onWallpaperChangedListener!!.OnWallpaperChanged()
            }
        }
    }

    interface OnWallpaperChangedListener {
        fun OnWallpaperChanged()
    }

    interface OnScalingChangedListener {
        fun OnScalingChanged(scaling: String)
    }

    interface OnShadeChangedListener {
        fun OnDimmerChanged(shadeIntensity: String)
    }

    internal fun setOnWallpaperChangedListener(onWallpaperChangedListener: OnWallpaperChangedListener) {
        this.onWallpaperChangedListener = onWallpaperChangedListener
    }

    internal fun setOnScalingChangedListener(onScalingChangedListener: OnScalingChangedListener) {
        this.onScalingChangedListener = onScalingChangedListener
    }

    internal fun setOnShadeChangedListener(onShadeChangedListener: OnShadeChangedListener) {
        this.onShadeChangedListener = onShadeChangedListener
    }

    private inner class OnPickWallpaperClickListener : Preference.OnPreferenceClickListener {
        override fun onPreferenceClick(preference: Preference): Boolean {
            val IMAGE_MIME_TYPE = "image/*"
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = IMAGE_MIME_TYPE
            intent.addCategory(Intent.CATEGORY_OPENABLE)

            if (intent.resolveActivity(activity.packageManager) != null) {
                startActivityForResult(intent, pickWallpaperIntentId)
            } else {
                Toast.makeText(activity, "No apps installed.", Toast.LENGTH_SHORT).show()
            }
            return true
        }
    }

    private inner class OnClearWallpaperClickListener : Preference.OnPreferenceClickListener {
        override fun onPreferenceClick(preference: Preference): Boolean {
            val preferences = PreferenceManager.getDefaultSharedPreferences(activity)
            val drawDefaultBackground_key = activity.resources.getString(R.string.pref_key_draw_default_background)
            val drawDefaultBackground_default = activity.resources.getBoolean(R.bool.pref_default_draw_default_background)

            val drawDefaultBackground = preferences.getBoolean(drawDefaultBackground_key, drawDefaultBackground_default)

            if (wallpaperManager.hasInternalizedWallpaper()) {
                wallpaperManager.deleteInternalizedWallpaper()
            } else {
                preferences.edit().putBoolean(drawDefaultBackground_key, !drawDefaultBackground).apply()
            }
            if (onWallpaperChangedListener != null) {
                onWallpaperChangedListener!!.OnWallpaperChanged()
            }
            return true
        }
    }

    private inner class OnScalingChangeListener : Preference.OnPreferenceChangeListener {
        override fun onPreferenceChange(preference: Preference, scalingObj: Any): Boolean {
            //Update summery
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference, scalingObj)
            if (onScalingChangedListener != null) {
                onScalingChangedListener!!.OnScalingChanged(scalingObj.toString())
            }
            return true
        }
    }

    private inner class OnShadeIntensityChangeListener : Preference.OnPreferenceChangeListener {
        override fun onPreferenceChange(preference: Preference, shadeIntensityObj: Any): Boolean {
            //Update summery
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference, shadeIntensityObj)
            if (onShadeChangedListener != null) {
                onShadeChangedListener!!.OnDimmerChanged(shadeIntensityObj.toString())
            }
            return true
        }
    }

    companion object {

        private val sBindPreferenceSummaryToValueListener = Preference.OnPreferenceChangeListener { preference, value ->
            val stringValue = value.toString()

            if (preference is ListPreference) {
                val index = preference.findIndexOfValue(stringValue)
                preference.setSummary(
                        if (index >= 0)
                            preference.entries[index]
                        else
                            null)
            } else {
                preference.summary = stringValue
            }
            true
        }

        private fun bindPreferenceSummaryToValue(preference: Preference) {
            preference.onPreferenceChangeListener = sBindPreferenceSummaryToValueListener

            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                    PreferenceManager
                            .getDefaultSharedPreferences(preference.context)
                            .getString(preference.key, ""))
        }
    }
}
