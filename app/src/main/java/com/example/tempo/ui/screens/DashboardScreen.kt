package com.example.tempo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Fireplace
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tempo.analytics.AchievementBadge
import com.example.tempo.analytics.CategoryStreak
import com.example.tempo.analytics.DailyComparisonStats
import com.example.tempo.analytics.DayOfWeekStat
import com.example.tempo.analytics.HabitStreak
import com.example.tempo.analytics.StatsCalculator
import com.example.tempo.analytics.UserLevel
import com.example.tempo.theme.AccentAmber
import com.example.tempo.theme.AccentRose
import com.example.tempo.theme.DarkBackground
import com.example.tempo.theme.DarkSurface
import com.example.tempo.theme.DarkSurfaceVariant
import com.example.tempo.theme.FavoriteGold
import com.example.tempo.theme.PrimaryIndigo
import com.example.tempo.theme.PrimaryViolet
import com.example.tempo.theme.SecondaryEmerald
import com.example.tempo.theme.TextPrimary
import com.example.tempo.theme.TextSecondary
import com.example.tempo.theme.parseHexColor
import com.example.tempo.ui.viewmodel.TempoViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: TempoViewModel,
    onNavigateBack: () -> Unit
) {
    val dailyStats by viewModel.dailyStats.collectAsState()
    val weeklyStats by viewModel.weeklyStats.collectAsState()
    val categoryStats by viewModel.categoryStats.collectAsState()
    val streakInfo by viewModel.streakInfo.collectAsState()

    val userLevel by viewModel.userLevel.collectAsState()
    val categoryStreaks by viewModel.categoryStreaks.collectAsState()
    val habitStreaks by viewModel.habitStreaks.collectAsState()
    val badges by viewModel.achievementBadges.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Gamified Analytics",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Gamification Level & XP Progress Card
            UserLevelCard(userLevel = userLevel)

            // Overall Streak & Best Streak Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        brush = Brush.horizontalGradient(listOf(AccentAmber, AccentRose)),
                        shape = RoundedCornerShape(20.dp)
                    ),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "OVERALL STREAK",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentAmber,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${streakInfo.currentStreakDays} Days",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "All-Time Best: ${streakInfo.longestStreakDays} days",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(AccentAmber.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fireplace,
                            contentDescription = "Streak",
                            tint = AccentAmber,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            // Category Streaks Section
            if (categoryStreaks.isNotEmpty()) {
                CategoryStreaksCard(categoryStreaks = categoryStreaks)
            }

            // Individual Habit Streaks Breakdown
            if (habitStreaks.isNotEmpty()) {
                HabitStreaksCard(habitStreaks = habitStreaks)
            }

            // Achievement Badges Grid
            AchievementBadgesCard(badges = badges)

            // Daily Comparison Card (Today vs Yesterday)
            DailyComparisonCard(dailyStats = dailyStats)

            // Weekly Trend Bar Chart
            WeeklyTrendCard(weeklyStats = weeklyStats)

            // Category Breakdown Progress Bars
            if (categoryStats.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PieChart,
                                contentDescription = "Categories",
                                tint = PrimaryIndigo,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Category Focus Share",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        categoryStats.forEach { catStat ->
                            val catColor = parseHexColor(catStat.category.colorHex)
                            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = catStat.category.name,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "${catStat.percentage.roundToInt()}% (${StatsCalculator.formatDuration(catStat.totalSeconds)})",
                                        fontSize = 12.sp,
                                        color = TextSecondary
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { catStat.percentage / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = catColor,
                                    trackColor = DarkSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UserLevelCard(userLevel: UserLevel) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(listOf(PrimaryIndigo, SecondaryEmerald)),
                shape = RoundedCornerShape(20.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(PrimaryIndigo.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MilitaryTech,
                            contentDescription = "Level",
                            tint = PrimaryIndigo,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "LEVEL ${userLevel.levelNumber}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryIndigo,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = userLevel.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                    }
                }

                Text(
                    text = "${userLevel.totalXp} XP",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = SecondaryEmerald
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "XP Progress to Next Rank",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
                Text(
                    text = "${userLevel.currentLevelXp} / ${userLevel.requiredLevelXp} XP",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { userLevel.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp)),
                color = SecondaryEmerald,
                trackColor = DarkSurfaceVariant
            )
        }
    }
}

