package com.nbennettsoftware.android.npad;

import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.view.GravityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedOutputStream;
import java.util.Scanner;

public class MainActivity extends AppCompatActivity {

    private final int GET_DOCUMENT_INTENT_ID = 1;
    private final int GET_IMAGE_INTENT_ID = 2;
    private final int GET_SAVE_INTENT_ID = 3;
    private final String SHARED_PREFS_TAG=this.getClass().getName();
    private final String PREFS_WALLPAPER_URI_TAG="wallpaperUri";
    private final String PREFS_WALLPAPER_NAME_ID= "WallpaperName";

    private boolean askToSave = false;
    private Uri currentDocumentUri;
    private EditText masterTextBox;
    private ImageView wallpaperImageView;
    private Toolbar masterToolbar;
    private int defaultWallpaperResource = R.mipmap.landmark_bridge_cliff_california;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        masterToolbar = (Toolbar)findViewById(R.id.masterToolbar);
        setSupportActionBar(masterToolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        masterTextBox = (EditText)findViewById(R.id.masterTextBox);
        wallpaperImageView = (ImageView)findViewById(R.id.wallpaperImageView);

        loadWallpaper();
        configureWallpaperImageView();

        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE);
    }

    void configureWallpaperImageView(){
        //Setting wallpaper to window size so
        Rect windowRect = new Rect();
        wallpaperImageView.getWindowVisibleDisplayFrame(windowRect);
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) wallpaperImageView.getLayoutParams();
        layoutParams.height = windowRect.height();
        wallpaperImageView.setLayoutParams(layoutParams);
    }

    private void storeWallpaperUri(Uri wallpaperUri){
        SharedPreferences preferences = getSharedPreferences(SHARED_PREFS_TAG, Context.MODE_PRIVATE);
        if(wallpaperUri==null) {
            wallpaperUri=Uri.parse("");
        }
        preferences.edit()
                .putString(PREFS_WALLPAPER_URI_TAG, wallpaperUri.toString())
                .commit();
    }

    private class NoWallpaperUriException extends Exception{}

    private Uri fetchWallpaperUri() throws NoWallpaperUriException {
        SharedPreferences preferences = getSharedPreferences(SHARED_PREFS_TAG, Context.MODE_PRIVATE);
        String UriPath = preferences.getString(PREFS_WALLPAPER_URI_TAG, null);
        if(UriPath==null || UriPath.length() <= 0) {
            throw new NoWallpaperUriException();
        }
        return Uri.parse(UriPath);
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

        String TEXT_MIME_TYPE="text/*";
        String IMAGE_MIME_TYPE="image/*";

        switch(item.getItemId()) {
            case (R.id.action_new):
                currentDocumentUri = null;
                masterTextBox.setText("");
                break;
            case (R.id.action_open):
                askForFile(TEXT_MIME_TYPE, GET_DOCUMENT_INTENT_ID);
                break;
            case (R.id.action_save):
                if(currentDocumentUri==null) {
                    askToSaveDocument(GET_SAVE_INTENT_ID);
                } else {
                    save();
                }
                break;
            case (R.id.action_saveas):
                askToSaveDocument(GET_SAVE_INTENT_ID);
                break;
            case (R.id.action_pickWallpaper):
                askForFile(IMAGE_MIME_TYPE, GET_IMAGE_INTENT_ID);
                break;
            case (R.id.action_clearWallpaper):
                storeWallpaperUri(null);
                loadWallpaper();
                break;
            default:
                break;
        }
        return true;
    }

    private void save() {
        if(currentDocumentUri==null) {
            askToSaveDocument(GET_SAVE_INTENT_ID);
        }
        ContentResolver resolver = getContentResolver();
        try{
            OutputStream outputStream = resolver.openOutputStream(currentDocumentUri);
            BufferedOutputStream bufOutputStream = new BufferedOutputStream(outputStream);
            bufOutputStream.write(masterTextBox.getText().toString().getBytes());
            bufOutputStream.flush();
            bufOutputStream.close();
            toast("Saved");
        } catch (IOException e) {
            e.printStackTrace();
            toast("Cannot Save!!, Sorry...");
        }

    }

    private void askToSaveDocument(int intentId) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.setType("text/*");
        intent.putExtra(Intent.EXTRA_TITLE, "My Document.txt");

        Intent chooser = Intent.createChooser(intent, "Select File");

        if (chooser.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(chooser, intentId);
        } else {
            toast("No apps installed to handle pick request");
        }
    }

    private void askForFile(String mimeType, int intentId) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType(mimeType);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);

        Intent chooser = Intent.createChooser(intent, "Select File");

        if (chooser.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(chooser, intentId);
        } else {
            toast("No apps installed to handle pick request");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == GET_IMAGE_INTENT_ID && resultCode == RESULT_OK) {
            Uri uri = data.getData();
            if(uri!=null) {
                storeWallpaperUri(uri);
                loadWallpaper();
            }
        }
        if (requestCode == GET_DOCUMENT_INTENT_ID && resultCode == RESULT_OK) {
            Uri uri = data.getData();
            if(uri!=null) {
                loadDocument(uri);
            }
        }
        if (requestCode == GET_SAVE_INTENT_ID && resultCode == RESULT_OK) {
            Uri uri = data.getData();
            if(uri!=null) {
                currentDocumentUri=uri;
                save();
            }
        }
    }

    private void loadWallpaper() {
        try{
            Uri wallpaperUri = fetchWallpaperUri();
            wallpaperImageView.setImageURI(wallpaperUri);
        } catch (NoWallpaperUriException e) {
            wallpaperImageView.setImageResource(defaultWallpaperResource);
        }
    }

    private void loadDocument(Uri openableUri) {
        ContentResolver resolver = getContentResolver();
        try {
            InputStream inputStream = resolver.openInputStream(openableUri);
            Scanner s = new Scanner(inputStream).useDelimiter("\\A");
            String content = s.hasNext() ? s.next() : "";
            masterTextBox.setText(content);
            currentDocumentUri = openableUri;
        } catch (IOException e) {
            toast("Oops, can't load document!");
        }
    }

    private void toast(String msg) {
        Toast toast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.TOP | Gravity.CENTER, 0,0);
        toast.show();
    }



}
