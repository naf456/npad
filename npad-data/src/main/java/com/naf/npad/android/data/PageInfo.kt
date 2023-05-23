package com.naf.npad.android.data

import androidx.room.TypeConverters
import java.time.LocalDateTime

@TypeConverters(AppDatabase.TypeConverters::class)
data class PageInfo (
    val uid: Int,
    var title: String?,
    var backgroundId: String?,
    val created: LocalDateTime,
    var modified: LocalDateTime,
        ) {

    fun getCreatedTimestamp() : String {
        val dayWeek = getWeekday(created.dayOfWeek.value) //"Mon."
        val dayMonth = getDate(created.dayOfMonth) //"7th"
        val month = getMonth(created.monthValue) //"Oct."
        val year = getYear(created.year) //"'94"
        return "$dayWeek $dayMonth $month $year"
    }

    private fun getDate(date: Int) = when(date) {
        1-> "1st"
        2-> "2nd"
        3-> "3nd"
        else-> "${date}th"
    }

    private fun getWeekday(weekday: Int) =  when(weekday) {
        1 -> "Mon"
        2-> "Tue"
        3-> "Wed"
        4-> "Thu"
        5-> "Fri"
        6-> "Sat"
        7-> "Sun"
        else -> "unk."
    }

    private fun getMonth(month: Int) = when(month) {
        1->"Jan"
        2->"Feb"
        3->"Mar"
        4->"Apr"
        5->"May"
        6->"Jun"
        7->"Jul"
        8->"Aug"
        9->"Sep"
        10->"Oct"
        11->"Nov"
        12->"Dec"
        else -> "unk."
    }

    private fun getYear(year: Int) : String {
        val str = year.toString()
        if(str.length < 2) return str
        return "'" + str.substring(str.length - 2)
    }

    private fun twelveHourTime(dateTime: LocalDateTime) : String {
        val hour = dateTime.hour
        val pm = hour > 12
        val minutes = dateTime.minute

        return when(pm) {
            false -> String.format("%d", hour) + ":" + String.format("%02d",minutes) + "am"
            true -> String.format("%d", hour - 12) + ":" + String.format("%02d",minutes)  + "pm"
        }

    }

}