@Composable
private fun CategoryStreaksCard(categoryStreaks: List<CategoryStreak>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Category,
                    contentDescription = "Category Streaks",
                    tint = SecondaryEmerald,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Category Streaks",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(categoryStreaks) { catStreak ->
                    val catColor = parseHexColor(catStreak.category.colorHex)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(DarkSurfaceVariant)
                            .border(1.dp, catColor.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(catColor)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = catStreak.category.name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Fireplace,
                                    contentDescription = null,
                                    tint = AccentAmber,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${catStreak.streakDays} Day Streak",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentAmber
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HabitStreaksCard(habitStreaks: List<HabitStreak>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Fireplace,
                    contentDescription = "Habit Streaks",
                    tint = AccentAmber,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Habit Streaks",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                habitStreaks.take(5).forEach { habitStreak ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSurfaceVariant)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = habitStreak.habit.title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Fireplace,
                                contentDescription = null,
                                tint = AccentAmber,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${habitStreak.streakDays} days",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentAmber
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AchievementBadgesCard(badges: List<AchievementBadge>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = "Badges",
                    tint = FavoriteGold,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Achievement Badges",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(badges) { badge ->
                    val badgeColor = if (badge.isUnlocked) FavoriteGold else TextSecondary.copy(alpha = 0.4f)
                    Box(
                        modifier = Modifier
                            .width(130.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(DarkSurfaceVariant)
                            .border(
                                width = 1.dp,
                                color = if (badge.isUnlocked) FavoriteGold.copy(alpha = 0.5f) else Color.Transparent,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(if (badge.isUnlocked) FavoriteGold.copy(alpha = 0.2f) else DarkBackground),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (badge.iconName) {
                                        "PlayArrow" -> Icons.Default.PlayArrow
                                        "LocalFireDepartment" -> Icons.Default.Fireplace
                                        "Star" -> Icons.Default.Star
                                        "Category" -> Icons.Default.Category
                                        "Timer" -> Icons.Default.Timer
                                        else -> Icons.Default.EmojiEvents
                                    },
                                    contentDescription = badge.title,
                                    tint = badgeColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = badge.title,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (badge.isUnlocked) TextPrimary else TextSecondary,
                                maxLines = 1
                            )

                            Text(
                                text = if (badge.isUnlocked) "Unlocked" else "Locked",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (badge.isUnlocked) SecondaryEmerald else TextSecondary.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyComparisonCard(dailyStats: DailyComparisonStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "TODAY VS PREVIOUS DAYS",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = SecondaryEmerald,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Today's Tracked Time",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                    Text(
                        text = StatsCalculator.formatDuration(dailyStats.todaySeconds),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                }

                val isUpVsYesterday = dailyStats.percentDiffVsYesterday >= 0
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isUpVsYesterday) SecondaryEmerald.copy(alpha = 0.15f) else AccentRose.copy(alpha = 0.15f)
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isUpVsYesterday) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                            contentDescription = "Trend",
                            tint = if (isUpVsYesterday) SecondaryEmerald else AccentRose,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${if (isUpVsYesterday) "+" else ""}${dailyStats.percentDiffVsYesterday.roundToInt()}% vs Yesterday",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isUpVsYesterday) SecondaryEmerald else AccentRose
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricColumn(
                    label = "Yesterday",
                    value = StatsCalculator.formatDuration(dailyStats.yesterdaySeconds)
                )
                MetricColumn(
                    label = "7-Day Daily Avg",
                    value = StatsCalculator.formatDuration(dailyStats.avg7DaySeconds)
                )
                MetricColumn(
                    label = "Sessions Today",
                    value = "${dailyStats.todayCompletedCount}"
                )
            }
        }
    }
}

@Composable
private fun MetricColumn(label: String, value: String) {
    Column {
        Text(text = label, fontSize = 11.sp, color = TextSecondary)
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
    }
}

@Composable
private fun WeeklyTrendCard(weeklyStats: List<DayOfWeekStat>) {
    val maxSeconds = maxOf(weeklyStats.maxOfOrNull { it.totalSeconds } ?: 1L, 3600L).toFloat()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "WEEKLY TREND (CURRENT WEEK)",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryViolet,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                weeklyStats.forEach { dayStat ->
                    val barHeightRatio = (dayStat.totalSeconds / maxSeconds).coerceIn(0.05f, 1f)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (dayStat.totalSeconds > 0) "${dayStat.totalSeconds / 60}m" else "",
                            fontSize = 9.sp,
                            color = if (dayStat.isToday) SecondaryEmerald else TextSecondary
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Box(
                            modifier = Modifier
                                .width(20.dp)
                                .fillMaxHeight(barHeightRatio)
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .background(
                                    if (dayStat.isToday) SecondaryEmerald else PrimaryIndigo
                                )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = dayStat.dayName,
                            fontSize = 11.sp,
                            fontWeight = if (dayStat.isToday) FontWeight.Bold else FontWeight.Normal,
                            color = if (dayStat.isToday) SecondaryEmerald else TextSecondary
                        )
                    }
                }
            }
        }
    }
}
