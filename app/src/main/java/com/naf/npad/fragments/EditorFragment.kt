package com.naf.npad.fragments

import android.os.Bundle
import android.view.*
import android.widget.FrameLayout
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.ActionMenuView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import androidx.transition.TransitionInflater
import com.naf.npad.*
import com.naf.npad.util.md5
import com.naf.npad.databinding.LayoutEditorBinding
import com.naf.npad.repository.PageEntity
import com.naf.npad.util.SafeCropImageToScreenActivityResultContract
import com.naf.npad.util.SafeGetContentActivityResultContract
import com.naf.npad.viewmodels.MainViewModel
import com.naf.npad.views.editor.KnifeTextHistoryWriter
import io.github.mthli.knife.KnifeText
import kotlinx.coroutines.launch

open class EditorFragment : Fragment(), ActionMenuView.OnMenuItemClickListener, PopupMenu.OnMenuItemClickListener {

    private lateinit var views: LayoutEditorBinding

    private lateinit var history: History
    private lateinit var knifeTextHistoryWriter: KnifeTextHistoryWriter

    private val mainViewModel : MainViewModel by activityViewModels()

    private lateinit var menuButton: MenuItem

    private var lastSaveDocHash : String = ""
    private var isSaving = false

    private val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            saveDocument()
            this.isEnabled = false
            this@EditorFragment.requireActivity().onBackPressedDispatcher.onBackPressed()
            this.isEnabled = true
        }
    }

    private val requestExternalImagePicker = registerForActivityResult(SafeGetContentActivityResultContract()) { imageUri ->
        imageUri?.let {
            requestExternalImageCropper.launch(it)
        }
    }

    private val requestExternalImageCropper = registerForActivityResult(SafeCropImageToScreenActivityResultContract()) { imageUri ->
        imageUri?.let {
            loading = true
            mainViewModel.viewModelScope.launch {
                mainViewModel.setCurrentPageBackgroundFromURI(it)
            }.invokeOnCompletion {
                loading = false
            }
        }
    }

    private var loading: Boolean = false
        set(loading) {
            lifecycleScope.launch {
                lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
                    if(loading) {
                        menuButton.actionView = layoutInflater.inflate(R.layout.layout_actionview_progressindicator, FrameLayout(requireContext()))
                        menuButton.isEnabled = false
                    } else {
                        menuButton.actionView = null
                        menuButton.isEnabled = true
                    }
                    field = loading
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        history = History(100)
        history.startRecording()

        requireActivity().onBackPressedDispatcher.addCallback(this,backPressedCallback)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {

        views = LayoutEditorBinding.inflate(inflater, container, false)

        setupMenus()
        setupOnSelectionMenu()
        setupTransitions()

        mainViewModel.currentPage.observe(this.viewLifecycleOwner) { pageEntity ->
            pageEntity?: return@observe
            loadPageEntity(pageEntity)
        }

        return views.root
    }

    private fun setupTransitions() {
        val inflater = TransitionInflater.from(requireContext())
        sharedElementEnterTransition = inflater.inflateTransition(R.transition.shared_image)
        sharedElementReturnTransition = inflater.inflateTransition(R.transition.shared_image)

        ViewCompat.setTransitionName(views.editorDocumentBackground, "editorWallpaper")
    }

    override fun onStart() {
        super.onStart()
        knifeTextHistoryWriter = KnifeTextHistoryWriter(views.editorKnifeText, history)
    }


    private fun loadPageEntity(pageEntity: PageEntity) = lifecycleScope.launch{

        lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {

            history.reset()
            views.editorKnifeText.scrollTo(0,0)

            val content = pageEntity.content?: ""
            views.editorKnifeText.fromHtml(content)
            lastSaveDocHash = md5(content)

            val title = pageEntity.title?: ""
            views.editorDocumentTitle.text = title.ifEmpty { "[Untitled]" }

            val backgroundId = pageEntity.backgroundId
            if(backgroundId != null) {
                val background = mainViewModel.getBackgroundBitmap(backgroundId)
                views.editorDocumentBackground.setImageBitmap(background)
            } else {
                views.editorDocumentBackground.setImageDrawable(
                    AppCompatResources.getDrawable(requireContext(), R.drawable.shape_plain_background)
                )
            }

        }
    }

    private fun setupMenus() {
        menuButton = views.editorUndoMenu.menu.add("More")
        menuButton.setIcon(R.drawable.ic_more_horiz)
        menuButton.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        views.editorUndoMenu.setOnMenuItemClickListener(this)
    }

    override fun onMenuItemClick(menuItem: MenuItem): Boolean {

        when (menuItem.title) {
            "More" -> {
                val popupMenu = PopupMenu(requireContext(), views.editorUndoMenu, Gravity.START)
                popupMenu.menuInflater.inflate(R.menu.menu_editor, popupMenu.menu)
                popupMenu.setOnMenuItemClickListener(this)
                popupMenu.show()
            }
        }

        when (menuItem.itemId) {
            R.id.editor_action_save -> saveDocument()
            R.id.editor_action_photo_mode -> enterPhotoMode()
            //R.id.editor_action_undo -> history.undo()
            //R.id.editor_action_redo -> history.redo()
            R.id.editor_action_wallpaper_set -> requestExternalImagePicker.launch("image/*")
            R.id.editor_action_wallpaper_clear -> mainViewModel.clearBackgroundForCurrentPage()
            R.id.editor_action_gotoSetting -> gotoSettings()
        }
        return true
    }

    private fun setupOnSelectionMenu() {
        views.editorKnifeText.customSelectionActionModeCallback = object : ActionMode.Callback {

            override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                activity?.menuInflater?.inflate(R.menu.menu_text_styling, menu)
                return true
            }

            override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                return false
            }

            override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
                item?: return false
                item.title?: return false
                when(item.title){
                    getString(R.string.text_styling_bold) -> {
                        views.editorKnifeText.bold(!views.editorKnifeText.contains(KnifeText.FORMAT_BOLD))
                        return true
                    }
                    getString(R.string.text_styling_italic) -> {
                        views.editorKnifeText.italic(!views.editorKnifeText.contains(KnifeText.FORMAT_ITALIC))
                        return true
                    }
                    getString(R.string.text_styling_underline) -> {
                        views.editorKnifeText.underline(!views.editorKnifeText.contains(KnifeText.FORMAT_UNDERLINED))
                        return true
                    }
                }
                return false
            }

            override fun onDestroyActionMode(mode: ActionMode?) {}

        }
    }

    private fun saveDocument() {
        isSaving = true
        val page = mainViewModel.currentPage.value ?: return
        val content = views.editorKnifeText.toHtml()
        val thisHash = md5(content)
        if(lastSaveDocHash != thisHash) {
            page.content = content
            mainViewModel.updatePage(page).invokeOnCompletion {
                isSaving = false
            }
        }
    }

    override fun onPause() {
        super.onPause()
        views.editorKnifeText.hideSoftInput()
    }

    private fun enterPhotoMode() {

        //Hide system bars
        WindowInsetsControllerCompat(requireActivity().window, requireView())
            .hide(WindowInsetsCompat.Type.systemBars())

        //Hide cursor
        views.editorKnifeText.hideSoftInput()
        views.editorKnifeText.clearFocus()
        views.editorKnifeText.isFocusable = false

        //Hide toolbar
        views.editorToolbarContainer.visibility = CoordinatorLayout.GONE

        //Activate invisible touch disable control
        views.editorPhotomodeExit.visibility = View.VISIBLE
        views.editorPhotomodeExit.setOnLongClickListener {
            exitPhotoMode()
            true
        }

        //Notify controls
        val toast = Toast.makeText(activity, R.string.toast_msg_photo_mode_instruction, Toast.LENGTH_SHORT)
        toast.setGravity(Gravity.BOTTOM or Gravity.CENTER, 0, 0)
        toast.show()
    }

    private fun exitPhotoMode() {

        //Allow textbox to be editable
        views.editorKnifeText.isFocusable = true
        views.editorKnifeText.isFocusableInTouchMode = true

        //Show toolbar
        views.editorToolbarContainer.visibility = View.VISIBLE

        //Show system bars
        WindowInsetsControllerCompat(requireActivity().window, requireView())
            .show(WindowInsetsCompat.Type.systemBars())

        //Hide exit touch control
        views.editorPhotomodeExit.visibility = View.GONE
    }

    private fun gotoSettings(){
        val settingsFragment = SettingsFragment()

        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, settingsFragment)
            .addToBackStack(null)
            .commit()
    }
}