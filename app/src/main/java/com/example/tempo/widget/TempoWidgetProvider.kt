package com.example.tempo.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.tempo.MainActivity
import com.example.tempo.R
import com.example.tempo.analytics.StatsCalculator
import com.example.tempo.data.model.ActiveTimer

class TempoWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId, emptyMap())
        }
    }

    companion object {
        fun updateAllWidgets(context: Context, activeTimers: Map<String, ActiveTimer>) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, TempoWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            for (appWidgetId in appWidgetIds) {
                updateWidget(context, appWidgetManager, appWidgetId, activeTimers)
            }
        }

        private fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            activeTimers: Map<String, ActiveTimer>
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_tempo_layout)

            // Intent for + Add Habit button in top right corner
            val addIntent = Intent(context, MainActivity::class.java).apply {
                action = "OPEN_ADD_HABIT"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val addPendingIntent = PendingIntent.getActivity(
                context,
                101,
                addIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_add, addPendingIntent)

            // Intent for clicking the widget body to open app
            val appIntent = Intent(context, MainActivity::class.java)
            val appPendingIntent = PendingIntent.getActivity(
                context,
                0,
                appIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_title, appPendingIntent)

            // Render active stacked timers status
            if (activeTimers.isEmpty()) {
                views.setTextViewText(R.id.widget_active_timers, "No active timers running\nTap + Add to start a habit!")
            } else {
                val timerSummary = activeTimers.values.joinToString("\n") { timer ->
                    "• ${timer.habitTitle}: ${StatsCalculator.formatTicker(timer.elapsedSeconds)}"
                }
                views.setTextViewText(R.id.widget_active_timers, "Active Stack (${activeTimers.size}):\n$timerSummary")
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
