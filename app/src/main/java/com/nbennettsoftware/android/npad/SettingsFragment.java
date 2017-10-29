package com.nbennettsoftware.android.npad;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.nbennettsoftware.android.npad.storage.WallpaperManager;

public class SettingsFragment extends PreferenceFragment {

    private Utils utils;
    private WallpaperManager wallpaperManager;
    private int PICK_WALLPAPER_INTENT_ID = 0;
    public OnWallpaperChangedListener onWallpaperChangedListener = null;
    public OnScalingChangedListener onScalingChangedListener = null;
    public OnShadeChangedListener onShadeChangedListener = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        utils = new Utils(this.getActivity());
        wallpaperManager = new WallpaperManager(getActivity());

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_key_font_size)));
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_key_shade_intensity)));
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_key_scaling)));

        //We let bindPrefernceSummaryToValue set are summary for us.
        findPreference(getString(R.string.pref_key_scaling)).setOnPreferenceChangeListener(new OnScalingChangeListener());
        findPreference(getString(R.string.pref_key_shade_intensity)).setOnPreferenceChangeListener(new OnShadeIntensityChangeListener());

        findPreference(getString(R.string.pref_key_pick_wallpaper)).setOnPreferenceClickListener(new OnPickWallpaperClickListener());
        findPreference(getString(R.string.pref_key_clear_wallpaper)).setOnPreferenceClickListener(new OnClearWallpaperClickListener());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == PICK_WALLPAPER_INTENT_ID && resultCode == Activity.RESULT_OK) {
            Uri uri = data.getData();
            if(uri==null) { return; }
            try{
                wallpaperManager.replaceInternalizeWallpaper(uri);
            } catch (WallpaperManager.ReplaceInternalWallpaperException e) {
                e.printStackTrace();
                Toast.makeText(getActivity(), "Cannot replace internal wallpaper", Toast.LENGTH_LONG).show();
            }
            if(onWallpaperChangedListener != null) {
                onWallpaperChangedListener.OnWallpaperChanged();
            }
        }
    }

    interface OnWallpaperChangedListener {
        void OnWallpaperChanged();
    }

    interface OnScalingChangedListener {
        void OnScalingChanged(String scaling);
    }

    interface OnShadeChangedListener {
        void OnShadeChanged(String shadeIntensity);
    }

    void setOnWallpaperChangedListener(OnWallpaperChangedListener onWallpaperChangedListener) {
        this.onWallpaperChangedListener = onWallpaperChangedListener;
    }

    void setOnScalingChangedListener(OnScalingChangedListener onScalingChangedListener) {
        this.onScalingChangedListener = onScalingChangedListener;
    }

    void setOnShadeChangedListener(OnShadeChangedListener onShadeChangedListener) {
        this.onShadeChangedListener = onShadeChangedListener;
    }

    private class OnPickWallpaperClickListener implements Preference.OnPreferenceClickListener {
        @Override
        public boolean onPreferenceClick(Preference preference) {
            final String IMAGE_MIME_TYPE="image/*";
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType(IMAGE_MIME_TYPE);
            intent.addCategory(Intent.CATEGORY_OPENABLE);

            if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
                startActivityForResult(intent, PICK_WALLPAPER_INTENT_ID);
            } else {
                utils.toast("No apps installed.");
            }
            return true;
        }
    }

    private class OnClearWallpaperClickListener implements Preference.OnPreferenceClickListener {
        @Override
        public boolean onPreferenceClick(Preference preference) {
            new WallpaperManager(getActivity()).deleteInternalizedWallpaper();
            if(onWallpaperChangedListener != null) {
                onWallpaperChangedListener.OnWallpaperChanged();
            }
            return true;
        }
    }

    private class OnScalingChangeListener implements Preference.OnPreferenceChangeListener {
        @Override
        public boolean onPreferenceChange(Preference preference, Object scalingObj) {
            //Update summery
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference, scalingObj);
            if(onScalingChangedListener != null) {
                onScalingChangedListener.OnScalingChanged(scalingObj.toString());
            }
            return true;
        }
    }

    private class OnShadeIntensityChangeListener implements Preference.OnPreferenceChangeListener {
        @Override
        public boolean onPreferenceChange(Preference preference, Object shadeIntensityObj) {
            //Update summery
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference, shadeIntensityObj);
            if(onShadeChangedListener != null) {
                onShadeChangedListener.OnShadeChanged(shadeIntensityObj.toString());
            }
            return true;
        }
    }

    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if (preference instanceof ListPreference) {
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);
            } else {
                preference.setSummary(stringValue);
            }
            return true;
        }
    };

    private static void bindPreferenceSummaryToValue(Preference preference) {
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }
}
