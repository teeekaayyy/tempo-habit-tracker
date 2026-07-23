package com.example.tempo.analytics

import com.example.tempo.data.model.Category
import com.example.tempo.data.model.Habit
import com.example.tempo.data.model.HabitSession
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class UserLevel(
    val levelNumber: Int,
    val title: String,
    val totalXp: Long,
    val currentLevelXp: Long,
    val requiredLevelXp: Long,
    val progress: Float
)

data class AchievementBadge(
    val id: String,
    val title: String,
    val description: String,
    val iconName: String,
    val isUnlocked: Boolean,
    val unlockedAtDateIso: String? = null
)

data class CategoryStreak(
    val category: Category,
    val streakDays: Int,
    val totalTrackedSeconds: Long
)

data class HabitStreak(
    val habit: Habit,
    val streakDays: Int,
    val totalTrackedSeconds: Long
)

object GamificationEngine {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun calculateUserLevel(sessions: List<HabitSession>): UserLevel {
        val totalMinutes = sessions.sumOf { it.durationSeconds } / 60
        val totalXp = totalMinutes // 1 XP per minute tracked

        // Level thresholds:
        // L1: 0-100 XP (Novice)
        // L2: 100-300 XP (Consistent)
        // L3: 300-600 XP (Achiever)
        // L4: 600-1000 XP (Master)
        // L5+: 1000+ XP (Legend)
        return when {
            totalXp < 100 -> UserLevel(
                levelNumber = 1,
                title = "Habit Novice",
                totalXp = totalXp,
                currentLevelXp = totalXp,
                requiredLevelXp = 100,
                progress = (totalXp / 100f).coerceIn(0f, 1f)
            )
            totalXp < 300 -> UserLevel(
                levelNumber = 2,
                title = "Consistent Tracker",
                totalXp = totalXp,
                currentLevelXp = totalXp - 100,
                requiredLevelXp = 200,
                progress = ((totalXp - 100) / 200f).coerceIn(0f, 1f)
            )
            totalXp < 600 -> UserLevel(
                levelNumber = 3,
                title = "Focus Achiever",
                totalXp = totalXp,
                currentLevelXp = totalXp - 300,
                requiredLevelXp = 300,
                progress = ((totalXp - 300) / 300f).coerceIn(0f, 1f)
            )
            totalXp < 1000 -> UserLevel(
                levelNumber = 4,
                title = "Habit Master",
                totalXp = totalXp,
                currentLevelXp = totalXp - 600,
                requiredLevelXp = 400,
                progress = ((totalXp - 600) / 400f).coerceIn(0f, 1f)
            )
            else -> {
                val extraLevel = ((totalXp - 1000) / 500).toInt()
                val currentInExtra = (totalXp - 1000) % 500
                UserLevel(
                    levelNumber = 5 + extraLevel,
                    title = "Habit Legend",
                    totalXp = totalXp,
                    currentLevelXp = currentInExtra,
                    requiredLevelXp = 500,
                    progress = (currentInExtra / 500f).coerceIn(0f, 1f)
                )
            }
        }
    }

    fun calculateCategoryStreaks(
        categories: List<Category>,
        habits: List<Habit>,
        sessions: List<HabitSession>
    ): List<CategoryStreak> {
        if (categories.isEmpty() || habits.isEmpty()) return emptyList()

        val habitMap = habits.associateBy { it.id }
        val categoryHabitIds = habits.groupBy { it.categoryId }

        return categories.map { category ->
            val catHabits = categoryHabitIds[category.id]?.map { it.id }?.toSet() ?: emptySet()
            val catSessions = sessions.filter { it.habitId in catHabits }

            val totalSeconds = catSessions.sumOf { it.durationSeconds }
            val streak = calculateDatesStreak(catSessions.map { it.dateIso }.toSet())

            CategoryStreak(
                category = category,
                streakDays = streak,
                totalTrackedSeconds = totalSeconds
            )
        }.sortedByDescending { it.streakDays }
    }

    fun calculateHabitStreaks(
        habits: List<Habit>,
        sessions: List<HabitSession>
    ): List<HabitStreak> {
        if (habits.isEmpty()) return emptyList()

        val sessionsByHabit = sessions.groupBy { it.habitId }

        return habits.map { habit ->
            val habitSessions = sessionsByHabit[habit.id] ?: emptyList()
            val totalSeconds = habitSessions.sumOf { it.durationSeconds }
            val streak = calculateDatesStreak(habitSessions.map { it.dateIso }.toSet())

            HabitStreak(
                habit = habit,
                streakDays = streak,
                totalTrackedSeconds = totalSeconds
            )
        }.sortedByDescending { it.streakDays }
    }

    fun getBadges(
        sessions: List<HabitSession>,
        habits: List<Habit>,
        categories: List<Category>
    ): List<AchievementBadge> {
        val totalTrackedMinutes = sessions.sumOf { it.durationSeconds } / 60
        val totalSessionsCount = sessions.size
        val activeDaysCount = sessions.map { it.dateIso }.toSet().size
        val overallStreak = calculateDatesStreak(sessions.map { it.dateIso }.toSet())
        val categoryStreaks = calculateCategoryStreaks(categories, habits, sessions)
        val maxCatStreak = categoryStreaks.maxOfOrNull { it.streakDays } ?: 0

        return listOf(
            AchievementBadge(
                id = "badge_first_step",
                title = "First Focus",
                description = "Completed your first habit session",
                iconName = "PlayArrow",
                isUnlocked = totalSessionsCount >= 1
            ),
            AchievementBadge(
                id = "badge_3_day_flame",
                title = "3-Day Flame",
                description = "Maintained a 3-day focus streak",
                iconName = "LocalFireDepartment",
                isUnlocked = overallStreak >= 3
            ),
            AchievementBadge(
                id = "badge_weekly_warrior",
                title = "Weekly Warrior",
                description = "Maintained a 7-day focus streak",
                iconName = "Star",
                isUnlocked = overallStreak >= 7
            ),
            AchievementBadge(
                id = "badge_category_champ",
                title = "Category Specialist",
                description = "Maintained a 5-day streak in a single category",
                iconName = "Category",
                isUnlocked = maxCatStreak >= 5
            ),
            AchievementBadge(
                id = "badge_100_mins",
                title = "100-Minute Mindset",
                description = "Tracked over 100 minutes of focus time",
                iconName = "Timer",
                isUnlocked = totalTrackedMinutes >= 100
            ),
            AchievementBadge(
                id = "badge_century_club",
                title = "Century Club",
                description = "Tracked over 1,000 minutes (16+ hours) of focus",
                iconName = "EmojiEvents",
                isUnlocked = totalTrackedMinutes >= 1000
            )
        )
    }

    private fun calculateDatesStreak(activeDates: Set<String>): Int {
        if (activeDates.isEmpty()) return 0

        val todayIso = StatsCalculator.getTodayIso()
        val yesterdayIso = StatsCalculator.getYesterdayIso()

        var currentStreak = 0
        var checkCal = Calendar.getInstance()

        val startFromToday = activeDates.contains(todayIso)
        val startFromYesterday = activeDates.contains(yesterdayIso)

        if (!startFromToday && !startFromYesterday) {
            return 0
        }

        if (!startFromToday && startFromYesterday) {
            checkCal.add(Calendar.DAY_OF_YEAR, -1)
        }

        while (activeDates.contains(dateFormat.format(checkCal.time))) {
            currentStreak++
            checkCal.add(Calendar.DAY_OF_YEAR, -1)
        }

        return currentStreak
    }
}
