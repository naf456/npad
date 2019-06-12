package com.nbennettsoftware.android.npad

import android.support.v7.app.ActionBar
import android.support.v7.app.AppCompatActivity
import android.view.Gravity
import android.view.View
import android.widget.Toast

class PhotoMode internal constructor(private val activity: AppCompatActivity) {
    private val touchOverlay: View? = null

    internal fun goIntoPhotoMode(activity: AppCompatActivity) {
        val decorView = activity.window.decorView

        decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)

        val actionBar = activity.supportActionBar
        actionBar?.hide()

        //editor.hideSoftInput();
        //editor.clearFocus();
        //editor.setFocusable(false);

        touchOverlay!!.visibility = View.VISIBLE
        touchOverlay.setOnLongClickListener {
            exitPhotoMode()
            true
        }

        val toast = Toast.makeText(activity, R.string.toast_msg_photo_mode_instruction, Toast.LENGTH_SHORT)
        toast.setGravity(Gravity.BOTTOM or Gravity.CENTER, 0, 0)
        toast.show()
    }

    internal fun exitPhotoMode() {
        touchOverlay!!.setOnClickListener(null)
        touchOverlay.visibility = View.GONE

        //editor.setFocusable(true);
        //editor.setFocusableInTouchMode(true);
        //editor.showSoftInput();

        val actionBar = activity.supportActionBar
        actionBar?.show()

        val decorView = activity.window.decorView
        decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
    }
}
