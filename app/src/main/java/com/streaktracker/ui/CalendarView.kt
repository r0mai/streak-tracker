package com.streaktracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.streaktracker.R
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
    selectedDay: LocalDate?,
    onDayClick: (LocalDate) -> Unit,
    onDismissPopup: () -> Unit,
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
                        contentDescription = stringResource(R.string.calendar_previous_month),
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
                        contentDescription = stringResource(R.string.calendar_next_month),
                        tint = if (isCurrentMonth) {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Day of Week Headers (Monday-first)
            val dayNames = listOf(
                stringResource(R.string.calendar_day_monday),
                stringResource(R.string.calendar_day_tuesday),
                stringResource(R.string.calendar_day_wednesday),
                stringResource(R.string.calendar_day_thursday),
                stringResource(R.string.calendar_day_friday),
                stringResource(R.string.calendar_day_saturday),
                stringResource(R.string.calendar_day_sunday)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                dayNames.forEach { day ->
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
            val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value - 1 // Monday = 0
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

                            val isSelected = selectedDay == date
                            val hasActivities = dayActivities.isNotEmpty() ||
                                (isToday && todayProgress > 0)

                            CalendarDay(
                                day = dayOfMonth,
                                completionState = completionState,
                                dominantActivityType = dominantActivityType,
                                isToday = isToday,
                                isFuture = isFuture,
                                isSelected = isSelected,
                                onClick = if (!isFuture && hasActivities) {
                                    { onDayClick(date) }
                                } else null,
                                modifier = Modifier.weight(1f)
                            )

                            // Show popup for selected day
                            if (isSelected && hasActivities) {
                                DaySummaryPopup(
                                    date = date,
                                    activities = dayActivities,
                                    dayStatus = dayStatus,
                                    todayProgress = if (isToday) todayProgress else null,
                                    dailyGoal = if (isToday) dailyGoal else dayStatus?.dailyGoal,
                                    onDismiss = onDismissPopup
                                )
                            }
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
    isSelected: Boolean,
    onClick: (() -> Unit)?,
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
        isSelected -> MaterialTheme.colorScheme.primary
        isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        else -> Color.Transparent
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .then(
                if (isToday || isSelected) {
                    Modifier.border(2.dp, borderColor, CircleShape)
                } else {
                    Modifier
                }
            )
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
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
            LegendItem(color = RunningColor, label = stringResource(R.string.running))
            LegendItem(color = AerobicColor, label = stringResource(R.string.aerobic))
            LegendItem(color = SwimmingColor, label = stringResource(R.string.swimming))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            CompletionLegendItem(isFilled = true, label = stringResource(R.string.legend_complete))
            CompletionLegendItem(isFilled = false, label = stringResource(R.string.legend_partial))
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

@Composable
fun DaySummaryPopup(
    date: LocalDate,
    activities: List<ActivityEntry>,
    dayStatus: DayStatus?,
    todayProgress: Int?,
    dailyGoal: Int?,
    onDismiss: () -> Unit
) {
    Popup(
        alignment = Alignment.TopCenter,
        offset = androidx.compose.ui.unit.IntOffset(0, -120),
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        Card(
            modifier = Modifier
                .padding(8.dp)
                .clickable(onClick = onDismiss),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Date header
                val dateFormatter = java.time.format.DateTimeFormatter.ofPattern(
                    "EEEE, MMMM d",
                    Locale.getDefault()
                )
                Text(
                    text = date.format(dateFormatter),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Group activities by type and sum durations
                val durationByType = activities
                    .groupBy { it.activityType }
                    .mapValues { (_, entries) -> entries.sumOf { it.duration } }

                // Show each activity type with duration
                ActivityType.entries.forEach { activityType ->
                    val duration = durationByType[activityType]
                    if (duration != null && duration > 0) {
                        val emoji = when (activityType) {
                            ActivityType.RUNNING -> "🏃"
                            ActivityType.AEROBIC -> "🏋️"
                            ActivityType.SWIMMING -> "🏊"
                        }
                        val label = when (activityType) {
                            ActivityType.RUNNING -> stringResource(R.string.running)
                            ActivityType.AEROBIC -> stringResource(R.string.aerobic)
                            ActivityType.SWIMMING -> stringResource(R.string.swimming)
                        }
                        Text(
                            text = "$emoji $label: $duration min",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // Divider and total
                val totalDuration = todayProgress ?: dayStatus?.totalDuration ?: activities.sumOf { it.duration }
                if (totalDuration > 0) {
                    Divider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )

                    val goalMet = dailyGoal?.let { totalDuration >= it } ?: (dayStatus?.completed == true)
                    Text(
                        text = if (goalMet) {
                            stringResource(R.string.popup_total_complete, totalDuration)
                        } else {
                            stringResource(R.string.popup_total, totalDuration)
                        },
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = if (goalMet) FireOrange else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    }
}
