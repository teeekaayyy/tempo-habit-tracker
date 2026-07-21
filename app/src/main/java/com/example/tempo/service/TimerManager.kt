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

class TimerManager(private val context: Context? = null) {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var tickerJob: Job? = null

    private val _activeTimers = MutableStateFlow<Map<String, ActiveTimer>>(emptyMap())
    val activeTimers: StateFlow<Map<String, ActiveTimer>> = _activeTimers.asStateFlow()

    init {
        startTicker()
    }

    private fun startTicker() {
        if (tickerJob?.isActive == true) return
        tickerJob = scope.launch {
            while (isActive) {
                delay(1000L)
                val currentMap = _activeTimers.value
                if (currentMap.isNotEmpty()) {
                    val updatedMap = currentMap.mapValues { (_, timer) ->
                        if (!timer.isPaused) {
                            timer.copy(elapsedSeconds = timer.elapsedSeconds + 1L)
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
            val newTimer = ActiveTimer(
                habitId = habit.id,
                habitTitle = habit.title,
                habitColorHex = habit.colorHex,
                habitIconName = habit.iconName,
                startTime = System.currentTimeMillis(),
                elapsedSeconds = 0L,
                isPaused = false
            )
            current[habit.id] = newTimer
            _activeTimers.value = current

            context?.let { ctx ->
                TimerService.startService(ctx, current.size, habit.title)
            }
        }
    }

    fun togglePauseTimer(habitId: String) {
        val current = _activeTimers.value.toMutableMap()
        val timer = current[habitId] ?: return
        current[habitId] = timer.copy(isPaused = !timer.isPaused)
        _activeTimers.value = current
    }

    fun endTimer(
        habitId: String,
        onSessionLogged: (habitId: String, startTime: Long, endTime: Long, durationSeconds: Long) -> Unit
    ) {
        val current = _activeTimers.value.toMutableMap()
        val timer = current.remove(habitId) ?: return
        _activeTimers.value = current

        if (current.isEmpty()) {
            context?.let { ctx -> TimerService.stopService(ctx) }
        } else {
            val remainingTitle = current.values.first().habitTitle
            context?.let { ctx -> TimerService.startService(ctx, current.size, remainingTitle) }
        }

        val endTime = System.currentTimeMillis()
        val duration = timer.elapsedSeconds
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
