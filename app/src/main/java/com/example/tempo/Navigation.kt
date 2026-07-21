package com.example.tempo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tempo.ui.screens.CloudSyncScreen
import com.example.tempo.ui.screens.DashboardScreen
import com.example.tempo.ui.screens.LandingTrackerScreen
import com.example.tempo.ui.viewmodel.TempoViewModel

@Composable
fun MainNavigation(
    tempoViewModel: TempoViewModel = viewModel()
) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.LandingTracker) }

    when (currentScreen) {
        is Screen.LandingTracker -> {
            LandingTrackerScreen(
                viewModel = tempoViewModel,
                onNavigateToDashboard = { currentScreen = Screen.Dashboard },
                onNavigateToCloudSync = { currentScreen = Screen.CloudSync }
            )
        }
        is Screen.Dashboard -> {
            DashboardScreen(
                viewModel = tempoViewModel,
                onNavigateBack = { currentScreen = Screen.LandingTracker }
            )
        }
        is Screen.CloudSync -> {
            CloudSyncScreen(
                viewModel = tempoViewModel,
                onNavigateBack = { currentScreen = Screen.LandingTracker }
            )
        }
    }
}
