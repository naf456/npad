package com.naf.npad.repository

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface PageDAO {

    @Query("SELECT * FROM Pages WHERE uid LIKE :id LIMIT 1")
    suspend fun retrieve(id: Int) : PageEntity?

    @Query("SELECT * FROM Pages")
    fun retrieveAll(): LiveData<List<PageEntity>>

    @Query("SELECT uid, title, backgroundId, created, modified FROM Pages")
    fun retrieveAllDetails(): LiveData<List<PageDetails>>

    @Insert
    fun insert(vararg pageEntities: PageEntity)

    @Update
    fun update(pageEntity: PageEntity)

    @Update(entity = PageEntity::class)
    fun update(pageDetails: PageDetails)

    @Delete
    fun delete(pageEntity: PageEntity)

    @Delete(entity = PageEntity::class)
    fun delete(pageDetails: PageDetails)
}