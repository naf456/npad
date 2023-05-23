package com.naf.npad.android.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

@Database(
    entities = [Page::class],
    version = 1)

abstract class AppDatabase : RoomDatabase() {
    abstract fun pageDAO() : PageDAO

    internal class TypeConverters {
        @TypeConverter
        fun localDateTimeToLong(localDateTime: LocalDateTime): Long {
            return localDateTime.toInstant(ZoneOffset.UTC).toEpochMilli()
        }

        @TypeConverter
        fun longToLocalDateTime(epochMilli: Long): LocalDateTime {
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMilli), ZoneId.systemDefault())
        }
    }
}