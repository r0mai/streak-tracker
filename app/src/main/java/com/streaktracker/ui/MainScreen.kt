package com.streaktracker.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streaktracker.data.ActivityType
import com.streaktracker.ui.theme.*

@Composable
fun MainScreen(
    uiState: MainUiState,
    onActivityClick: (ActivityType) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    
    Column(
        modifier = modifier
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
        Spacer(modifier = Modifier.height(32.dp))
        
        // Streak Display
        StreakDisplay(
            streak = uiState.currentStreak,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        // Today's Status
        TodayStatus(
            todayActivity = uiState.todayActivity?.activityType,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        // Activity Buttons
        ActivityButtons(
            onActivityClick = onActivityClick,
            selectedActivity = uiState.todayActivity?.activityType,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        // Calendar
        CalendarView(
            currentMonth = uiState.currentMonth,
            activities = uiState.monthActivities,
            onPreviousMonth = onPreviousMonth,
            onNextMonth = onNextMonth,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun StreakDisplay(
    streak: Int,
    modifier: Modifier = Modifier
) {
    val animatedStreak by animateFloatAsState(
        targetValue = streak.toFloat(),
        animationSpec = tween(durationMillis = 500),
        label = "streak"
    )
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Fire emoji with glow effect
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            FireOrangeLight.copy(alpha = 0.4f),
                            Color.Transparent
                        )
                    )
                )
        ) {
            Text(
                text = "🔥",
                fontSize = 48.sp
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = animatedStreak.toInt().toString(),
            style = MaterialTheme.typography.displayLarge.copy(
                fontWeight = FontWeight.Bold,
                color = FireOrange
            )
        )
        
        Text(
            text = if (streak == 1) "day streak" else "days streak",
            style = MaterialTheme.typography.titleMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}

@Composable
fun TodayStatus(
    todayActivity: ActivityType?,
    modifier: Modifier = Modifier
) {
    val (statusText, statusColor) = when (todayActivity) {
        ActivityType.RUNNING -> "Today: Running 🏃" to RunningColor
        ActivityType.AEROBIC -> "Today: Aerobic 🏋️" to AerobicColor
        ActivityType.SWIMMING -> "Today: Swimming 🏊" to SwimmingColor
        null -> "No activity logged today" to MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    AnimatedVisibility(
        visible = true,
        enter = fadeIn() + scaleIn()
    ) {
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (todayActivity != null) {
                    statusColor.copy(alpha = 0.15f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                }
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (todayActivity != null) {
                    Text(
                        text = "✓ ",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = statusColor,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = if (todayActivity != null) statusColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (todayActivity != null) FontWeight.SemiBold else FontWeight.Normal
                    )
                )
            }
        }
    }
}

@Composable
fun ActivityButtons(
    onActivityClick: (ActivityType) -> Unit,
    selectedActivity: ActivityType?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        ActivityButton(
            emoji = "🏃",
            label = "Running",
            color = RunningColor,
            isSelected = selectedActivity == ActivityType.RUNNING,
            onClick = { onActivityClick(ActivityType.RUNNING) }
        )
        
        ActivityButton(
            emoji = "🏋️",
            label = "Aerobic",
            color = AerobicColor,
            isSelected = selectedActivity == ActivityType.AEROBIC,
            onClick = { onActivityClick(ActivityType.AEROBIC) }
        )
        
        ActivityButton(
            emoji = "🏊",
            label = "Swimming",
            color = SwimmingColor,
            isSelected = selectedActivity == ActivityType.SWIMMING,
            onClick = { onActivityClick(ActivityType.SWIMMING) }
        )
    }
}

@Composable
fun ActivityButton(
    emoji: String,
    label: String,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = tween(durationMillis = 200),
        label = "scale"
    )
    
    Card(
        onClick = onClick,
        modifier = modifier
            .size(100.dp)
            .scale(scale),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                color.copy(alpha = 0.2f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 4.dp
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, color)
        } else null
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
                    color = if (isSelected) color else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                ),
                textAlign = TextAlign.Center
            )
            
            if (isSelected) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "✓",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = color,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

