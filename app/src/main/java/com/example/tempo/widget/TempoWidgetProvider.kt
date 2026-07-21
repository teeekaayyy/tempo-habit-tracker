package com.example.tempo.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.example.tempo.MainActivity
import com.example.tempo.R
import com.example.tempo.data.model.BackupData
import com.example.tempo.service.TimerManager
import kotlinx.serialization.json.Json
import java.io.File

class TempoWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_TOGGLE_HABIT_TIMER) {
            val habitId = intent.getStringExtra(EXTRA_HABIT_ID) ?: return
            
            // Toggle timer in background without launching app
            val timerManager = TimerManager(context)
            val isRunning = timerManager.isTimerRunning(habitId)
            
            if (isRunning) {
                timerManager.endTimer(habitId) { _, _, _, _ -> }
            } else {
                val habit = loadHabitById(context, habitId)
                if (habit != null) {
                    timerManager.startTimer(habit)
                }
            }

            // Refresh widget ListView
            updateAllWidgets(context)
        }
    }

    companion object {
        const val ACTION_TOGGLE_HABIT_TIMER = "com.example.tempo.ACTION_WIDGET_TOGGLE_HABIT"
        const val EXTRA_HABIT_ID = "extra_habit_id"

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, TempoWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName) ?: return
            
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_list_view)
            for (id in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, id)
            }
        }

        private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_tempo)

            // Service Intent for scrollable ListView
            val serviceIntent = Intent(context, TempoWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            views.setRemoteAdapter(R.id.widget_list_view, serviceIntent)

            // PendingIntent Template for 1-tap start/stop broadcast without opening main app
            val toggleIntent = Intent(context, TempoWidgetProvider::class.java).apply {
                action = ACTION_TOGGLE_HABIT_TIMER
            }
            val togglePendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                toggleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            views.setPendingIntentTemplate(R.id.widget_list_view, togglePendingIntent)

            // Header click opens main app
            val openAppIntent = Intent(context, MainActivity::class.java)
            val openAppPendingIntent = PendingIntent.getActivity(
                context, 0, openAppIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_status, openAppPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun loadHabitById(context: Context, habitId: String) = try {
            val file = File(context.filesDir, "tempo_data_v1.json")
            if (file.exists()) {
                val json = Json { ignoreUnknownKeys = true }
                val backupData = json.decodeFromString<BackupData>(file.readText())
                backupData.habits.find { it.id == habitId }
            } else null
        } catch (e: Exception) {
            null
        }
    }
}
