package com.naf.npad.android.data

import androidx.room.TypeConverters
import java.time.LocalDateTime

@TypeConverters(AppDatabase.TypeConverters::class)
data class PageDetail (
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