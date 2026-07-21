package com.example.tempo.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class HabitCategory(val displayName: String, val defaultColorHex: String) {
    HEALTH("Health & Wellness", "#10B981"),
    FITNESS("Fitness & Exercise", "#F59E0B"),
    PRODUCTIVITY("Productivity & Work", "#6366F1"),
    MINDFULNESS("Mindfulness & Rest", "#8B5CF6"),
    LEARNING("Learning & Growth", "#EC4899"),
    OTHER("Custom & Personal", "#3B82F6")
}

@Serializable
data class Habit(
    val id: String,
    val title: String,
    val description: String = "",
    val category: HabitCategory = HabitCategory.OTHER,
    val colorHex: String = "#6366F1",
    val iconName: String = "Star",
    val targetDurationMinutes: Int = 30,
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

fun Habit.formattedTargetDuration(): String {
    if (targetDurationMinutes <= 0) return "Open-ended"
    val hours = targetDurationMinutes / 60
    val mins = targetDurationMinutes % 60
    return when {
        hours > 0 && mins > 0 -> "${hours}h ${mins}m"
        hours > 0 -> "${hours} hrs"
        else -> "${mins} mins"
    }
}

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
    val habitColorHex: String,
    val habitIconName: String,
    val startTime: Long,
    var elapsedSeconds: Long = 0L,
    var isPaused: Boolean = false
)

@Serializable
data class BackupData(
    val version: Int = 1,
    val exportTimestamp: Long = System.currentTimeMillis(),
    val habits: List<Habit> = emptyList(),
    val sessions: List<HabitSession> = emptyList()
)
