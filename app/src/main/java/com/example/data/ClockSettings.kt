package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clock_settings")
data class ClockSettings(
    @PrimaryKey val id: Int = 1,
    val ledColorHex: String = "#FFFFFF", // Elegant pure white default
    val use24HourFormat: Boolean = false,
    val showSeconds: Boolean = false, // Seconds removed by default
    val isSlanted: Boolean = false, // Straight vertical by default
    val slantAngle: Float = 0f, // No tilt
    val inactiveOpacity: Float = 0.0f, // Invisible unlit segments (only lit segments visible)
    val colonBlinkStyle: String = "BLINK_SLOW", // BLINK_SLOW, BLINK_FAST, SOLID, OFF
    val tickSoundEnabled: Boolean = false,
    val alarmVibrate: Boolean = true,
    val timerDurationSeconds: Long = 60L // 1 minute default countdown
)
