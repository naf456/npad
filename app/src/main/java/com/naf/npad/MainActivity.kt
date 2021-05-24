package com.naf.npad

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager


class MainActivity : AppCompatActivity() {

    companion object {
        const val EDITOR_FRAGMENT_TAG = "Editor"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportFragmentManager.beginTransaction()
                .replace(R.id.main_fragment_container, EditorFragment(), EDITOR_FRAGMENT_TAG)
                .commit()
    }



    override fun onBackPressed() {
        val editorFragment = supportFragmentManager.findFragmentByTag(EDITOR_FRAGMENT_TAG)
        if(editorFragment != null && editorFragment is EditorFragment
            && editorFragment.isVisible && editorFragment.isResumed) {
            editorFragment.onBackPressed()
        } else {
            super.onBackPressed()
        }
    }

    fun superBackPressed() {
        super.onBackPressed()
    }
}
