package com.naf.npad.viewmodels

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.*
import androidx.preference.PreferenceManager
import com.naf.npad.repository.BackgroundImageStore
import com.naf.npad.repository.PageEntity
import com.naf.npad.repository.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AppViewModel(private val _application: Application) : AndroidViewModel(_application) {

    private val repo = Repository(_application)
    private val backgroundImageStore = BackgroundImageStore(_application)

    val pages = liveData(Dispatchers.IO) {
        emitSource(repo.documents)
    }

    val pagesByModified = MediatorLiveData<List<PageEntity>>()

    init {
        pagesByModified.addSource(pages) {
            pagesByModified.postValue(it.sortedBy { it.modified }.reversed())
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
        currentPage.observeForever {
            if(it != null) lastOpenedPageId = it.uid
        }
    }

    fun newPage(
        title: String? = null,
        content: String? = null
    ) {
        viewModelScope.launch {
            repo.newPage(title = title, content = content)
        }
    }

    fun updatePage(pageEntity: PageEntity) = viewModelScope.launch {
        repo.updatePage(pageEntity)
    }

    suspend fun getPageWithId(id: Int) : PageEntity? {
            return repo.getPageWithId(id)
    }

    fun duplicatePage(page: PageEntity) = viewModelScope.launch {
        repo.newPage(
            title = page.title,
            content = page.title,
            backgroundId = page.backgroundId
        )
    }

    fun deletePage(pageEntity: PageEntity) = viewModelScope.launch {
        repo.deletePage(pageEntity)
    }

    val drawerOpen = MutableLiveData<Boolean>(false)

    suspend fun setCurrentPageBackgroundFromURI(uri: Uri) {
            val currentPage = currentPage.value ?: return
            /*val updatedPage = */repo.setPageBackgroundFromUri(currentPage, uri)
            this.currentPage.postValue(currentPage)
    }

    fun clearBackgroundForCurrentPage() = viewModelScope.launch {
        currentPage.value?.let {
            val newPage = repo.clearPageBackground(it)
            currentPage.postValue(newPage)
        }
    }

    suspend fun getBackgroundBitmap(backgroundId: String) : Bitmap? {
        return try {
            backgroundImageStore.retrieve(backgroundId)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getRandomBackgroundBitmap() : Bitmap? {
        return backgroundImageStore.retrieveRandom()
    }

    suspend fun getThumbnailForBackground(backgroundId: String): Bitmap? {
        return repo.getBackgroundThumbnail(backgroundId)
    }
}