package com.example.tempo.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tempo.analytics.AchievementBadge
import com.example.tempo.analytics.CategoryStat
import com.example.tempo.analytics.CategoryStreak
import com.example.tempo.analytics.DailyComparisonStats
import com.example.tempo.analytics.DayOfWeekStat
import com.example.tempo.analytics.GamificationEngine
import com.example.tempo.analytics.HabitStreak
import com.example.tempo.analytics.StatsCalculator
import com.example.tempo.analytics.StreakInfo
import com.example.tempo.analytics.UserLevel
import com.example.tempo.data.auth.AuthManager
import com.example.tempo.data.model.ActiveTimer
import com.example.tempo.data.model.Category
import com.example.tempo.data.model.Habit
import com.example.tempo.data.model.HabitSession
import com.example.tempo.data.repository.TempoRepository
import com.example.tempo.service.TimerManager
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TempoViewModel(application: Application) : AndroidViewModel(application) {

    val authManager = AuthManager(application.applicationContext)
    val repository = TempoRepository(application.applicationContext)
    val timerManager = TimerManager(application.applicationContext)

    val currentUser: StateFlow<FirebaseUser?> = authManager.currentUser

    val categories: StateFlow<List<Category>> = repository.categories
    val habits: StateFlow<List<Habit>> = repository.habits
    val sessions: StateFlow<List<HabitSession>> = repository.sessions
    val activeTimers: StateFlow<Map<String, ActiveTimer>> = timerManager.activeTimers
    val lastSyncTimestamp: StateFlow<Long?> = repository.lastSyncTimestamp

    init {
        viewModelScope.launch {
            currentUser.collect { user ->
                timerManager.setUser(user)
                repository.setUser(user)
            }
        }
    }

    val dailyStats: StateFlow<DailyComparisonStats> = sessions
        .combine(habits) { sessList, _ -> StatsCalculator.getDailyComparison(sessList) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            DailyComparisonStats(0, 0, 0, 0, 0, 0.0, 0.0)
        )

    val weeklyStats: StateFlow<List<DayOfWeekStat>> = sessions
        .combine(habits) { sessList, _ -> StatsCalculator.getWeeklyStats(sessList) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    val categoryStats: StateFlow<List<CategoryStat>> = combine(categories, habits, sessions) { catList, habitList, sessList ->
        StatsCalculator.getCategoryStats(catList, habitList, sessList)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val streakInfo: StateFlow<StreakInfo> = sessions
        .combine(habits) { sessList, _ -> StatsCalculator.getStreakInfo(sessList) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            StreakInfo(0, 0)
        )

    val userLevel: StateFlow<UserLevel> = sessions
        .combine(habits) { sessList, _ -> GamificationEngine.calculateUserLevel(sessList) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            UserLevel(1, "Habit Novice", 0, 0, 100, 0f)
        )

    val categoryStreaks: StateFlow<List<CategoryStreak>> = combine(categories, habits, sessions) { catList, habitList, sessList ->
        GamificationEngine.calculateCategoryStreaks(catList, habitList, sessList)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val habitStreaks: StateFlow<List<HabitStreak>> = combine(habits, sessions) { habitList, sessList ->
        GamificationEngine.calculateHabitStreaks(habitList, sessList)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val achievementBadges: StateFlow<List<AchievementBadge>> = combine(categories, habits, sessions) { catList, habitList, sessList ->
        GamificationEngine.getBadges(sessList, habitList, catList)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    fun addCategory(category: Category) {
        repository.addCategory(category)
    }

    fun updateCategory(category: Category) {
        repository.updateCategory(category)
    }

    fun deleteCategory(categoryId: String) {
        repository.deleteCategory(categoryId)
    }

    fun addHabit(habit: Habit) {
        repository.addHabit(habit)
    }

    fun updateHabit(habit: Habit) {
        repository.updateHabit(habit)
    }

    fun toggleFavorite(habitId: String) {
        repository.toggleFavorite(habitId)
    }

    fun deleteHabit(habitId: String) {
        if (timerManager.isTimerRunning(habitId)) {
            timerManager.endTimer(habitId) { _, _, _, _ -> }
        }
        repository.deleteHabit(habitId)
    }

    fun startTimer(habit: Habit) {
        timerManager.startTimer(habit)
    }

    fun togglePauseTimer(habitId: String) {
        timerManager.togglePauseTimer(habitId)
    }

    fun endTimer(habitId: String) {
        timerManager.endTimer(habitId) { hId, startTime, endTime, durationSec ->
            repository.logSession(hId, startTime, endTime, durationSec)
        }
    }

    fun signOut(onComplete: () -> Unit) {
        timerManager.setUser(null)
        repository.setUser(null)
        authManager.signOut(onComplete)
    }

    suspend fun exportBackup(): String {
        return repository.exportJson()
    }

    suspend fun importBackup(jsonContent: String): Boolean {
        return repository.importJson(jsonContent)
    }

    fun clearAllData() {
        repository.clearAllData()
    }
}
