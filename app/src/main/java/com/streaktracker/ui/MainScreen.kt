package com.streaktracker.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import com.streaktracker.R
import com.streaktracker.data.ActivityType
import com.streaktracker.data.SettingsDataStore
import com.streaktracker.ui.theme.*

@Composable
fun MainScreen(
    uiState: MainUiState,
    onActivitySelect: (ActivityType) -> Unit,
    onAddDuration: (Int) -> Unit,
    onClearDuration: () -> Unit,
    onConfirmActivity: () -> Unit,
    onCancelInput: () -> Unit,
    onOpenSettings: () -> Unit,
    onCloseSettings: () -> Unit,
    onSetDailyGoal: (Int) -> Unit,
    onSetReminderTime: (Int, Int) -> Unit,
    onSetLanguage: (String) -> Unit,
    onDayClick: (java.time.LocalDate) -> Unit,
    onClearSelectedDay: () -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    // Handle back button when settings is open
    BackHandler(enabled = uiState.showSettings) {
        onCloseSettings()
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    )
                )
                .verticalScroll(scrollState)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Settings Button Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = stringResource(R.string.settings_content_description),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Streak Display (hidden while loading)
            if (!uiState.isLoading) {
                StreakDisplay(
                    streak = uiState.currentStreak,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
            }

            // Progress Bar
            ProgressDisplay(
                progress = uiState.todayProgress,
                goal = uiState.dailyGoal,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            )

            // Activity Buttons or Input Panel
            AnimatedContent(
                targetState = uiState.selectedActivityType,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith
                            fadeOut(animationSpec = tween(300))
                },
                label = "activity_panel"
            ) { selectedType ->
                if (selectedType == null) {
                    ActivityButtons(
                        onActivitySelect = onActivitySelect,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                } else {
                    ActivityInputPanel(
                        activityType = selectedType,
                        pendingDuration = uiState.pendingDuration,
                        onAddDuration = onAddDuration,
                        onClearDuration = onClearDuration,
                        onConfirm = onConfirmActivity,
                        onCancel = onCancelInput,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                }
            }

            // Calendar
            CalendarView(
                currentMonth = uiState.currentMonth,
                activities = uiState.monthActivities,
                dayStatuses = uiState.monthDayStatuses,
                todayProgress = uiState.todayProgress,
                dailyGoal = uiState.dailyGoal,
                selectedDay = uiState.selectedDay,
                onDayClick = onDayClick,
                onDismissPopup = onClearSelectedDay,
                onPreviousMonth = onPreviousMonth,
                onNextMonth = onNextMonth,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))
        }

        // Settings Panel Overlay
        AnimatedVisibility(
            visible = uiState.showSettings,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it })
        ) {
            SettingsPanel(
                currentGoal = uiState.dailyGoal,
                reminderHour = uiState.reminderHour,
                reminderMinute = uiState.reminderMinute,
                currentLanguage = uiState.language,
                onSetGoal = onSetDailyGoal,
                onSetReminderTime = onSetReminderTime,
                onSetLanguage = onSetLanguage,
                onClose = onCloseSettings
            )
        }
    }
}

