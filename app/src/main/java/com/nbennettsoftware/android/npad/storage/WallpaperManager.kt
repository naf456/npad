package com.nbennettsoftware.android.npad.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import android.net.Uri
import android.provider.MediaStore

import com.nbennettsoftware.android.npad.R

import java.io.File
import java.io.IOException

class WallpaperManager(private val context: Context) {

    private val prefsKeyInternalWallpaperName: String = context.resources.getString(R.string.pref_key_internal_wallpaper_name)

    val internalizedWallpaper: File
        @Throws(NoInternalWallpaperException::class)
        get() {
            val fileName = preferences.getString(prefsKeyInternalWallpaperName, null)
                    ?: throw NoInternalWallpaperException()
            val internalDirPath = context.filesDir.absolutePath
            return File(internalDirPath, fileName)
        }

    private val preferences: SharedPreferences
        get() = PreferenceManager.getDefaultSharedPreferences(context)

    @Throws(ReplaceInternalWallpaperException::class)
    fun replaceInternalizeWallpaper(wallpaperUri: Uri) {
        try {
            deleteInternalizedWallpaper()

            val displayName = getWallpaperFileName(wallpaperUri)
            val contentResolver = context.contentResolver

            //Copy the wallpaper to internal storage
            val outputStream = context.openFileOutput(displayName, 0)
            val inputStream = contentResolver.openInputStream(wallpaperUri)

            inputStream.use { input ->
                outputStream.use { output ->
                    input!!.copyTo(output)
                }
            }

            outputStream.close()
            inputStream!!.close()

            //Store new wallpaper name in prefs.
            preferences.edit()
                    .putString(prefsKeyInternalWallpaperName, displayName)
                    .apply()
        } catch (e: IOException) {
            throw ReplaceInternalWallpaperException()
        } catch (e: UriDataRetrievalException) {
            throw ReplaceInternalWallpaperException()
        }

    }

    inner class ReplaceInternalWallpaperException : Exception()

    fun deleteInternalizedWallpaper() {
        try {
            val internalWallpaper = internalizedWallpaper
            internalWallpaper.delete()
            preferences.edit().remove(prefsKeyInternalWallpaperName).apply()
        } catch (e: NoInternalWallpaperException) {
            //Do nothing
        }

    }

    fun hasInternalizedWallpaper(): Boolean {
        val preferences = preferences
        val wallpaper = preferences.getString(prefsKeyInternalWallpaperName, null)
        return wallpaper != null && wallpaper.isNotEmpty()
    }

    inner class NoInternalWallpaperException : Exception()

    private inner class UriDataRetrievalException : Exception()

    @Throws(UriDataRetrievalException::class)
    private fun getWallpaperFileName(uri: Uri): String? {

        lateinit var stringData: String
        val contentResolver = context.contentResolver
        val projection = arrayOf(MediaStore.MediaColumns.DISPLAY_NAME)

        contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val columnId = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                stringData = cursor.getString(columnId)
            }
        }?: throw UriDataRetrievalException()

        return stringData
    }
}
