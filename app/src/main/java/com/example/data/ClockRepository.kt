package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ClockRepository(private val clockDao: ClockDao) {
    
    val settingsFlow: Flow<ClockSettings> = clockDao.getSettingsFlow().map { it ?: ClockSettings() }
    val alarmsFlow: Flow<List<Alarm>> = clockDao.getAlarmsFlow()

    suspend fun getSettings(): ClockSettings {
        return clockDao.getSettings() ?: ClockSettings()
    }

    suspend fun updateSettings(settings: ClockSettings) {
        clockDao.insertOrUpdateSettings(settings)
    }

    suspend fun saveAlarm(alarm: Alarm) {
        clockDao.insertOrUpdateAlarm(alarm)
    }

    suspend fun deleteAlarm(alarm: Alarm) {
        clockDao.deleteAlarm(alarm)
    }

    suspend fun deleteAlarmById(alarmId: Int) {
        clockDao.deleteAlarmById(alarmId)
    }
}
