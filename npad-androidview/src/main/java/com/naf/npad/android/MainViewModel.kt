package com.naf.npad.android

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.*
import androidx.preference.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainViewModel(private val app: Application) : AndroidViewModel(app) {

    private val appDataRepo = com.naf.npad.android.data.AppRepository(app)
    private val backgroundImageStore = com.naf.npad.android.data.BackgroundImageStore(app)

    val pagesDetail = liveData(Dispatchers.IO) {
        emitSource(appDataRepo.pagesDetail)
    }

    val pagesDetailByModified = MediatorLiveData<List<com.naf.npad.android.data.PageDetail>>()

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

    val currentPage = MutableLiveData<com.naf.npad.android.data.PageEntity>()

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

    fun updatePage(pageEntity: com.naf.npad.android.data.PageEntity) = viewModelScope.launch {
        appDataRepo.updatePage(pageEntity)
    }

    fun updatePage(pageDetails: com.naf.npad.android.data.PageDetail) = viewModelScope.launch {
        appDataRepo.updatePage(pageDetails)
    }

    suspend fun getPageWithId(id: Int) : com.naf.npad.android.data.PageEntity? {
            return appDataRepo.getPageWithId(id)
    }

    suspend fun getPageDetailsWithId(id: Int) : com.naf.npad.android.data.PageDetail? {
        return appDataRepo.getPageDetailsWithId(id)
    }

    fun duplicatePage(page: com.naf.npad.android.data.PageEntity) = viewModelScope.launch {
        appDataRepo.newPage(
            title = page.title,
            content = page.content,
            backgroundId = page.backgroundId
        )
    }

    fun duplicatePage(pageDetails: com.naf.npad.android.data.PageDetail) = viewModelScope.launch {
        appDataRepo.getPageWithId(pageDetails.uid)?.let { page ->
            duplicatePage(page)
        }
    }

    fun deletePage(pageEntity: com.naf.npad.android.data.PageEntity) = viewModelScope.launch {
        appDataRepo.deletePage(pageEntity)
    }

    fun deletePage(details: com.naf.npad.android.data.PageDetail) = viewModelScope.launch {
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