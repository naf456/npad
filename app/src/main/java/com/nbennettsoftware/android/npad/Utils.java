package com.nbennettsoftware.android.npad;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.storage.StorageManager;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.nbennettsoftware.android.npad.storage.WallpaperManager;
import com.nbennettsoftware.android.npad.widget.FullScreenImageView;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import pl.droidsonroids.gif.GifDrawable;


class Utils {

    private Activity activity;
    private WallpaperManager wallpaperManager;
    private int defaultWallpaperResource = R.mipmap.stary_night;

    Utils(Activity _activity) {
        activity = _activity;
        wallpaperManager = new WallpaperManager(activity);
    }

    void saveWallpaper(Uri wallpaperUri) {
        if (wallpaperUri == null) { toast("Can't set wallpaper."); return; }
        try{
            new WallpaperManager(activity).replaceInternalizeWallpaper(wallpaperUri);
        } catch (WallpaperManager.ReplaceInternalWallpaperException e){
            e.printStackTrace();
            toast("Can't save wallpaper.", Toast.LENGTH_LONG);
        }
    }

    void applyWallpaper(Uri wallpaperUri, FullScreenImageView imageView) {
        if (wallpaperUri == null) { toast("Can't set wallpaper."); return; }
        try{
            new WallpaperManager(activity).replaceInternalizeWallpaper(wallpaperUri);
            applyWallpaper(imageView);
        }

        catch (WallpaperManager.ReplaceInternalWallpaperException e){
            e.printStackTrace();
            toast("Can't set wallpaper.", Toast.LENGTH_LONG);
        }
    }

    void applyWallpaper(FullScreenImageView wallpaperImageView) {
        try {
            File wallpaperFile = wallpaperManager.getInternalizedWallpaper();
            wallpaperImageView.setImageResource(defaultWallpaperResource);
            if(wallpaperFile.getName().endsWith(".gif")) {
                GifDrawable drawable = new GifDrawable(wallpaperFile);
                wallpaperImageView.setImageDrawable(drawable);
            } else {
                Drawable drawable = Drawable.createFromPath(wallpaperFile.getPath());
                wallpaperImageView.setImageDrawable(drawable);
            }
        } catch (WallpaperManager.NoInternalWallpaperException e) {
            wallpaperImageView.setImageResource(defaultWallpaperResource);
        } catch (IOException e) {
            wallpaperImageView.setImageResource(defaultWallpaperResource);
            toast("Can't copy wallpaper", Toast.LENGTH_LONG);
        }
    }

    void clearWallpaper(){
        wallpaperManager.deleteInternalizedWallpaper();
    }

    void applyShade(View shade) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);
        String shadeIntensityKey = activity.getString(R.string.pref_key_shade_intensity);
        String shadeIntensityDefault = activity.getString(R.string.pref_default_shade_intensity);
        String shadeIntensity = preferences.getString(shadeIntensityKey, shadeIntensityDefault);
        applyShade(shade, shadeIntensity);
    }

    void applyShade(View shade, String shadeIntensity) {
        String SHADE_OFF = activity.getString(R.string.shade_off);
        String SHADE_SUBTLE = activity.getString(R.string.shade_subtle);
        String SHADE_MODERATE = activity.getString(R.string.shade_moderate);
        String SHADE_INTENSE = activity.getString(R.string.shade_intense);

        if(shadeIntensity.equals(SHADE_OFF)) {
            shade.setBackgroundResource(android.R.color.transparent);
        } else if(shadeIntensity.equals(SHADE_SUBTLE)) {
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

    void updateWallpaperLayout(ImageView wallpaperImageView){
        applyBehindKeyboard(wallpaperImageView);
        applyWallpaperScaling(wallpaperImageView);
    }

    void applyBehindKeyboard(ImageView wallpaperImageView){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);
        String behindKeyboardKey = activity.getString(R.string.pref_key_behind_keyboard);
        String behindKeyboardDefaultString = activity.getString(R.string.pref_default_behind_keyboard);
        Boolean behindKeyboardDefault = Boolean.parseBoolean(behindKeyboardDefaultString);
        Boolean behindKeyboard = preferences.getBoolean(behindKeyboardKey, behindKeyboardDefault);
        if(behindKeyboard == true) {
            DisplayMetrics dm = new DisplayMetrics();
            activity.getWindowManager().getDefaultDisplay().getMetrics(dm);
            ViewGroup.LayoutParams params = wallpaperImageView.getLayoutParams();
            params.height = dm.heightPixels;
            wallpaperImageView.setLayoutParams(params);
        } else {
            ViewGroup.LayoutParams params = wallpaperImageView.getLayoutParams();
            params.height = ViewGroup.LayoutParams.MATCH_PARENT;
            wallpaperImageView.setLayoutParams(params);
        }
    }

    void applyWallpaperScaling(ImageView wallpaperImageView){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);
        String scalingKey = activity.getString(R.string.pref_key_scaling);
        String scalingDefault = activity.getString(R.string.pref_default_scaling);
        String scaling = preferences.getString(scalingKey, scalingDefault);
        applyWallpaperScaling(wallpaperImageView, scaling);
    }

    void applyWallpaperScaling(ImageView wallpaperImageView, String scaling){
        String TO_WIDTH = activity.getString(R.string.to_width);
        String TO_HEIGHT = activity.getString(R.string.to_height);
        String STRETCH = activity.getString(R.string.stretch);

        if(scaling.equals(TO_WIDTH)) {
            wallpaperImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        } else if(scaling.equals(TO_HEIGHT)) {
            wallpaperImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        } else if (scaling.equals(STRETCH)) {
            wallpaperImageView.setScaleType(ImageView.ScaleType.FIT_XY);
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
