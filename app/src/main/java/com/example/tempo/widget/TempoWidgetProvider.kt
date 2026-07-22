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
import com.example.tempo.data.model.ActiveTimer

class TempoWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        var currentActiveTimers: Map<String, ActiveTimer> = emptyMap()

        fun updateAllWidgets(context: Context, activeTimers: Map<String, ActiveTimer>) {
            currentActiveTimers = activeTimers
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, TempoWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

            for (appWidgetId in appWidgetIds) {
                appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_habit_list)
                updateWidget(context, appWidgetManager, appWidgetId)
            }
        }

        private fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
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

            // Title opens main app
            val appIntent = Intent(context, MainActivity::class.java)
            val appPendingIntent = PendingIntent.getActivity(
                context,
                0,
                appIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_title, appPendingIntent)

            // Bind RemoteViewsAdapter for habit list
            val serviceIntent = Intent(context, TempoWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            views.setRemoteAdapter(R.id.widget_habit_list, serviceIntent)
            views.setEmptyView(R.id.widget_habit_list, R.id.widget_empty_view)

            // PendingIntent template for ▶ Start / ⏹ Stop item buttons
            val actionIntent = Intent(context, TempoWidgetReceiver::class.java)
            val actionPendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                actionIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            views.setPendingIntentTemplate(R.id.widget_habit_list, actionPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
