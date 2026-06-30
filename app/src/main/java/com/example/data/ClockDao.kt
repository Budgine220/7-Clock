package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ClockDao {
    // Clock Settings queries
    @Query("SELECT * FROM clock_settings WHERE id = 1")
    fun getSettingsFlow(): Flow<ClockSettings?>

    @Query("SELECT * FROM clock_settings WHERE id = 1")
    suspend fun getSettings(): ClockSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSettings(settings: ClockSettings)

    // Alarms queries
    @Query("SELECT * FROM alarms ORDER BY hour ASC, minute ASC")
    fun getAlarmsFlow(): Flow<List<Alarm>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAlarm(alarm: Alarm)

    @Delete
    suspend fun deleteAlarm(alarm: Alarm)
    
    @Query("DELETE FROM alarms WHERE id = :alarmId")
    suspend fun deleteAlarmById(alarmId: Int)
}
