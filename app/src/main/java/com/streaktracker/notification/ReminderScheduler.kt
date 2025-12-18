package com.streaktracker.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.streaktracker.data.SettingsDataStore
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object ReminderScheduler {
    
    private const val TAG = "ReminderScheduler"
    private const val REQUEST_CODE = 1001
    
    private fun getReminderTime(context: Context): LocalTime {
        val settings = SettingsDataStore(context)
        return runBlocking {
            val hour = settings.getReminderHour()
            val minute = settings.getReminderMinute()
            Log.d(TAG, "Reminder time from settings: $hour:$minute")
            LocalTime.of(hour, minute)
        }
    }
    
    /**
     * Schedule reminder using the time stored in settings.
     */
    fun scheduleReminder(context: Context) {
        val reminderTime = getReminderTime(context)
        scheduleReminderAt(context, reminderTime.hour, reminderTime.minute)
    }
    
    /**
     * Schedule reminder at a specific time. Use this when you have the time directly
     * (e.g., right after user changes it) to avoid race conditions with DataStore.
     */
    fun scheduleReminderAt(context: Context, hour: Int, minute: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        // Check if we can schedule exact alarms on Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val canSchedule = alarmManager.canScheduleExactAlarms()
            Log.d(TAG, "Can schedule exact alarms: $canSchedule")
            if (!canSchedule) {
                // Fall back to inexact alarm if permission not granted
                Log.w(TAG, "Exact alarm permission not granted, using inexact alarm")
                scheduleInexactReminderAt(context, hour, minute)
                return
            }
        }
        
        val triggerTime = calculateNextTriggerTimeFor(hour, minute)
        val triggerDateTime = LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(triggerTime),
            ZoneId.systemDefault()
        )
        Log.d(TAG, "Scheduling exact alarm for: ${triggerDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))} (requested time: $hour:$minute)")
        
        val pendingIntent = createPendingIntent(context)
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
                Log.d(TAG, "Exact alarm scheduled successfully")
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
                Log.d(TAG, "Exact alarm (pre-M) scheduled successfully")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException scheduling exact alarm", e)
            // Fallback to inexact alarm if exact alarm permission is denied
            scheduleInexactReminderAt(context, hour, minute)
        }
    }
    
    private fun scheduleInexactReminderAt(context: Context, hour: Int, minute: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerTime = calculateNextTriggerTimeFor(hour, minute)
        scheduleInexactReminderAtMillis(context, alarmManager, triggerTime)
    }
    
    fun scheduleNextReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val reminderTime = getReminderTime(context)
        
        // Schedule for tomorrow at the configured time
        val tomorrow = LocalDate.now().plusDays(1)
        val triggerTime = tomorrow.atTime(reminderTime)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        
        Log.d(TAG, "Scheduling next reminder for tomorrow at ${reminderTime.hour}:${reminderTime.minute}")
        
        val pendingIntent = createPendingIntent(context)
        
        // Check if we can schedule exact alarms on Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                scheduleInexactReminderAtMillis(context, alarmManager, triggerTime)
                return
            }
        }
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            scheduleInexactReminderAtMillis(context, alarmManager, triggerTime)
        }
    }
    
    fun cancelReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = createPendingIntent(context)
        alarmManager.cancel(pendingIntent)
    }
    
    private fun calculateNextTriggerTimeFor(hour: Int, minute: Int): Long {
        val reminderTime = LocalTime.of(hour, minute)
        val now = LocalDateTime.now()
        var nextReminder = now.toLocalDate().atTime(reminderTime)
        
        if (now.isAfter(nextReminder)) {
            // If it's past the reminder time, schedule for tomorrow
            nextReminder = nextReminder.plusDays(1)
        }
        
        return nextReminder
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }
    
    private fun createPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_SHOW_REMINDER
        }
        
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
    
    private fun scheduleInexactReminderAtMillis(context: Context, alarmManager: AlarmManager, triggerTime: Long) {
        val pendingIntent = createPendingIntent(context)
        
        // Use setWindow for a reasonable window around the target time
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        } else {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
    }
}

