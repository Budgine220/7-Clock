package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clock_settings")
data class ClockSettings(
    @PrimaryKey val id: Int = 1,
    val ledColorHex: String = "#00F0FF", // Cyberpunk Cyan default
    val use24HourFormat: Boolean = false,
    val showSeconds: Boolean = true,
    val isSlanted: Boolean = true,
    val slantAngle: Float = 8f,
    val inactiveOpacity: Float = 0.05f,
    val colonBlinkStyle: String = "BLINK_SLOW", // BLINK_SLOW, BLINK_FAST, SOLID, OFF
    val tickSoundEnabled: Boolean = false,
    val alarmVibrate: Boolean = true,
    val timerDurationSeconds: Long = 60L // 1 minute default countdown
)
