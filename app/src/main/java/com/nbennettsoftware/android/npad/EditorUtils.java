package com.nbennettsoftware.android.npad;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Intent;
import android.net.Uri;

class EditorUtils {

    static void startDocumentPickerForResult(Activity activity, int intentId) {
        final String TEXT_MIME_TYPE="text/*";
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType(TEXT_MIME_TYPE);
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        Intent chooser = Intent.createChooser(intent, "Select Document");

        if (chooser.resolveActivity(activity.getPackageManager()) != null) {
            ActivityOptions options = ActivityOptions.makeCustomAnimation(
                    activity, android.R.anim.fade_in, android.R.anim.fade_out);
            activity.startActivityForResult(chooser, intentId, options.toBundle());
        } else {
            Utils.toast(activity, "No apps installed.");
        }
    }

    static void startSaveFilePickerForResult(Activity activity, int intentId) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.setType("text/*");
        intent.putExtra(Intent.EXTRA_TITLE, "document_name.txt");

        Intent chooser = Intent.createChooser(intent, "Select Document");

        if (chooser.resolveActivity(activity.getPackageManager()) != null) {
            ActivityOptions options = ActivityOptions.makeCustomAnimation(
                    activity, android.R.anim.fade_in, android.R.anim.fade_out);
            activity.startActivityForResult(chooser, intentId, options.toBundle());
        } else {
            Utils.toast(activity, "No apps installed.");
        }
    }
}
