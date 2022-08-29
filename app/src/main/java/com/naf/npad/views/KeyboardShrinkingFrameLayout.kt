package com.naf.npad.views

import android.app.Activity
import android.content.Context
import android.graphics.Point
import android.graphics.Rect
import androidx.annotation.AttrRes
import android.util.AttributeSet
import android.view.View
import android.view.ViewTreeObserver
import android.widget.FrameLayout

/*
 * In fullscreen mode or with translucent system bars's, android fails to resize the window on IME
 * input, which means the text box doesn't shrink to the IME, the IME obscuring everything.
 * This class counteracts that. It's simple, and probably "brittle" (I might flat out not work on
 * some devices.)
 * NOTE: You'll need/still need adjustResize in the windowSoftInputMode, as that provides the call
 *  to the globalLayoutListener.
 */

class KeyboardShrinkingFrameLayout : FrameLayout {
    private var decorView: View? = null

    private var onGlobalLayoutListener: ViewTreeObserver.OnGlobalLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
        val visibleDisplayFrame = Rect()
        decorView!!.getWindowVisibleDisplayFrame(visibleDisplayFrame)

        val display = Point()

        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.R){
            (context as Activity).windowManager.defaultDisplay.getRealSize(display)
        } else {
            context.display?.getRealSize(display)
        }

        val displayHeight = display.y

        val visibleHeight = visibleDisplayFrame.bottom
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

        decorView!!.viewTreeObserver.addOnGlobalLayoutListener(onGlobalLayoutListener)
    }

}
