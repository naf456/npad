package com.naf.npad

import android.app.ActivityOptions
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.ActionMode
import android.view.Menu
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import android.view.MenuItem
import androidx.preference.PreferenceManager
import com.naf.npad.databinding.ActivityEditorBinding

import com.naf.npad.dialog.WarnUnsavedChangesDialog

import java.io.BufferedOutputStream
import java.io.IOException
import java.util.Scanner

import io.github.mthli.knife.KnifeText

class EditorActivity : AppCompatActivity(), Toolbar.OnMenuItemClickListener {

    companion object {
        internal const val OPEN_DOCUMENT_REQUEST = 1
        internal const val SAVE_DOCUMENT_REQUEST = 2

        val DOCUMENT_TYPE_PLAIN_TEXT = ".txt"
        val DOCUMENT_TYPE_NPAD_ML = ".npml"
        val DEFAULT_DOCUMENT_TYPE = DOCUMENT_TYPE_NPAD_ML
    }

    private lateinit var views: ActivityEditorBinding
    private lateinit var knifeText : KnifeText

    private var currentDocumentUri : Uri? = null
    private var currentDocumentType = DEFAULT_DOCUMENT_TYPE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        views = ActivityEditorBinding.inflate(layoutInflater)
        knifeText = views.editorKnifeText
        setContentView(views.root)
        setupToolbar()
        setupSelectionStyling()
        resetEditor()
        applyFontSize()


    }

    private fun setupToolbar() {
        menuInflater.inflate(R.menu.activity_editor_menu, views.editorToolbar.menu)
        views.editorToolbar.setOnMenuItemClickListener(this)
    }

    private fun setupSelectionStyling() {
        knifeText.customSelectionActionModeCallback = object : ActionMode.Callback {
            override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                menuInflater.inflate(R.menu.knife_text_selection_styling, menu)
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
            R.id.editor_action_gotoSetting -> startSettings()
            R.id.editor_action_undo -> if (knifeText.undoValid()) knifeText.undo()
            R.id.editor_action_redo -> if (knifeText.redoValid()) knifeText.redo()
        }
        return true
    }

    private fun applyFontSize() {
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
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

    private fun startSettings() {
        val settingsIntent = Intent(this, SettingsActivity::class.java)
        val options = ActivityOptions.makeCustomAnimation(this, android.R.anim.fade_in, android.R.anim.fade_out)
        startActivity(settingsIntent, options.toBundle())
    }

    private fun newDocument() {
        warnUnsavedChanges { this@EditorActivity.resetEditor() }
    }

    private fun resetEditor() {
        currentDocumentUri = null
        currentDocumentType = DEFAULT_DOCUMENT_TYPE
        knifeText.setText("")
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

    private fun continueOpeningDocument(documentUri: Uri?) {
        documentUri?: return
        currentDocumentUri = documentUri

        val extension = Utls.Uri.getExtension(this@EditorActivity, documentUri)

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

            val resolver = contentResolver

            val inputStream = resolver.openInputStream(currentDocumentUri!!) ?: throw NullPointerException()

            val scanner = Scanner(inputStream).useDelimiter("\\A")
            val content = if (scanner.hasNext()) scanner.next() else ""

            inputStream.close()

            if (currentDocumentType == DOCUMENT_TYPE_NPAD_ML) {
                knifeText.fromHtml(content)
            } else {
                knifeText.setText(content)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Utls.toast(this, "Can't read document")
            resetEditor()
        }

    }

    private fun saveDocument() {

        if (currentDocumentUri == null) {
            saveDocumentAs()
            return
        }

        try {
            val resolver = contentResolver
            val outputStream = resolver.openOutputStream(currentDocumentUri!!) ?: throw IOException()

            val bufOutputStream = BufferedOutputStream(outputStream)
            val bytes: ByteArray
            if (currentDocumentType == DOCUMENT_TYPE_NPAD_ML) {
                bytes = knifeText.toHtml().toByteArray()
            } else {
                bytes = knifeText.text.toString().toByteArray()
            }
            bufOutputStream.write(bytes)
            bufOutputStream.close()

            Utls.toast(this, "Saved")

        } catch (e: IOException) {
            e.printStackTrace()
            Utls.toastLong(this, "Save Failed! Please try \"Save As...\".")
        } catch (e: SecurityException) {
            e.printStackTrace()
            Utls.toastLong(this, "Permission Error. Please try \"Save As...\".")
        }

    }

    private fun saveDocumentAs() {
        startSaveFilePicker()
    }

    private fun startSaveFilePicker() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.type = "*/*"
        intent.putExtra(Intent.EXTRA_TITLE, "document_name" + DOCUMENT_TYPE_NPAD_ML)
        val chooser = Intent.createChooser(intent, "Select Npad Document")
        startActivityForResult(chooser, SAVE_DOCUMENT_REQUEST)
    }

    private fun continueSavingDocument(documentUri: Uri?) {
        if (documentUri == null) return

        val extension = Utls.Uri.getExtension(this@EditorActivity, documentUri)

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

        if (OPEN_DOCUMENT_REQUEST == requestCode && RESULT_OK == resultCode) {
            val documentUri = intent!!.data
            continueOpeningDocument(documentUri)
        }

        if (SAVE_DOCUMENT_REQUEST == requestCode && RESULT_OK == resultCode) {
            val documentUri = intent!!.data
            continueSavingDocument(documentUri)
        }
    }

    private fun warnUnsavedChanges(onWarningFinished : ()->Unit) {
        val warningDialog = WarnUnsavedChangesDialog()
        warningDialog.onWarningFinished = onWarningFinished
        warningDialog.show(supportFragmentManager, null)
    }
}
