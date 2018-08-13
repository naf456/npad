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

public class WallpaperView extends GifImageView {

    private WallpaperManager mWallpaperManager;
    private Paint mDimmerPaint;

    public WallpaperView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();    }

    public WallpaperView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();    }

    public WallpaperView(Context context) {
        super(context);
        init();
    }

    private void init() {
        mWallpaperManager = new WallpaperManager(getContext());

        mDimmerPaint = new Paint();
        mDimmerPaint.setColor(Color.BLACK);
        mDimmerPaint.setAlpha(0);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawRect(0,0, canvas.getWidth(), canvas.getHeight(), mDimmerPaint);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        applyWallpaperFromPreferences();
        applyScalingFromPreferences();
        forceWallpaperBelowKeyboard();
    }

    public void applyWallpaperFromPreferences() {
        int defaultWallpaperResource = R.mipmap.abstract_background;

        try {
            File wallpaperFile = mWallpaperManager.getInternalizedWallpaper();

            setImageResource(defaultWallpaperResource);

            if(wallpaperFile.getName().endsWith(".gif")) {
                GifDrawable drawable = new GifDrawable(wallpaperFile);
                setImageDrawable(drawable);
            }
            else {
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
            mDimmerPaint.setARGB(0,0,0,0); //transparent
        } else if(dimmingIntensity.equals(DIMMER_SUBTLE)) {
            mDimmerPaint.setColor(getResources().getColor(R.color.dimmerSubtle));
        } else if (dimmingIntensity.equals(DIMMER_MODERATE)) {
            mDimmerPaint.setColor(getResources().getColor(R.color.dimmerModerate));
        } else if (dimmingIntensity.equals(DIMMER_INTENSE)) {
            mDimmerPaint.setColor(getResources().getColor(R.color.dimmerIntense));
        }
        invalidate();
    }

    private void forceWallpaperBelowKeyboard(){
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
