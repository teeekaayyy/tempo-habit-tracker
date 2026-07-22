package com.example.tempo.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.tempo.data.auth.AuthManager
import com.example.tempo.data.repository.TempoRepository
import com.example.tempo.service.TimerManager

class TempoWidgetReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val habitId = intent.getStringExtra(EXTRA_HABIT_ID) ?: return

        val authManager = AuthManager(context)
        val currentUser = authManager.getCurrentAccount()
        val repository = TempoRepository(context, currentUser)
        val timerManager = TimerManager(context)

        val habits = repository.habits.value
        val habit = habits.firstOrNull { it.id == habitId }

        when (action) {
            ACTION_WIDGET_START_TIMER -> {
                if (habit != null) {
                    timerManager.startTimer(habit)
                }
            }
            ACTION_WIDGET_STOP_TIMER -> {
                timerManager.endTimer(habitId) { hId, startTime, endTime, durationSec ->
                    repository.logSession(hId, startTime, endTime, durationSec)
                }
            }
        }

        TempoWidgetProvider.updateAllWidgets(context, timerManager.activeTimers.value)
    }

    companion object {
        const val ACTION_WIDGET_START_TIMER = "com.example.tempo.ACTION_WIDGET_START_TIMER"
        const val ACTION_WIDGET_STOP_TIMER = "com.example.tempo.ACTION_WIDGET_STOP_TIMER"
        const val EXTRA_HABIT_ID = "extra_habit_id"
    }
}
