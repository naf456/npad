package com.naf.npad.fragments

import android.app.Activity
import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.naf.npad.R

import com.naf.npad.repository.WallpaperManager
import com.naf.npad.util.SafeGetContentActivityResultContract

class SettingsFragment : PreferenceFragmentCompat() {

    private lateinit var wallpaperManager: WallpaperManager

    private val getContent = registerForActivityResult(SafeGetContentActivityResultContract()) {
        it?.let {
            wallpaperManager.replaceInternalizeWallpaper(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        wallpaperManager = WallpaperManager(activity as Activity)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_key_scaling))!!)

        //We let bindPreferenceSummaryToValue set are summary for us.
        findPreference<Preference>(getString(R.string.pref_key_scaling))?.onPreferenceChangeListener = OnScalingChangeListener()
    }

    private inner class OnPickWallpaperClickListener : Preference.OnPreferenceClickListener {
        override fun onPreferenceClick(preference: Preference): Boolean {
            val imageMimeType = "image/*"
            getContent.launch(imageMimeType)
            return true
        }
    }

    private inner class OnClearWallpaperClickListener : Preference.OnPreferenceClickListener {
        override fun onPreferenceClick(preference: Preference): Boolean {
            if (wallpaperManager.hasInternalizedWallpaper()) {
                wallpaperManager.deleteInternalizedWallpaper()
            }
            return true
        }
    }

    private inner class OnScalingChangeListener : Preference.OnPreferenceChangeListener {
        override fun onPreferenceChange(preference: Preference, scalingObj: Any): Boolean {
            //Update summery
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference, scalingObj)
            return true
        }
    }

    private inner class OnShadeIntensityChangeListener : Preference.OnPreferenceChangeListener {
        override fun onPreferenceChange(preference: Preference, shadeIntensityObj: Any): Boolean {
            //Update summery
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference, shadeIntensityObj)
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

            if(preference.key == preference.context.getString(R.string.pref_key_dimmer_intensity))
                return

            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.context)
                        .getString(preference.key, ""))
        }
    }
}
