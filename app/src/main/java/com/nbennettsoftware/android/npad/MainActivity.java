package com.nbennettsoftware.android.npad;

import android.app.ActivityOptions;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.nbennettsoftware.android.npad.dialog.WarnUnsavedChangesDialog;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Scanner;

import io.github.mthli.knife.KnifeText;

public class MainActivity extends AppCompatActivity {

    private final int OPEN_DOCUMENT_REQUEST = 1;
    private final int SAVE_DOCUMENT_REQUEST = 2;

    private KnifeText editor;

    private NpadDocument currentDocument = new NpadDocument(null, null);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_editor);

        setSupportActionBar((Toolbar) findViewById(R.id.editor_toolbar));
        getSupportActionBar().setTitle("");

        editor = findViewById(R.id.editor_knifeText);

        resetEditor();

        applyFontSize();
    }

    void applyFontSize() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String fontSizeKey = getString(R.string.pref_key_font_size);
        String fontSizeDefault = getString(R.string.pref_default_font_size);
        String fontSize = preferences.getString(fontSizeKey, fontSizeDefault);
        try {
            int fontSizeInt = Integer.parseInt(fontSize);
            editor.setTextSize(fontSizeInt);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_editor_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        super.onOptionsItemSelected(menuItem);

        switch (menuItem.getItemId()) {
            case R.id.editor_action_new:
                newDocument();
                break;
            case R.id.editor_action_open:
                openDocument();
                break;
            case R.id.editor_action_save:
                saveDocument(currentDocument);
                break;
            case R.id.editor_action_save_as:
                saveDocumentAs();
                break;
            case R.id.editor_action_gotoSetting:
                ActivityOptions options = ActivityOptions.makeCustomAnimation(
                        this, android.R.anim.fade_in, android.R.anim.fade_out);
                startActivity(new Intent().setClass(this, SettingsActivity.class), options.toBundle());
                break;
            case R.id.editor_action_text_bold:
                editor.bold(!editor.contains(KnifeText.FORMAT_BOLD));
                break;
            case R.id.editor_action_text_italic:
                editor.italic(!editor.contains(KnifeText.FORMAT_ITALIC));
                break;
            case R.id.editor_action_text_underline:
                editor.underline(!editor.contains(KnifeText.FORMAT_UNDERLINED));
                break;
            case R.id.editor_action_undo:
                if (editor.undoValid()) editor.undo();
                break;
            case R.id.editor_action_redo:
                if (editor.redoValid()) editor.redo();
                break;

        }
        return true;
    }

    private void newDocument() {
        warnUnsavedChanges(new WarnUnsavedChangesDialog.OnWarningFinished() {
            @Override
            public void _continue() {
                resetEditor();
            }
        });
    }

    void resetEditor(){
        currentDocument = new NpadDocument(null, null);
        editor.setText("");
    }

    void openDocument() {
        warnUnsavedChanges(new WarnUnsavedChangesDialog.OnWarningFinished() {
            @Override
            public void _continue() {
                startDocumentOpenActivity();
            }
        });
    }

    void startDocumentOpenActivity() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        Intent chooser = Intent.createChooser(intent, "Select NpadDocument (.txt or .npml)");

        if (chooser.resolveActivity(getPackageManager()) != null) {
            ActivityOptions options = ActivityOptions.makeCustomAnimation(
                    this, android.R.anim.fade_in, android.R.anim.fade_out);
            startActivityForResult(chooser, OPEN_DOCUMENT_REQUEST, options.toBundle());
        } else {
            Utls.toastLong(this, "No apps installed.");
        }
    }

    private void continueOpeningDocument(Uri documentUri) {
        if(documentUri==null) return;

        String extension = Utls._Uri.getExtension(MainActivity.this, documentUri);

        if(extension.equals(NpadDocument.TYPE_NPAD_ML)) {
            loadDocumentContent(new NpadDocument(documentUri, NpadDocument.TYPE_NPAD_ML));
        } else {
            loadDocumentContent(new NpadDocument(documentUri, NpadDocument.TYPE_PLAIN_TEXT));
        }
    }

    private void loadDocumentContent(NpadDocument document) {
        try {

            ContentResolver resolver = getContentResolver();

            InputStream inputStream = resolver.openInputStream(document.getUri());
                if(inputStream==null) throw new NullPointerException();

            Scanner scanner = new Scanner(inputStream).useDelimiter("\\A");
            String content = scanner.hasNext() ? scanner.next() : "";

            inputStream.close();

            if(document.getType().equals(NpadDocument.TYPE_NPAD_ML)) {
                editor.fromHtml(content);
            } else {
                editor.setText(content);
            }

            currentDocument = document;

        } catch (Exception e) {
            e.printStackTrace();
            Utls.toast(this, "Can't read document");
        }
    }

    private void saveDocument(NpadDocument document) {

        if(document == null || document.getUri() == null) {
            saveDocumentAs();
            return;
        }

        try{
            ContentResolver resolver = getContentResolver();
            OutputStream outputStream = resolver.openOutputStream(currentDocument.getUri());
            if(outputStream == null){ throw new IOException(); }

            BufferedOutputStream bufOutputStream = new BufferedOutputStream(outputStream);
            byte[] bytes;
            if(currentDocument.getType().equals(NpadDocument.TYPE_NPAD_ML)) {
                bytes = editor.toHtml().getBytes();
            } else {
                bytes = editor.getText().toString().getBytes();
            }
            bufOutputStream.write(bytes);
            bufOutputStream.close();

            Utls.toast(this, "Saved");

        } catch (IOException e) {
            e.printStackTrace();
            Utls.toastLong(this, "Save Failed! Please try \"Save As...\".");
        } catch (SecurityException e) {
            e.printStackTrace();
            Utls.toastLong(this,"Permission Error. Please try \"Save As...\".");
        }

    }

    void saveDocumentAs(){ startSaveActivity(); }

    void startSaveActivity() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_TITLE, "document_name" + NpadDocument.TYPE_NPAD_ML);
        Intent chooser = Intent.createChooser(intent, "Select Npad Document");
        startActivityForResult(chooser, SAVE_DOCUMENT_REQUEST);
    }

    private void continueSavingDocument(Uri documentUri){
        if(documentUri==null) return;

        String extension = Utls._Uri.getExtension(MainActivity.this, documentUri);

        if(extension.equals(NpadDocument.TYPE_NPAD_ML)) {
            saveDocument(new NpadDocument(documentUri, NpadDocument.TYPE_NPAD_ML));
        } else {
            saveDocument(new NpadDocument(documentUri, NpadDocument.TYPE_PLAIN_TEXT));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if(OPEN_DOCUMENT_REQUEST == requestCode && RESULT_OK == resultCode) {
            Uri documentUri = intent.getData();
            continueOpeningDocument(documentUri);
        }

        if(SAVE_DOCUMENT_REQUEST == requestCode && RESULT_OK == resultCode) {
            Uri documentUri = intent.getData();
            continueSavingDocument(documentUri);
        }
    }

    void warnUnsavedChanges(WarnUnsavedChangesDialog.OnWarningFinished onWarningFinished){
        WarnUnsavedChangesDialog warningDialog = new WarnUnsavedChangesDialog();
        warningDialog.setOnWarningFinished(onWarningFinished);
        warningDialog.show(getSupportFragmentManager(), null);
    }
}
