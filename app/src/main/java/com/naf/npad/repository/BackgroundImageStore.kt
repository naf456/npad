package com.naf.npad.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ThumbnailUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.lang.Exception

class BackgroundImageStore(val context: Context) {

    companion object {
        const val CONTENT_DIRECTORY = "content/"
        const val THUMBNAIL_DIRECTORY = "thumbnails"
        val alphabet : List<Char> = ('a'..'b') + ('A'..'B') + ('0'..'9')
    }

    private val contentDir : File
        get() {
            val contentDir = File(context.filesDir, CONTENT_DIRECTORY)
            return if(!contentDir.exists()) {
                val mkdirSuccess = contentDir.mkdir()
                if(!mkdirSuccess) throw IOException("Cannot create internal content directory!")
                contentDir
            } else
                contentDir
        }

    private val thumbnailDir : File
        get() {
            val thumbnailDir = File(contentDir, THUMBNAIL_DIRECTORY)
            return if(!thumbnailDir.exists()) {
                val success = thumbnailDir.mkdir()
                if(!success) throw IOException("Cannot create internal thumbnail directory!")
                thumbnailDir
            } else
                thumbnailDir
        }

    /**
     * @return id of the bitmap entry in store
     */
    suspend fun store(bitmap: Bitmap) : String = withContext(Dispatchers.IO) {
        var uid = generateUID()
        var file = File(contentDir, uid)
        //Make sure unique file doesn't actually exist
        while(file.exists()) {
            uid = generateUID()
            file = File(contentDir, uid)
        }
        file.createNewFile()
        val stream = file.outputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG,0, stream)
        uid
    }

    suspend fun update(imageId: String, bitmap: Bitmap) : String {
        val newBackgroundId = store(bitmap)
        val imageFile = File(contentDir, imageId)
        val thumbnailFile = File(thumbnailDir, imageId)
        if(imageFile.exists()) imageFile.delete()
        if(thumbnailFile.exists()) imageFile.delete()
        return newBackgroundId
    }

    private suspend fun thumbnail(backgroundId: String, width: Int, height: Int) : Bitmap?
    = withContext(Dispatchers.IO){
        val bitmap = retrieve(backgroundId) ?: return@withContext null
        val thumbnail = ThumbnailUtils.extractThumbnail(bitmap, width, height)
        val file = File(thumbnailDir, backgroundId)
        if (file.exists()) file.delete()
        file.createNewFile()

        val stream = file.outputStream()
        thumbnail.compress(Bitmap.CompressFormat.PNG, 0, stream)
        return@withContext thumbnail
    }

    fun retrieve(imageId: String) : Bitmap? {
        val imageFile = File(contentDir, imageId)

        return if(imageFile.exists())
            BitmapFactory.decodeStream(imageFile.inputStream())
        else
            null
    }

    class ImageStoreUpdateException(msg: String): Exception(msg)

    fun delete(imageId: String) : Boolean {
        val imageFile = File(contentDir, imageId)
        return if(imageFile.exists()) imageFile.delete() else true
    }

    private fun generateUID() : String {
        return String((List(20) { alphabet.random() }).toCharArray())
    }

    suspend fun getBackgroundThumbnail(backgroundId: String, width: Int, height: Int) : Bitmap? {
            val thumbnailFile = File(thumbnailDir, backgroundId)
            if(!thumbnailFile.exists()) {
                return thumbnail(backgroundId = backgroundId, width = width, height = height)
            }
            val bitmap = BitmapFactory.decodeStream(thumbnailFile.inputStream())
            if(bitmap.width != width || bitmap.height != height) {
                return thumbnail(backgroundId = backgroundId, width = width, height = height)
            }
            return bitmap
    }

}