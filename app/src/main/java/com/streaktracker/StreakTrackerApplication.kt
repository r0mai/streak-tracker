package com.streaktracker

import android.app.Application
import com.streaktracker.data.ActivityDatabase
import com.streaktracker.data.SettingsDataStore
import com.streaktracker.notification.NotificationHelper
import com.streaktracker.notification.ReminderScheduler
import com.streaktracker.repository.ActivityRepository

class StreakTrackerApplication : Application() {

    val database: ActivityDatabase by lazy {
        ActivityDatabase.getDatabase(this)
    }

    val settings: SettingsDataStore by lazy {
        SettingsDataStore(this)
    }

    val repository: ActivityRepository by lazy {
        ActivityRepository(
            activityDao = database.activityDao(),
            dayStatusDao = database.dayStatusDao(),
            settings = settings
        )
    }

    override fun onCreate() {
        super.onCreate()

        // Create notification channel
        NotificationHelper.createNotificationChannel(this)

        // Schedule daily reminder using AlarmManager for exact timing
        ReminderScheduler.scheduleReminder(this)
    }
}

