package com.nbennettsoftware.android.npad;


import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;

import com.nbennettsoftware.android.npad.widget.WallpaperImageView;

public class SettingsActivity extends AppCompatActivity
        implements SettingsFragment.OnWallpaperChangedListener,
                    SettingsFragment.OnShadeChangedListener,
                    SettingsFragment.OnScalingChangedListener{

    private Utils utils;
    private WallpaperImageView wallpaperImageView;

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

        utils = new Utils(this);
        wallpaperImageView = (WallpaperImageView)findViewById(R.id.settings_wallpaperImageView);

        refreshUi();
    }

    @Override
    public void OnWallpaperChanged() {
        refreshUi();
    }

    @Override
    public void OnShadeChanged(String shadeIntensity) {
        utils.applyShade(findViewById(R.id.settings_overlay), shadeIntensity);
    }

    @Override
    public void OnScalingChanged(String scaling) {
        wallpaperImageView.setScalingFromPreference(scaling);
    }

    void refreshUi(){
        wallpaperImageView.setImageToWallpaper();
        wallpaperImageView.setScalingFromPreferences();
        wallpaperImageView.setBelowKeyboard();
        utils.applyShade(findViewById(R.id.settings_overlay));

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
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
