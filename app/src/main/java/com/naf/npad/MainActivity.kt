package com.naf.npad

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.naf.npad.databinding.ActivityMainBinding
import com.naf.npad.dialogs.WarnExitDialog
import com.naf.npad.fragments.PageManagerFragment
import com.naf.npad.fragments.EditorFragment
import com.naf.npad.repository.PageEntity
import com.naf.npad.viewmodels.AppViewModel
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity(), PageManagerFragment.PageManagerFragmentDelegate {

    companion object {
        const val EDITOR_FRAGMENT_TAG = "Editor"
        const val PAGEMANAGER_FRAGMENT_TAG = "PageManager"
    }

    lateinit var binding: ActivityMainBinding

    private val appViewModel : AppViewModel by viewModels()

    private val currentFragment: Fragment get() {
        return supportFragmentManager.findFragmentById(R.id.fragmentContainer) ?: Fragment()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val pageManagerFragment = PageManagerFragment()
        pageManagerFragment.delegate = this

        supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, pageManagerFragment, PAGEMANAGER_FRAGMENT_TAG)
                .commit()

        supportFragmentManager.addOnBackStackChangedListener {
            when(currentFragment) {
                is PageManagerFragment -> binding.wallpaperImageView.applyWallpaperFromPreferences()
            }
        }

        appViewModel.currentPage.observe(this) { page ->
            page?: return@observe
            val backgroundId = page.backgroundId
            if(backgroundId != null) {
                lifecycleScope.launchWhenCreated {
                    val background = appViewModel.getBackgroundBitmap(backgroundId)
                    binding.wallpaperImageView.setImageBitmap(background)
                }
            } else {
                binding.wallpaperImageView.applyWallpaperFromPreferences()
            }
        }

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
                val dialog = WarnExitDialog()
                dialog.onDialogFinished = {
                    superBackPressed()
                }
                dialog.show(supportFragmentManager, null)
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

    override fun onPageSelected(page: PageEntity) {
        openEditor(page)
    }

    private fun openEditor(page: PageEntity){
        appViewModel.currentPage.value = page
        supportFragmentManager.commit {
            setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.slide_out_right,
                android.R.anim.slide_in_left,
                android.R.anim.fade_out
            )
            replace(R.id.fragmentContainer, EditorFragment(), EDITOR_FRAGMENT_TAG)
            addToBackStack(null)
        }
    }
}
