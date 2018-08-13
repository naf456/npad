package com.nbennettsoftware.android.npad;


import android.os.Bundle;

import com.nbennettsoftware.android.npad.widgets.WallpaperView;

public class SettingsActivity extends NpadActivity
        implements SettingsFragment.OnWallpaperChangedListener,
                    SettingsFragment.OnShadeChangedListener,
                    SettingsFragment.OnScalingChangedListener{

    private WallpaperView wallpaperView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);



        SettingsFragment settingsFragment = new SettingsFragment();
        settingsFragment.setOnWallpaperChangedListener(this);
        settingsFragment.setOnShadeChangedListener(this);
        settingsFragment.setOnScalingChangedListener(this);

        getFragmentManager().beginTransaction()
                .replace(R.id.settings_fragment_placeholder, settingsFragment)
                .commit();


        wallpaperView = findViewById(R.id.settings_wallpaperImageView);

        refreshUi();
    }

    @Override
    public void OnWallpaperChanged() {
        refreshUi();
    }

    @Override
    public void OnDimmerChanged(String dimmerIntensity) {
        wallpaperView.applyWallpaperDimmer(dimmerIntensity);
    }

    @Override
    public void OnScalingChanged(String scaling) {
        wallpaperView.applyScaling(scaling);
    }

    void refreshUi(){
        wallpaperView.applyWallpaperFromPreferences();
        wallpaperView.applyWallpaperDimmerFromPreferences();
        wallpaperView.applyScalingFromPreferences();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshUi();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

}
