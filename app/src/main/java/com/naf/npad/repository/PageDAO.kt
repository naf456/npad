package com.naf.npad.repository

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface PageDAO {
    @Query("SELECT * FROM Pages")
    fun getAll(): LiveData<List<PageEntity>>

    @Query("SELECT * FROM Pages WHERE title LIKE :title LIMIT 1")
    fun findByTitle(title: String): PageEntity

    @Query("SELECT * FROM Pages WHERE uid LIKE :id LIMIT 1")
    suspend fun findById(id: Int) : PageEntity?

    @Insert
    fun insertAll(vararg pageEntityArray: PageEntity)

    @Update
    fun updatePage(pageEntity: PageEntity)

    @Delete
    fun delete(pageEntity: PageEntity)
}