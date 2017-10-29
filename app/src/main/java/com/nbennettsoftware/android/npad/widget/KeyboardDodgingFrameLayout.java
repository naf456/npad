package com.nbennettsoftware.android.npad.widget;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;

public class KeyboardDodgingFrameLayout extends FrameLayout {
    private View decorView;

    public KeyboardDodgingFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public KeyboardDodgingFrameLayout(Context context) {
        super(context);
        init(context);
    }

    public KeyboardDodgingFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context){
        this.decorView = ((Activity)context).getWindow().getDecorView();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            decorView.getViewTreeObserver().addOnGlobalLayoutListener(onGlobalLayoutListener);
        }
    }

    ViewTreeObserver.OnGlobalLayoutListener onGlobalLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
            Rect displayFrameRect = new Rect();
            decorView.getWindowVisibleDisplayFrame(displayFrameRect);

            int height = decorView.getContext().getResources().getDisplayMetrics().heightPixels;
            int heightDiff = height - displayFrameRect.bottom;

            if (heightDiff != 0) {
                if (getPaddingBottom() != heightDiff) {
                    setPadding(0, 0, 0, heightDiff);
                }
            } else {
                if (getPaddingBottom() != 0) {
                    setPadding(0, 0, 0, 0);
                }
            }
        }
    };

}
