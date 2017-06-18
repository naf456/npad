package com.nbennettsoftware.android.npad;

import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class NpadActivity extends AppCompatActivity {

    List<OnActivityResultListener> onActivityResultListeners = new ArrayList<>();

    interface OnActivityResultListener {
        void OnActivityResult(int requestCode, int resultCode, Intent data);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        for(OnActivityResultListener onActivityResultListener : onActivityResultListeners) {
            onActivityResultListener.OnActivityResult(requestCode, resultCode, data);
        }

    }

    void addActivityResultListener(OnActivityResultListener onActivityResultListener) {
        onActivityResultListeners.add(onActivityResultListener);
    }

    void removeActivityResultListener(OnActivityResultListener onActivityResultListener) {
        onActivityResultListeners.remove(onActivityResultListener);
    }

}
