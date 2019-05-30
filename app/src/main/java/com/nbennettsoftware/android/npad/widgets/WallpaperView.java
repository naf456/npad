package com.nbennettsoftware.android.npad.widgets;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.nbennettsoftware.android.npad.R;
import com.nbennettsoftware.android.npad.storage.WallpaperManager;

import java.io.File;
import java.io.IOException;

import pl.droidsonroids.gif.GifDrawable;
import pl.droidsonroids.gif.GifImageView;

public class WallpaperView extends GifImageView implements SharedPreferences.OnSharedPreferenceChangeListener {

    private WallpaperManager wallpaperManager;
    private Paint dimmerPaint;

    public WallpaperView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        if(!isInEditMode()) {
            init();
        }
    }

    public WallpaperView(Context context, AttributeSet attrs) {
        super(context, attrs);
        if(!isInEditMode()) {
            init();
        }
    }

    public WallpaperView(Context context) {
        super(context);
        if(!isInEditMode()) {
            init();
        }
    }

    private void init() {
        wallpaperManager = new WallpaperManager(getContext());

        this.dimmerPaint = new Paint();
        this.dimmerPaint.setColor(Color.BLACK);
        this.dimmerPaint.setAlpha(0);


        PreferenceManager.getDefaultSharedPreferences(getContext())
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawRect(0,0, getWidth(), getHeight(), dimmerPaint);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if(!isInEditMode()) {
            this.applyWallpaperFromPreferences();
            this.applyWallpaperDimmerFromPreferences();
            this.applyScalingFromPreferences();
            this.forceWallpaperBehindKeyboard();
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        String wallpaper_key = getResources().getString(R.string.pref_key_internal_wallpaper_name);
        String dimmerIntensity_key = getResources().getString(R.string.pref_key_dimmer_intensity);
        String scaling_key = getResources().getString(R.string.pref_key_scaling);

        if(key.equals(wallpaper_key)){
            applyWallpaperFromPreferences();
        } else if(key.equals(dimmerIntensity_key)) {
            applyWallpaperDimmerFromPreferences();
        } else if (key.equals(scaling_key)) {
            applyScalingFromPreferences();
        }
    }

    public void applyWallpaperFromPreferences() {
        try {
            File wallpaperFile = wallpaperManager.getInternalizedWallpaper();

            this.setDefaultWallpaper();

            if(wallpaperFile.getName().endsWith(".gif")) {
                GifDrawable drawable = new GifDrawable(wallpaperFile);
                this.setImageDrawable(drawable);
            }
            else {
                Drawable drawable = Drawable.createFromPath(wallpaperFile.getPath());
                setImageDrawable(drawable);
            }
        } catch (WallpaperManager.NoInternalWallpaperException e) {
            this.setDefaultWallpaper();
        }catch (IOException e) {
            this.setDefaultWallpaper();
            Toast.makeText(getContext(), "Can't copy wallpaper", Toast.LENGTH_LONG).show();
        }
    }

    public void setDefaultWallpaper(){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());

        String drawDefaultBackground_key = this.getResources().getString(R.string.pref_key_draw_default_background);
        boolean drawDefaultBackground_default = this.getResources().getBoolean(R.bool.pref_default_draw_default_background);

        boolean drawDefaultBackground = preferences.getBoolean(drawDefaultBackground_key, drawDefaultBackground_default);

        if(drawDefaultBackground) {
            int defaultWallpaperResource = R.mipmap.beautiful_background;
            this.setImageResource(defaultWallpaperResource);
        } else {
            this.setImageDrawable(null);
        }

    }

    public void applyWallpaperDimmerFromPreferences(){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        String dimmerIntensityKey = getContext().getString(R.string.pref_key_dimmer_intensity);
        String dimmerIntensityDefault = getContext().getString(R.string.pref_default_shade_intensity);
        String dimmerIntensity = preferences.getString(dimmerIntensityKey, dimmerIntensityDefault);
        applyWallpaperDimmer(dimmerIntensity);
    }

    public void applyWallpaperDimmer(String dimmingIntensity) {
        String DIMMER_OFF = getContext().getString(R.string.dimmer_off);
        String DIMMER_SUBTLE = getContext().getString(R.string.dimmer_subtle);
        String DIMMER_MODERATE = getContext().getString(R.string.dimmer_moderate);
        String DIMMER_INTENSE = getContext().getString(R.string.dimmer_intense);

        if(dimmingIntensity.equals(DIMMER_OFF)) {
            dimmerPaint.setARGB(0,0,0,0); //transparent
        } else if(dimmingIntensity.equals(DIMMER_SUBTLE)) {
            dimmerPaint.setColor(getResources().getColor(R.color.dimmerSubtle));
        } else if (dimmingIntensity.equals(DIMMER_MODERATE)) {
            dimmerPaint.setColor(getResources().getColor(R.color.dimmerModerate));
        } else if (dimmingIntensity.equals(DIMMER_INTENSE)) {
            dimmerPaint.setColor(getResources().getColor(R.color.dimmerIntense));
        }
        invalidate();
    }

    private void forceWallpaperBehindKeyboard(){
        /* Instead of Match Parent, which shrinks the wallpaper when the soft keyboard is active,
        set height to window height. */
        Point screenSize = new Point();

        ((Activity)getContext()).getWindowManager().getDefaultDisplay().getRealSize(screenSize);

        ViewGroup.LayoutParams params = getLayoutParams();
        params.height = screenSize.y;
        setLayoutParams(params);
    }



    public void applyScalingFromPreferences(){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        String scalingKey = getContext().getString(R.string.pref_key_scaling);
        String scalingDefault = getContext().getString(R.string.pref_default_scaling);
        String scaling = preferences.getString(scalingKey, scalingDefault);
        applyScaling(scaling);
    }

    public void applyScaling(String scaling){
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
}
