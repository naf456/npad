package com.naf.npad.viewmodels

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.*
import androidx.preference.PreferenceManager
import com.naf.npad.repository.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AppViewModel(private val _application: Application) : AndroidViewModel(_application) {

    private val appDataRepo = AppDataRepository(_application)
    private val backgroundImageStore = BackgroundImageStore(_application)

    val pagesDetail = liveData(Dispatchers.IO) {
        emitSource(appDataRepo.pagesDetail)
    }

    val pagesDetailByModified = MediatorLiveData<List<PageDetails>>()

    init {
        pagesDetailByModified.addSource(pagesDetail) { pageDetails ->
            pagesDetailByModified.postValue(pageDetails.sortedBy { it.modified }.reversed())
        }
    }

    var lastOpenedPageId : Int?
    get() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(_application)
        val key = _application.resources.getString(com.naf.npad.R.string.pref_key_last_opened_page_id)
        val id = prefs.getInt(key, -1)
        return if (id > -1) id else null
    }
    set(value) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(_application)
        val editor = prefs.edit()
        val key = _application.resources.getString(com.naf.npad.R.string.pref_key_last_opened_page_id)
        if (value == null) editor.remove(key) else editor.putInt(key, value)
        editor.apply()
    }

    val autoloadPage: Boolean
    get() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(_application)
        val key = _application.resources.getString(com.naf.npad.R.string.pref_key_page_autoload)
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

    fun updatePage(pageDetails: PageDetails) = viewModelScope.launch {
        appDataRepo.updatePage(pageDetails)
    }

    suspend fun getPageWithId(id: Int) : PageEntity? {
            return appDataRepo.getPageWithId(id)
    }

    fun duplicatePage(page: PageEntity) = viewModelScope.launch {
        appDataRepo.newPage(
            title = page.title,
            content = page.content,
            backgroundId = page.backgroundId
        )
    }

    fun duplicatePage(pageDetails: PageDetails) = viewModelScope.launch {
        appDataRepo.getPageWithId(pageDetails.uid)?.let { page ->
            duplicatePage(page)
        }
    }

    fun deletePage(pageEntity: PageEntity) = viewModelScope.launch {
        appDataRepo.deletePage(pageEntity)
    }

    fun deletePage(details: PageDetails) = viewModelScope.launch {
        appDataRepo.deletePage(details)
    }

    suspend fun setCurrentPageBackgroundFromURI(uri: Uri) {
            val currentPage = currentPage.value ?: return
            appDataRepo.setPageBackgroundFromUri(currentPage, uri)
            this.currentPage.postValue(currentPage)
    }

    suspend fun getBackgroundBitmap(backgroundId: String) : Bitmap? {
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