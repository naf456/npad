package com.nbennettsoftware.android.npad;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;


class Utils {

    private Activity activity;
    private StorageManager storageManager;
    private int defaultWallpaperResource = R.mipmap.stary_night;

    Utils(Activity _activity) {
        activity = _activity;
        storageManager = new StorageManager(activity);
    }

    void saveWallpaper(Uri wallpaperUri) {
        if (wallpaperUri == null) { toast("Can't set wallpaper."); return; }
        try{
            new StorageManager(activity).replaceInternalizeWallpaper(wallpaperUri);
        } catch (StorageManager.ReplaceInternalWallpaperException e){
            e.printStackTrace();
            toast("Can't save wallpaper.", Toast.LENGTH_LONG);
        }
    }

    void applyWallpaper(Uri wallpaperUri, ImageView imageView) {
        if (wallpaperUri == null) { toast("Can't set wallpaper."); return; }
        try{
            new StorageManager(activity).replaceInternalizeWallpaper(wallpaperUri);
            applyWallpaper(imageView);
        }

        catch (StorageManager.ReplaceInternalWallpaperException e){
            e.printStackTrace();
            toast("Can't set wallpaper.", Toast.LENGTH_LONG);
        }
    }

    void applyWallpaper(ImageView wallpaperImageView) {
        try {
            File wallpaperFile = storageManager.getInternalizedWallpaper();
            wallpaperImageView.setImageResource(defaultWallpaperResource);
            FileInputStream wallpaperStream = new FileInputStream(wallpaperFile);
            Bitmap wallpaperBitmap = BitmapFactory.decodeStream(wallpaperStream);
            wallpaperStream.close();
            wallpaperImageView.setImageBitmap(wallpaperBitmap);
        } catch (StorageManager.NoInternalWallpaperException e) {
            wallpaperImageView.setImageResource(defaultWallpaperResource);
        } catch (IOException e) {
            wallpaperImageView.setImageResource(defaultWallpaperResource);
            toast("Can't copy wallpaper", Toast.LENGTH_LONG);
        }
    }

    void clearWallpaper(){
        storageManager.deleteInternalizedWallpaper();
    }

    void applyShade(View shade) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);
        String shadeIntensityKey = activity.getString(R.string.pref_key_shade_intensity);
        String shadeIntensityDefault = activity.getString(R.string.pref_default_shade_intensity);
        String shadeIntensity = preferences.getString(shadeIntensityKey, shadeIntensityDefault);
        applyShade(shade, shadeIntensity);
    }

    void applyShade(View shade, String shadeIntensity) {
        String SHADE_SUBTLE = activity.getString(R.string.shade_subtle);
        String SHADE_MODERATE = activity.getString(R.string.shade_moderate);
        String SHADE_INTENSE = activity.getString(R.string.shade_intense);

        if(shadeIntensity.equals(SHADE_SUBTLE)) {
            shade.setBackgroundResource(R.color.subtleShade);
        } else if (shadeIntensity.equals(SHADE_MODERATE)) {
            shade.setBackgroundResource(R.color.moderateShade);
        } else if (shadeIntensity.equals(SHADE_INTENSE)) {
            shade.setBackgroundResource(R.color.intenseShade);
        }
    }

    void applyFontSize(TextView textView) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);
        String fontSizeKey = activity.getString(R.string.pref_key_font_size);
        String fontSizeDefault = activity.getString(R.string.pref_default_font_size);
        String fontSize = preferences.getString(fontSizeKey, fontSizeDefault);
        try {
            Integer fontSizeInt = Integer.parseInt(fontSize);
            textView.setTextSize(fontSizeInt);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
    }

    void toast(String msg) {
        toast(msg, Toast.LENGTH_SHORT);
    }

    void toast(String msg, int length) {
        Toast toast = Toast.makeText(activity, msg, length);
        toast.setGravity(Gravity.TOP | Gravity.CENTER, 0,0);
        toast.show();
    }

    static void toast(Context context, String msg) {
        toast(context, msg, Toast.LENGTH_SHORT);
    }

    static void toast(Context context, String msg, int length) {
        Toast toast = Toast.makeText(context, msg, length);
        toast.setGravity(Gravity.TOP | Gravity.CENTER, 0,0);
        toast.show();
    }
}
