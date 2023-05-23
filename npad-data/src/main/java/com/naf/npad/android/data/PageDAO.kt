package com.naf.npad.android.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface PageDAO {

    @Query("SELECT uid, title, backgroundId, created, modified FROM Pages WHERE uid LIKE :id LIMIT 1")
    fun retrieveInfo(id: Int): PageInfo?

    @Query("SELECT uid, title, backgroundId, created, modified FROM Pages")
    fun retrieveAllInfo(): LiveData<List<PageInfo>>

    @Query("SELECT * FROM Pages WHERE uid LIKE :id LIMIT 1")
    suspend fun retrieve(id: Int) : Page

    @Insert
    fun insert(vararg pages: Page)

    @Update
    fun update(page: Page)

    @Update(entity = Page::class)
    fun update(pageDetails: PageInfo)

    @Query("DELETE from Pages WHERE uid = :id")
    fun delete(id: Int)
}