package com.naf.npad.widgets

import android.app.Activity
import android.content.Context
import android.graphics.Point
import android.graphics.Rect
import android.os.Build
import androidx.annotation.AttrRes
import android.util.AttributeSet
import android.view.View
import android.view.ViewTreeObserver
import android.widget.FrameLayout

/*
 * In fullscreen modroe or with translucent system bars's, andid fails to resize the window on IME
 * input, which means the text editor doesn't shrink to the IME, the IME obscuring everything.
 * This class counteracts that. It's simple, and probably "brittle" (I might flat out not work on
 * some devices.)
 * NOTE: You'll need/still need adjustResize in the windowSoftInputMode, as that provides the call
 *  to the globalLayoutListener.
 */

class KeyboardShrinkingFrameLayout : FrameLayout {
    private var decorView: View? = null

    internal var onGlobalLayoutListener: ViewTreeObserver.OnGlobalLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
        val visibleDisplayFrame = Rect()
        decorView!!.getWindowVisibleDisplayFrame(visibleDisplayFrame)
        val windowHeight = decorView!!.context.resources.displayMetrics.heightPixels

        val display = Point()
        (context as Activity).windowManager.defaultDisplay.getRealSize(display)
        val displayHeight = display.y

        val visibleHeight = visibleDisplayFrame.bottom //Terrible api, I dislike rect!
        val heightDiff = displayHeight - visibleHeight

        if (heightDiff > 0) {
            this@KeyboardShrinkingFrameLayout.setPadding(0, 0, 0, heightDiff)
        } else {
            this@KeyboardShrinkingFrameLayout.setPadding(0, 0, 0, 0)
        }
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        if (!isInEditMode) {
            init(context)
        }
    }

    constructor(context: Context) : super(context) {
        if (!isInEditMode) {
            init(context)
        }
    }

    constructor(context: Context, attrs: AttributeSet?, @AttrRes defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        if (!isInEditMode) {
            init(context)
        }
    }

    private fun init(context: Context) {
        this.decorView = (context as Activity).window.decorView

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            decorView!!.viewTreeObserver.addOnGlobalLayoutListener(onGlobalLayoutListener)
        }
    }

}
