package com.streaktracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface ActivityDao {
    @Insert
    suspend fun insertActivity(activity: ActivityEntry)

    @Query("SELECT * FROM activities WHERE date = :date ORDER BY timestamp ASC")
    suspend fun getActivitiesForDate(date: LocalDate): List<ActivityEntry>

    @Query("SELECT * FROM activities WHERE date = :date ORDER BY timestamp ASC")
    fun getActivitiesForDateFlow(date: LocalDate): Flow<List<ActivityEntry>>

    @Query("SELECT COALESCE(SUM(duration), 0) FROM activities WHERE date = :date")
    suspend fun getTotalDurationForDate(date: LocalDate): Int

    @Query("SELECT COALESCE(SUM(duration), 0) FROM activities WHERE date = :date")
    fun getTotalDurationForDateFlow(date: LocalDate): Flow<Int>

    @Query("SELECT MIN(date) FROM activities")
    suspend fun getEarliestDate(): LocalDate?

    @Query("SELECT * FROM activities ORDER BY date DESC, timestamp DESC")
    fun getAllActivities(): Flow<List<ActivityEntry>>

    @Query("SELECT * FROM activities WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC, timestamp ASC")
    fun getActivitiesInRange(startDate: LocalDate, endDate: LocalDate): Flow<List<ActivityEntry>>

    @Query("SELECT * FROM activities ORDER BY date DESC")
    suspend fun getAllActivitiesList(): List<ActivityEntry>

    @Query("DELETE FROM activities WHERE id = :id")
    suspend fun deleteActivity(id: Long)

    @Query("DELETE FROM activities WHERE date = :date")
    suspend fun deleteActivitiesForDate(date: LocalDate)
}

