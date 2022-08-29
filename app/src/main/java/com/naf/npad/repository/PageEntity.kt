package com.naf.npad.repository

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import java.time.LocalDateTime

@Entity(tableName = "Pages")
@TypeConverters(com.naf.npad.repository.TypeConverters::class)
data class PageEntity (
    var title: String? = null,
    var content : String? = null,
    var backgroundId: String? = null,
    var created: LocalDateTime,
    var modified: LocalDateTime
        ) {
    @PrimaryKey(autoGenerate = true) var uid: Int? = null

    fun getCreatedTimestamp() : String {
        val day = String.format("%02d", created.dayOfMonth)
        val month = String.format("%02d", created.monthValue)
        val year = created.year
        val hour = String.format("%02d", created.hour)
        val minute = String.format("%02d", created.minute)
        return "$day/$month/$year $hour:$minute"
    }
}