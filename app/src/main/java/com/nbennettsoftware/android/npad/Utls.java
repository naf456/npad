package com.nbennettsoftware.android.npad;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.Gravity;
import android.widget.Toast;

public class Utls {

    static void toast(Context context, String msg) {
        Toast toast = Toast.makeText(context, msg, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.TOP | Gravity.CENTER, 0,0);
        toast.show();
    }

    static void toastLong(Context context, String msg){
        Toast toast = Toast.makeText(context, msg, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.TOP | Gravity.CENTER, 0,0);
        toast.show();
    }

    static class _Uri {
        static String getDisplayName(Context context, android.net.Uri uri){
            String[] fileDisplayNameColumn = {MediaStore.Files.FileColumns.DISPLAY_NAME};

            Cursor cursor = context.getContentResolver().query(uri, fileDisplayNameColumn, null, null, null);
            if(cursor == null) {return null;}

            if (cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndex(fileDisplayNameColumn[0]);
                return cursor.getString(columnIndex);
            }
            cursor.close();
            return null;
        }

        static String getExtension(Context context, android.net.Uri uri){
            String displayName = getDisplayName(context, uri);
            if(displayName == null) { return ""; }
            String extension = displayName.substring(displayName.lastIndexOf("."));
            return (extension!=null)? extension : "";
        }
    }
}
