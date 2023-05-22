package com.naf.npad.android

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.*
import androidx.preference.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainViewModel(private val app: Application) : AndroidViewModel(app) {

    private val repository = com.naf.npad.android.data.Repository(app)
    private val backgroundImageStore = com.naf.npad.android.data.BackgroundImageStore(app)

    val pagesDetail = liveData(Dispatchers.IO) {
        emitSource(repository.pagesDetail)
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
            repository.newPage(title = title, content = content)
        }
    }

    fun updatePage(pageEntity: com.naf.npad.android.data.PageEntity) = viewModelScope.launch {
        repository.updatePage(pageEntity)
    }

    fun updatePage(pageDetails: com.naf.npad.android.data.PageDetail) = viewModelScope.launch {
        repository.updatePage(pageDetails)
    }

    suspend fun getPageWithId(id: Int) : com.naf.npad.android.data.PageEntity? {
            return repository.getPageWithId(id)
    }

    suspend fun getPageDetailsWithId(id: Int) : com.naf.npad.android.data.PageDetail? {
        return repository.getPageDetailsWithId(id)
    }

    fun duplicatePage(pageDetails: com.naf.npad.android.data.PageDetail) = viewModelScope.launch {
        repository.duplicatePage(pageDetails)
    }

    fun deletePage(pageEntity: com.naf.npad.android.data.PageEntity) = viewModelScope.launch {
        repository.deletePage(pageEntity)
    }

    fun deletePage(details: com.naf.npad.android.data.PageDetail) = viewModelScope.launch {
        repository.deletePage(details)
    }

    suspend fun setCurrentPageBackgroundFromURI(uri: Uri) {
            val currentPage = currentPage.value ?: return
            repository.setPageBackgroundFromUri(currentPage, uri)
            this.currentPage.postValue(currentPage)
    }

    suspend fun setCurrentPageBackgroundFromBitmap(bitmap: Bitmap) {
        val currentPage = currentPage.value ?: return
        repository.setPageBackgroundFromBitmap(currentPage, bitmap)
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
        return repository.getBackgroundThumbnail(backgroundId, width, height)
    }

    fun clearBackgroundForCurrentPage() = viewModelScope.launch {
        
        currentPage.value?.let {
            val newPage = repository.clearPageBackground(it)
            currentPage.postValue(newPage)
        }
    }
}