package com.naf.npad.android.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import java.time.LocalDateTime

@Entity(tableName = "Pages")
@TypeConverters(AppDatabase.TypeConverters::class)
data class Page (
    var title: String? = null,
    var content : String? = null,
    var backgroundId: String? = null,
    var created: LocalDateTime = LocalDateTime.now(),
    var modified: LocalDateTime = LocalDateTime.now()
        ) {
    @PrimaryKey(autoGenerate = true) var uid: Int? = null

    companion object {
        fun fromInfo(info: PageInfo, content: String? = null) : Page {
            return Page(
                title = info.title,
                content = content,
                backgroundId = info.backgroundId,
                created = info.created,
                modified = info.modified
            )
        }
    }
}