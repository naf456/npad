package com.naf.npad.repository

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [PageEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun pageDAO() : PageDAO
}