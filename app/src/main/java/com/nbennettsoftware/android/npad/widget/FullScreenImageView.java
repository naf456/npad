package com.nbennettsoftware.android.npad.widget;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.ViewGroup;
import android.widget.ImageView;

import pl.droidsonroids.gif.GifImageView;

public class FullScreenImageView extends GifImageView {

    public FullScreenImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public FullScreenImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FullScreenImageView(Context context) {
        super(context);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
    }
}
