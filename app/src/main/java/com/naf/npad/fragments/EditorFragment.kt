package com.naf.npad.fragments

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.ActionMenuView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.naf.npad.*
import com.naf.npad.Utls.Uri.md5
import com.naf.npad.databinding.FragmentEditorBinding
import com.naf.npad.dialogs.WarnSaveDialog
import com.naf.npad.repository.PageEntity
import com.naf.npad.util.SafeGetContentActivityResultContract
import com.naf.npad.viewmodels.AppViewModel
import com.naf.npad.views.editor.KnifeTextHistoryWriter
import io.github.mthli.knife.KnifeText


open class
EditorFragment : Fragment(), ActionMenuView.OnMenuItemClickListener {

    private lateinit var views: FragmentEditorBinding
    private lateinit var knifeText : KnifeText
    private lateinit var history: History
    private lateinit var knifeTextHistoryWriter: KnifeTextHistoryWriter

    private val appViewModel : AppViewModel by activityViewModels()

    private var lastSaveDocHash : String = ""
    private val content: String
    get() = knifeText.toHtml()

    private var isSaving = false

    private val getContent = registerForActivityResult(SafeGetContentActivityResultContract()) {
        it?.let { lifecycleScope.launchWhenCreated {  appViewModel.setCurrentPageBackgroundFromURI(it) } }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        history = History(100)
        history.startRecording()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {

        views = FragmentEditorBinding.inflate(inflater, container, false)
        knifeText = views.editorKnifeText
        setupMenus()
        setupOnSelectionMenu()

        appViewModel.currentPage.observe(viewLifecycleOwner) { pageEntity ->
            pageEntity?: return@observe
            loadPageEntity(pageEntity)
        }

        return views.root
    }

    override fun onStart() {
        super.onStart()
        knifeTextHistoryWriter = KnifeTextHistoryWriter(knifeText, history)
    }

    private fun loadPageEntity(pageEntity: PageEntity){
        if(pageEntity.content == null) {
            knifeText.text.clear()
        } else {
            knifeText.fromHtml(pageEntity.content)
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
        val inflater = requireActivity().menuInflater
        inflater.inflate(R.menu.fragment_editor_menu, views.editorUndoMenu.menu)
        inflater.inflate(R.menu.editor_quicksave_menu, views.editorQuickSaveMenu.menu)
        views.editorUndoMenu.setOnMenuItemClickListener(this)
        views.editorQuickSaveMenu.setOnMenuItemClickListener(this)
    }

    private fun setupOnSelectionMenu() {
        knifeText.customSelectionActionModeCallback = object : ActionMode.Callback {

            override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                activity?.menuInflater?.inflate(R.menu.knife_text_selection_styling, menu)
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
                        knifeText.bold(!knifeText.contains(KnifeText.FORMAT_BOLD))
                        return true
                    }
                    getString(R.string.text_styling_italic) -> {
                        knifeText.italic(!knifeText.contains(KnifeText.FORMAT_ITALIC))
                        return true
                    }
                    getString(R.string.text_styling_underline) -> {
                        knifeText.underline(!knifeText.contains(KnifeText.FORMAT_UNDERLINED))
                        return true
                    }
                }
                return false
            }

            override fun onDestroyActionMode(mode: ActionMode?) {}

        }
    }

    override fun onMenuItemClick(menuItem: MenuItem): Boolean {

        when (menuItem.itemId) {
            R.id.editor_action_save -> saveDocument()
            R.id.action_quicksave -> saveDocument()
            R.id.editor_action_photo_mode -> enterPhotoMode()
            R.id.editor_action_undo -> history.undo()
            R.id.editor_action_redo -> history.redo()
            R.id.editor_action_wallpaper_set -> getContent.launch("image/*")
            R.id.editor_action_wallpaper_clear -> appViewModel.clearBackgroundForCurrentPage()
        }
        return true
    }

    private fun saveDocument() {
        isSaving = true
        val page = appViewModel.currentPage.value ?: return
        page.content = knifeText.toHtml()
        appViewModel.updatePage(page).invokeOnCompletion {
            isSaving = false
        }
    }

    override fun onPause() {
        super.onPause()
        knifeText.hideSoftInput()
    }

    private fun resetEditor() {
        history.reset()
        knifeText.setText("")
        lastSaveDocHash = md5(content)
        history.startRecording()
    }

    private fun enterPhotoMode() {

        //Hide system bars
        WindowInsetsControllerCompat(requireActivity().window, requireView())
            .hide(WindowInsetsCompat.Type.systemBars())

        //Hide cursor
        knifeText.hideSoftInput()
        knifeText.clearFocus()
        knifeText.isFocusable = false

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
        knifeText.isFocusable = true
        knifeText.isFocusableInTouchMode = true

        //Show toolbar
        views.editorToolbarContainer.visibility = View.VISIBLE

        //Show system bars
        WindowInsetsControllerCompat(requireActivity().window, requireView())
            .show(WindowInsetsCompat.Type.systemBars())

        //Hide exit touch control
        views.editorPhotomodeExit.visibility = View.GONE
    }

    fun onBackPressed() {
        val d = WarnSaveDialog()
        d.onDialogFinished = {
            val act = activity
            if(act is MainActivity) {
                act.superBackPressed()
            }
        }
        d.show(parentFragmentManager, null)
    }
}