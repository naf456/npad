package com.naf.npad

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.naf.npad.databinding.FragmentEditorBinding
import com.naf.npad.dialog.WarnUnsavedChangesDialog
import io.github.mthli.knife.KnifeText
import java.io.BufferedOutputStream
import java.io.IOException
import java.util.*


open class EditorFragment : Fragment(), Toolbar.OnMenuItemClickListener {

    companion object {
        internal const val OPEN_DOCUMENT_REQUEST = 1
        internal const val SAVE_DOCUMENT_REQUEST = 2

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
        history.startRecording()
    }

    private fun openDocument() {
        warnUnsavedChanges { startDocumentPicker() }
    }

    private fun startDocumentPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.type = "*/*"
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        val intentChooser = Intent.createChooser(intent, "Select Npad Document (.txt or .npml)")

        startActivityForResult(intentChooser, OPEN_DOCUMENT_REQUEST)
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
            history.startRecording()
        } catch (e: Exception) {
            e.printStackTrace()
            Utls.toast(requireContext(), "Can't read document")
            resetEditor()
        }

    }

    private fun continueOpeningDocument(documentUri: Uri?) {
        documentUri?: return

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

    private fun saveDocument() {

        if (currentDocumentUri == null) {
            saveDocumentAs()
            return
        }

        try {
            val resolver = requireActivity().contentResolver
            val outputStream = resolver.openOutputStream(currentDocumentUri!!) ?: throw IOException()

            val bufOutputStream = BufferedOutputStream(outputStream)
            val bytes: ByteArray = if (currentDocumentType == DOCUMENT_TYPE_NPAD_ML)
                knifeText.toHtml().toByteArray()
            else
                knifeText.text.toString().toByteArray()
            bufOutputStream.write(bytes)
            bufOutputStream.close()

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
        startSaveFilePicker()
    }

    private fun startSaveFilePicker() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.type = "*/*"
        intent.putExtra(Intent.EXTRA_TITLE, "document_name$DOCUMENT_TYPE_NPAD_ML")
        val chooser = Intent.createChooser(intent, "Select Npad Document")
        startActivityForResult(chooser, SAVE_DOCUMENT_REQUEST)
    }

    private fun continueSavingDocument(documentUri: Uri?) {
        if (documentUri == null) return

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

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)

        if (OPEN_DOCUMENT_REQUEST == requestCode && AppCompatActivity.RESULT_OK == resultCode) {
            val documentUri = intent!!.data
            continueOpeningDocument(documentUri)
        }

        if (SAVE_DOCUMENT_REQUEST == requestCode && AppCompatActivity.RESULT_OK == resultCode) {
            val documentUri = intent!!.data
            continueSavingDocument(documentUri)
        }
    }

    private fun warnUnsavedChanges(onWarningFinished: () -> Unit) {
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
}