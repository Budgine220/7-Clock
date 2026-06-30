package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarms")
data class Alarm(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val hour: Int,
    val minute: Int,
    val label: String = "Alarm",
    val isEnabled: Boolean = true,
    val repeatDays: String = "", // Comma-separated days: "1,2,3,4,5,6,7" (1=Mon, 7=Sun)
    val vibrate: Boolean = true
) {
    fun getFormattedTime(): String {
        val amPm = if (hour >= 12) "PM" else "AM"
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return String.format("%02d:%02d %s", displayHour, minute, amPm)
    }

    fun getRepeatDaysList(): List<Int> {
        if (repeatDays.isEmpty()) return emptyList()
        return repeatDays.split(",").mapNotNull { it.toIntOrNull() }
    }
}
