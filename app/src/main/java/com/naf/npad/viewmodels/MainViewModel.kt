package com.naf.npad.viewmodels

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.*
import androidx.preference.PreferenceManager
import com.naf.npad.repository.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainViewModel(private val app: Application) : AndroidViewModel(app) {

    private val appDataRepo = AppRepository(app)
    private val backgroundImageStore = BackgroundImageStore(app)

    val pagesDetail = liveData(Dispatchers.IO) {
        emitSource(appDataRepo.pagesDetail)
    }

    val pagesDetailByModified = MediatorLiveData<List<PageDetail>>()

    init {
        pagesDetailByModified.addSource(pagesDetail) { pageDetails ->
            pagesDetailByModified.postValue(pageDetails.sortedBy { it.modified }.reversed())
        }
    }



    var lastOpenedPageId : Int?
    get() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(app)
        val key = app.resources.getString(com.naf.npad.R.string.pref_key_last_opened_page_id)
        val pageId = prefs.getInt(key, -1)
        return if (pageId > -1) pageId else null
    }
    set(value) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(app)
        val editor = prefs.edit()
        val key = app.resources.getString(com.naf.npad.R.string.pref_key_last_opened_page_id)
        if (value == null) editor.remove(key) else editor.putInt(key, value)
        editor.apply()
    }

    val autoloadPage: Boolean
    get() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(app)
        val key = app.resources.getString(com.naf.npad.R.string.pref_key_page_autoload)
        return prefs.getBoolean(key, false)
    }

    val currentPage = MutableLiveData<PageEntity>()

    init {
        currentPage.observeForever { currentPage ->
            if(currentPage != null) lastOpenedPageId = currentPage.uid
        }
    }

    fun newPage(title: String? = null, content: String? = null) {
        viewModelScope.launch {
            appDataRepo.newPage(title = title, content = content)
        }
    }

    fun updatePage(pageEntity: PageEntity) = viewModelScope.launch {
        appDataRepo.updatePage(pageEntity)
    }

    fun updatePage(pageDetails: PageDetail) = viewModelScope.launch {
        appDataRepo.updatePage(pageDetails)
    }

    suspend fun getPageWithId(id: Int) : PageEntity? {
            return appDataRepo.getPageWithId(id)
    }

    suspend fun getPageDetailsWithId(id: Int) : PageDetail? {
        return appDataRepo.getPageDetailsWithId(id)
    }

    fun duplicatePage(page: PageEntity) = viewModelScope.launch {
        appDataRepo.newPage(
            title = page.title,
            content = page.content,
            backgroundId = page.backgroundId
        )
    }

    fun duplicatePage(pageDetails: PageDetail) = viewModelScope.launch {
        appDataRepo.getPageWithId(pageDetails.uid)?.let { page ->
            duplicatePage(page)
        }
    }

    fun deletePage(pageEntity: PageEntity) = viewModelScope.launch {
        appDataRepo.deletePage(pageEntity)
    }

    fun deletePage(details: PageDetail) = viewModelScope.launch {
        appDataRepo.deletePage(details)
    }

    suspend fun setCurrentPageBackgroundFromURI(uri: Uri) {
            val currentPage = currentPage.value ?: return
            appDataRepo.setPageBackgroundFromUri(currentPage, uri)
            this.currentPage.postValue(currentPage)
    }

    suspend fun setCurrentPageBackgroundFromBitmap(bitmap: Bitmap) {
        val currentPage = currentPage.value ?: return
        appDataRepo.setPageBackgroundFromBitmap(currentPage, bitmap)
        this.currentPage.postValue(currentPage)
    }

    fun getBackgroundBitmap(backgroundId: String) : Bitmap? {
        return try {
            backgroundImageStore.retrieve(backgroundId)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getThumbnailForBackground(backgroundId: String, width: Int, height: Int): Bitmap? {
        return appDataRepo.getBackgroundThumbnail(backgroundId, width, height)
    }

    fun clearBackgroundForCurrentPage() = viewModelScope.launch {
        
        currentPage.value?.let {
            val newPage = appDataRepo.clearPageBackground(it)
            currentPage.postValue(newPage)
        }
    }
}