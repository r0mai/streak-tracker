package com.streaktracker.repository

import com.streaktracker.data.ActivityDao
import com.streaktracker.data.ActivityEntry
import com.streaktracker.data.ActivityType
import com.streaktracker.data.DayStatus
import com.streaktracker.data.DayStatusDao
import com.streaktracker.data.SettingsDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.combine
import java.time.LocalDate

class ActivityRepository(
    private val activityDao: ActivityDao,
    private val dayStatusDao: DayStatusDao,
    private val settings: SettingsDataStore
) {

    // ===== Activity Logging =====

    suspend fun logActivity(activityType: ActivityType, duration: Int) {
        val entry = ActivityEntry(
            date = LocalDate.now(),
            activityType = activityType,
            duration = duration
        )
        activityDao.insertActivity(entry)

        // Update today's DayStatus (non-finalized)
        updateTodayStatus()
    }

    private suspend fun updateTodayStatus() {
        val today = LocalDate.now()
        val totalDuration = activityDao.getTotalDurationForDate(today)
        val dailyGoal = settings.dailyGoalMinutes.first()

        val status = DayStatus(
            date = today,
            totalDuration = totalDuration,
            dailyGoal = dailyGoal,
            completed = totalDuration >= dailyGoal,
            finalized = false
        )
        dayStatusDao.upsert(status)
    }

    // ===== Today's Progress =====

    fun getTodayProgress(): Flow<Pair<Int, Int>> {
        return combine(
            activityDao.getTotalDurationForDateFlow(LocalDate.now()),
            settings.dailyGoalMinutes
        ) { total, goal ->
            Pair(total, goal)
        }
    }

    fun getTodayActivities(): Flow<List<ActivityEntry>> {
        return activityDao.getActivitiesForDateFlow(LocalDate.now())
    }

    // ===== Streak Calculation =====

    /**
     * Calculate the current streak.
     *
     * The streak is the number of consecutive completed days,
     * counting backwards from today (or yesterday if today is not complete yet).
     */
    suspend fun calculateStreak(): Int {
        // Ensure past days are finalized
        finalizePastDays()

        val today = LocalDate.now()
        val dailyGoal = settings.dailyGoalMinutes.first()

        // Check if today is complete (live calculation)
        val todayTotal = activityDao.getTotalDurationForDate(today)
        val todayComplete = todayTotal >= dailyGoal

        var streak = if (todayComplete) 1 else 0
        var checkDate = today.minusDays(1)

        // Check past days from DayStatus table
        while (true) {
            val dayStatus = dayStatusDao.getStatus(checkDate)
            if (dayStatus == null || !dayStatus.completed) break
            streak++
            checkDate = checkDate.minusDays(1)
        }

        return streak
    }

    /**
     * Check if the streak is at risk (today's goal not yet met).
     */
    suspend fun isStreakAtRisk(): Boolean {
        val todayTotal = activityDao.getTotalDurationForDate(LocalDate.now())
        val dailyGoal = settings.dailyGoalMinutes.first()
        return todayTotal < dailyGoal
    }

    // ===== Day Finalization =====

    /**
     * Finalize all past days that haven't been finalized yet.
     * This ensures no gaps in the DayStatus table.
     */
    suspend fun finalizePastDays() {
        val today = LocalDate.now()

        // Find the earliest date we need to consider
        val earliestActivity = activityDao.getEarliestDate()
        val earliestDayStatus = dayStatusDao.getEarliestDate()

        val startDate = when {
            earliestActivity != null && earliestDayStatus != null ->
                if (earliestActivity < earliestDayStatus) earliestActivity else earliestDayStatus
            earliestActivity != null -> earliestActivity
            earliestDayStatus != null -> earliestDayStatus
            else -> return // No data at all
        }

        // Process each day from startDate to yesterday
        var date = startDate
        val dailyGoal = settings.dailyGoalMinutes.first()

        while (date < today) {
            finalizeDay(date, dailyGoal)
            date = date.plusDays(1)
        }
    }

    private suspend fun finalizeDay(date: LocalDate, defaultGoal: Int) {
        val existing = dayStatusDao.getStatus(date)
        if (existing?.finalized == true) return // Already finalized

        val totalDuration = activityDao.getTotalDurationForDate(date)
        val goal = existing?.dailyGoal ?: defaultGoal

        val status = DayStatus(
            date = date,
            totalDuration = totalDuration,
            dailyGoal = goal,
            completed = totalDuration >= goal,
            finalized = true
        )
        dayStatusDao.upsert(status)
    }

    // ===== Calendar & History =====

    fun getActivitiesForMonth(year: Int, month: Int): Flow<List<ActivityEntry>> {
        val startDate = LocalDate.of(year, month, 1)
        val endDate = startDate.plusMonths(1).minusDays(1)
        return activityDao.getActivitiesInRange(startDate, endDate)
    }

    fun getDayStatusesForMonth(year: Int, month: Int): Flow<List<DayStatus>> {
        val startDate = LocalDate.of(year, month, 1)
        val endDate = startDate.plusMonths(1).minusDays(1)
        return dayStatusDao.getStatusesInRange(startDate, endDate)
    }

    fun getActivitiesInRange(startDate: LocalDate, endDate: LocalDate): Flow<List<ActivityEntry>> {
        return activityDao.getActivitiesInRange(startDate, endDate)
    }

    fun getAllActivities(): Flow<List<ActivityEntry>> {
        return activityDao.getAllActivities()
    }

    // ===== Settings =====

    fun getDailyGoal(): Flow<Int> {
        return settings.dailyGoalMinutes
    }

    suspend fun setDailyGoal(minutes: Int) {
        settings.setDailyGoal(minutes)
        // Update today's status with new goal (if today exists and is not finalized)
        updateTodayStatus()
    }

    fun getReminderTime(): Flow<Pair<Int, Int>> {
        return combine(
            settings.reminderHour,
            settings.reminderMinute
        ) { hour, minute ->
            Pair(hour, minute)
        }
    }

    suspend fun setReminderTime(hour: Int, minute: Int) {
        settings.setReminderTime(hour, minute)
    }
}
