package com.example.tempo.data.model

import kotlinx.serialization.Serializable

@Serializable
data class UserAccount(
    val userId: String,
    val username: String,
    val pinHash: String,
    val createdAt: Long = System.currentTimeMillis()
)
