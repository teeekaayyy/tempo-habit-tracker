package com.example.tempo.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.example.tempo.MainActivity
import com.example.tempo.R
import com.example.tempo.data.model.ActiveTimer
import com.example.tempo.data.model.BackupData
import com.example.tempo.data.model.Habit
import kotlinx.serialization.json.Json
import java.io.File

class TempoWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_TOGGLE_HABIT_TIMER) {
            val habitId = intent.getStringExtra(EXTRA_HABIT_ID) ?: return
            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("EXTRA_START_HABIT_ID", habitId)
            }
            context.startActivity(launchIntent)
        }
    }

    companion object {
        const val ACTION_TOGGLE_HABIT_TIMER = "com.example.tempo.ACTION_WIDGET_TOGGLE_HABIT"
        const val EXTRA_HABIT_ID = "extra_habit_id"

        private val json = Json { ignoreUnknownKeys = true }

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, TempoWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
                for (id in appWidgetIds) {
                    updateAppWidget(context, appWidgetManager, id)
                }
            }
        }

        private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_tempo)

            // Header click opens app
            val openAppIntent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context, 0, openAppIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_status, pendingIntent)

            // Load habits from JSON file
            val habits = loadHabitsFromDisk(context)
            val favorites = habits.filter { it.isFavorite }.ifEmpty { habits.take(3) }
            val activeTimerMap = loadActiveTimersFromPrefs(context)

            views.removeAllViews(R.id.widget_habits_container)

            if (favorites.isEmpty()) {
                views.setTextViewText(R.id.widget_status, "No Favorites Yet")
            } else {
                val activeCount = activeTimerMap.size
                views.setTextViewText(
                    R.id.widget_status,
                    if (activeCount > 0) "⚡ $activeCount Active" else "⭐ ${favorites.size} Favorites"
                )

                for (habit in favorites) {
                    val rowView = RemoteViews(context.packageName, R.layout.widget_habit_row)
                    rowView.setTextViewText(R.id.item_title, habit.title)

                    val targetText = if (habit.targetDurationMinutes <= 0) "Open-ended" else if (habit.targetDurationMinutes >= 60) "${habit.targetDurationMinutes / 60}h target" else "${habit.targetDurationMinutes}m target"
                    rowView.setTextViewText(R.id.item_subtitle, targetText)

                    val activeTimer = activeTimerMap[habit.id]
                    if (activeTimer != null) {
                        // Timer is running! Show live progress bar & duration
                        val now = System.currentTimeMillis()
                        val elapsedSec = if (!activeTimer.isPaused) (now - activeTimer.startTime) / 1000L else activeTimer.elapsedSeconds
                        val formattedTime = formatSeconds(elapsedSec)

                        rowView.setViewVisibility(R.id.item_progress_container, View.VISIBLE)
                        rowView.setTextViewText(R.id.item_timer_text, "⏱️ $formattedTime tracking...")

                        if (habit.targetDurationMinutes > 0) {
                            val targetSec = habit.targetDurationMinutes * 60L
                            val percent = ((elapsedSec.toDouble() / targetSec.toDouble()) * 100).toInt().coerceIn(0, 100)
                            rowView.setProgressBar(R.id.item_progress_bar, 100, percent, false)
                            rowView.setTextViewText(R.id.item_progress_percent, "$percent%")
                        } else {
                            rowView.setProgressBar(R.id.item_progress_bar, 100, 100, true) // Indeterminate for open-ended
                            rowView.setTextViewText(R.id.item_progress_percent, "∞")
                        }

                        rowView.setTextViewText(R.id.item_action_btn, "⏹ Stop")
                    } else {
                        rowView.setViewVisibility(R.id.item_progress_container, View.GONE)
                        rowView.setTextViewText(R.id.item_action_btn, "▶ Start")
                    }

                    // Click handler to toggle timer or open app
                    val actionIntent = Intent(context, TempoWidgetProvider::class.java).apply {
                        action = ACTION_TOGGLE_HABIT_TIMER
                        putExtra(EXTRA_HABIT_ID, habit.id)
                    }
                    val actionPendingIntent = PendingIntent.getBroadcast(
                        context,
                        habit.id.hashCode(),
                        actionIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    rowView.setOnClickPendingIntent(R.id.item_action_btn, actionPendingIntent)
                    rowView.setOnClickPendingIntent(R.id.habit_row_root, actionPendingIntent)

                    views.addView(R.id.widget_habits_container, rowView)
                }
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun loadHabitsFromDisk(context: Context): List<Habit> {
            return try {
                val file = File(context.filesDir, "tempo_data_v1.json")
                if (file.exists()) {
                    val backupData = json.decodeFromString<BackupData>(file.readText())
                    backupData.habits
                } else emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }

        private fun loadActiveTimersFromPrefs(context: Context): Map<String, ActiveTimer> {
            return try {
                val prefs = context.getSharedPreferences("tempo_active_timers", Context.MODE_PRIVATE)
                val savedJson = prefs.getString("active_timers_json", null) ?: return emptyMap()
                val list = json.decodeFromString<List<ActiveTimer>>(savedJson)
                list.associateBy { it.habitId }
            } catch (e: Exception) {
                emptyMap()
            }
        }

        private fun formatSeconds(seconds: Long): String {
            val h = seconds / 3600
            val m = (seconds % 3600) / 60
            val s = seconds % 60
            return if (h > 0) String.format("%02d:%02d:%02d", h, m, s) else String.format("%02d:%02d", m, s)
        }
    }
}
