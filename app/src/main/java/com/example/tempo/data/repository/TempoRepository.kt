package com.example.tempo.data.repository

import android.content.Context
import com.example.tempo.data.model.BackupData
import com.example.tempo.data.model.Category
import com.example.tempo.data.model.DefaultCategories
import com.example.tempo.data.model.Habit
import com.example.tempo.data.model.HabitSession
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
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

    private val firestore = FirebaseFirestore.getInstance()

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private var currentUser: FirebaseUser? = null

    private var categoriesListener: ListenerRegistration? = null
    private var habitsListener: ListenerRegistration? = null
    private var sessionsListener: ListenerRegistration? = null

    private val _categories = MutableStateFlow<List<Category>>(DefaultCategories)
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _habits = MutableStateFlow<List<Habit>>(emptyList())
    val habits: StateFlow<List<Habit>> = _habits.asStateFlow()

    private val _sessions = MutableStateFlow<List<HabitSession>>(emptyList())
    val sessions: StateFlow<List<HabitSession>> = _sessions.asStateFlow()

    private val _lastSyncTimestamp = MutableStateFlow<Long?>(null)
    val lastSyncTimestamp: StateFlow<Long?> = _lastSyncTimestamp.asStateFlow()

    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    fun setUser(user: FirebaseUser?) {
        if (currentUser?.uid == user?.uid) return
        detachListeners()
        currentUser = user

        if (user == null) {
            _categories.value = DefaultCategories
            _habits.value = emptyList()
            _sessions.value = emptyList()
            return
        }

        loadLocalCacheForUser(user.uid)
        attachFirestoreListeners(user.uid)
    }

    private fun detachListeners() {
        categoriesListener?.remove()
        habitsListener?.remove()
        sessionsListener?.remove()
        categoriesListener = null
        habitsListener = null
        sessionsListener = null
    }

    private fun loadLocalCacheForUser(userId: String) {
        repositoryScope.launch {
            try {
                val file = File(context.filesDir, "tempo_data_$userId.json")
                if (file.exists()) {
                    val rawJson = file.readText()
                    val backupData = json.decodeFromString<BackupData>(rawJson)
                    _categories.value = if (backupData.categories.isNotEmpty()) backupData.categories else DefaultCategories
                    _habits.value = backupData.habits
                    _sessions.value = backupData.sessions
                    _lastSyncTimestamp.value = backupData.exportTimestamp
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun saveLocalCacheForUser(userId: String) {
        repositoryScope.launch {
            try {
                val file = File(context.filesDir, "tempo_data_$userId.json")
                val now = System.currentTimeMillis()
                _lastSyncTimestamp.value = now
                val backup = BackupData(
                    version = 2,
                    exportTimestamp = now,
                    categories = _categories.value,
                    habits = _habits.value,
                    sessions = _sessions.value
                )
                val rawJson = json.encodeToString(backup)
                file.writeText(rawJson)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun attachFirestoreListeners(userId: String) {
        val userRef = firestore.collection("users").document(userId)

        // Categories listener
        categoriesListener = userRef.collection("categories")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) {
                    val catList = snapshot.documents.mapNotNull { doc ->
                        val id = doc.getString("id") ?: doc.id
                        val name = doc.getString("name") ?: return@mapNotNull null
                        val colorHex = doc.getString("colorHex") ?: "#6366F1"
                        val iconName = doc.getString("iconName") ?: "Category"
                        Category(id, name, colorHex, iconName)
                    }

                    if (catList.isEmpty()) {
                        // Seed default categories
                        DefaultCategories.forEach { cat ->
                            userRef.collection("categories").document(cat.id).set(cat)
                        }
                        _categories.value = DefaultCategories
                    } else {
                        _categories.value = catList
                    }
                    saveLocalCacheForUser(userId)
                }
            }

        // Habits listener
        habitsListener = userRef.collection("habits")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) {
                    val habitList = snapshot.documents.mapNotNull { doc ->
                        val id = doc.getString("id") ?: doc.id
                        val title = doc.getString("title") ?: return@mapNotNull null
                        val description = doc.getString("description") ?: ""
                        val categoryId = doc.getString("categoryId") ?: "cat_prod"
                        val iconName = doc.getString("iconName") ?: "Timer"
                        val targetDurationMinutes = doc.getLong("targetDurationMinutes")?.toInt() ?: 30
                        val isFavorite = doc.getBoolean("isFavorite") ?: false
                        val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                        Habit(id, title, description, categoryId, iconName, targetDurationMinutes, isFavorite, createdAt)
                    }
                    _habits.value = habitList
                    saveLocalCacheForUser(userId)
                }
            }

        // Sessions listener
        sessionsListener = userRef.collection("sessions")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) {
                    val sessionList = snapshot.documents.mapNotNull { doc ->
                        val id = doc.getString("id") ?: doc.id
                        val habitId = doc.getString("habitId") ?: return@mapNotNull null
                        val startTime = doc.getLong("startTime") ?: 0L
                        val endTime = doc.getLong("endTime") ?: 0L
                        val durationSeconds = doc.getLong("durationSeconds") ?: 0L
                        val dateIso = doc.getString("dateIso") ?: ""
                        HabitSession(id, habitId, startTime, endTime, durationSeconds, dateIso)
                    }
                    _sessions.value = sessionList
                    saveLocalCacheForUser(userId)
                }
            }
    }

    fun addCategory(category: Category) {
        val user = currentUser ?: return
        firestore.collection("users").document(user.uid)
            .collection("categories").document(category.id).set(category)
    }

    fun updateCategory(category: Category) {
        addCategory(category)
    }

    fun deleteCategory(categoryId: String) {
        val user = currentUser ?: return
        firestore.collection("users").document(user.uid)
            .collection("categories").document(categoryId).delete()
    }

    fun addHabit(habit: Habit) {
        val user = currentUser ?: return
        firestore.collection("users").document(user.uid)
            .collection("habits").document(habit.id).set(habit)
    }

    fun updateHabit(updatedHabit: Habit) {
        addHabit(updatedHabit)
    }

    fun toggleFavorite(habitId: String) {
        val user = currentUser ?: return
        val habit = _habits.value.firstOrNull { it.id == habitId } ?: return
        val updated = habit.copy(isFavorite = !habit.isFavorite)
        addHabit(updated)
    }

    fun deleteHabit(habitId: String) {
        val user = currentUser ?: return
        val batch = firestore.batch()
        val habitRef = firestore.collection("users").document(user.uid).collection("habits").document(habitId)
        batch.delete(habitRef)

        val userSessions = _sessions.value.filter { it.habitId == habitId }
        userSessions.forEach { session ->
            val sessRef = firestore.collection("users").document(user.uid).collection("sessions").document(session.id)
            batch.delete(sessRef)
        }
        batch.commit()
    }

    fun logSession(habitId: String, startTime: Long, endTime: Long, durationSeconds: Long) {
        if (durationSeconds <= 0) return
        val user = currentUser ?: return
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
        firestore.collection("users").document(user.uid)
            .collection("sessions").document(newSession.id).set(newSession)
    }

    suspend fun exportJson(): String = withContext(Dispatchers.IO) {
        val backup = BackupData(
            version = 2,
            exportTimestamp = System.currentTimeMillis(),
            categories = _categories.value,
            habits = _habits.value,
            sessions = _sessions.value
        )
        json.encodeToString(backup)
    }

    suspend fun importJson(jsonContent: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val user = currentUser ?: return@withContext false
            val backup = json.decodeFromString<BackupData>(jsonContent)

            val batch = firestore.batch()
            val userRef = firestore.collection("users").document(user.uid)

            backup.categories.forEach { cat ->
                batch.set(userRef.collection("categories").document(cat.id), cat)
            }
            backup.habits.forEach { habit ->
                batch.set(userRef.collection("habits").document(habit.id), habit)
            }
            backup.sessions.forEach { sess ->
                batch.set(userRef.collection("sessions").document(sess.id), sess)
            }
            batch.commit()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun clearAllData() {
        val user = currentUser ?: return
        val batch = firestore.batch()
        val userRef = firestore.collection("users").document(user.uid)

        _categories.value.forEach { cat ->
            batch.delete(userRef.collection("categories").document(cat.id))
        }
        _habits.value.forEach { habit ->
            batch.delete(userRef.collection("habits").document(habit.id))
        }
        _sessions.value.forEach { sess ->
            batch.delete(userRef.collection("sessions").document(sess.id))
        }
        batch.commit()
    }
}
