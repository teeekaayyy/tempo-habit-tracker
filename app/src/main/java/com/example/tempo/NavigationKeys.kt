package com.example.tempo

import kotlinx.serialization.Serializable

sealed class Screen {
    @Serializable
    data object LandingTracker : Screen()

    @Serializable
    data object Dashboard : Screen()

    @Serializable
    data object CloudSync : Screen()
}
