package com.naf.npad.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ThumbnailUtils
import android.util.TypedValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FilenameFilter
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
    suspend fun store(bitmap: Bitmap) : String {
        return withContext(Dispatchers.IO) {
            var uid = generateUID()
            var file = File(contentDir, uid)
            //Make sure filename is unique
            while(file.exists()) {
                uid = generateUID()
                file = File(contentDir, generateUID())
            }
            file.createNewFile()
            val stream = file.outputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG,0, stream)
            return@withContext uid
        }
    }

    suspend fun update(imageId: String, bitmap: Bitmap) : String {
        val imageFile = getBackgroundFile(imageId)
        imageFile?.delete()
        storeImageAs(imageId, bitmap)
        return imageId
    }

    private suspend fun thumbnail(backgroundId: String, width: Int, height: Int) : Bitmap? {
        return withContext(Dispatchers.IO) {
            val bitmap = retrieve(backgroundId) ?: return@withContext null
            val thumbnail = ThumbnailUtils.extractThumbnail(bitmap, width, height)
            val file = File(thumbnailDir, backgroundId)
            if (file.exists()) file.delete()
            file.createNewFile()

            val stream = file.outputStream()
            thumbnail.compress(Bitmap.CompressFormat.PNG, 0, stream)
            return@withContext thumbnail
        }
    }

    private suspend fun storeImageAs(imageId: String, bitmap: Bitmap): String {
        return withContext(Dispatchers.IO) {
            val file = File(contentDir, imageId)
            if (file.exists()) throw IOException("Can't store as '$imageId', a corresponding entry already exists!")

            val fileCreationSuccess = file.createNewFile()
            if(!fileCreationSuccess) throw IOException("Cannot create file for entry!")

            val stream = file.outputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 0, stream)
            return@withContext imageId
        }
    }

    suspend fun retrieve(imageId: String) : Bitmap? {
        return withContext(Dispatchers.IO) {
            val imageFile = getBackgroundFile(imageId)

            return@withContext if(imageFile != null)
                BitmapFactory.decodeStream(imageFile.inputStream())
            else
                null
        }
    }

    suspend fun retrieveRandom() : Bitmap? {
        return withContext(Dispatchers.IO) {
            val allBackgroundFiles = contentDir.listFiles() ?: return@withContext null
            val imageFile = allBackgroundFiles[IntRange(0,allBackgroundFiles.size - 1).random()]

            return@withContext if(imageFile != null)
                BitmapFactory.decodeStream(imageFile.inputStream())
            else
                null
        }
    }

    class ImageStoreUpdateException(msg: String): Exception(msg)

    fun delete(imageId: String) : Boolean {
        val imageFile = getBackgroundFile(imageId)
        return imageFile?.delete() ?: true
    }

    private fun getBackgroundFile(backgroundId: String) : File? {
        val backgroundFile = File(contentDir, backgroundId)
        if(!backgroundFile.exists()) return null
        return backgroundFile
    }

    private fun generateUID() : String {
        return String((List(20) { alphabet.random() }).toCharArray())
    }

    suspend fun getBackgroundThumbnail(backgroundId: String, width: Int, height: Int) : Bitmap? {
        return withContext(Dispatchers.IO) {
            val thumbnailFile = File(thumbnailDir, backgroundId)
            if(!thumbnailFile.exists()) {
                return@withContext thumbnail(backgroundId = backgroundId, width = width, height = height)
            }
            val bitmap = BitmapFactory.decodeStream(thumbnailFile.inputStream())
            if(bitmap.width != width || bitmap.height != height) {
                return@withContext thumbnail(backgroundId = backgroundId, width = width, height = height)
            }
            return@withContext bitmap
        }
    }
}