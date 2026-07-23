package com.example.tempo.service

import android.content.Context
import com.example.tempo.data.model.ActiveTimer
import com.example.tempo.data.model.Habit
import com.example.tempo.widget.TempoWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class TimerManager(private val context: Context? = null) {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var tickerJob: Job? = null

    private val json = Json { ignoreUnknownKeys = true }

    private val timersFile: File?
        get() = context?.let { File(it.filesDir, "active_timers_v1.json") }

    private val _activeTimers = MutableStateFlow<Map<String, ActiveTimer>>(emptyMap())
    val activeTimers: StateFlow<Map<String, ActiveTimer>> = _activeTimers.asStateFlow()

    init {
        loadPersistedTimers()
        startTicker()
    }

    private fun loadPersistedTimers() {
        scope.launch {
            try {
                val file = timersFile
                if (file != null && file.exists()) {
                    val raw = file.readText()
                    val map = json.decodeFromString<Map<String, ActiveTimer>>(raw)
                    val now = System.currentTimeMillis()
                    val updatedMap = map.mapValues { (_, timer) ->
                        if (!timer.isPaused) {
                            val realElapsedSec = (now - timer.startTime) / 1000L
                            timer.copy(elapsedSeconds = maxOf(timer.elapsedSeconds, realElapsedSec))
                        } else {
                            timer
                        }
                    }
                    _activeTimers.value = updatedMap

                    if (updatedMap.isNotEmpty() && context != null) {
                        val firstTimer = updatedMap.values.first()
                        TimerService.startService(context, updatedMap.size, firstTimer.habitTitle)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun persistActiveTimers() {
        scope.launch(Dispatchers.IO) {
            try {
                val file = timersFile ?: return@launch
                val map = _activeTimers.value
                if (map.isEmpty()) {
                    if (file.exists()) file.delete()
                } else {
                    val raw = json.encodeToString(map)
                    file.writeText(raw)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
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
                            val realElapsed = (now - timer.startTime) / 1000L
                            timer.copy(elapsedSeconds = maxOf(timer.elapsedSeconds + 1L, realElapsed))
                        } else {
                            timer
                        }
                    }
                    _activeTimers.value = updatedMap
                    notifyWidgetUpdate()
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
                categoryId = habit.categoryId,
                habitIconName = habit.iconName,
                startTime = now,
                elapsedSeconds = 0L,
                isPaused = false
            )
            current[habit.id] = newTimer
            _activeTimers.value = current
            persistActiveTimers()
            notifyWidgetUpdate()

            context?.let {
                TimerService.startService(it, current.size, habit.title)
            }
        }
    }

    fun togglePauseTimer(habitId: String) {
        val current = _activeTimers.value.toMutableMap()
        val timer = current[habitId] ?: return
        val isNowPaused = !timer.isPaused
        current[habitId] = timer.copy(isPaused = isNowPaused)
        _activeTimers.value = current
        persistActiveTimers()
        notifyWidgetUpdate()
    }

    fun endTimer(
        habitId: String,
        onSessionLogged: (habitId: String, startTime: Long, endTime: Long, durationSeconds: Long) -> Unit
    ) {
        val current = _activeTimers.value.toMutableMap()
        val timer = current.remove(habitId) ?: return
        _activeTimers.value = current

        val endTime = System.currentTimeMillis()
        val duration = maxOf(timer.elapsedSeconds, (endTime - timer.startTime) / 1000L)
        if (duration > 0) {
            onSessionLogged(habitId, timer.startTime, endTime, duration)
        }

        persistActiveTimers()
        notifyWidgetUpdate()

        context?.let {
            if (current.isEmpty()) {
                TimerService.stopService(it)
            } else {
                val nextTimer = current.values.first()
                TimerService.startService(it, current.size, nextTimer.habitTitle)
            }
        }
    }

    fun isTimerRunning(habitId: String): Boolean {
        return _activeTimers.value.containsKey(habitId)
    }

    fun getTimer(habitId: String): ActiveTimer? {
        return _activeTimers.value[habitId]
    }

    private fun notifyWidgetUpdate() {
        context?.let {
            TempoWidgetProvider.updateAllWidgets(it, _activeTimers.value)
        }
    }
}
