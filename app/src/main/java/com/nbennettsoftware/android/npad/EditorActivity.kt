package com.nbennettsoftware.android.npad

import android.app.ActivityOptions
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import android.view.MenuItem
import androidx.preference.PreferenceManager

import com.nbennettsoftware.android.npad.dialog.WarnUnsavedChangesDialog

import java.io.BufferedOutputStream
import java.io.IOException
import java.util.Scanner

import io.github.mthli.knife.KnifeText
import kotlinx.android.synthetic.main.activity_editor.*

const val OPEN_DOCUMENT_REQUEST = 1
const val SAVE_DOCUMENT_REQUEST = 2

class EditorActivity : AppCompatActivity(), Toolbar.OnMenuItemClickListener {

    private var currentDocument = NpadDocument(null, null)
    private lateinit var photoMode : PhotoMode

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_editor)
        setupToolbar()
        resetEditor()
        applyFontSize()

        photoMode = PhotoMode(
                activity = this,
                rootView = editor_rootView,
                contentView = editor_contentView,
                controlsView = editor_controls,
                hideSystemWindows = true)
    }

    private fun setupToolbar() {
        menuInflater.inflate(R.menu.activity_editor_menu, editor_toolbar.menu)
        editor_toolbar.setOnMenuItemClickListener(this)
    }

    override fun onMenuItemClick(menuItem: MenuItem): Boolean {

        when (menuItem.itemId) {
            R.id.editor_action_new -> newDocument()
            R.id.editor_action_open -> openDocument()
            R.id.editor_action_save -> saveDocument(currentDocument)
            R.id.editor_action_save_as -> saveDocumentAs()
            R.id.editor_action_photoMode -> activatePhotomode()
            R.id.editor_action_gotoSetting -> startSettings()
            R.id.editor_action_text_bold -> editor_knifeText.bold(!editor_knifeText.contains(KnifeText.FORMAT_BOLD))
            R.id.editor_action_text_italic -> editor_knifeText.italic(!editor_knifeText.contains(KnifeText.FORMAT_ITALIC))
            R.id.editor_action_text_underline -> editor_knifeText.underline(!editor_knifeText.contains(KnifeText.FORMAT_UNDERLINED))
            R.id.editor_action_undo -> if (editor_knifeText.undoValid()) editor_knifeText.undo()
            R.id.editor_action_redo -> if (editor_knifeText.redoValid()) editor_knifeText.redo()
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
            editor_knifeText.textSize = fontSizeInt.toFloat()
        } catch (e: NumberFormatException) {
            e.printStackTrace()
        }

    }

    private fun activatePhotomode() {
        photoMode.enterPhotoMode()
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
        currentDocument = NpadDocument(null, null)
        editor_knifeText.setText("")
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
        if (documentUri == null) return

        val extension = Utls.Uri.getExtension(this@EditorActivity, documentUri)

        if (extension == NpadDocument.TYPE_NPAD_ML) {
            loadDocumentContent(NpadDocument(documentUri, NpadDocument.TYPE_NPAD_ML))
        } else {
            loadDocumentContent(NpadDocument(documentUri, NpadDocument.TYPE_PLAIN_TEXT))
        }
    }

    private fun loadDocumentContent(document: NpadDocument) {
        try {

            val resolver = contentResolver

            val inputStream = resolver.openInputStream(document.uri!!) ?: throw NullPointerException()

            val scanner = Scanner(inputStream).useDelimiter("\\A")
            val content = if (scanner.hasNext()) scanner.next() else ""

            inputStream.close()

            if (document.type == NpadDocument.TYPE_NPAD_ML) {
                editor_knifeText.fromHtml(content)
            } else {
                editor_knifeText.setText(content)
            }

            currentDocument = document

        } catch (e: Exception) {
            e.printStackTrace()
            Utls.toast(this, "Can't read document")
        }

    }

    private fun saveDocument(document: NpadDocument?) {

        if (document?.uri == null) {
            saveDocumentAs()
            return
        }

        try {
            val resolver = contentResolver
            val outputStream = resolver.openOutputStream(document.uri) ?: throw IOException()

            val bufOutputStream = BufferedOutputStream(outputStream)
            val bytes: ByteArray
            if (currentDocument.type == NpadDocument.TYPE_NPAD_ML) {
                bytes = editor_knifeText.toHtml().toByteArray()
            } else {
                bytes = editor_knifeText.text.toString().toByteArray()
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
        intent.putExtra(Intent.EXTRA_TITLE, "document_name" + NpadDocument.TYPE_NPAD_ML)
        val chooser = Intent.createChooser(intent, "Select Npad Document")
        startActivityForResult(chooser, SAVE_DOCUMENT_REQUEST)
    }

    private fun continueSavingDocument(documentUri: Uri?) {
        if (documentUri == null) return

        val extension = Utls.Uri.getExtension(this@EditorActivity, documentUri)

        if (extension == NpadDocument.TYPE_NPAD_ML) {
            saveDocument(NpadDocument(documentUri, NpadDocument.TYPE_NPAD_ML))
        } else {
            saveDocument(NpadDocument(documentUri, NpadDocument.TYPE_PLAIN_TEXT))
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
