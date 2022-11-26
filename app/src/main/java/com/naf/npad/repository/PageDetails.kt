package com.naf.npad.repository

import java.time.LocalDateTime
import androidx.room.TypeConverters

@TypeConverters(com.naf.npad.repository.TypeConverters::class)
data class PageDetails (
    val uid: Int,
    var title: String?,
    var backgroundId: String?,
    val created: LocalDateTime,
    var modified: LocalDateTime,
        ) {

    fun getCreatedTimestamp() : String {
        val day = String.format("%02d", created.dayOfMonth)
        val month = String.format("%02d", created.monthValue)
        val year = created.year
        val hour = String.format("%02d", created.hour)
        val minute = String.format("%02d", created.minute)
        return "$day/$month/$year $hour:$minute"
    }
}