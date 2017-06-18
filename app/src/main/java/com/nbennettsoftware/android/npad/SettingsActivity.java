package com.nbennettsoftware.android.npad;


import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.ImageView;

public class SettingsActivity extends AppCompatActivity
        implements SettingsFragment.OnWallpaperChangedListener, SettingsFragment.OnShadeChangedListener{

    private Utils utils;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        SettingsFragment settingsFragment = new SettingsFragment();
        settingsFragment.setOnWallpaperChangedListener(this);
        settingsFragment.setOnShadeChangedListener(this);

        getFragmentManager().beginTransaction()
                .replace(R.id.settings_fragment_placeholder, settingsFragment)
                .commit();

        utils = new Utils(this);

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

    void refreshUi(){
        utils.applyWallpaper((ImageView) findViewById(R.id.settings_wallpaperImageView));
        utils.applyShade(findViewById(R.id.settings_overlay));
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}
