package com.streaktracker.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {

    companion object {
        private val DAILY_GOAL_KEY = intPreferencesKey("daily_goal_minutes")
        private val REMINDER_HOUR_KEY = intPreferencesKey("reminder_hour")
        private val REMINDER_MINUTE_KEY = intPreferencesKey("reminder_minute")
        
        const val DEFAULT_DAILY_GOAL = 30
        const val DEFAULT_REMINDER_HOUR = 20 // 8 PM
        const val DEFAULT_REMINDER_MINUTE = 0

        val GOAL_OPTIONS = listOf(15, 30, 60, 90, 120)
    }

    val dailyGoalMinutes: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[DAILY_GOAL_KEY] ?: DEFAULT_DAILY_GOAL
    }

    val reminderHour: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[REMINDER_HOUR_KEY] ?: DEFAULT_REMINDER_HOUR
    }

    val reminderMinute: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[REMINDER_MINUTE_KEY] ?: DEFAULT_REMINDER_MINUTE
    }

    suspend fun setDailyGoal(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[DAILY_GOAL_KEY] = minutes
        }
    }

    suspend fun setReminderTime(hour: Int, minute: Int) {
        context.dataStore.edit { preferences ->
            preferences[REMINDER_HOUR_KEY] = hour
            preferences[REMINDER_MINUTE_KEY] = minute
        }
    }

    suspend fun getReminderHour(): Int {
        return context.dataStore.data.first()[REMINDER_HOUR_KEY] ?: DEFAULT_REMINDER_HOUR
    }

    suspend fun getReminderMinute(): Int {
        return context.dataStore.data.first()[REMINDER_MINUTE_KEY] ?: DEFAULT_REMINDER_MINUTE
    }
}
