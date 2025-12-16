package com.streaktracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.streaktracker.data.ActivityEntry
import com.streaktracker.data.ActivityType
import com.streaktracker.data.DayStatus
import com.streaktracker.ui.theme.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.*

enum class DayCompletionState {
    NO_ACTIVITY,
    PARTIAL,
    COMPLETE
}

@Composable
fun CalendarView(
    currentMonth: YearMonth,
    activities: List<ActivityEntry>,
    dayStatuses: Map<LocalDate, DayStatus>,
    todayProgress: Int,
    dailyGoal: Int,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()
    val isCurrentMonth = currentMonth == YearMonth.now()

    // Group activities by date for display
    val activitiesByDate = activities.groupBy { it.date }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Month Navigation Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPreviousMonth) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowLeft,
                        contentDescription = "Previous month",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${currentMonth.year}",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )

                IconButton(
                    onClick = onNextMonth,
                    enabled = !isCurrentMonth
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = "Next month",
                        tint = if (isCurrentMonth) {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Day of Week Headers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa").forEach { day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Calendar Grid
            val firstDayOfMonth = currentMonth.atDay(1)
            val lastDayOfMonth = currentMonth.atEndOfMonth()
            val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7 // Sunday = 0
            val daysInMonth = lastDayOfMonth.dayOfMonth

            val totalCells = firstDayOfWeek + daysInMonth
            val rows = (totalCells + 6) / 7

            for (row in 0 until rows) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (col in 0..6) {
                        val cellIndex = row * 7 + col
                        val dayOfMonth = cellIndex - firstDayOfWeek + 1

                        if (dayOfMonth in 1..daysInMonth) {
                            val date = currentMonth.atDay(dayOfMonth)
                            val dayActivities = activitiesByDate[date] ?: emptyList()
                            val dayStatus = dayStatuses[date]
                            val isToday = date == today
                            val isFuture = date.isAfter(today)

                            // Determine completion state
                            val completionState = when {
                                isFuture -> DayCompletionState.NO_ACTIVITY
                                isToday -> {
                                    // Use live progress for today
                                    when {
                                        todayProgress >= dailyGoal -> DayCompletionState.COMPLETE
                                        todayProgress > 0 -> DayCompletionState.PARTIAL
                                        else -> DayCompletionState.NO_ACTIVITY
                                    }
                                }
                                dayStatus != null -> {
                                    when {
                                        dayStatus.completed -> DayCompletionState.COMPLETE
                                        dayStatus.totalDuration > 0 -> DayCompletionState.PARTIAL
                                        else -> DayCompletionState.NO_ACTIVITY
                                    }
                                }
                                dayActivities.isNotEmpty() -> DayCompletionState.PARTIAL
                                else -> DayCompletionState.NO_ACTIVITY
                            }

                            // Get dominant activity type for coloring
                            val dominantActivityType = dayActivities
                                .groupBy { it.activityType }
                                .maxByOrNull { it.value.sumOf { entry -> entry.duration } }
                                ?.key

                            CalendarDay(
                                day = dayOfMonth,
                                completionState = completionState,
                                dominantActivityType = dominantActivityType,
                                isToday = isToday,
                                isFuture = isFuture,
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            // Empty cell
                            Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Legend
            CalendarLegend()
        }
    }
}

@Composable
fun CalendarDay(
    day: Int,
    completionState: DayCompletionState,
    dominantActivityType: ActivityType?,
    isToday: Boolean,
    isFuture: Boolean,
    modifier: Modifier = Modifier
) {
    val activityColor = dominantActivityType?.let { getActivityColor(it) }

    val backgroundColor = when {
        completionState == DayCompletionState.COMPLETE && activityColor != null ->
            activityColor.copy(alpha = 0.3f)
        completionState == DayCompletionState.PARTIAL && activityColor != null ->
            activityColor.copy(alpha = 0.15f)
        isToday -> CalendarToday
        else -> Color.Transparent
    }

    val textColor = when {
        isFuture -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        completionState == DayCompletionState.COMPLETE && activityColor != null -> activityColor
        completionState == DayCompletionState.PARTIAL && activityColor != null ->
            activityColor.copy(alpha = 0.7f)
        isToday -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }

    val borderColor = when {
        isToday -> MaterialTheme.colorScheme.primary
        else -> Color.Transparent
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .then(
                if (isToday) {
                    Modifier.border(2.dp, borderColor, CircleShape)
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = day.toString(),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = textColor,
                    fontWeight = when {
                        isToday -> FontWeight.Bold
                        completionState != DayCompletionState.NO_ACTIVITY -> FontWeight.SemiBold
                        else -> FontWeight.Normal
                    }
                )
            )

            // Completion indicator
            when (completionState) {
                DayCompletionState.COMPLETE -> {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(activityColor ?: FireOrange)
                    )
                }
                DayCompletionState.PARTIAL -> {
                    // Half-filled or hollow dot for partial
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .border(
                                width = 1.5.dp,
                                color = activityColor ?: MaterialTheme.colorScheme.primary,
                                shape = CircleShape
                            )
                    )
                }
                DayCompletionState.NO_ACTIVITY -> {
                    // No indicator
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
fun CalendarLegend() {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            LegendItem(color = RunningColor, label = "Running")
            LegendItem(color = AerobicColor, label = "Aerobic")
            LegendItem(color = SwimmingColor, label = "Swimming")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            CompletionLegendItem(isFilled = true, label = "Complete")
            CompletionLegendItem(isFilled = false, label = "Partial")
        }
    }
}

@Composable
fun LegendItem(
    color: Color,
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}

@Composable
fun CompletionLegendItem(
    isFilled: Boolean,
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .then(
                    if (isFilled) {
                        Modifier.background(MaterialTheme.colorScheme.primary)
                    } else {
                        Modifier.border(
                            width = 1.5.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        )
                    }
                )
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}

fun getActivityColor(activityType: ActivityType): Color {
    return when (activityType) {
        ActivityType.RUNNING -> RunningColor
        ActivityType.AEROBIC -> AerobicColor
        ActivityType.SWIMMING -> SwimmingColor
    }
}
