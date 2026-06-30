package com.example.ui

import android.app.Application
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*

class ClockViewModel(application: Application) : AndroidViewModel(application) {

    private val database = ClockDatabase.getDatabase(application)
    private val repository = ClockRepository(database.clockDao())

    // UI and Persistence state
    val settings: StateFlow<ClockSettings> = repository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ClockSettings())

    val alarms: StateFlow<List<Alarm>> = repository.alarmsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Display Mode (CLOCK, STOPWATCH, TIMER)
    private val _currentMode = MutableStateFlow(DisplayMode.CLOCK)
    val currentMode: StateFlow<DisplayMode> = _currentMode.asStateFlow()

    // Real-time Clock State
    private val _timeState = MutableStateFlow(TimeState())
    val timeState: StateFlow<TimeState> = _timeState.asStateFlow()

    // Stopwatch State
    private val _stopwatchState = MutableStateFlow(StopwatchState())
    val stopwatchState: StateFlow<StopwatchState> = _stopwatchState.asStateFlow()

    // Timer State
    private val _timerState = MutableStateFlow(TimerState())
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    // Alarm Waking Up State (True if an alarm is actively ringing)
    private val _ringingAlarm = MutableStateFlow<Alarm?>(null)
    val ringingAlarm: StateFlow<Alarm?> = _ringingAlarm.asStateFlow()

    // Timer Finished State (True if timer has finished counting down)
    private val _isTimerRinging = MutableStateFlow(false)
    val isTimerRinging: StateFlow<Boolean> = _isTimerRinging.asStateFlow()

    // Audio & Haptics
    private var toneGenerator: ToneGenerator? = null
    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = application.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            application.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    // Active Coroutines
    private var clockJob: Job? = null
    private var stopwatchJob: Job? = null
    private var timerJob: Job? = null
    private var ringingAlarmJob: Job? = null
    private var timerRingingJob: Job? = null

    // Set of alarm IDs that have already been triggered this minute to avoid multiple triggers
    private val triggeredAlarmsThisMinute = mutableSetOf<String>()
    private var lastCheckedMinute = -1

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 85)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        startClockUpdates()
    }

    override fun onCleared() {
        super.onCleared()
        clockJob?.cancel()
        stopwatchJob?.cancel()
        timerJob?.cancel()
        ringingAlarmJob?.cancel()
        timerRingingJob?.cancel()
        toneGenerator?.release()
    }

    // --- SOUNDS AND VIBRATIONS ---

    private fun playBeepTone() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_CDMA_PIP, 60)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun playAlarmAlertTone() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_CDMA_HIGH_L, 300)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun playTimerDoneTone() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 400)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun playClickTone() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 15)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun vibrateDevice(pattern: LongArray) {
        vibrator?.let { v ->
            if (v.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createWaveform(pattern, -1))
                } else {
                    @Suppress("DEPRECATION")
                    v.vibrate(pattern, -1)
                }
            }
        }
    }

    // --- CLOCK CONTROLS ---

    private fun startClockUpdates() {
        clockJob?.cancel()
        clockJob = viewModelScope.launch(Dispatchers.Default) {
            var colonsVisible = true
            var lastColonToggle = System.currentTimeMillis()

            while (isActive) {
                val now = System.currentTimeMillis()
                val calendar = Calendar.getInstance()

                // Check colons blinking interval
                val currentBlinkStyle = settings.value.colonBlinkStyle
                val blinkInterval = when (currentBlinkStyle) {
                    "BLINK_FAST" -> 250L
                    "BLINK_SLOW" -> 500L
                    "SOLID" -> Long.MAX_VALUE
                    "OFF" -> 0L
                    else -> 500L
                }

                if (currentBlinkStyle == "OFF") {
                    colonsVisible = false
                } else if (currentBlinkStyle == "SOLID") {
                    colonsVisible = true
                } else if (now - lastColonToggle >= blinkInterval) {
                    colonsVisible = !colonsVisible
                    lastColonToggle = now
                }

                // Format Time
                val use24h = settings.value.use24HourFormat
                val hourFormat = if (use24h) "HH" else "hh"
                val minuteFormat = "mm"
                val secondFormat = "ss"

                val hours = SimpleDateFormat(hourFormat, Locale.getDefault()).format(calendar.time)
                val minutes = SimpleDateFormat(minuteFormat, Locale.getDefault()).format(calendar.time)
                val seconds = SimpleDateFormat(secondFormat, Locale.getDefault()).format(calendar.time)
                
                val amPm = if (!use24h) {
                    if (calendar.get(Calendar.AM_PM) == Calendar.AM) "AM" else "PM"
                } else {
                    ""
                }

                val dayOfWeek = SimpleDateFormat("EEE", Locale.getDefault()).format(calendar.time).uppercase()
                val dateString = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(calendar.time).uppercase()

                _timeState.update {
                    it.copy(
                        hours = hours,
                        minutes = minutes,
                        seconds = seconds,
                        colonsVisible = colonsVisible,
                        amPm = amPm,
                        dayOfWeek = dayOfWeek,
                        dateString = dateString
                    )
                }

                // Tick Sound
                if (settings.value.tickSoundEnabled && (now % 1000 in 0..100)) {
                    // Only click once per second
                    val sec = calendar.get(Calendar.SECOND)
                    if (_timeState.value.seconds.toIntOrNull() != sec) {
                        playClickTone()
                    }
                }

                // Check Alarm Triggering (once per second)
                checkAlarms(calendar)

                delay(50L) // Fast update to remain super responsive
            }
        }
    }

    private fun checkAlarms(calendar: Calendar) {
        val currentMinute = calendar.get(Calendar.MINUTE)
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentDayOfWeek = when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            Calendar.SUNDAY -> 7
            else -> 1
        }

        // Reset triggered cache if minute changes
        if (currentMinute != lastCheckedMinute) {
            triggeredAlarmsThisMinute.clear()
            lastCheckedMinute = currentMinute
        }

        val activeRinging = _ringingAlarm.value
        if (activeRinging != null) return // Already ringing

        val activeAlarms = alarms.value.filter { it.isEnabled }
        for (alarm in activeAlarms) {
            val key = "${alarm.id}_${currentHour}_${currentMinute}"
            if (alarm.hour == currentHour && alarm.minute == currentMinute && !triggeredAlarmsThisMinute.contains(key)) {
                
                // Check if repeat is set. If empty, triggers on any day once.
                val repeatDays = alarm.getRepeatDaysList()
                if (repeatDays.isEmpty() || repeatDays.contains(currentDayOfWeek)) {
                    triggeredAlarmsThisMinute.add(key)
                    triggerAlarm(alarm)
                    break
                }
            }
        }
    }

    private fun triggerAlarm(alarm: Alarm) {
        _ringingAlarm.value = alarm
        _currentMode.value = DisplayMode.CLOCK // Switch to clock mode to show alarm screen

        // Start repetitive alarm beep and vibrate job
        ringingAlarmJob?.cancel()
        ringingAlarmJob = viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                playAlarmAlertTone()
                if (alarm.vibrate) {
                    vibrateDevice(longArrayOf(0, 300, 200, 300))
                }
                delay(1200L) // Beep repeating interval
            }
        }
    }

    fun snoozeAlarm() {
        val alarm = _ringingAlarm.value ?: return
        dismissAlarm()

        // Snooze for 5 minutes by creating a one-off temporary alarm
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MINUTE, 5)
        val snoozeHour = calendar.get(Calendar.HOUR_OF_DAY)
        val snoozeMin = calendar.get(Calendar.MINUTE)

        viewModelScope.launch {
            repository.saveAlarm(
                Alarm(
                    hour = snoozeHour,
                    minute = snoozeMin,
                    label = "Snooze (${alarm.label})",
                    isEnabled = true,
                    repeatDays = "", // One shot
                    vibrate = alarm.vibrate
                )
            )
        }
    }

    fun dismissAlarm() {
        _ringingAlarm.value = null
        ringingAlarmJob?.cancel()
        
        // Turn off disabled state for one-shot alarms
        val activeRinging = _ringingAlarm.value
        if (activeRinging != null && activeRinging.repeatDays.isEmpty()) {
            viewModelScope.launch {
                repository.saveAlarm(activeRinging.copy(isEnabled = false))
            }
        }
    }

    // --- STOPWATCH CONTROLS ---

    fun startStopwatch() {
        if (_stopwatchState.value.isRunning) return
        playClickTone()
        val stopwatch = _stopwatchState.value
        val startTime = System.currentTimeMillis() - stopwatch.accumulatedTime

        _stopwatchState.update { it.copy(isRunning = true) }

        stopwatchJob?.cancel()
        stopwatchJob = viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                val elapsed = System.currentTimeMillis() - startTime
                
                val mins = (elapsed / 60000) % 100
                val secs = (elapsed / 1000) % 60
                val centis = (elapsed / 10) % 100

                _stopwatchState.update {
                    it.copy(
                        minutes = String.format("%02d", mins),
                        seconds = String.format("%02d", secs),
                        centiseconds = String.format("%02d", centis),
                        accumulatedTime = elapsed
                    )
                }
                delay(15) // Centisecond precision requires fast updates (approx 60fps)
            }
        }
    }

    fun pauseStopwatch() {
        if (!_stopwatchState.value.isRunning) return
        playClickTone()
        stopwatchJob?.cancel()
        _stopwatchState.update { it.copy(isRunning = false) }
    }

    fun lapStopwatch() {
        playClickTone()
        val state = _stopwatchState.value
        val lapNum = state.laps.size + 1
        val totalFormatted = "${state.minutes}:${state.seconds}.${state.centiseconds}"
        
        val lastLapTime = state.laps.firstOrNull()?.totalTimeMs ?: 0L
        val lapDiffMs = state.accumulatedTime - lastLapTime
        val lapDiffMins = (lapDiffMs / 60000) % 100
        val lapDiffSecs = (lapDiffMs / 1000) % 60
        val lapDiffCentis = (lapDiffMs / 10) % 100
        val lapFormatted = String.format("%02d:%02d.%02d", lapDiffMins, lapDiffSecs, lapDiffCentis)

        val newLap = StopwatchLap(
            number = lapNum,
            lapTime = lapFormatted,
            totalTime = totalFormatted,
            totalTimeMs = state.accumulatedTime
        )
        
        _stopwatchState.update {
            it.copy(laps = listOf(newLap) + it.laps) // New laps added at top
        }
    }

    fun resetStopwatch() {
        playClickTone()
        stopwatchJob?.cancel()
        _stopwatchState.value = StopwatchState()
    }

    // --- TIMER CONTROLS ---

    fun startTimer(durationSeconds: Long? = null) {
        if (_timerState.value.isRunning) return
        playBeepTone()

        val activeDuration = durationSeconds ?: _timerState.value.totalDurationSeconds
        val totalMs = activeDuration * 1000L
        val targetEndTime = System.currentTimeMillis() + (if (_timerState.value.remainingSeconds > 0) {
            _timerState.value.remainingSeconds * 1000L
        } else {
            totalMs
        })

        // Save last used duration
        if (durationSeconds != null) {
            viewModelScope.launch {
                val currentSettings = repository.getSettings()
                repository.updateSettings(currentSettings.copy(timerDurationSeconds = durationSeconds))
            }
        }

        _timerState.update {
            it.copy(
                isRunning = true,
                totalDurationSeconds = activeDuration
            )
        }

        timerJob?.cancel()
        timerJob = viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                val remainingMs = targetEndTime - System.currentTimeMillis()

                if (remainingMs <= 0) {
                    _timerState.update {
                        it.copy(
                            hours = "00",
                            minutes = "00",
                            seconds = "00",
                            remainingSeconds = 0L,
                            isRunning = false,
                            progress = 0f
                        )
                    }
                    triggerTimerFinished()
                    break
                }

                val totalRemainingSecs = (remainingMs + 999L) / 1000L // Round up
                val hrs = totalRemainingSecs / 3600
                val mins = (totalRemainingSecs % 3600) / 60
                val secs = totalRemainingSecs % 60
                val progress = totalRemainingSecs.toFloat() / activeDuration.toFloat()

                _timerState.update {
                    it.copy(
                        hours = String.format("%02d", hrs),
                        minutes = String.format("%02d", mins),
                        seconds = String.format("%02d", secs),
                        remainingSeconds = totalRemainingSecs,
                        progress = progress
                    )
                }

                delay(100L)
            }
        }
    }

    fun pauseTimer() {
        if (!_timerState.value.isRunning) return
        playClickTone()
        timerJob?.cancel()
        _timerState.update { it.copy(isRunning = false) }
    }

    fun resetTimer() {
        playClickTone()
        timerJob?.cancel()
        val originalDuration = _timerState.value.totalDurationSeconds
        val hrs = originalDuration / 3600
        val mins = (originalDuration % 3600) / 60
        val secs = originalDuration % 60

        _timerState.update {
            it.copy(
                hours = String.format("%02d", hrs),
                minutes = String.format("%02d", mins),
                seconds = String.format("%02d", secs),
                remainingSeconds = originalDuration,
                isRunning = false,
                progress = 1f
            )
        }
        dismissTimerRinging()
    }

    private fun triggerTimerFinished() {
        _isTimerRinging.value = true

        // Buzz timer alarm
        timerRingingJob?.cancel()
        timerRingingJob = viewModelScope.launch(Dispatchers.Default) {
            var counter = 0
            while (isActive && counter < 10) { // Limit to 10 repeating buzzes (approx 12 secs)
                playTimerDoneTone()
                vibrateDevice(longArrayOf(0, 500, 200, 500))
                counter++
                delay(1200L)
            }
            _isTimerRinging.value = false
        }
    }

    fun dismissTimerRinging() {
        _isTimerRinging.value = false
        timerRingingJob?.cancel()
    }

    // --- DISPLAY MODE CONTROLS ---

    fun setDisplayMode(mode: DisplayMode) {
        if (_currentMode.value == mode) return
        playClickTone()
        _currentMode.value = mode
    }

    // --- SETTINGS CRUD CONTROLS ---

    fun updateLedColor(hex: String) {
        playClickTone()
        viewModelScope.launch {
            val currentSettings = repository.getSettings()
            repository.updateSettings(currentSettings.copy(ledColorHex = hex))
        }
    }

    fun toggleFormat24h() {
        playClickTone()
        viewModelScope.launch {
            val currentSettings = repository.getSettings()
            repository.updateSettings(currentSettings.copy(use24HourFormat = !currentSettings.use24HourFormat))
        }
    }

    fun toggleShowSeconds() {
        playClickTone()
        viewModelScope.launch {
            val currentSettings = repository.getSettings()
            repository.updateSettings(currentSettings.copy(showSeconds = !currentSettings.showSeconds))
        }
    }

    fun toggleSlanted() {
        playClickTone()
        viewModelScope.launch {
            val currentSettings = repository.getSettings()
            repository.updateSettings(currentSettings.copy(isSlanted = !currentSettings.isSlanted))
        }
    }

    fun updateSlantAngle(angle: Float) {
        viewModelScope.launch {
            val currentSettings = repository.getSettings()
            repository.updateSettings(currentSettings.copy(slantAngle = angle))
        }
    }

    fun updateInactiveOpacity(opacity: Float) {
        viewModelScope.launch {
            val currentSettings = repository.getSettings()
            repository.updateSettings(currentSettings.copy(inactiveOpacity = opacity))
        }
    }

    fun updateColonBlinkStyle(style: String) {
        playClickTone()
        viewModelScope.launch {
            val currentSettings = repository.getSettings()
            repository.updateSettings(currentSettings.copy(colonBlinkStyle = style))
        }
    }

    fun toggleTickSound() {
        playClickTone()
        viewModelScope.launch {
            val currentSettings = repository.getSettings()
            repository.updateSettings(currentSettings.copy(tickSoundEnabled = !currentSettings.tickSoundEnabled))
        }
    }

    // --- ALARM CRUD CONTROLS ---

    fun addOrUpdateAlarm(alarm: Alarm) {
        playClickTone()
        viewModelScope.launch {
            repository.saveAlarm(alarm)
        }
    }

    fun deleteAlarm(alarm: Alarm) {
        playClickTone()
        viewModelScope.launch {
            repository.deleteAlarm(alarm)
        }
    }

    fun toggleAlarmEnabled(alarm: Alarm) {
        playClickTone()
        viewModelScope.launch {
            repository.saveAlarm(alarm.copy(isEnabled = !alarm.isEnabled))
        }
    }
}

// --- SUPPORTING DATA CLASSES ---

enum class DisplayMode {
    CLOCK,
    STOPWATCH,
    TIMER,
    ALARMS
}

data class TimeState(
    val hours: String = "00",
    val minutes: String = "00",
    val seconds: String = "00",
    val colonsVisible: Boolean = true,
    val amPm: String = "",
    val dayOfWeek: String = "",
    val dateString: String = ""
)

data class StopwatchState(
    val isRunning: Boolean = false,
    val minutes: String = "00",
    val seconds: String = "00",
    val centiseconds: String = "00",
    val accumulatedTime: Long = 0L,
    val laps: List<StopwatchLap> = emptyList()
)

data class StopwatchLap(
    val number: Int,
    val lapTime: String,
    val totalTime: String,
    val totalTimeMs: Long
)

data class TimerState(
    val isRunning: Boolean = false,
    val hours: String = "00",
    val minutes: String = "00",
    val seconds: String = "00",
    val remainingSeconds: Long = 60L,
    val totalDurationSeconds: Long = 60L,
    val progress: Float = 1.0f
)
