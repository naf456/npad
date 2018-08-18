package com.nbennettsoftware.android.npad;

import android.app.ActivityOptions;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import com.nbennettsoftware.android.npad.widgets.SaveFileDialog;
import com.nbennettsoftware.android.npad.widgets.WallpaperView;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class EditorActivity extends NpadActivity {

    private final int PICK_DOCUMENT_INTENT_ID = 1;
    private final int PICK_SAVE_FILE_INTENT_ID = 2;

    private EditText mEditor;
    private WallpaperView mWallpaperView;

    private Uri mCurrentDocumentUri;
    private boolean mCurrentDocumentNeedsSaving;

    private List<OnDocumentSaveListener> onDocumentSaveListeners = new ArrayList<>();

    private TextWatcher mRequireSavingOnText = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            mCurrentDocumentNeedsSaving = true;
        }
        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) { }
        @Override
        public void afterTextChanged(Editable editable) { }
    };

    void toast(String msg) {
        Toast toast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.TOP | Gravity.CENTER, 0,0);
        toast.show();
    }

    void toastLong(String msg){
        Toast toast = Toast.makeText(this, msg, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.TOP | Gravity.CENTER, 0,0);
        toast.show();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_editor);

        mEditor = findViewById(R.id.editor_npadEditText);
        mWallpaperView = findViewById(R.id.editor_wallpaperImageView);

        mEditor.addTextChangedListener(mRequireSavingOnText);

        setSupportActionBar((Toolbar)findViewById(R.id.editor_toolbar));
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        clearDocument();

        configureUI();
    }

    void configureUI(){
        mWallpaperView.applyWallpaperFromPreferences();
        mWallpaperView.applyWallpaperDimmerFromPreferences();
        mWallpaperView.applyScalingFromPreferences();
        applyFontSize();
    }

    void applyFontSize() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String fontSizeKey = getString(R.string.pref_key_font_size);
        String fontSizeDefault = getString(R.string.pref_default_font_size);
        String fontSize = preferences.getString(fontSizeKey, fontSizeDefault);
        try {
            Integer fontSizeInt = Integer.parseInt(fontSize);
            mEditor.setTextSize(fontSizeInt);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        configureUI();
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

        switch(menuItem.getItemId()) {
            case (R.id.action_new):
                newDocument();
                break;
            case (R.id.action_open):
                openDocument();
                break;
            case (R.id.action_save):
                saveDocument();
                break;
            case (R.id.action_saveas):
                saveDocumentAs();
                break;
            case (R.id.action_gotoSetting):
                ActivityOptions options = ActivityOptions.makeCustomAnimation(
                        this, android.R.anim.fade_in, android.R.anim.fade_out);
                startActivity(new Intent().setClass(this, SettingsActivity.class), options.toBundle());
                break;

        }
        return true;
    }


    void askUserToSaveDocument(SaveFileDialog.OnSaveDialogFinished onSaveDialogFinished){
        SaveFileDialog saveFileDialog = new SaveFileDialog();
        saveFileDialog.setOnSaveDialogFinished(onSaveDialogFinished);
        saveFileDialog.show(getSupportFragmentManager(), null);
    }

    private void newDocument() {
        if(mCurrentDocumentNeedsSaving) {
            askUserToSaveDocument(new SaveFileDialog.OnSaveDialogFinished() {
                @Override
                public void Save() {
                    addOnDocumentSavedListener(new OnDocumentSaveListener() {
                        @Override
                        void onSaved() {
                            clearDocument();
                            removeOnDocumentSaveListener(this);
                        }
                    });
                    saveDocument();
                }

                @Override
                public void doNotSave() {
                    clearDocument();
                }
            });
        } else {
            clearDocument();
        }
    }

     private void clearDocument() {
        mCurrentDocumentUri = null;
        mEditor.setText("");
        //Blank documents considered useless
        mCurrentDocumentNeedsSaving = false;
    }



    private void openDocument() {

        this.addOnActivityResultListener(new OnActivityResultListener() {
            @Override
            public void OnActivityResult(int requestCode, int resultCode, Intent data) {
                if (requestCode == PICK_DOCUMENT_INTENT_ID && resultCode == RESULT_OK) {
                    Uri uri = data.getData();
                    if(uri==null) { return; }
                    loadDocument(uri);
                }
                EditorActivity.this.removeOnActivityResultListener(this);
            }
        });

        if(mCurrentDocumentNeedsSaving) {
            askUserToSaveDocument(new SaveFileDialog.OnSaveDialogFinished() {
                @Override
                public void Save() {
                    addOnDocumentSavedListener(new OnDocumentSaveListener() {
                        @Override
                        void onSaved() {
                            startDocumentPickerForResult();
                            removeOnDocumentSaveListener(this);
                        }
                    });
                    saveDocument();

                }

                @Override
                public void doNotSave() {
                    startDocumentPickerForResult();
                }
            });
        } else {
            startDocumentPickerForResult();
        }

    }

    void startDocumentPickerForResult() {
        final String TEXT_MIME_TYPE="text/*";
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType(TEXT_MIME_TYPE);
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        Intent chooser = Intent.createChooser(intent, "Select Document");

        if (chooser.resolveActivity(getPackageManager()) != null) {
            ActivityOptions options = ActivityOptions.makeCustomAnimation(
                    this, android.R.anim.fade_in, android.R.anim.fade_out);
            startActivityForResult(chooser, PICK_DOCUMENT_INTENT_ID, options.toBundle());
        } else {
            toastLong("No apps installed.");
        }
    }

    private void loadDocument(Uri openableUri) {
        ContentResolver resolver = getContentResolver();
        try {
            InputStream inputStream = resolver.openInputStream(openableUri);
            if(inputStream==null){ throw new IOException(); }

            Scanner s = new Scanner(inputStream).useDelimiter("\\A");
            String content = s.hasNext() ? s.next() : "";

            inputStream.close();

            mEditor.setText(content);
            mCurrentDocumentUri = openableUri;
            mCurrentDocumentNeedsSaving =false;

        } catch (IOException e) {
            e.printStackTrace();
            toast("Can't read document");
        }
    }

    private abstract class OnDocumentSaveListener {
        abstract void onSaved();
    }

    private void saveDocument() {

        if(mCurrentDocumentUri == null) {
            saveDocumentAs();
            return;
        }

        ContentResolver resolver = getContentResolver();
        try{
            OutputStream outputStream = resolver.openOutputStream(mCurrentDocumentUri);
            if(outputStream == null){ throw new IOException(); }

            BufferedOutputStream bufOutputStream = new BufferedOutputStream(outputStream);
            bufOutputStream.write(mEditor.getText().toString().getBytes());
            bufOutputStream.close();

            mCurrentDocumentNeedsSaving = false;

            for(OnDocumentSaveListener l : onDocumentSaveListeners) {
                l.onSaved();
            }

            toast("Saved");

        } catch (IOException e) {
            e.printStackTrace();
            toastLong("Save Failed! Please try \"Save As...\".");
        } catch (SecurityException e) {
            e.printStackTrace();
            toastLong("Permission Error. Please try \"Save As...\".");
        }

    }

    private void saveDocumentAs(){
        addOnActivityResultListener(new OnActivityResultListener() {
            @Override
            public void OnActivityResult(int requestCode, int resultCode, Intent data) {
                if (requestCode == PICK_SAVE_FILE_INTENT_ID && resultCode == RESULT_OK) {
                    Uri uri = data.getData();
                    if(uri==null) { return; }
                    mCurrentDocumentUri =uri;
                    saveDocument();
                }
                removeOnActivityResultListener(this);
            }
        });
        startSaveFilePickerForResult();
    }

    void startSaveFilePickerForResult() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.setType("text/*");
        intent.putExtra(Intent.EXTRA_TITLE, "document_name.txt");

        Intent chooser = Intent.createChooser(intent, "Select Document");

        if (chooser.resolveActivity(getPackageManager()) != null) {
            ActivityOptions options = ActivityOptions.makeCustomAnimation(
                    this, android.R.anim.fade_in, android.R.anim.fade_out);
            startActivityForResult(chooser, PICK_SAVE_FILE_INTENT_ID, options.toBundle());
        } else {
            toastLong("No apps installed.");
        }
    }

    private void addOnDocumentSavedListener(OnDocumentSaveListener listener){
        onDocumentSaveListeners.add(listener);
    }

    private void removeOnDocumentSaveListener(OnDocumentSaveListener listener){
        onDocumentSaveListeners.remove(listener);
    }

    @Override
    public void onBackPressed() {
        if(mCurrentDocumentNeedsSaving) {
            askUserToSaveDocument(new SaveFileDialog.OnSaveDialogFinished() {
                @Override
                public void Save() {
                    saveDocument();
                    EditorActivity.super.onBackPressed();
                }

                @Override
                public void doNotSave() {
                    EditorActivity.super.onBackPressed();
                }
            });
        } else {
            super.onBackPressed();
        }
    }
}
