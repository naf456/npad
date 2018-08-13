package com.nbennettsoftware.android.npad;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class NpadActivity extends AppCompatActivity {

    protected final int DEFAULT_SYSTEM_UI_VISIBILITY = View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;

    List<OnActivityResultListener> onActivityResultListeners = new ArrayList<>();

    interface OnActivityResultListener {
        void OnActivityResult(int requestCode, int resultCode, Intent data);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().getDecorView().setSystemUiVisibility(DEFAULT_SYSTEM_UI_VISIBILITY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        for(OnActivityResultListener onActivityResultListener : onActivityResultListeners) {
            onActivityResultListener.OnActivityResult(requestCode, resultCode, data);
        }

    }

    void addOnActivityResultListener(OnActivityResultListener onActivityResultListener) {
        onActivityResultListeners.add(onActivityResultListener);
    }

    void removeOnActivityResultListener(OnActivityResultListener onActivityResultListener) {
        onActivityResultListeners.remove(onActivityResultListener);
    }

}
