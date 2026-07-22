package com.example.tempo.data.auth

import android.content.Context
import android.content.SharedPreferences
import com.example.tempo.data.model.UserAccount
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.MessageDigest
import java.util.UUID

class AuthManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("tempo_auth_prefs", Context.MODE_PRIVATE)

    private val json = Json { ignoreUnknownKeys = true }

    fun isFirstLaunch(): Boolean {
        return !prefs.contains("user_account")
    }

    fun getCurrentAccount(): UserAccount? {
        val rawJson = prefs.getString("user_account", null) ?: return null
        return try {
            json.decodeFromString<UserAccount>(rawJson)
        } catch (e: Exception) {
            null
        }
    }

    fun createAccount(username: String, pin: String): UserAccount {
        val pinHash = hashPin(pin)
        val userId = "user_" + UUID.randomUUID().toString().take(8)
        val account = UserAccount(
            userId = userId,
            username = username.trim(),
            pinHash = pinHash
        )
        val rawJson = json.encodeToString(account)
        prefs.edit().putString("user_account", rawJson).apply()
        return account
    }

    fun verifyPin(pin: String): Boolean {
        val account = getCurrentAccount() ?: return false
        return account.pinHash == hashPin(pin)
    }

    fun logout() {
        prefs.edit().remove("user_account").apply()
    }

    private fun hashPin(pin: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(pin.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
