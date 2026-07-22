package com.example.tempo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tempo.ui.screens.AuthScreen
import com.example.tempo.ui.screens.CloudSyncScreen
import com.example.tempo.ui.screens.DashboardScreen
import com.example.tempo.ui.screens.LandingTrackerScreen
import com.example.tempo.ui.viewmodel.TempoViewModel

@Composable
fun MainNavigation(
    openAddHabitDirectly: Boolean = false,
    tempoViewModel: TempoViewModel = viewModel()
) {
    val currentUser by tempoViewModel.currentUser.collectAsState()
    var currentScreen by remember { mutableStateOf<Screen>(Screen.LandingTracker) }

    // If opening app for the first time, ask user to create Username & PIN!
    if (currentUser == null || tempoViewModel.authManager.isFirstLaunch()) {
        AuthScreen(
            onAccountCreated = { username, pin ->
                tempoViewModel.createAccount(username, pin)
                currentScreen = Screen.LandingTracker
            }
        )
    } else {
        when (currentScreen) {
            is Screen.LandingTracker -> {
                LandingTrackerScreen(
                    viewModel = tempoViewModel,
                    openAddHabitDirectly = openAddHabitDirectly,
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
}
