package com.naf.npad.fragments

import android.os.Bundle
import android.view.*
import android.widget.FrameLayout
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.ActionMenuView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.naf.npad.*
import com.naf.npad.Utls.Uri.md5
import com.naf.npad.databinding.LayoutEditorBinding
import com.naf.npad.repository.PageEntity
import com.naf.npad.util.SafeGetContentActivityResultContract
import com.naf.npad.viewmodels.AppViewModel
import com.naf.npad.views.editor.KnifeTextHistoryWriter
import io.github.mthli.knife.KnifeText
import kotlinx.coroutines.launch


open class
EditorFragment : Fragment(), ActionMenuView.OnMenuItemClickListener, PopupMenu.OnMenuItemClickListener {

    private lateinit var views: LayoutEditorBinding

    private lateinit var history: History
    private lateinit var knifeTextHistoryWriter: KnifeTextHistoryWriter

    private val appViewModel : AppViewModel by activityViewModels()

    private val content: String
        get() = views.knifeText.toHtml()

    private lateinit var menuButton: MenuItem

    private var lastSaveDocHash : String = ""
    private var isSaving = false

    private val getImageFromAndroid = registerForActivityResult(SafeGetContentActivityResultContract()) { imageUri ->
        imageUri?.let {
            lifecycleScope.launchWhenCreated {
                loading = true
                appViewModel.viewModelScope.launch {
                    appViewModel.setCurrentPageBackgroundFromURI(it)
                }.invokeOnCompletion {
                    loading = false
                }
            }
        }
    }

    private var loading: Boolean = false
        set(it) {
            if(it) {
                menuButton.actionView = layoutInflater.inflate(R.layout.layout_actionview_progressindicator, FrameLayout(requireContext()))
                menuButton.isEnabled = false
            } else {
                menuButton.actionView = null
                menuButton.isEnabled = true
            }
            field = it
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        history = History(100)
        history.startRecording()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {

        views = LayoutEditorBinding.inflate(inflater, container, false)

        setupMenus()
        setupOnSelectionMenu()

        appViewModel.currentPage.observe(this.viewLifecycleOwner) { pageEntity ->
            pageEntity?: return@observe
            loadPageEntity(pageEntity)
        }

        return views.root
    }

    override fun onStart() {
        super.onStart()
        knifeTextHistoryWriter = KnifeTextHistoryWriter(views.knifeText, history)
    }

    private fun loadPageEntity(pageEntity: PageEntity){

        pageEntity.content?.let { content ->
            views.knifeText.fromHtml(content)
            lastSaveDocHash = md5(content)
        }?: run {
            views.knifeText.text.clear()
            lastSaveDocHash = md5("")
        }

        val backgroundId = pageEntity.backgroundId
        if(backgroundId != null) {
            lifecycleScope.launchWhenCreated {
                val background = appViewModel.getBackgroundBitmap(backgroundId)
                views.wallpaperImageView.setImageBitmap(background)
            }
        } else {
            views.wallpaperImageView.setImageDrawable(
                AppCompatResources.getDrawable(requireContext(), R.drawable.shape_plain_background)
            )
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
            R.id.editor_action_wallpaper_set -> getImageFromAndroid.launch("image/*")
            R.id.editor_action_wallpaper_clear -> appViewModel.clearBackgroundForCurrentPage()
        }
        return true
    }

    private fun setupOnSelectionMenu() {
        views.knifeText.customSelectionActionModeCallback = object : ActionMode.Callback {

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
                        views.knifeText.bold(!views.knifeText.contains(KnifeText.FORMAT_BOLD))
                        return true
                    }
                    getString(R.string.text_styling_italic) -> {
                        views.knifeText.italic(!views.knifeText.contains(KnifeText.FORMAT_ITALIC))
                        return true
                    }
                    getString(R.string.text_styling_underline) -> {
                        views.knifeText.underline(!views.knifeText.contains(KnifeText.FORMAT_UNDERLINED))
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
        val page = appViewModel.currentPage.value ?: return
        val content = views.knifeText.toHtml()
        val thisHash = md5(content)
        if(lastSaveDocHash != thisHash) {
            page.content = content
            appViewModel.updatePage(page).invokeOnCompletion {
                isSaving = false
            }
        }
    }

    override fun onPause() {
        super.onPause()
        views.knifeText.hideSoftInput()
    }

    private fun resetEditor() {
        history.reset()
        views.knifeText.setText("")
        lastSaveDocHash = md5(content)
        history.startRecording()
    }

    private fun enterPhotoMode() {

        //Hide system bars
        WindowInsetsControllerCompat(requireActivity().window, requireView())
            .hide(WindowInsetsCompat.Type.systemBars())

        //Hide cursor
        views.knifeText.hideSoftInput()
        views.knifeText.clearFocus()
        views.knifeText.isFocusable = false

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
        views.knifeText.isFocusable = true
        views.knifeText.isFocusableInTouchMode = true

        //Show toolbar
        views.editorToolbarContainer.visibility = View.VISIBLE

        //Show system bars
        WindowInsetsControllerCompat(requireActivity().window, requireView())
            .show(WindowInsetsCompat.Type.systemBars())

        //Hide exit touch control
        views.editorPhotomodeExit.visibility = View.GONE
    }

    fun onBackPressed() {
        saveDocument()
        val act = activity
        if(act is MainActivity) {
            act.superBackPressed()
        }
    }
}