package com.nbennettsoftware.android.npad;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.nbennettsoftware.android.npad.storage.WallpaperManager;
import com.nbennettsoftware.android.npad.widget.WallpaperImageView;

import java.io.File;
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

    void toast(String msg) {
        toast(msg, Toast.LENGTH_SHORT);
    }

    void toast(String msg, int length) {
        Toast toast = Toast.makeText(activity, msg, length);
        toast.setGravity(Gravity.TOP | Gravity.CENTER, 0,0);
        toast.show();
    }

    static public void toast(Context context, String msg) {
        toast(context, msg, Toast.LENGTH_SHORT);
    }

    static public void toast(Context context, String msg, int length) {
        Toast toast = Toast.makeText(context, msg, length);
        toast.setGravity(Gravity.TOP | Gravity.CENTER, 0,0);
        toast.show();
    }
}
