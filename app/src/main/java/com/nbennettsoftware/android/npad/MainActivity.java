package com.nbennettsoftware.android.npad;

import android.app.ActivityOptions;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Scanner;

public class MainActivity extends NpadActivity {

    private final int PICK_DOCUMENT_INTENT_ID = 1;
    private final int PICK_SAVE_FILE_INTENT_ID = 3;

    private boolean documentIsModified = false;
    private Uri currentDocumentUri;
    private String currentDocumentName;
    private EditText mainTextBox;
    private ImageView wallpaperImageView;
    private int defaultWallpaperResource = R.mipmap.stary_night;
    private StorageManager storageManager;
    private Utils utils;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mainTextBox = (EditText)findViewById(R.id.main_textBox);
        wallpaperImageView = (ImageView)findViewById(R.id.main_wallpaperImageView);
        storageManager = new StorageManager(this);
        utils = new Utils(this);

        setSupportActionBar((Toolbar)findViewById(R.id.main_toolbar));
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        mainTextBox.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void afterTextChanged(Editable s) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                documentIsModified=true;
            }
        });

        refreshUI();
        setWallpaperFullHeight();
    }

    void refreshUI(){
        utils.applyWallpaper(wallpaperImageView);
        utils.applyFontSize((TextView) findViewById(R.id.main_textBox));
        utils.applyShade(findViewById(R.id.main_overlay));
    }

    void setWallpaperFullHeight(){
        //Setting wallpaper to window size so
        Rect windowRect = new Rect();
        wallpaperImageView.getWindowVisibleDisplayFrame(windowRect);
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) wallpaperImageView.getLayoutParams();
        layoutParams.height = windowRect.height();
        wallpaperImageView.setLayoutParams(layoutParams);
    }

    private void setupKeyboardListener(){
        final ViewGroup mainFrame = (ViewGroup) findViewById(R.id.main_container);
        mainFrame.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Rect r = new Rect();
                //r will be populated with the coordinates of your view that area still visible.
                mainFrame.getWindowVisibleDisplayFrame(r);

                int heightDiff = mainFrame.getRootView().getHeight() - (r.bottom - r.top);
                if (heightDiff > 100) {
                    ViewGroup.LayoutParams layoutParams = mainFrame.getLayoutParams();
                } else {
                    Rect windowRect = new Rect();
                    getWindow().getDecorView().getWindowVisibleDisplayFrame(windowRect);
                    ViewGroup.LayoutParams layoutParams = mainFrame.getLayoutParams();
                    layoutParams.height =windowRect.bottom;
                    mainFrame.setLayoutParams(layoutParams);
                }
            }
        });

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
                unloadDocument();
                break;
            case (R.id.action_open):
                pickDocument();
                break;
            case (R.id.action_save):
                save();
                break;
            case (R.id.action_saveas):
                pickSaveFile();
                break;
            case (R.id.action_gotoSetting):
                ActivityOptions options = ActivityOptions.makeCustomAnimation(
                        this, android.R.anim.fade_in, android.R.anim.fade_out);
                startActivity(new Intent().setClass(this, SettingsActivity.class), options.toBundle());
                break;
        }
        return true;
    }

    private void save() {
        if(currentDocumentUri==null) {
            pickSaveFile();
            return;
        }
        ContentResolver resolver = getContentResolver();
        try{
            OutputStream outputStream = resolver.openOutputStream(currentDocumentUri);
            if(outputStream==null){ throw new IOException(); }
            BufferedOutputStream bufOutputStream = new BufferedOutputStream(outputStream);
            bufOutputStream.write(mainTextBox.getText().toString().getBytes());
            bufOutputStream.close();
            utils.toast("Saved");
        } catch (IOException e) {
            e.printStackTrace();
            utils.toast("Save Failed! Please try \"Save As...\".", Toast.LENGTH_LONG);
        } catch (SecurityException e) {
            e.printStackTrace();
            utils.toast("Permission Error. Please try \"Save As...\".", Toast.LENGTH_LONG);
        }

    }

    private void pickDocument() {
        final String TEXT_MIME_TYPE="text/*";
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType(TEXT_MIME_TYPE);
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        Intent chooser = Intent.createChooser(intent, "Select Document");

        if (chooser.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(chooser, PICK_DOCUMENT_INTENT_ID);
        } else {
            utils.toast("No apps installed.");
        }
    }

    private void pickSaveFile() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.setType("text/*");
        intent.putExtra(Intent.EXTRA_TITLE, "document_name.txt");

        Intent chooser = Intent.createChooser(intent, "Select Document");

        if (chooser.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(chooser, PICK_SAVE_FILE_INTENT_ID);
        } else {
            utils.toast("No apps installed.");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_DOCUMENT_INTENT_ID && resultCode == RESULT_OK) {
            Uri uri = data.getData();
            if(uri==null) { return; }
            loadDocument(uri);
        }
        if (requestCode == PICK_SAVE_FILE_INTENT_ID && resultCode == RESULT_OK) {
            Uri uri = data.getData();
            if(uri==null) { return; }
            currentDocumentUri=uri;
            save();
        }
    }

    private void unloadDocument() {
        currentDocumentUri = null;
        currentDocumentName = null;
        mainTextBox.setText("");
        documentIsModified=false;
    }

    private void loadDocument(Uri openableUri) {
        ContentResolver resolver = getContentResolver();
        try {
            InputStream inputStream = resolver.openInputStream(openableUri);
            if(inputStream==null){ throw new IOException(); }
            Scanner s = new Scanner(inputStream).useDelimiter("\\A");
            String content = s.hasNext() ? s.next() : "";
            inputStream.close();
            mainTextBox.setText(content);
            currentDocumentUri = openableUri;
            currentDocumentName = getDocumentDisplayName(openableUri);
            if(currentDocumentName==null) { currentDocumentName=""; }
        } catch (IOException e) {
            e.printStackTrace();
            utils.toast("Can't read document.");
        }
    }

    private String getDocumentDisplayName(Uri uri){
        String stringData=null;
        ContentResolver resolver = getContentResolver();
        String[] projection = {OpenableColumns.DISPLAY_NAME};
        Cursor cursor = resolver.query(uri,projection,null,null,null);
        if(cursor==null) { return null; }
        if(cursor.moveToFirst()){
            int columnId = cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME);
            stringData = cursor.getString(columnId);
        }
        cursor.close();
        return stringData;
    }
}
