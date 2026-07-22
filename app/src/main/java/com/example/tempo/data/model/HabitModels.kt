package com.example.tempo.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Habit(
    val id: String,
    val title: String,
    val description: String = "",
    val categoryId: String = "cat_prod",
    val iconName: String = "Star",
    val targetDurationMinutes: Int = 30,
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class HabitSession(
    val id: String,
    val habitId: String,
    val startTime: Long,
    val endTime: Long,
    val durationSeconds: Long,
    val dateIso: String // "YYYY-MM-DD"
)

@Serializable
data class ActiveTimer(
    val habitId: String,
    val habitTitle: String,
    val categoryId: String,
    val habitIconName: String,
    val startTime: Long,
    var elapsedSeconds: Long = 0L,
    var isPaused: Boolean = false
)

@Serializable
data class BackupData(
    val version: Int = 2,
    val exportTimestamp: Long = System.currentTimeMillis(),
    val account: UserAccount? = null,
    val categories: List<Category> = emptyList(),
    val habits: List<Habit> = emptyList(),
    val sessions: List<HabitSession> = emptyList()
)
