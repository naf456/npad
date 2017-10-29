package com.nbennettsoftware.android.npad.storage;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.MediaStore;

import com.nbennettsoftware.android.npad.R;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class WallpaperManager {
    private Context context;
    private String prefs_key_internal_wallpaper_name;

    public WallpaperManager(Context context) {
        this.context = context;
        prefs_key_internal_wallpaper_name = context.getResources().getString(R.string.pref_key_internal_wallpaper_name);
    }

    public void replaceInternalizeWallpaper(Uri wallpaperUri) throws ReplaceInternalWallpaperException {
        try {
            ContentResolver resolver = context.getContentResolver();
            String displayName = getWallpaperFileName(wallpaperUri);

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
                    .putString(prefs_key_internal_wallpaper_name, displayName)
                    .apply();

        } catch (IOException | UriDataRetrievalException e ) {
            throw new ReplaceInternalWallpaperException();
        }
    }

    public class ReplaceInternalWallpaperException extends Exception {}

    public void deleteInternalizedWallpaper() {
        try {
            File internalWallpaper = getInternalizedWallpaper();
            internalWallpaper.delete();
            getPreferences().edit().remove(prefs_key_internal_wallpaper_name).apply();
        } catch (NoInternalWallpaperException e) {
            //Do nothing
        }
    }

    public class NoInternalWallpaperException extends Exception{};

    public File getInternalizedWallpaper() throws NoInternalWallpaperException {
        String fileName = getPreferences().getString(prefs_key_internal_wallpaper_name, null);
        if(fileName==null){ throw new NoInternalWallpaperException(); }
        String internalDirPath = context.getFilesDir().getAbsolutePath();
        return new File(internalDirPath, fileName);
    }

    private SharedPreferences getPreferences(){
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    private class UriDataRetrievalException extends Exception{}

    private String getWallpaperFileName(Uri uri) throws UriDataRetrievalException {
        String stringData=null;
        ContentResolver resolver = context.getContentResolver();
        String[] projection = {MediaStore.MediaColumns.DISPLAY_NAME};
        Cursor cursor = resolver.query(uri,projection,null,null,null);
        if(cursor==null) { throw new UriDataRetrievalException(); }
        if(cursor.moveToFirst()){
            int columnId = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME);
            stringData = cursor.getString(columnId);
        }
        cursor.close();
        return stringData;
    }

}
