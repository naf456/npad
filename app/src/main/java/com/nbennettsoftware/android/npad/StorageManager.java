package com.nbennettsoftware.android.npad;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

class StorageManager {
    private Context context;
    private final String SHARED_PREFS_NAME=this.getClass().getName();
    private final String PREFS_ID_INTERNAL_WALLPAPER_NAME="internal_wallpaper_name";

    StorageManager(Context context) {
        this.context = context;
    }

    void replaceInternalizeWallpaper(Uri wallpaperUri) throws ReplaceInternalWallpaperException {
        try {
            ContentResolver resolver = context.getContentResolver();
            String displayName = getDisplayNameFromUri(wallpaperUri);

            deleteInternalizedWallpaper();

            //Copy the wallpaper to internal storage
            OutputStream outputStream = context.openFileOutput(displayName,0);
            InputStream inputStream = resolver.openInputStream(wallpaperUri);

            //Once an number of bytes are read, write said bytes to output.
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1 ){
                outputStream.write(buffer, 0, bytesRead);
            }

            outputStream.close();
            inputStream.close();

            //Store new wallpaper name in prefs.
            getPreferences().edit()
                    .putString(PREFS_ID_INTERNAL_WALLPAPER_NAME, displayName)
                    .apply();

        } catch (IOException | UriDataRetrievalException e ) {
            throw new ReplaceInternalWallpaperException();
        }
    }

    class ReplaceInternalWallpaperException extends Exception {}

    void deleteInternalizedWallpaper() {
        try {
            File internalWallpaper = getInternalizedWallpaper();
            internalWallpaper.delete();
            getPreferences().edit().remove(PREFS_ID_INTERNAL_WALLPAPER_NAME).apply();
        } catch (NoInternalWallpaperException e) {
            //Do nothing
        }
    }

    class NoInternalWallpaperException extends Exception{};

    File getInternalizedWallpaper() throws NoInternalWallpaperException {
        String fileName = getPreferences().getString(PREFS_ID_INTERNAL_WALLPAPER_NAME, null);
        if(fileName==null){ throw new NoInternalWallpaperException(); }
        String internalDirPath = context.getFilesDir().getAbsolutePath();
        return new File(internalDirPath, fileName);
    }

    private SharedPreferences getPreferences(){
        return context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
    }

    private class UriDataRetrievalException extends Exception{}

    private String getDisplayNameFromUri(Uri uri) throws UriDataRetrievalException {
        try {
            return getStringFromUri(uri, OpenableColumns.DISPLAY_NAME);
        } catch (BadCursorException e) {
            throw new UriDataRetrievalException();
        }
    }

    private class BadCursorException extends Exception{}

    private String getStringFromUri(Uri uri, String columnName) throws BadCursorException {
        String stringData=null;
        ContentResolver resolver = context.getContentResolver();
        String[] projection = {columnName};
        Cursor cursor = resolver.query(uri,projection,null,null,null);
        if(cursor==null) { throw new BadCursorException(); }
        if(cursor.moveToFirst()){
            int columnId = cursor.getColumnIndexOrThrow(columnName);
            stringData = cursor.getString(columnId);
        }
        cursor.close();
        return stringData;
    }

}
