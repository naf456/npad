package com.naf.npad.android.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface PageDAO {

    @Query("SELECT * FROM Pages WHERE uid LIKE :id LIMIT 1")
    suspend fun retrieve(id: Int) : PageEntity?

    @Query("SELECT uid, title, backgroundId, created, modified FROM Pages WHERE uid LIKE :id LIMIT 1")
    fun retrievePageDetails(id: Int): PageDetail?

    @Query("SELECT * FROM Pages")
    fun retrieveAll(): LiveData<List<PageEntity>>

    @Query("SELECT uid, title, backgroundId, created, modified FROM Pages")
    fun retrieveAllDetails(): LiveData<List<PageDetail>>

    @Insert
    fun insert(vararg pageEntities: PageEntity)

    @Update
    fun update(pageEntity: PageEntity)

    @Update(entity = PageEntity::class)
    fun update(pageDetails: PageDetail)

    @Delete
    fun delete(pageEntity: PageEntity)

    @Delete(entity = PageEntity::class)
    fun delete(pageDetails: PageDetail)
}