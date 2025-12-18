package com.streaktracker.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.streaktracker.data.ActivityDatabase
import com.streaktracker.data.SettingsDataStore
import com.streaktracker.repository.ActivityRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_SHOW_REMINDER) {
            val pendingResult = goAsync()
            
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val database = ActivityDatabase.getDatabase(context)
                    val settings = SettingsDataStore(context)
                    val repository = ActivityRepository(
                        activityDao = database.activityDao(),
                        dayStatusDao = database.dayStatusDao(),
                        settings = settings
                    )
                    
                    // Check if daily goal is not yet met
                    val isStreakAtRisk = repository.isStreakAtRisk()
                    
                    if (isStreakAtRisk) {
                        NotificationHelper.showReminderNotification(context)
                    }
                    
                    // Schedule next reminder for tomorrow
                    ReminderScheduler.scheduleNextReminder(context)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
    
    companion object {
        const val ACTION_SHOW_REMINDER = "com.streaktracker.ACTION_SHOW_REMINDER"
    }
}

