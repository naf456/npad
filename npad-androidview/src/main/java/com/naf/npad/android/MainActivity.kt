package com.naf.npad.android

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.naf.npad.R
import com.naf.npad.databinding.AppMainBinding
import com.naf.npad.android.browser.BrowserFragment
import com.naf.npad.android.editor.EditorFragment
import com.naf.npad.android.util.toast
import kotlinx.coroutines.*


class MainActivity : AppCompatActivity() {

    private lateinit var viewBinding: AppMainBinding

    private val mainViewModel : MainViewModel by viewModels()

    private val pageManagerDelegate = object : BrowserFragment.BrowserFragmentDelegate {

        override fun onPageSelected(pageId: Int, viewHolder: BrowserFragment.PageItemViewHolder) {
            lifecycleScope.launch {
                val page = mainViewModel.getPageWithId(pageId) ?: return@launch
                val sharedViews = mapOf<View, String>(
                    viewHolder.thumbnailImageView to "editorWallpaper"
                )
                launchEditor(page)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = AppMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        val browserFragment = BrowserFragment()
        browserFragment.delegate = pageManagerDelegate

        supportFragmentManager.commit {
            replace(R.id.fragmentContainer, browserFragment, browserFragment::class.simpleName)
        }

        /*
        if(mainViewModel.autoloadPage) {
            lifecycleScope.launch {
                val lastPageId = mainViewModel.lastOpenedPageId?: return@launch
                val lastPage = mainViewModel.getPageWithId(lastPageId) ?: return@launch
                launchEditor(page = lastPage, toast = true)
            }
        }
        */
    }

    private fun launchEditor(page: com.naf.npad.android.data.PageEntity, toast: Boolean = false, sharedViews: Map<View, String>? = null){
        mainViewModel.currentPage.value = page

        supportFragmentManager.commit {
            sharedViews?.let {
                setReorderingAllowed(true)
                for(sharedView in sharedViews) {
                    addSharedElement(sharedView.key, sharedView.value)
                }
            }
            setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.fade_out,
            android.R.anim.fade_in, android.R.anim.fade_out)
            replace(R.id.fragmentContainer, EditorFragment(), EditorFragment::class.simpleName)
            addToBackStack(null)
        }

        if(toast) {
            val title = if (page.title.isNullOrEmpty()) "[Untitled]" else page.title
            toast(
                this@MainActivity,
                "Loaded '$title'"
            )
        }
    }

    fun openSettings() {
        val settingsFragment = SettingsFragment()

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, settingsFragment)
            .addToBackStack(null)
            .commit()
    }
}
