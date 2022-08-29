package com.naf.npad

import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Toast

@Suppress("DEPRECATION")
class PhotoMode (private val activity: AppCompatActivity, private val controlView: View) {

    internal fun enter() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity.window.setDecorFitsSystemWindows(false)
        } else {
            activity.window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_IMMERSIVE
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
        }

        activity.supportActionBar?.hide()

        //editor.hideSoftInput();
        //editor.clearFocus();
        //editor.setFocusable(false);
        val toast = Toast.makeText(activity, R.string.toast_msg_photo_mode_instruction, Toast.LENGTH_SHORT)
        toast.setGravity(Gravity.BOTTOM or Gravity.CENTER, 0, 0)
        toast.show()
    }

    private fun createExitView() {
        val rootView = activity.window.findViewById<ViewGroup>(android.R.id.content)
        val exitView = View(activity.applicationContext)
        exitView.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT)
        exitView.setOnLongClickListener(object : View.OnLongClickListener {
            override fun onLongClick(v: View?): Boolean {
                rootView.removeView(exitView)
                exit()
                return true
            }

        })
        rootView.addView(exitView)
    }

    private fun exit() {
        val actionBar = activity.supportActionBar
        actionBar?.show()

        val decorView = activity.window.decorView
        decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
    }
}
