package com.nbennettsoftware.android.npad;

import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.View;
import android.widget.Toast;

public class PhotoMode {

    private AppCompatActivity activity;
    private View touchOverlay;

    PhotoMode(AppCompatActivity activity){
        this.activity = activity;
    }

    void goIntoPhotoMode(AppCompatActivity activity) {
        View decorView = activity.getWindow().getDecorView();

        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

        ActionBar actionBar = activity.getSupportActionBar();
        if(actionBar != null) {
            actionBar.hide();
        }

        //editor.hideSoftInput();
        //editor.clearFocus();
        //editor.setFocusable(false);

        touchOverlay.setVisibility(View.VISIBLE);
        touchOverlay.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                exitPhotoMode();
                return true;
            }
        });

        Toast toast = Toast.makeText(activity, R.string.toast_msg_photo_mode_instruction, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.BOTTOM | Gravity.CENTER, 0, 0);
        toast.show();
    }

    void exitPhotoMode() {
        touchOverlay.setOnClickListener(null);
        touchOverlay.setVisibility(View.GONE);

        //editor.setFocusable(true);
        //editor.setFocusableInTouchMode(true);
        //editor.showSoftInput();

        ActionBar actionBar = activity.getSupportActionBar();
        if(actionBar != null) {
            actionBar.show();
        }

        View decorView = activity.getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
    }
}
