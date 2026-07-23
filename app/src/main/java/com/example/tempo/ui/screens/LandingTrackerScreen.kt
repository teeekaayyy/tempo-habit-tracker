package com.example.tempo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.tempo.data.model.Habit
import com.example.tempo.theme.DarkBackground
import com.example.tempo.theme.DarkSurface
import com.example.tempo.theme.PrimaryIndigo
import com.example.tempo.theme.PrimaryViolet
import com.example.tempo.theme.SecondaryEmerald
import com.example.tempo.theme.TextPrimary
import com.example.tempo.theme.TextSecondary
import com.example.tempo.ui.components.ActiveTimersHeader
import com.example.tempo.ui.components.HabitTile
import com.example.tempo.ui.dialogs.AddEditHabitDialog
import com.example.tempo.ui.dialogs.CategoryManagerDialog
import com.example.tempo.ui.viewmodel.TempoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LandingTrackerScreen(
    viewModel: TempoViewModel,
    openAddHabitDirectly: Boolean = false,
    onNavigateToDashboard: () -> Unit,
    onNavigateToCloudSync: () -> Unit
) {
    val habits by viewModel.habits.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val activeTimers by viewModel.activeTimers.collectAsState()

    var showAddDialog by remember { mutableStateOf(openAddHabitDirectly) }
    var habitToEdit by remember { mutableStateOf<Habit?>(null) }
    var showCategoryDialog by remember { mutableStateOf(false) }

    val categoryMap = remember(categories) { categories.associateBy { it.id } }

    // Single unified list sorted with Favorites at the top!
    val sortedHabits = remember(habits) {
        habits.sortedWith(compareByDescending<Habit> { it.isFavorite }.thenByDescending { it.createdAt })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(listOf(PrimaryIndigo, SecondaryEmerald))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = "Logo",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Tempo",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary
                            )
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showCategoryDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Category,
                            contentDescription = "Categories",
                            tint = TextPrimary
                        )
                    }
                    IconButton(onClick = onNavigateToDashboard) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = "Analytics Dashboard",
                            tint = SecondaryEmerald
                        )
                    }
                    IconButton(onClick = onNavigateToCloudSync) {
                        Icon(
                            imageVector = Icons.Default.Cloud,
                            contentDescription = "Cloud & Backup",
                            tint = PrimaryIndigo
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    habitToEdit = null
                    showAddDialog = true
                },
                containerColor = PrimaryIndigo,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Habit")
            }
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Live Active Timers Header Banner
            ActiveTimersHeader(
                activeTimers = activeTimers,
                categoryMap = categoryMap,
                onTogglePause = { habitId -> viewModel.togglePauseTimer(habitId) },
                onEndTimer = { habitId -> viewModel.endTimer(habitId) }
            )

            if (habits.isEmpty()) {
                // Initial clean onboarding state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                brush = Brush.verticalGradient(listOf(PrimaryIndigo, PrimaryViolet)),
                                shape = RoundedCornerShape(24.dp)
                            ),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryIndigo.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = "Habit Tracker",
                                    tint = PrimaryIndigo,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Welcome to Tempo",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Your tracker is clean with no pre-built habits.\nAdd your custom habits and tap Start to track with stopwatch timers!",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center
                                )
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = {
                                    habitToEdit = null
                                    showAddDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Create Your First Habit",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            } else {
                // Unified single list of habits (Favorites sorted to the top)
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
                ) {
                    items(
                        items = sortedHabits,
                        key = { it.id }
                    ) { habit ->
                        val isRunning = activeTimers.containsKey(habit.id)
                        val cat = categoryMap[habit.categoryId]
                        HabitTile(
                            habit = habit,
                            category = cat,
                            isTimerRunning = isRunning,
                            onStartTimer = { viewModel.startTimer(habit) },
                            onToggleFavorite = { viewModel.toggleFavorite(habit.id) },
                            onEdit = {
                                habitToEdit = habit
                                showAddDialog = true
                            },
                            onDelete = { viewModel.deleteHabit(habit.id) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddEditHabitDialog(
            categories = categories,
            initialHabit = habitToEdit,
            onDismiss = {
                showAddDialog = false
                habitToEdit = null
            },
            onSave = { savedHabit ->
                if (habitToEdit != null) {
                    viewModel.updateHabit(savedHabit)
                } else {
                    viewModel.addHabit(savedHabit)
                }
                showAddDialog = false
                habitToEdit = null
            }
        )
    }

    if (showCategoryDialog) {
        CategoryManagerDialog(
            categories = categories,
            onDismiss = { showCategoryDialog = false },
            onAddCategory = { cat -> viewModel.addCategory(cat) },
            onDeleteCategory = { catId -> viewModel.deleteCategory(catId) }
        )
    }
}
