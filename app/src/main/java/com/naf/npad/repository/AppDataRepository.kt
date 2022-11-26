package com.naf.npad.repository

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.liveData
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.Exception
import java.time.LocalDateTime

class AppDataRepository(val application: Application) {
    private val database =
        Room.databaseBuilder(application, AppDatabase::class.java, "app-database")
            .build()

    private val pageDAO = database.pageDAO()

    private val backgroundImageStore = BackgroundImageStore(application)

    val pagesDetail = liveData {
        emitSource(pageDAO.retrieveAllDetails())
    }

    suspend fun newPage(
        title: String? = null,
        content: String? = null,
        backgroundId: String? = null) {

        withContext(Dispatchers.IO) {
            pageDAO.insert(
                PageEntity(
                    title = title,
                    content = content,
                    backgroundId = backgroundId,
                    created = LocalDateTime.now(),
                    modified = LocalDateTime.now()
                )
            )
        }
    }

    suspend fun getPageWithId(id: Int) : PageEntity? {
        return withContext(Dispatchers.IO){
            return@withContext pageDAO.retrieve(id)
        }
    }

    suspend fun updatePage(pageEntity: PageEntity) {
        withContext(Dispatchers.IO) {
            pageEntity.modified = LocalDateTime.now()
            pageDAO.update(pageEntity)
        }
    }

    suspend fun updatePage(pageDetails: PageDetails) {
        withContext(Dispatchers.IO) {
            pageDetails.modified = LocalDateTime.now()
            pageDAO.update(pageDetails)
        }
    }

    suspend fun deletePage(pageEntity: PageEntity) {
        withContext(Dispatchers.IO){
            pageEntity.backgroundId?.let {
                backgroundImageStore.delete(it)
            }
            pageDAO.delete(pageEntity)
        }
    }

    suspend fun deletePage(pageDetails: PageDetails) {
        withContext(Dispatchers.IO){
            pageDetails.backgroundId?.let {
                backgroundImageStore.delete(it)
            }
            pageDAO.delete(pageDetails)
        }
    }

    suspend fun setPageBackgroundFromUri(pageEntity: PageEntity, uri: Uri) : PageEntity
    = withContext(Dispatchers.IO) {
        val stream = application.contentResolver.openInputStream(uri) ?: throw Exception("No such Uri: $uri")
        val bitmap = BitmapFactory.decodeStream(stream) //Make sure we can actually read the bitmap
        val curBackgroundId = pageEntity.backgroundId
        val newBackgroundId = if(curBackgroundId != null)
            backgroundImageStore.update(curBackgroundId, bitmap)
        else
            backgroundImageStore.store(bitmap)

        pageEntity.backgroundId = newBackgroundId
        updatePage(pageEntity)
        return@withContext pageEntity
    }

    suspend fun clearPageBackground(pageEntity: PageEntity) : PageEntity {
        val backgroundId = pageEntity.backgroundId ?: return pageEntity
        backgroundImageStore.delete(backgroundId)
        pageEntity.backgroundId = null
        updatePage(pageEntity)
        return pageEntity
    }

    suspend fun getBackgroundThumbnail(backgroundId: String, width: Int, height: Int) : Bitmap? {
        return backgroundImageStore.getBackgroundThumbnail(backgroundId, width, height)
    }
}