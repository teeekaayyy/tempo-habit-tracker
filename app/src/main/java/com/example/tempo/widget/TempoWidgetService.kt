package com.example.tempo.widget

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.tempo.R
import com.example.tempo.analytics.StatsCalculator
import com.example.tempo.data.auth.AuthManager
import com.example.tempo.data.model.ActiveTimer
import com.example.tempo.data.model.Category
import com.example.tempo.data.model.Habit
import com.example.tempo.data.repository.TempoRepository

class TempoWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return TempoWidgetFactory(applicationContext)
    }
}

class TempoWidgetFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {

    private var habits: List<Habit> = emptyList()
    private var categoryMap: Map<String, Category> = emptyMap()
    private var activeTimers: Map<String, ActiveTimer> = emptyMap()

    override fun onCreate() {}

    override fun onDataSetChanged() {
        val authManager = AuthManager(context)
        val currentUser = authManager.getCurrentAccount()
        val repository = TempoRepository(context, currentUser)

        habits = repository.habits.value
        categoryMap = repository.categories.value.associateBy { it.id }
        activeTimers = TempoWidgetProvider.currentActiveTimers
    }

    override fun onDestroy() {}

    override fun getCount(): Int = habits.size

    override fun getViewAt(position: Int): RemoteViews {
        if (position >= habits.size) return RemoteViews(context.packageName, R.layout.widget_habit_item)

        val habit = habits[position]
        val category = categoryMap[habit.categoryId]
        val catColorHex = category?.colorHex ?: "#6366F1"
        val activeTimer = activeTimers[habit.id]
        val isRunning = activeTimer != null

        val views = RemoteViews(context.packageName, R.layout.widget_habit_item)

        views.setTextViewText(R.id.widget_item_title, habit.title)
        views.setTextViewText(R.id.widget_item_subtitle, "${category?.name ?: "Habit"} • ${habit.targetDurationMinutes}m target")

        // Parse category color
        try {
            val colorInt = Color.parseColor(catColorHex)
            views.setInt(R.id.widget_item_color_bar, "setBackgroundColor", colorInt)
        } catch (e: Exception) {
            views.setInt(R.id.widget_item_color_bar, "setBackgroundColor", Color.parseColor("#6366F1"))
        }

        if (isRunning && activeTimer != null) {
            views.setViewVisibility(R.id.widget_item_ticker, android.view.View.VISIBLE)
            views.setTextViewText(R.id.widget_item_ticker, StatsCalculator.formatTicker(activeTimer.elapsedSeconds))
            views.setTextViewText(R.id.widget_item_btn_action, "⏹ Stop")
            views.setInt(R.id.widget_item_btn_action, "setBackgroundColor", Color.parseColor("#F43F5E"))

            val fillInIntent = Intent().apply {
                action = TempoWidgetReceiver.ACTION_WIDGET_STOP_TIMER
                putExtra(TempoWidgetReceiver.EXTRA_HABIT_ID, habit.id)
            }
            views.setOnClickFillInIntent(R.id.widget_item_btn_action, fillInIntent)
        } else {
            views.setViewVisibility(R.id.widget_item_ticker, android.view.View.GONE)
            views.setTextViewText(R.id.widget_item_btn_action, "▶ Start")
            views.setInt(R.id.widget_item_btn_action, "setBackgroundColor", Color.parseColor("#6366F1"))

            val fillInIntent = Intent().apply {
                action = TempoWidgetReceiver.ACTION_WIDGET_START_TIMER
                putExtra(TempoWidgetReceiver.EXTRA_HABIT_ID, habit.id)
            }
            views.setOnClickFillInIntent(R.id.widget_item_btn_action, fillInIntent)
        }

        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = true
}
