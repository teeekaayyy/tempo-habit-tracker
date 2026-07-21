package com.example.tempo.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.tempo.MainActivity

class TimerService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP_SERVICE) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val activeCount = intent?.getIntExtra(EXTRA_ACTIVE_COUNT, 1) ?: 1
        val habitTitle = intent?.getStringExtra(EXTRA_HABIT_TITLE) ?: "Habit"

        createNotificationChannel()
        val notification = buildNotification(habitTitle, activeCount)
        startForeground(NOTIFICATION_ID, notification)

        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Active Habit Timers",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows ongoing habit tracking notification while timers are active."
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(title: String, activeCount: Int): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = if (activeCount > 1) {
            "$activeCount habits currently tracking in background"
        } else {
            "Tracking: $title in background"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Tempo Habit Tracker")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "tempo_timer_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START_SERVICE = "com.example.tempo.action.START_TIMER_SERVICE"
        const val ACTION_STOP_SERVICE = "com.example.tempo.action.STOP_TIMER_SERVICE"
        const val EXTRA_ACTIVE_COUNT = "extra_active_count"
        const val EXTRA_HABIT_TITLE = "extra_habit_title"

        fun startService(context: Context, activeCount: Int, title: String) {
            val intent = Intent(context, TimerService::class.java).apply {
                action = ACTION_START_SERVICE
                putExtra(EXTRA_ACTIVE_COUNT, activeCount)
                putExtra(EXTRA_HABIT_TITLE, title)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, TimerService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            context.startService(intent)
        }
    }
}
