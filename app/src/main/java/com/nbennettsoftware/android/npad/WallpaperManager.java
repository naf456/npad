package com.nbennettsoftware.android.npad;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by natha on 02/06/2017.
 */

public class WallpaperManager {

    private Context context;
    private String sharedPreferencesId = this.getClass().getName();
    private String wallpaperPreferenceId = "wallpaper";

    public String getWallpaperFilePath(){
        SharedPreferences preferences = context.getSharedPreferences(sharedPreferencesId, Context.MODE_PRIVATE);
        return preferences.getString(wallpaperPreferenceId, "");
    }

    public void setWallpaperPath(String path){
        SharedPreferences preferences = context.getSharedPreferences(sharedPreferencesId, Context.MODE_PRIVATE);
        preferences.edit().putString(wallpaperPreferenceId, path).apply();
    }

    public WallpaperManager(Context context) {
        this.context = context;
    }

    private static void setWallpaper() {

    }
}
