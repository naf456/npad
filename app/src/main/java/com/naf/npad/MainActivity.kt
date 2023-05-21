package com.naf.npad

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.naf.npad.databinding.LayoutMainBinding
import com.naf.npad.fragments.PageManagerFragment
import com.naf.npad.fragments.EditorFragment
import com.naf.npad.repository.PageEntity
import com.naf.npad.util.toast
import com.naf.npad.viewmodels.MainViewModel
import kotlinx.coroutines.*


class MainActivity : AppCompatActivity() {

    private lateinit var viewBinding: LayoutMainBinding

    private val mainViewModel : MainViewModel by viewModels()

    private val pageManagerDelegate = object : PageManagerFragment.PageManagerFragmentDelegate {

        override fun onPageSelected(pageId: Int, viewHolder: PageManagerFragment.PageItemViewHolder) {
            lifecycleScope.launch {
                val page = mainViewModel.getPageWithId(pageId) ?: return@launch
                val sharedViews = mapOf<View, String>(
                    viewHolder.thumbnailImageView to "editorWallpaper"
                )
                launchEditor(page, sharedViews = sharedViews)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = LayoutMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        val pageManagerFragment = PageManagerFragment()
        pageManagerFragment.delegate = pageManagerDelegate

        supportFragmentManager.commit {
            replace(R.id.fragmentContainer, pageManagerFragment, pageManagerFragment::class.simpleName)
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

    private fun launchEditor(page: PageEntity, toast: Boolean = false, sharedViews: Map<View, String>? = null){
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
}