@Composable
fun StreakDisplay(
    streak: Int,
    modifier: Modifier = Modifier
) {
    // Track previous streak to detect extensions
    var previousStreak by remember { mutableStateOf(streak) }
    val animatedValue = remember { Animatable(streak.toFloat()) }

    // Handle streak changes
    LaunchedEffect(streak) {
        if (streak > previousStreak && previousStreak > 0) {
            // Streak extended! Celebrate by replaying from 1
            animatedValue.snapTo(1f)
            animatedValue.animateTo(
                targetValue = streak.toFloat(),
                animationSpec = tween(durationMillis = 150 * streak) // ~150ms per day
            )
        } else {
            // No animation: first load, reset, or decrease
            animatedValue.snapTo(streak.toFloat())
        }
        previousStreak = streak
    }

    val displayedStreak = animatedValue.value.toInt()
    val hasStreak = streak > 0

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = if (hasStreak) {
                            listOf(
                                FireOrangeLight.copy(alpha = 0.4f),
                                Color.Transparent
                            )
                        } else {
                            listOf(
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f),
                                Color.Transparent
                            )
                        }
                    )
                )
        ) {
            Text(
                text = if (hasStreak) "🔥" else "💤",
                fontSize = 48.sp
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        if (hasStreak) {
            Text(
                text = displayedStreak.toString(),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = FireOrange
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = stringResource(R.string.streak_day),
                style = MaterialTheme.typography.titleLarge.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        } else {
            Text(
                text = stringResource(R.string.streak_none),
                style = MaterialTheme.typography.titleLarge.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Composable
fun ProgressDisplay(
    progress: Int,
    goal: Int,
    modifier: Modifier = Modifier
) {
    val progressFraction = if (goal > 0) (progress.toFloat() / goal).coerceAtMost(1f) else 0f
    val isComplete = progress >= goal

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (isComplete) {
                FireOrange.copy(alpha = 0.15f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(if (isComplete) R.string.progress_goal_reached else R.string.progress_today),
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = if (isComplete) FireOrange else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isComplete) FontWeight.SemiBold else FontWeight.Normal
                    )
                )
                Text(
                    text = stringResource(R.string.progress_format, progress, goal),
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = if (isComplete) FireOrange else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = progressFraction,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (isComplete) FireOrange else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }
    }
}

@Composable
fun ActivityButtons(
    onActivitySelect: (ActivityType) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        ActivityButton(
            emoji = "🏃",
            label = stringResource(R.string.running),
            color = RunningColor,
            onClick = { onActivitySelect(ActivityType.RUNNING) }
        )

        ActivityButton(
            emoji = "🏋️",
            label = stringResource(R.string.aerobic),
            color = AerobicColor,
            onClick = { onActivitySelect(ActivityType.AEROBIC) }
        )

        ActivityButton(
            emoji = "🏊",
            label = stringResource(R.string.swimming),
            color = SwimmingColor,
            onClick = { onActivitySelect(ActivityType.SWIMMING) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityButton(
    emoji: String,
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.size(100.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = emoji,
                fontSize = 32.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ActivityInputPanel(
    activityType: ActivityType,
    pendingDuration: Int,
    onAddDuration: (Int) -> Unit,
    onClearDuration: () -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (emoji, label, color) = when (activityType) {
        ActivityType.RUNNING -> Triple("🏃", stringResource(R.string.running), RunningColor)
        ActivityType.AEROBIC -> Triple("🏋️", stringResource(R.string.aerobic), AerobicColor)
        ActivityType.SWIMMING -> Triple("🏊", stringResource(R.string.swimming), SwimmingColor)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Duration label
            Text(
                text = if (pendingDuration > 0) {
                    stringResource(R.string.input_duration_format, pendingDuration, label, emoji)
                } else {
                    "$label $emoji"
                },
                style = MaterialTheme.typography.headlineSmall.copy(
                    color = color,
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Duration increment buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                DurationButton(
                    text = stringResource(R.string.duration_5min),
                    onClick = { onAddDuration(5) }
                )
                DurationButton(
                    text = stringResource(R.string.duration_15min),
                    onClick = { onAddDuration(15) }
                )
                DurationButton(
                    text = stringResource(R.string.duration_1h),
                    onClick = { onAddDuration(60) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action buttons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.action_cancel))
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedButton(
                    onClick = onClearDuration,
                    modifier = Modifier.weight(1f),
                    enabled = pendingDuration > 0
                ) {
                    Text(stringResource(R.string.action_clear))
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                    enabled = pendingDuration > 0,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = color
                    )
                ) {
                    Text(stringResource(R.string.action_ok))
                }
            }
        }
    }
}

@Composable
fun DurationButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Text(text)
    }
}

@Composable
fun SettingsPanel(
    currentGoal: Int,
    reminderHour: Int,
    reminderMinute: Int,
    currentLanguage: String,
    onSetGoal: (Int) -> Unit,
    onSetReminderTime: (Int, Int) -> Unit,
    onSetLanguage: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
                TextButton(onClick = onClose) {
                    Text(stringResource(R.string.action_done))
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = stringResource(R.string.settings_daily_goal),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                SettingsDataStore.GOAL_OPTIONS.forEach { goalOption ->
                    GoalOptionRow(
                        minutes = goalOption,
                        isSelected = goalOption == currentGoal,
                        onSelect = { onSetGoal(goalOption) }
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = stringResource(R.string.settings_reminder_time),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.settings_reminder_description),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                ReminderTimePicker(
                    hour = reminderHour,
                    minute = reminderMinute,
                    onTimeChanged = onSetReminderTime
                )

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = stringResource(R.string.settings_language),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                SettingsDataStore.LANGUAGE_OPTIONS.forEach { languageCode ->
                    LanguageOptionRow(
                        languageCode = languageCode,
                        isSelected = languageCode == currentLanguage,
                        onSelect = { onSetLanguage(languageCode) }
                    )
                }
            }
        }
    }
}

@Composable
fun ReminderTimePicker(
    hour: Int,
    minute: Int,
    onTimeChanged: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Hour picker
            TimePickerWheel(
                value = hour,
                range = 0..23,
                onValueChange = { newHour -> onTimeChanged(newHour, minute) },
                formatValue = { String.format("%02d", it) }
            )

            Text(
                text = ":",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            // Minute picker
            TimePickerWheel(
                value = minute,
                range = 0..59,
                step = 5,
                onValueChange = { newMinute -> onTimeChanged(hour, newMinute) },
                formatValue = { String.format("%02d", it) }
            )
        }
    }
}

@Composable
fun TimePickerWheel(
    value: Int,
    range: IntRange,
    step: Int = 1,
    onValueChange: (Int) -> Unit,
    formatValue: (Int) -> String,
    modifier: Modifier = Modifier
) {
    val values = range.filter { it % step == 0 }
    val currentIndex = values.indexOf(value).coerceAtLeast(0)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Up button (increases value)
        IconButton(
            onClick = {
                val newIndex = (currentIndex + 1) % values.size
                onValueChange(values[newIndex])
            }
        ) {
            Text(
                text = "▲",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Current value
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = formatValue(value),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
            )
        }

        // Down button (decreases value)
        IconButton(
            onClick = {
                val newIndex = (currentIndex - 1 + values.size) % values.size
                onValueChange(values[newIndex])
            }
        ) {
            Text(
                text = "▼",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalOptionRow(
    minutes: Int,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onSelect,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when {
                    minutes < 60 -> stringResource(R.string.goal_minutes_format, minutes)
                    minutes % 60 == 0 -> stringResource(R.string.goal_hours_format, minutes / 60)
                    else -> stringResource(R.string.goal_hours_minutes_format, minutes / 60, minutes % 60)
                },
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
            )
            RadioButton(
                selected = isSelected,
                onClick = onSelect
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageOptionRow(
    languageCode: String,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displayName = if (languageCode == "system") {
        stringResource(R.string.language_system)
    } else {
        val locale = java.util.Locale(languageCode)
        locale.getDisplayLanguage(locale).replaceFirstChar { it.uppercaseChar() }
    }

    Card(
        onClick = onSelect,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = displayName,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
            )
            RadioButton(
                selected = isSelected,
                onClick = onSelect
            )
        }
    }
}
