package com.naf.npad.android

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.*
import androidx.preference.PreferenceManager
import com.naf.npad.android.data.PageInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainViewModel(private val app: Application) : AndroidViewModel(app) {

    private val repository = com.naf.npad.android.data.Repository(app)
    private val backgroundStore = com.naf.npad.android.data.BackgroundStore(app)

    val pagesDetail = liveData(Dispatchers.IO) {
        emitSource(repository.pagesDetail)
    }

    val pagesDetailByModified = MediatorLiveData<List<com.naf.npad.android.data.PageInfo>>()

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

    val currentPage = MutableLiveData<com.naf.npad.android.data.Page>()

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

    fun updatePage(page: com.naf.npad.android.data.Page) = viewModelScope.launch {
        repository.updatePage(page)
    }

    fun updatePage(pageDetails: com.naf.npad.android.data.PageInfo) = viewModelScope.launch {
        repository.updatePage(pageDetails)
    }

    suspend fun getPageWithId(id: Int) : com.naf.npad.android.data.Page? {
            return repository.getPageWithId(id)
    }

    suspend fun getPageInfoWithId(id: Int) : PageInfo? {
        return repository.getPageInfoWithId(id)
    }

    fun duplicatePage(pageDetails: com.naf.npad.android.data.PageInfo) = viewModelScope.launch {
        repository.duplicatePage(pageDetails)
    }

    fun deletePage(details: com.naf.npad.android.data.PageInfo) = viewModelScope.launch {
        repository.deletePage(details)
    }

    suspend fun setCurrentPageBackgroundFromURI(uri: Uri) {
            val currentPage = currentPage.value ?: return
            repository.setBackgroundForPageFromUri(currentPage, uri)
            this.currentPage.postValue(currentPage)
    }

    val currentPageBackground : Bitmap?
        get(){
            val curPage = currentPage.value?: return null
            val backgroundId = curPage.backgroundId?: return null
            return backgroundStore.retrieve(backgroundId)
        }

    suspend fun getThumbnailForBackground(backgroundId: String, width: Int, height: Int): Bitmap? {
        return repository.getBackgroundThumbnail(backgroundId, width, height)
    }

    fun clearBackgroundForCurrentPage() = viewModelScope.launch {
        currentPage.value?.let {
            currentPage.postValue(
                repository.clearBackgroundForPage(it)
            )
        }
    }
}