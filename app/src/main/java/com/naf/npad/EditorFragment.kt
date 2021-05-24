package com.naf.npad

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.naf.npad.Utls.Uri.md5
import com.naf.npad.databinding.FragmentEditorBinding
import com.naf.npad.dialog.WarnUnsavedChangesDialog
import io.github.mthli.knife.KnifeText
import java.io.BufferedOutputStream
import java.io.IOException
import java.util.*


open class EditorFragment : Fragment(), Toolbar.OnMenuItemClickListener {

    companion object {
        const val DOCUMENT_TYPE_PLAIN_TEXT = ".txt"
        const val DOCUMENT_TYPE_NPAD_ML = ".npml"
        const val DEFAULT_DOCUMENT_TYPE = DOCUMENT_TYPE_NPAD_ML
    }

    private lateinit var views: FragmentEditorBinding
    private lateinit var knifeText : KnifeText
    private lateinit var history: History
    private lateinit var knifeTextHistoryWriter: KnifeTextHistoryWriter

    private var currentDocumentUri : Uri? = null
    private var currentDocumentType = DEFAULT_DOCUMENT_TYPE
    private var lastSaveDocHash : String = ""
    private val content: String
    get() =  if(currentDocumentType == DOCUMENT_TYPE_NPAD_ML)
            knifeText.toHtml()
    else
            knifeText.text.toString()

    private lateinit var documentFilePicker : ActivityResultLauncher<Array<String>>
    private lateinit var saveFilePicker: ActivityResultLauncher<String>
    private val openDocumentCallback = ActivityResultCallback<Uri> {
        continueOpeningDocument(it)
    }
    private val saveFileCallback = ActivityResultCallback<Uri> { continueSavingDocument(it) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        documentFilePicker = registerForActivityResult(ActivityResultContracts.OpenDocument(), openDocumentCallback)
        saveFilePicker = registerForActivityResult(ActivityResultContracts.CreateDocument(), saveFileCallback)

        history = History(100)
        history.startRecording()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {

        views = FragmentEditorBinding.inflate(inflater, container, false)
        knifeText = views.editorKnifeText
        setupToolbar()
        setupSelectionStyling()
        applyFontSize()
        return views.root
    }

    override fun onStart() {
        super.onStart()
        knifeTextHistoryWriter = KnifeTextHistoryWriter(knifeText, history)
        applyFontSize()
    }

    private fun applyFontSize() {
        val preferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val fontSizeKey = getString(R.string.pref_key_font_size)
        val fontSizeDefault = getString(R.string.pref_default_font_size)
        val fontSize = preferences.getString(fontSizeKey, fontSizeDefault)
        try {
            val fontSizeInt = Integer.parseInt(fontSize!!)
            knifeText.textSize = fontSizeInt.toFloat()
        } catch (e: NumberFormatException) {
            e.printStackTrace()
        }

    }

    private fun setupToolbar() {
        requireActivity().menuInflater.inflate(R.menu.activity_editor_menu, views.editorToolbar.menu)
        views.editorToolbar.setOnMenuItemClickListener(this)
    }

    private fun setupSelectionStyling() {
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
            R.id.editor_action_new -> newDocument()
            R.id.editor_action_open -> openDocument()
            R.id.editor_action_save -> saveDocument()
            R.id.editor_action_save_as -> saveDocumentAs()
            R.id.editor_action_photo_mode -> enterPhotoMode()
            R.id.editor_action_gotoSetting -> startSettings()
            R.id.editor_action_undo -> history.undo()
            R.id.editor_action_redo -> history.redo()
        }
        return true
    }

