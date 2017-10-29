package com.nbennettsoftware.android.npad.widget;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.nbennettsoftware.android.npad.R;
import com.nbennettsoftware.android.npad.storage.WallpaperManager;

import java.io.File;
import java.io.IOException;

import pl.droidsonroids.gif.GifDrawable;
import pl.droidsonroids.gif.GifImageView;

public class WallpaperImageView extends GifImageView {
    private WallpaperManager wallpaperManager;

    public WallpaperImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();    }

    public WallpaperImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();    }

    public WallpaperImageView(Context context) {
        super(context);
        init();
    }

    private void init(){
        this.wallpaperManager = new WallpaperManager(getContext());
    }

    private class ThisOnSharedPreferenceChangeListener
            implements SharedPreferences.OnSharedPreferenceChangeListener  {

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            Resources resources = getResources();
            String scalingKey = resources.getString(R.string.pref_key_scaling);
            String belowKeyboardKey = resources.getString(R.string.pref_key_behind_keyboard);
            if(key == scalingKey) {
                setScalingFromPreferences();
            } else if (key == belowKeyboardKey) {
                setBelowKeyboard();
            }
        }
    }

    public void setImageToWallpaper() {
        int defaultWallpaperResource = R.mipmap.stary_night;
        try {
            File wallpaperFile = wallpaperManager.getInternalizedWallpaper();
            setImageResource(defaultWallpaperResource);
            if(wallpaperFile.getName().endsWith(".gif")) {
                GifDrawable drawable = new GifDrawable(wallpaperFile);
                setImageDrawable(drawable);
            } else {
                Drawable drawable = Drawable.createFromPath(wallpaperFile.getPath());
                setImageDrawable(drawable);
            }
        } catch (WallpaperManager.NoInternalWallpaperException e) {
            setImageResource(defaultWallpaperResource);
        }catch (IOException e) {
            setImageResource(defaultWallpaperResource);
            Toast.makeText(getContext(), "Can't copy wallpaper", Toast.LENGTH_LONG).show();
        }
    }

    public void setBelowKeyboard(){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        String behindKeyboardKey = getContext().getString(R.string.pref_key_behind_keyboard);
        String behindKeyboardDefaultString = getContext().getString(R.string.pref_default_behind_keyboard);
        Boolean behindKeyboardDefault = Boolean.parseBoolean(behindKeyboardDefaultString);
        Boolean behindKeyboard = preferences.getBoolean(behindKeyboardKey, behindKeyboardDefault);
        if(behindKeyboard) {
            Point screenSize = new Point();
            ((Activity)getContext()).getWindowManager().getDefaultDisplay().getRealSize(screenSize);
            ViewGroup.LayoutParams params = getLayoutParams();
            params.height = screenSize.y;
            setLayoutParams(params);
        } else {
            ViewGroup.LayoutParams params = getLayoutParams();
            params.height = ViewGroup.LayoutParams.MATCH_PARENT;
            setLayoutParams(params);
        }
    }

    public void setScalingFromPreferences(){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        String scalingKey = getContext().getString(R.string.pref_key_scaling);
        String scalingDefault = getContext().getString(R.string.pref_default_scaling);
        String scaling = preferences.getString(scalingKey, scalingDefault);
        setScalingFromPreference(scaling);
    }

    public void setScalingFromPreference(String scaling){
        String TO_WIDTH = getContext().getString(R.string.to_width);
        String TO_HEIGHT = getContext().getString(R.string.to_height);
        String STRETCH = getContext().getString(R.string.stretch);

        if(scaling.equals(TO_WIDTH)) {
            setScaleType(ImageView.ScaleType.FIT_CENTER);
        } else if(scaling.equals(TO_HEIGHT)) {
            setScaleType(ImageView.ScaleType.CENTER_CROP);
        } else if (scaling.equals(STRETCH)) {
            setScaleType(ImageView.ScaleType.FIT_XY);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        setImageToWallpaper();
        setScalingFromPreferences();
        setBelowKeyboard();
    }
}
