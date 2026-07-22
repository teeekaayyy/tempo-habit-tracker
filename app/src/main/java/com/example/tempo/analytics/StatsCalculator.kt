package com.example.tempo.analytics

import com.example.tempo.data.model.Category
import com.example.tempo.data.model.Habit
import com.example.tempo.data.model.HabitSession
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DailyComparisonStats(
    val todaySeconds: Long,
    val yesterdaySeconds: Long,
    val avg7DaySeconds: Long,
    val todayCompletedCount: Int,
    val yesterdayCompletedCount: Int,
    val percentDiffVsYesterday: Double,
    val percentDiffVsAvg: Double
)

data class DayOfWeekStat(
    val dayName: String,
    val dateIso: String,
    val totalSeconds: Long,
    val isToday: Boolean
)

data class CategoryStat(
    val category: Category,
    val totalSeconds: Long,
    val percentage: Float
)

data class StreakInfo(
    val currentStreakDays: Int,
    val longestStreakDays: Int
)

object StatsCalculator {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun getTodayIso(): String = dateFormat.format(Date())

    fun getYesterdayIso(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        return dateFormat.format(cal.time)
    }

    fun getDailyComparison(sessions: List<HabitSession>): DailyComparisonStats {
        val todayIso = getTodayIso()
        val yesterdayIso = getYesterdayIso()

        val todaySessions = sessions.filter { it.dateIso == todayIso }
        val yesterdaySessions = sessions.filter { it.dateIso == yesterdayIso }

        val todaySeconds = todaySessions.sumOf { it.durationSeconds }
        val yesterdaySeconds = yesterdaySessions.sumOf { it.durationSeconds }

        // Past 7 days (excluding today) average
        val past7DaysIso = (1..7).map { daysAgo ->
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
            dateFormat.format(cal.time)
        }
        val past7DaysSeconds = sessions.filter { it.dateIso in past7DaysIso }.sumOf { it.durationSeconds }
        val avg7DaySeconds = if (past7DaysIso.isNotEmpty()) past7DaysSeconds / past7DaysIso.size else 0L

        val percentVsYesterday = if (yesterdaySeconds > 0) {
            ((todaySeconds - yesterdaySeconds).toDouble() / yesterdaySeconds) * 100.0
        } else if (todaySeconds > 0) {
            100.0
        } else {
            0.0
        }

        val percentVsAvg = if (avg7DaySeconds > 0) {
            ((todaySeconds - avg7DaySeconds).toDouble() / avg7DaySeconds) * 100.0
        } else if (todaySeconds > 0) {
            100.0
        } else {
            0.0
        }

        return DailyComparisonStats(
            todaySeconds = todaySeconds,
            yesterdaySeconds = yesterdaySeconds,
            avg7DaySeconds = avg7DaySeconds,
            todayCompletedCount = todaySessions.size,
            yesterdayCompletedCount = yesterdaySessions.size,
            percentDiffVsYesterday = percentVsYesterday,
            percentDiffVsAvg = percentVsAvg
        )
    }

    fun getWeeklyStats(sessions: List<HabitSession>): List<DayOfWeekStat> {
        val calendar = Calendar.getInstance()
        // Set to Monday of current week
        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)

        val todayIso = getTodayIso()
        val days = mutableListOf<DayOfWeekStat>()
        val dayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

        for (i in 0 until 7) {
            val dateIso = dateFormat.format(calendar.time)
            val daySessions = sessions.filter { it.dateIso == dateIso }
            val seconds = daySessions.sumOf { it.durationSeconds }
            days.add(
                DayOfWeekStat(
                    dayName = dayLabels[i],
                    dateIso = dateIso,
                    totalSeconds = seconds,
                    isToday = (dateIso == todayIso)
                )
            )
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        return days
    }

    fun getCategoryStats(categories: List<Category>, habits: List<Habit>, sessions: List<HabitSession>): List<CategoryStat> {
        if (sessions.isEmpty() || habits.isEmpty() || categories.isEmpty()) return emptyList()

        val catMap = categories.associateBy { it.id }
        val habitMap = habits.associateBy { it.id }
        val categoryTotals = mutableMapOf<Category, Long>()

        for (session in sessions) {
            val habit = habitMap[session.habitId] ?: continue
            val category = catMap[habit.categoryId] ?: continue
            val current = categoryTotals.getOrDefault(category, 0L)
            categoryTotals[category] = current + session.durationSeconds
        }

        val grandTotal = categoryTotals.values.sum().toFloat()
        if (grandTotal == 0f) return emptyList()

        return categoryTotals.map { (cat, totalSec) ->
            CategoryStat(
                category = cat,
                totalSeconds = totalSec,
                percentage = (totalSec / grandTotal) * 100f
            )
        }.sortedByDescending { it.totalSeconds }
    }

    fun getStreakInfo(sessions: List<HabitSession>): StreakInfo {
        if (sessions.isEmpty()) return StreakInfo(0, 0)

        val activeDates = sessions.map { it.dateIso }.toSet()
        val todayIso = getTodayIso()
        val yesterdayIso = getYesterdayIso()

        var currentStreak = 0
        var checkCal = Calendar.getInstance()

        // Check starting from today or yesterday
        val startFromToday = activeDates.contains(todayIso)
        val startFromYesterday = activeDates.contains(yesterdayIso)

        if (!startFromToday && !startFromYesterday) {
            currentStreak = 0
        } else {
            if (!startFromToday && startFromYesterday) {
                checkCal.add(Calendar.DAY_OF_YEAR, -1)
            }
            while (activeDates.contains(dateFormat.format(checkCal.time))) {
                currentStreak++
                checkCal.add(Calendar.DAY_OF_YEAR, -1)
            }
        }

        // Longest streak
        val sortedDates = activeDates.sorted()
        var longestStreak = 0
        var tempStreak = 0
        var prevCal: Calendar? = null

        for (dateStr in sortedDates) {
            val curCal = Calendar.getInstance().apply {
                time = dateFormat.parse(dateStr) ?: Date()
            }
            if (prevCal == null) {
                tempStreak = 1
            } else {
                val diffDays = ((curCal.timeInMillis - prevCal.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()
                if (diffDays == 1) {
                    tempStreak++
                } else if (diffDays > 1) {
                    tempStreak = 1
                }
            }
            if (tempStreak > longestStreak) {
                longestStreak = tempStreak
            }
            prevCal = curCal
        }

        return StreakInfo(
            currentStreakDays = currentStreak,
            longestStreakDays = maxOf(currentStreak, longestStreak)
        )
    }

    fun formatDuration(seconds: Long): String {
        val hours = seconds / 3600
        val mins = (seconds % 3600) / 60
        val secs = seconds % 60
        return when {
            hours > 0 -> String.format(Locale.getDefault(), "%dh %02dm", hours, mins)
            mins > 0 -> String.format(Locale.getDefault(), "%dm %02ds", mins, secs)
            else -> String.format(Locale.getDefault(), "%ds", secs)
        }
    }

    fun formatTicker(seconds: Long): String {
        val hours = seconds / 3600
        val mins = (seconds % 3600) / 60
        val secs = seconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, mins, secs)
    }
}
