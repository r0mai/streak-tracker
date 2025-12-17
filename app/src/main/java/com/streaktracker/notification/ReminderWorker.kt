package com.streaktracker.notification

import android.content.Context
import androidx.work.*
import com.streaktracker.data.ActivityDatabase
import com.streaktracker.data.SettingsDataStore
import com.streaktracker.repository.ActivityRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

class ReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                val database = ActivityDatabase.getDatabase(applicationContext)
                val settings = SettingsDataStore(applicationContext)
                val repository = ActivityRepository(
                    activityDao = database.activityDao(),
                    dayStatusDao = database.dayStatusDao(),
                    settings = settings
                )

                // Check if daily goal is not yet met
                val isStreakAtRisk = repository.isStreakAtRisk()

                if (isStreakAtRisk) {
                    // Show notification
                    NotificationHelper.showReminderNotification(applicationContext)
                }

                // Schedule next reminder for tomorrow
                scheduleNextReminder(applicationContext)

                Result.success()
            } catch (_: Exception) {
                Result.retry()
            }
        }
    }
    
    companion object {
        const val WORK_NAME = "streak_reminder_work"
        private val REMINDER_TIME = LocalTime.of(20, 0) // 8 PM
        
        fun scheduleReminder(context: Context) {
            val workManager = WorkManager.getInstance(context)
            
            // Calculate delay until next 8 PM
            val now = LocalDateTime.now()
            var nextReminder = now.toLocalDate().atTime(REMINDER_TIME)
            
            if (now.isAfter(nextReminder)) {
                // If it's past 8 PM, schedule for tomorrow
                nextReminder = nextReminder.plusDays(1)
            }
            
            val delay = Duration.between(now, nextReminder)
            
            val reminderRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(delay.toMinutes(), TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(false)
                        .build()
                )
                .addTag(WORK_NAME)
                .build()
            
            workManager.enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                reminderRequest
            )
        }
        
        private fun scheduleNextReminder(context: Context) {
            val workManager = WorkManager.getInstance(context)
            
            // Schedule for tomorrow at 8 PM
            val now = LocalDateTime.now()
            val nextReminder = now.toLocalDate().plusDays(1).atTime(REMINDER_TIME)
            val delay = Duration.between(now, nextReminder)
            
            val reminderRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(delay.toMinutes(), TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(false)
                        .build()
                )
                .addTag(WORK_NAME)
                .build()
            
            workManager.enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                reminderRequest
            )
        }
        
        fun cancelReminder(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}

