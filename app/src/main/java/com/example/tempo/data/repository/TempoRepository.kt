package com.example.tempo.data.repository

import android.content.Context
import com.example.tempo.data.model.BackupData
import com.example.tempo.data.model.Habit
import com.example.tempo.data.model.HabitSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class TempoRepository(private val context: Context) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val dataFile: File
        get() = File(context.filesDir, "tempo_data_v1.json")

    private val _habits = MutableStateFlow<List<Habit>>(emptyList())
    val habits: StateFlow<List<Habit>> = _habits.asStateFlow()

    private val _sessions = MutableStateFlow<List<HabitSession>>(emptyList())
    val sessions: StateFlow<List<HabitSession>> = _sessions.asStateFlow()

    private val _lastSyncTimestamp = MutableStateFlow<Long?>(null)
    val lastSyncTimestamp: StateFlow<Long?> = _lastSyncTimestamp.asStateFlow()

    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    init {
        loadDataFromDisk()
    }

    private fun loadDataFromDisk() {
        repositoryScope.launch {
            try {
                if (dataFile.exists()) {
                    val rawJson = dataFile.readText()
                    val backupData = json.decodeFromString<BackupData>(rawJson)
                    _habits.value = backupData.habits
                    _sessions.value = backupData.sessions
                    _lastSyncTimestamp.value = backupData.exportTimestamp
                } else {
                    // Starts empty as requested (0 pre-built habits)
                    _habits.value = emptyList()
                    _sessions.value = emptyList()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun saveDataToDisk() = withContext(Dispatchers.IO) {
        try {
            val now = System.currentTimeMillis()
            _lastSyncTimestamp.value = now
            val backup = BackupData(
                version = 1,
                exportTimestamp = now,
                habits = _habits.value,
                sessions = _sessions.value
            )
            val rawJson = json.encodeToString(backup)
            dataFile.writeText(rawJson)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun addHabit(habit: Habit) {
        repositoryScope.launch {
            val updated = _habits.value + habit
            _habits.value = updated
            saveDataToDisk()
        }
    }

    fun updateHabit(updatedHabit: Habit) {
        repositoryScope.launch {
            val updated = _habits.value.map { if (it.id == updatedHabit.id) updatedHabit else it }
            _habits.value = updated
            saveDataToDisk()
        }
    }

    fun toggleFavorite(habitId: String) {
        repositoryScope.launch {
            val updated = _habits.value.map {
                if (it.id == habitId) it.copy(isFavorite = !it.isFavorite) else it
            }
            _habits.value = updated
            saveDataToDisk()
        }
    }

    fun deleteHabit(habitId: String) {
        repositoryScope.launch {
            val updatedHabits = _habits.value.filter { it.id != habitId }
            val updatedSessions = _sessions.value.filter { it.habitId != habitId }
            _habits.value = updatedHabits
            _sessions.value = updatedSessions
            saveDataToDisk()
        }
    }

    fun logSession(habitId: String, startTime: Long, endTime: Long, durationSeconds: Long) {
        if (durationSeconds <= 0) return
        repositoryScope.launch {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val dateIso = dateFormat.format(Date(endTime))
            val newSession = HabitSession(
                id = UUID.randomUUID().toString(),
                habitId = habitId,
                startTime = startTime,
                endTime = endTime,
                durationSeconds = durationSeconds,
                dateIso = dateIso
            )
            val updated = _sessions.value + newSession
            _sessions.value = updated
            saveDataToDisk()
        }
    }

    suspend fun exportJson(): String = withContext(Dispatchers.IO) {
        val backup = BackupData(
            version = 1,
            exportTimestamp = System.currentTimeMillis(),
            habits = _habits.value,
            sessions = _sessions.value
        )
        json.encodeToString(backup)
    }

    suspend fun importJson(jsonContent: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val backup = json.decodeFromString<BackupData>(jsonContent)
            _habits.value = backup.habits
            _sessions.value = backup.sessions
            saveDataToDisk()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun clearAllData() {
        repositoryScope.launch {
            _habits.value = emptyList()
            _sessions.value = emptyList()
            saveDataToDisk()
        }
    }
}
