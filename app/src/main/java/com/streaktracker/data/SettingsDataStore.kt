package com.streaktracker.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {

    companion object {
        private val DAILY_GOAL_KEY = intPreferencesKey("daily_goal_minutes")
        const val DEFAULT_DAILY_GOAL = 30

        val GOAL_OPTIONS = listOf(15, 30, 60, 90, 120)
    }

    val dailyGoalMinutes: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[DAILY_GOAL_KEY] ?: DEFAULT_DAILY_GOAL
    }

    suspend fun setDailyGoal(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[DAILY_GOAL_KEY] = minutes
        }
    }

    suspend fun getDailyGoalOnce(): Int {
        var goal = DEFAULT_DAILY_GOAL
        context.dataStore.data.collect { preferences ->
            goal = preferences[DAILY_GOAL_KEY] ?: DEFAULT_DAILY_GOAL
            return@collect
        }
        return goal
    }
}