    private fun startSettings() {
        val settingsFragment = SettingsFragment()

        requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.main_fragment_container, settingsFragment)
                .addToBackStack(null)
                .commit()
    }

    override fun onPause() {
        super.onPause()
        knifeText.hideSoftInput()
    }

    private fun newDocument() {
        warnUnsavedChanges { this@EditorFragment.resetEditor() }
    }

    private fun resetEditor() {
        currentDocumentUri = null
        currentDocumentType = DEFAULT_DOCUMENT_TYPE
        history.reset()
        knifeText.setText("")
        lastSaveDocHash = md5(content)
        history.startRecording()
    }

    private fun openDocument() {
        warnUnsavedChanges {
            val input = arrayOf("*/*")
            documentFilePicker.launch(input)
        }
    }

    private fun continueOpeningDocument(documentUri: Uri) {

        resetEditor()

        currentDocumentUri = documentUri

        val extension = Utls.Uri.getExtension(requireContext(), documentUri)

        if (extension == DOCUMENT_TYPE_NPAD_ML) {
            currentDocumentType = DOCUMENT_TYPE_NPAD_ML
            loadDocumentContent()
        } else {
            currentDocumentType = DOCUMENT_TYPE_PLAIN_TEXT
            loadDocumentContent()
        }
    }

    private fun loadDocumentContent() {
        try {

            val resolver = activity?.contentResolver

            val inputStream = resolver?.openInputStream(currentDocumentUri!!) ?: throw NullPointerException()

            val scanner = Scanner(inputStream).useDelimiter("\\A")
            val content = if (scanner.hasNext()) scanner.next() else ""

            inputStream.close()

            history.stopRecording()
            if (currentDocumentType == DOCUMENT_TYPE_NPAD_ML) {
                knifeText.fromHtml(content)
            } else {
                knifeText.setText(content)
            }
            lastSaveDocHash = md5(content)
            history.startRecording()
        } catch (e: Exception) {
            e.printStackTrace()
            Utls.toast(requireContext(), "Can't read document")
            resetEditor()
        }

    }

    private fun saveDocument() {

        if (currentDocumentUri == null) {
            saveDocumentAs()
            return
        }

        try {
            val resolver = requireActivity().contentResolver
            val outputStream = resolver.openOutputStream(currentDocumentUri!!) ?: throw IOException()

            val bufOutputStream = BufferedOutputStream(outputStream)
            bufOutputStream.write(content.toByteArray())
            bufOutputStream.close()

            lastSaveDocHash = md5(content)

            Utls.toast(requireContext(), "Saved")

        } catch (e: IOException) {
            e.printStackTrace()
            Utls.toastLong(requireContext(), "Save Failed! Please try \"Save As...\".")
        } catch (e: SecurityException) {
            e.printStackTrace()
            Utls.toastLong(requireContext(), "Permission Error. Please try \"Save As...\".")
        }

    }

    private fun saveDocumentAs() {
        val suggestedFilename = "awesome_document_name_here.npml"
        saveFilePicker.launch(suggestedFilename)
    }

    private fun continueSavingDocument(documentUri: Uri) {

        val extension = Utls.Uri.getExtension(requireContext(), documentUri)

        if (extension == DOCUMENT_TYPE_NPAD_ML) {
            currentDocumentUri = documentUri
            currentDocumentType = DOCUMENT_TYPE_NPAD_ML
            saveDocument()
        } else {
            currentDocumentUri = documentUri
            currentDocumentType = DOCUMENT_TYPE_PLAIN_TEXT
            saveDocument()
        }
    }

    private fun warnUnsavedChanges(onWarningFinished: () -> Unit) {
        if(md5(content) == lastSaveDocHash) {
            onWarningFinished()
            return
        }
        val warningDialog = WarnUnsavedChangesDialog()
        warningDialog.onWarningFinished = onWarningFinished
        warningDialog.show(requireActivity().supportFragmentManager, null)
    }

    @Suppress("DEPRECATION")
    private fun enterPhotoMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity?.window?.setDecorFitsSystemWindows(false)
        } else {
            activity?.window?.decorView?.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_IMMERSIVE
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
        }

        knifeText.hideSoftInput()
        knifeText.clearFocus()
        knifeText.isFocusable = false

        views.editorToolbar.visibility = View.GONE

        views.editorPhotomodeExit.visibility = View.VISIBLE
        views.editorPhotomodeExit.setOnLongClickListener {
            exitPhotoMode()
            true
        }

        val toast = Toast.makeText(activity, R.string.toast_msg_photo_mode_instruction, Toast.LENGTH_SHORT)
        toast.setGravity(Gravity.BOTTOM or Gravity.CENTER, 0, 0)
        toast.show()
    }

    @Suppress("DEPRECATION")
    private fun exitPhotoMode() {
        knifeText.isFocusable = true

        views.editorToolbar.visibility = View.VISIBLE

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity?.window?.setDecorFitsSystemWindows(true)
        } else {
            activity?.window?.decorView?.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }

        views.editorPhotomodeExit.visibility = View.GONE
    }

    fun onBackPressed() {
        warnUnsavedChanges {
            val act = activity
            if(act is MainActivity) {
                act.superBackPressed()
            }
        }
    }

}