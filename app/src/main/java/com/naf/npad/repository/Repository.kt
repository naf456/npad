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

class Repository(val application: Application) {
    private val database =
        Room.databaseBuilder(application, AppDatabase::class.java, "app-database")
            .build()

    private val pageDAO = database.pageDAO()

    private val backgroundImageStore = BackgroundImageStore(application)

    val documents = liveData {
        emitSource(pageDAO.getAll())
    }

    suspend fun newPage(
        title: String? = null,
        content: String? = null,
        backgroundId: String? = null) {

        withContext(Dispatchers.IO) {
            pageDAO.insertAll(
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
            return@withContext pageDAO.findById(id)
        }
    }

    suspend fun updatePage(pageEntity: PageEntity) {
        withContext(Dispatchers.IO) {
            pageEntity.modified = LocalDateTime.now()
            pageDAO.updatePage(pageEntity)
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

    suspend fun setPageBackgroundFromUri(pageEntity: PageEntity, uri: Uri) : PageEntity {
        val stream = application.contentResolver.openInputStream(uri) ?: throw Exception("No such Uri: $uri")
        val bitmap = BitmapFactory.decodeStream(stream)
        val curBackgroundId = pageEntity.backgroundId
        val backgroundId = if(curBackgroundId != null)
            backgroundImageStore.update(curBackgroundId, bitmap)
        else
            backgroundImageStore.store(bitmap)

        pageEntity.backgroundId = backgroundId
        updatePage(pageEntity)
        return pageEntity
    }

    suspend fun clearPageBackground(pageEntity: PageEntity) : PageEntity {
        val backgroundId = pageEntity.backgroundId ?: return pageEntity
        backgroundImageStore.delete(backgroundId)
        pageEntity.backgroundId = null
        updatePage(pageEntity)
        return pageEntity
    }

    suspend fun getBackgroundThumbnail(backgroundId: String) : Bitmap? {
        return backgroundImageStore.getBackgroundThumbnail(backgroundId)
    }
}