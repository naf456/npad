package com.naf.npad.android.data

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

class Repository(private val application: Application) {
    private val database =
        Room.databaseBuilder(application, AppDatabase::class.java, "app-database")
            .build()

    private val pageDAO = database.pageDAO()

    private val backgroundStore = BackgroundStore(application)

    val pagesDetail = liveData {
        emitSource(pageDAO.retrieveAllInfo())
    }

    suspend fun newPage(
        title: String? = null,
        content: String? = null,
        backgroundId: String? = null) {

        withContext(Dispatchers.IO) {
            pageDAO.insert(
                Page(
                    title = title,
                    content = content,
                    backgroundId = backgroundId,
                    created = LocalDateTime.now(),
                    modified = LocalDateTime.now()
                )
            )
        }
    }

    suspend fun getPageWithId(id: Int) : Page? {
        return withContext(Dispatchers.IO){
            return@withContext pageDAO.retrieve(id)
        }
    }

    suspend fun getPageInfoWithId(id: Int) : PageInfo? {
        return withContext(Dispatchers.IO){
            return@withContext pageDAO.retrieveInfo(id)
        }
    }

    suspend fun updatePage(page: Page) {
        withContext(Dispatchers.IO) {
            page.modified = LocalDateTime.now()
            pageDAO.update(page)
        }
    }

    suspend fun updatePage(pageDetails: PageInfo) {
        withContext(Dispatchers.IO) {
            pageDetails.modified = LocalDateTime.now()
            pageDAO.update(pageDetails)
        }
    }

    suspend fun deletePage(pageDetails: PageInfo) {
        withContext(Dispatchers.IO){
            pageDetails.backgroundId?.let {
                backgroundStore.delete(it)
            }
            pageDAO.delete(pageDetails.uid)
        }
    }

    suspend fun duplicatePage(pageDetails: PageInfo) {
        withContext(Dispatchers.IO) {
            //Todo exception?
            val oldPage = pageDAO.retrieve(pageDetails.uid)?: return@withContext
            val dupPage = Page()
            dupPage.title = oldPage.title
            dupPage.content = oldPage.content
            dupPage.backgroundId = oldPage.backgroundId?.let { backgroundId ->
                //Duplicate background
                backgroundStore.retrieve(backgroundId)?.let {
                    backgroundStore.store(it)
                }
            }
            pageDAO.insert(dupPage)
        }
    }

    suspend fun setBackgroundForPageFromUri(page: Page, uri: Uri) : Page {
        return withContext(Dispatchers.IO) {
            val stream = application.contentResolver.openInputStream(uri) ?: throw Exception("No such Uri: $uri")
            val bitmap = BitmapFactory.decodeStream(stream)
            val newPage = setPageBackgroundFromBitmap(page, bitmap)
            updatePage(newPage)
            return@withContext page
        }
    }

    suspend fun setPageBackgroundFromBitmap(page: Page, bitmap: Bitmap) : Page {
        return withContext(Dispatchers.IO) {
            val curBackgroundId = page.backgroundId
            val newBackgroundId = if (curBackgroundId != null)
                backgroundStore.update(curBackgroundId, bitmap)
            else
                backgroundStore.store(bitmap)

            page.backgroundId = newBackgroundId
            return@withContext page
        }
    }

    suspend fun clearBackgroundForPage(page: Page) : Page {
        val backgroundId = page.backgroundId ?: return page
        backgroundStore.delete(backgroundId)
        page.backgroundId = null
        updatePage(page)
        return page
    }

    suspend fun getBackgroundThumbnail(backgroundId: String, width: Int, height: Int) : Bitmap? {
        return backgroundStore.getBackgroundThumbnail(backgroundId, width, height)
    }
}