package com.example.tempo.widget

import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.tempo.R
import com.example.tempo.data.model.ActiveTimer
import com.example.tempo.data.model.BackupData
import com.example.tempo.data.model.Habit
import kotlinx.serialization.json.Json
import java.io.File

class TempoWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return TempoWidgetFactory(applicationContext)
    }
}

class TempoWidgetFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {

    private val json = Json { ignoreUnknownKeys = true }
    private var habits: List<Habit> = emptyList()
    private var activeTimerMap: Map<String, ActiveTimer> = emptyMap()

    override fun onCreate() {}

    override fun onDataSetChanged() {
        habits = loadHabitsFromDisk(context)
        activeTimerMap = loadActiveTimersFromPrefs(context)
    }

    override fun onDestroy() {}

    override fun getCount(): Int {
        val favorites = habits.filter { it.isFavorite }.ifEmpty { habits }
        return favorites.size
    }

    override fun getViewAt(position: Int): RemoteViews {
        val favorites = habits.filter { it.isFavorite }.ifEmpty { habits }
        if (position < 0 || position >= favorites.size) {
            return RemoteViews(context.packageName, R.layout.widget_habit_row)
        }

        val habit = favorites[position]
        val rowView = RemoteViews(context.packageName, R.layout.widget_habit_row)

        rowView.setTextViewText(R.id.item_title, habit.title)

        val targetText = if (habit.targetDurationMinutes <= 0) "Open-ended"
        else if (habit.targetDurationMinutes >= 60) "${habit.targetDurationMinutes / 60}h target"
        else "${habit.targetDurationMinutes}m target"
        rowView.setTextViewText(R.id.item_subtitle, targetText)

        val activeTimer = activeTimerMap[habit.id]
        if (activeTimer != null) {
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
                rowView.setProgressBar(R.id.item_progress_bar, 100, 100, true)
                rowView.setTextViewText(R.id.item_progress_percent, "∞")
            }

            rowView.setTextViewText(R.id.item_action_btn, "⏹ Stop")
        } else {
            rowView.setViewVisibility(R.id.item_progress_container, View.GONE)
            rowView.setTextViewText(R.id.item_action_btn, "▶ Start")
        }

        // FillInIntent for direct 1-tap start/stop broadcast without opening main app
        val fillInIntent = Intent().apply {
            putExtra(TempoWidgetProvider.EXTRA_HABIT_ID, habit.id)
        }
        rowView.setOnClickFillInIntent(R.id.item_action_btn, fillInIntent)
        rowView.setOnClickFillInIntent(R.id.habit_row_root, fillInIntent)

        return rowView
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = true

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
