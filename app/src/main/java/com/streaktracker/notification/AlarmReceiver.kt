package com.streaktracker.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.streaktracker.data.ActivityDatabase
import com.streaktracker.data.SettingsDataStore
import com.streaktracker.repository.ActivityRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "AlarmReceiver"
        const val ACTION_SHOW_REMINDER = "com.streaktracker.ACTION_SHOW_REMINDER"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive called with action: ${intent.action}")
        
        if (intent.action == ACTION_SHOW_REMINDER) {
            Log.d(TAG, "Processing reminder action")
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
                    Log.d(TAG, "Is streak at risk: $isStreakAtRisk")
                    
                    if (isStreakAtRisk) {
                        Log.d(TAG, "Showing notification")
                        NotificationHelper.showReminderNotification(context)
                    } else {
                        Log.d(TAG, "Goal already met, skipping notification")
                    }
                    
                    // Schedule next reminder for tomorrow
                    Log.d(TAG, "Scheduling next reminder")
                    ReminderScheduler.scheduleNextReminder(context)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in alarm receiver", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}

