package com.streaktracker.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

object ReminderScheduler {
    
    private val REMINDER_TIME = LocalTime.of(20, 0) // 8 PM
    private const val REQUEST_CODE = 1001
    
    fun scheduleReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        // Check if we can schedule exact alarms on Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                // Fall back to inexact alarm if permission not granted
                scheduleInexactReminder(context, alarmManager)
                return
            }
        }
        
        val triggerTime = calculateNextTriggerTime()
        val pendingIntent = createPendingIntent(context)
        
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
            // Fallback to inexact alarm if exact alarm permission is denied
            scheduleInexactReminder(context, alarmManager)
        }
    }
    
    fun scheduleNextReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        // Schedule for tomorrow at 8 PM
        val tomorrow = LocalDate.now().plusDays(1)
        val triggerTime = tomorrow.atTime(REMINDER_TIME)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        
        val pendingIntent = createPendingIntent(context)
        
        // Check if we can schedule exact alarms on Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                scheduleInexactReminderAt(context, alarmManager, triggerTime)
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
            scheduleInexactReminderAt(context, alarmManager, triggerTime)
        }
    }
    
    fun cancelReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = createPendingIntent(context)
        alarmManager.cancel(pendingIntent)
    }
    
    private fun calculateNextTriggerTime(): Long {
        val now = java.time.LocalDateTime.now()
        var nextReminder = now.toLocalDate().atTime(REMINDER_TIME)
        
        if (now.isAfter(nextReminder)) {
            // If it's past 8 PM, schedule for tomorrow
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
    
    private fun scheduleInexactReminder(context: Context, alarmManager: AlarmManager) {
        val triggerTime = calculateNextTriggerTime()
        scheduleInexactReminderAt(context, alarmManager, triggerTime)
    }
    
    private fun scheduleInexactReminderAt(context: Context, alarmManager: AlarmManager, triggerTime: Long) {
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

