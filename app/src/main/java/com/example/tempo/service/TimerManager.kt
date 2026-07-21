package com.example.tempo.service

import android.content.Context
import com.example.tempo.data.model.ActiveTimer
import com.example.tempo.data.model.Habit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class TimerManager(private val context: Context? = null) {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var tickerJob: Job? = null

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val prefs = context?.getSharedPreferences("tempo_active_timers", Context.MODE_PRIVATE)

    private val _activeTimers = MutableStateFlow<Map<String, ActiveTimer>>(emptyMap())
    val activeTimers: StateFlow<Map<String, ActiveTimer>> = _activeTimers.asStateFlow()

    init {
        restoreTimersFromPrefs()
        startTicker()
    }

    private fun restoreTimersFromPrefs() {
        val savedJson = prefs?.getString("active_timers_json", null) ?: return
        try {
            val list = json.decodeFromString<List<ActiveTimer>>(savedJson)
            val now = System.currentTimeMillis()
            val map = mutableMapOf<String, ActiveTimer>()
            for (timer in list) {
                val elapsed = if (!timer.isPaused) {
                    (now - timer.startTime) / 1000L
                } else {
                    timer.elapsedSeconds
                }
                map[timer.habitId] = timer.copy(elapsedSeconds = maxOf(0L, elapsed))
            }
            _activeTimers.value = map

            if (map.isNotEmpty() && context != null) {
                TimerService.startService(context, map.size, map.values.first().habitTitle)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveTimersToPrefs(map: Map<String, ActiveTimer>) {
        try {
            val rawJson = json.encodeToString(map.values.toList())
            prefs?.edit()?.putString("active_timers_json", rawJson)?.apply()
            context?.let { com.example.tempo.widget.TempoWidgetProvider.updateAllWidgets(it) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startTicker() {
        if (tickerJob?.isActive == true) return
        tickerJob = scope.launch {
            while (isActive) {
                delay(1000L)
                val currentMap = _activeTimers.value
                if (currentMap.isNotEmpty()) {
                    val now = System.currentTimeMillis()
                    val updatedMap = currentMap.mapValues { (_, timer) ->
                        if (!timer.isPaused) {
                            val elapsed = (now - timer.startTime) / 1000L
                            timer.copy(elapsedSeconds = maxOf(0L, elapsed))
                        } else {
                            timer
                        }
                    }
                    _activeTimers.value = updatedMap
                }
            }
        }
    }

    fun startTimer(habit: Habit) {
        val current = _activeTimers.value.toMutableMap()
        if (!current.containsKey(habit.id)) {
            val now = System.currentTimeMillis()
            val newTimer = ActiveTimer(
                habitId = habit.id,
                habitTitle = habit.title,
                habitColorHex = habit.colorHex,
                habitIconName = habit.iconName,
                startTime = now,
                elapsedSeconds = 0L,
                isPaused = false
            )
            current[habit.id] = newTimer
            _activeTimers.value = current
            saveTimersToPrefs(current)

            context?.let { ctx ->
                TimerService.startService(ctx, current.size, habit.title)
            }
        }
    }

    fun togglePauseTimer(habitId: String) {
        val current = _activeTimers.value.toMutableMap()
        val timer = current[habitId] ?: return
        val updated = timer.copy(isPaused = !timer.isPaused)
        current[habitId] = updated
        _activeTimers.value = current
        saveTimersToPrefs(current)
    }

    fun endTimer(
        habitId: String,
        onSessionLogged: (habitId: String, startTime: Long, endTime: Long, durationSeconds: Long) -> Unit
    ) {
        val current = _activeTimers.value.toMutableMap()
        val timer = current.remove(habitId) ?: return
        _activeTimers.value = current
        saveTimersToPrefs(current)

        if (current.isEmpty()) {
            context?.let { ctx -> TimerService.stopService(ctx) }
        } else {
            val remainingTitle = current.values.first().habitTitle
            context?.let { ctx -> TimerService.startService(ctx, current.size, remainingTitle) }
        }

        val endTime = System.currentTimeMillis()
        val duration = maxOf(timer.elapsedSeconds, (endTime - timer.startTime) / 1000L)
        if (duration > 0) {
            onSessionLogged(habitId, timer.startTime, endTime, duration)
        }
    }

    fun isTimerRunning(habitId: String): Boolean {
        return _activeTimers.value.containsKey(habitId)
    }

    fun getTimer(habitId: String): ActiveTimer? {
        return _activeTimers.value[habitId]
    }
}
