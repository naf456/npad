package com.nbennettsoftware.android.npad

import androidx.appcompat.app.AppCompatActivity
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast

const val HIDE_SYSTEM_WINDOWS_FLAGS = (View.SYSTEM_UI_FLAG_FULLSCREEN
        or View.SYSTEM_UI_FLAG_IMMERSIVE
        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)

const val EXIT_TOUCH_FRAME_TAG = "photoMode_exitTouchFrame"

class PhotoMode (private val activity: AppCompatActivity,
                 private val rootView: ViewGroup,
                 private val contentView : ViewGroup,
                 private val controlsView: View,
                 private val hideSystemWindows : Boolean) {

    private val view = rootView//A random view reference to access context etc...
    private var touchFrame: FrameLayout? = null
    private var systemUiFlagsBackup = view.systemUiVisibility
    private var systemWindowsHidden = false

    fun enterPhotoMode() {
        hideControls()
        hideKeyboard()
        defocusEditText()
        setContentViewUnfocusable()
        if(hideSystemWindows) doHideSystemWindows()
        applyPhotoModeExitOverlay()
    }

    private fun hideKeyboard(){
        val imm = activity.getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun hideControls() {
        controlsView.visibility = View.INVISIBLE
    }

    private fun showControls() {
        controlsView.visibility = View.VISIBLE
    }

    private fun defocusEditText() {
        val focusedView = view.findFocus()
        if(focusedView is EditText) {
            focusedView.clearFocus()
        }
    }

    private fun doHideSystemWindows(){
        systemUiFlagsBackup = view.systemUiVisibility
        view.systemUiVisibility = HIDE_SYSTEM_WINDOWS_FLAGS
        systemWindowsHidden = true
    }

    private fun unhideSystemWindows(){
        view.systemUiVisibility = systemUiFlagsBackup
    }

    private fun setContentViewUnfocusable(){
        contentView.isFocusable = false
        contentView.isFocusableInTouchMode = false
    }

    private fun setContentViewFocusable() {
        contentView.isFocusable = true
        contentView.isFocusableInTouchMode = false
    }

    private fun applyPhotoModeExitOverlay() {
        touchFrame = FrameLayout(view.context)
        touchFrame!!.tag = EXIT_TOUCH_FRAME_TAG
        val layoutParam = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT)

        touchFrame!!.setOnLongClickListener{
            exitPhotoMode()
            true
        }

        rootView.addView(touchFrame, layoutParam)

        val toast = Toast.makeText(activity, R.string.toast_msg_photo_mode_instruction, Toast.LENGTH_SHORT)
        toast.setGravity(Gravity.BOTTOM or Gravity.CENTER, 0, 0)
        toast.show()
    }

    private fun removePotentialPhotoModeExitOverlay() {
        if(touchFrame == null) return
        rootView.removeView(touchFrame)
        touchFrame = null
    }

    private fun exitPhotoMode() {
        showControls()
        setContentViewFocusable()
        if(systemWindowsHidden) unhideSystemWindows()
        removePotentialPhotoModeExitOverlay()
    }
}
