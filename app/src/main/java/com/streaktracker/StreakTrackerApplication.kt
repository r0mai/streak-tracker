package com.streaktracker

import android.app.Application
import com.streaktracker.data.ActivityDatabase
import com.streaktracker.notification.NotificationHelper
import com.streaktracker.notification.ReminderWorker
import com.streaktracker.repository.ActivityRepository

class StreakTrackerApplication : Application() {
    
    val database: ActivityDatabase by lazy { 
        ActivityDatabase.getDatabase(this) 
    }
    
    val repository: ActivityRepository by lazy { 
        ActivityRepository(database.activityDao()) 
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // Create notification channel
        NotificationHelper.createNotificationChannel(this)
        
        // Schedule daily reminder
        ReminderWorker.scheduleReminder(this)
    }
}

