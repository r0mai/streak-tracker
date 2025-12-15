package com.streaktracker.repository

import com.streaktracker.data.ActivityDao
import com.streaktracker.data.ActivityEntry
import com.streaktracker.data.ActivityType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class ActivityRepository(private val activityDao: ActivityDao) {

    fun getTodayActivity(): Flow<ActivityEntry?> {
        return getActivityForDate(LocalDate.now())
    }

    fun getActivityForDate(date: LocalDate): Flow<ActivityEntry?> {
        return activityDao.getActivityForDateFlow(date)
    }

    suspend fun getTodayActivityDirect(): ActivityEntry? {
        return activityDao.getActivityForDate(LocalDate.now())
    }

    suspend fun logActivity(activityType: ActivityType) {
        val entry = ActivityEntry(
            date = LocalDate.now(),
            activityType = activityType
        )
        activityDao.insertActivity(entry)
    }

    fun getActivitiesForMonth(year: Int, month: Int): Flow<List<ActivityEntry>> {
        val startDate = LocalDate.of(year, month, 1)
        val endDate = startDate.plusMonths(1).minusDays(1)
        return activityDao.getActivitiesInRange(startDate, endDate)
    }

    fun getActivitiesInRange(startDate: LocalDate, endDate: LocalDate): Flow<List<ActivityEntry>> {
        return activityDao.getActivitiesInRange(startDate, endDate)
    }

    /**
     * Calculate the current streak.
     * 
     * The streak is the number of consecutive days with logged activity,
     * counting backwards from today (or yesterday if today has no activity yet).
     */
    suspend fun calculateStreak(): Int {
        val activities = activityDao.getAllActivitiesList()
        if (activities.isEmpty()) return 0

        val activityDates = activities.map { it.date }.toSet()
        val today = LocalDate.now()
        
        var streak = 0
        var currentDate = today
        
        // Check if today has activity
        if (activityDates.contains(today)) {
            streak = 1
            currentDate = today.minusDays(1)
        } else {
            // If no activity today, start checking from yesterday
            currentDate = today.minusDays(1)
        }
        
        // Count consecutive days backwards
        while (activityDates.contains(currentDate)) {
            streak++
            currentDate = currentDate.minusDays(1)
        }
        
        return streak
    }

    /**
     * Check if the streak is at risk (no activity logged today).
     */
    suspend fun isStreakAtRisk(): Boolean {
        val todayActivity = activityDao.getActivityForDate(LocalDate.now())
        return todayActivity == null
    }

    fun getAllActivities(): Flow<List<ActivityEntry>> {
        return activityDao.getAllActivities()
    }
}

