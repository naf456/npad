package com.nbennettsoftware.android.npad.widgets;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;

/*
 * In fullscreen modroe or with translucent system bars's, andid fails to resize the window on IME
 * input, which means the text editor doesn't shrink to the IME, the IME obscuring everything.
 * This class counteracts that. It's simple, and probably "brittle" (I might flat out not work on
 * some devices.)
 * NOTE: You'll need/still need adjustResize in the windowSoftInputMode, as that provides the call
 *  to the globalLayoutListener.
 */

public class KeyboardShrinkingFrameLayout extends FrameLayout {
    private View decorView;

    public KeyboardShrinkingFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        if(!isInEditMode()) {
            init(context);
        }
    }

    public KeyboardShrinkingFrameLayout(Context context) {
        super(context);
        if(!isInEditMode()) {
            init(context);
        }
    }

    public KeyboardShrinkingFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if(!isInEditMode()) {
            init(context);
        }
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
            Rect visibleDisplayFrame = new Rect();
            decorView.getWindowVisibleDisplayFrame(visibleDisplayFrame);
            int windowHeight = decorView.getContext().getResources().getDisplayMetrics().heightPixels;

            Point display = new Point();
            ((Activity)getContext()).getWindowManager().getDefaultDisplay().getRealSize(display);
            int displayHeight = display.y;

            int visibleHeight = visibleDisplayFrame.bottom; //Terrible api, I dislike rect!
            int heightDiff = displayHeight - visibleHeight;

            if (heightDiff > 0) {
                KeyboardShrinkingFrameLayout.this.setPadding(0, 0, 0, heightDiff);
            } else {
                KeyboardShrinkingFrameLayout.this.setPadding(0, 0, 0, 0);
            }
        }
    };

}
