package com.nbennettsoftware.android.npad;

import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class NpadActivity extends AppCompatActivity {

    OnActivityResultListener onActivityResultListener = null;

    interface OnActivityResultListener {
        void OnActivityResult(int requestCode, int resultCode, Intent data);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(onActivityResultListener != null) {
            onActivityResultListener.OnActivityResult(requestCode, resultCode, data);
        }
    }
}
