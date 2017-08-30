package com.nbennettsoftware.android.npad;

import android.app.ActivityOptions;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.nbennettsoftware.android.npad.storage.WallpaperManager;
import com.nbennettsoftware.android.npad.widget.FullScreenImageView;
import com.nbennettsoftware.android.npad.widget.NpadEditText;
import com.nbennettsoftware.android.npad.widget.NpadScrollView;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Scanner;

public class MainActivity extends NpadActivity {

    private final int PICK_DOCUMENT_INTENT_ID = 1;
    private final int PICK_SAVE_FILE_INTENT_ID = 3;

    private Uri currentDocumentUri;
    private NpadEditText npadEditText;
    private FullScreenImageView wallpaperImageView;
    private int defaultWallpaperResource = R.mipmap.stary_night;
    private WallpaperManager wallpaperManager;
    private Utils utils;
    private KeyboardDodger keyboardDodger;

    private OnDocumentSavedListener onDocumentSavedListener;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        utils = new Utils(this);
        npadEditText = (NpadEditText)findViewById(R.id.main_npadEditText);
        wallpaperImageView = (FullScreenImageView)findViewById(R.id.main_wallpaperImageView);
        wallpaperManager = new WallpaperManager(this);

        NpadScrollView npadScrollView = (NpadScrollView)findViewById(R.id.main_npadScrollView);
        npadScrollView.setFocusedViewOnClick(npadEditText);

        keyboardDodger = new KeyboardDodger(this, (ViewGroup)findViewById(R.id.mainFrame).getParent());

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setSupportActionBar((Toolbar)findViewById(R.id.main_toolbar));
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        refreshUI();
    }

    void setupEditorControls(){
    }

    void refreshUI(){
        utils.applyWallpaper(wallpaperImageView);
        utils.updateWallpaperLayout(wallpaperImageView);
        utils.applyFontSize((TextView) findViewById(R.id.main_npadEditText));
        utils.applyShade(findViewById(R.id.main_overlay));
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshUI();
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        switch(item.getItemId()) {
            case (R.id.action_new):
                if(npadEditText.needsSaving()) {
                    SaveFileDialog saveFileDialog = new SaveFileDialog();
                    saveFileDialog.setOnSaveDialogFinished(new SaveFileDialog.OnSaveDialogFinished() {
                        @Override
                        public void doSave() {
                            setOnDocumentSavedListener(new OnDocumentSavedListener() {
                                @Override
                                public void onSave() {
                                    clearOnDocumentSavedListener();
                                    unloadDocument();
                                }
                            });
                            saveDocument();
                        }

                        @Override
                        public void doContinue() {
                            unloadDocument();
                        }
                    });
                    saveFileDialog.show(getSupportFragmentManager(), null);
                } else {
                    unloadDocument();
                }
                break;
            case (R.id.action_open):
                if(npadEditText.needsSaving()) {
                    SaveFileDialog saveFileDialog = new SaveFileDialog();
                    saveFileDialog.setOnSaveDialogFinished(new SaveFileDialog.OnSaveDialogFinished() {
                        @Override
                        public void doSave() {
                            setOnDocumentSavedListener(new OnDocumentSavedListener() {
                                @Override
                                public void onSave() {
                                    clearOnDocumentSavedListener();
                                    openDocument();
                                }
                            });
                            saveDocument();
                        }

                        @Override
                        public void doContinue() {
                            openDocument();
                        }
                    });
                    saveFileDialog.show(getSupportFragmentManager(), null);
                } else {
                    openDocument();
                }
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

    private void unloadDocument() {
        currentDocumentUri = null;
        npadEditText.setText("");
        npadEditText.notifySave();
    }

    private void openDocument() {
        addActivityResultListener(new OnActivityResultListener() {
            @Override
            public void OnActivityResult(int requestCode, int resultCode, Intent data) {
                if (requestCode == PICK_DOCUMENT_INTENT_ID && resultCode == RESULT_OK) {
                    Uri uri = data.getData();
                    if(uri==null) { return; }
                    loadDocument(uri);
                }
                removeActivityResultListener(this);
            }
        });
        EditorUtils.startDocumentPickerForResult(this, PICK_DOCUMENT_INTENT_ID);
    }

    private void saveDocument() {
        if(currentDocumentUri==null) {
            saveDocumentAs();
            return;
        }
        ContentResolver resolver = getContentResolver();
        try{
            OutputStream outputStream = resolver.openOutputStream(currentDocumentUri);
            if(outputStream==null){ throw new IOException(); }
            BufferedOutputStream bufOutputStream = new BufferedOutputStream(outputStream);
            bufOutputStream.write(npadEditText.getText().toString().getBytes());
            bufOutputStream.close();
            utils.toast("Saved");
            npadEditText.notifySave();
            if(onDocumentSavedListener != null) {
                onDocumentSavedListener.onSave();
            }
        } catch (IOException e) {
            e.printStackTrace();
            utils.toast("Save Failed! Please try \"Save As...\".", Toast.LENGTH_LONG);
        } catch (SecurityException e) {
            e.printStackTrace();
            utils.toast("Permission Error. Please try \"Save As...\".", Toast.LENGTH_LONG);
        }

    }

    private void saveDocumentAs(){
        addActivityResultListener(new OnActivityResultListener() {
            @Override
            public void OnActivityResult(int requestCode, int resultCode, Intent data) {
                if (requestCode == PICK_SAVE_FILE_INTENT_ID && resultCode == RESULT_OK) {
                    Uri uri = data.getData();
                    if(uri==null) { return; }
                    currentDocumentUri=uri;
                    saveDocument();
                }
                removeActivityResultListener(this);
            }
        });
        EditorUtils.startSaveFilePickerForResult(this, PICK_SAVE_FILE_INTENT_ID);
    }

    interface OnDocumentSavedListener {
        void onSave();
    }

    void setOnDocumentSavedListener(OnDocumentSavedListener onDocumentSavedListener) {
        this.onDocumentSavedListener = onDocumentSavedListener;
    }

    void clearOnDocumentSavedListener(){
        this.onDocumentSavedListener = null;
    }

    private void loadDocument(Uri openableUri) {
        ContentResolver resolver = getContentResolver();
        try {
            InputStream inputStream = resolver.openInputStream(openableUri);
            if(inputStream==null){ throw new IOException(); }
            Scanner s = new Scanner(inputStream).useDelimiter("\\A");
            String content = s.hasNext() ? s.next() : "";
            inputStream.close();
            npadEditText.setText(content);
            currentDocumentUri = openableUri;
            npadEditText.notifySave();
        } catch (IOException e) {
            e.printStackTrace();
            utils.toast("Can't read document.");
        }
    }

    @Override
    public void onBackPressed() {
        if(npadEditText.needsSaving()) {
            SaveFileDialog saveFileDialog = new SaveFileDialog();
            saveFileDialog.setOnSaveDialogFinished(new SaveFileDialog.OnSaveDialogFinished() {
                @Override
                public void doSave() {
                    setOnDocumentSavedListener(new OnDocumentSavedListener() {
                        @Override
                        public void onSave() {
                            MainActivity.super.onBackPressed();
                        }
                    });
                    saveDocument();
                }

                @Override
                public void doContinue() {
                    MainActivity.super.onBackPressed();
                }
            });
            saveFileDialog.show(getSupportFragmentManager(), null);
        } else {
            super.onBackPressed();
        }
    }


}
