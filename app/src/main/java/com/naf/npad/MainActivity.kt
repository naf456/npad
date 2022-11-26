package com.naf.npad

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.naf.npad.databinding.LayoutMainBinding
import com.naf.npad.fragments.PageManagerFragment
import com.naf.npad.fragments.EditorFragment
import com.naf.npad.repository.PageEntity
import com.naf.npad.viewmodels.AppViewModel
import kotlinx.coroutines.*


class MainActivity : AppCompatActivity(), PageManagerFragment.PageManagerFragmentDelegate {

    companion object {
        const val EDITOR_FRAGMENT_TAG = "Editor"
        const val PAGEMANAGER_FRAGMENT_TAG = "PageManager"
    }

    lateinit var binding: LayoutMainBinding

    private val appViewModel : AppViewModel by viewModels()

    private val currentFragment: Fragment get() {
        return supportFragmentManager.findFragmentById(R.id.fragmentContainer) ?: Fragment()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LayoutMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        val pageManagerFragment = PageManagerFragment()
        pageManagerFragment.delegate = this

        supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, pageManagerFragment, PAGEMANAGER_FRAGMENT_TAG)
                .commit()


        //Check for autoload document
        if(appViewModel.autoloadPage) {
            appViewModel.lastOpenedPageId?.let {
                lifecycleScope.launch {
                    val page = appViewModel.getPageWithId(it) ?: return@launch
                    openEditor(page)
                    Utls.toast(
                        this@MainActivity,
                        "Auto-loaded ${if (page.title.isNullOrEmpty()) "[Untitled]" else page.title}"
                    )
                }
            }
        }
    }

    override fun onBackPressed() {
        when(currentFragment) {
            is PageManagerFragment -> {
                superBackPressed()
            }
            is EditorFragment -> {
                (currentFragment as EditorFragment).onBackPressed()
            }
            else -> {
                superBackPressed()
            }
        }
    }

    fun superBackPressed() {
        super.onBackPressed()
    }

    override fun onPageSelected(pageId: Int): Unit = runBlocking {
        launch {
            appViewModel.getPageWithId(pageId)?.let { page ->
                openEditor(page)
            }
        }
    }

    private fun openEditor(page: PageEntity){
        appViewModel.currentPage.value = page
        supportFragmentManager.commit {
            /*setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out,
                android.R.anim.slide_in_left,
                android.R.anim.slide_out_right
            )*/
            replace(R.id.fragmentContainer, EditorFragment(), EDITOR_FRAGMENT_TAG)
            addToBackStack(null)
        }
    }
}
