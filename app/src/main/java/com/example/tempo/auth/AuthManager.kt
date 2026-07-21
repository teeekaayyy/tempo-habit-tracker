package com.example.tempo.auth

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class UserAccount(
    val uid: String,
    val email: String,
    val displayName: String,
    val photoUrl: String = "",
    val isLoggedIn: Boolean = true
)

class AuthManager(context: Context) {
    private val prefs = context.getSharedPreferences("tempo_auth_prefs", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    private val _currentUser = MutableStateFlow<UserAccount?>(null)
    val currentUser: StateFlow<UserAccount?> = _currentUser.asStateFlow()

    init {
        loadUserFromPrefs()
    }

    private fun loadUserFromPrefs() {
        val savedJson = prefs.getString("user_account_json", null)
        if (savedJson != null) {
            try {
                _currentUser.value = json.decodeFromString<UserAccount>(savedJson)
            } catch (e: Exception) {
                _currentUser.value = null
            }
        } else {
            // Default demo account
            _currentUser.value = UserAccount(
                uid = "google_user_default",
                email = "thiru.priyakathir@gmail.com",
                displayName = "Thiru (Google Account)"
            )
        }
    }

    fun loginWithGoogle(email: String, displayName: String): UserAccount {
        val uid = "google_" + email.hashCode().coerceAtLeast(0)
        val user = UserAccount(
            uid = uid,
            email = email,
            displayName = displayName
        )
        _currentUser.value = user
        saveUserToPrefs(user)
        return user
    }

    fun logout() {
        _currentUser.value = null
        prefs.edit().remove("user_account_json").apply()
    }

    private fun saveUserToPrefs(user: UserAccount) {
        try {
            val rawJson = json.encodeToString(user)
            prefs.edit().putString("user_account_json", rawJson).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
