package com.streaktracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface ActivityDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: ActivityEntry)

    @Query("SELECT * FROM activities WHERE date = :date LIMIT 1")
    suspend fun getActivityForDate(date: LocalDate): ActivityEntry?

    @Query("SELECT * FROM activities WHERE date = :date LIMIT 1")
    fun getActivityForDateFlow(date: LocalDate): Flow<ActivityEntry?>

    @Query("SELECT * FROM activities ORDER BY date DESC")
    fun getAllActivities(): Flow<List<ActivityEntry>>

    @Query("SELECT * FROM activities WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    fun getActivitiesInRange(startDate: LocalDate, endDate: LocalDate): Flow<List<ActivityEntry>>

    @Query("SELECT * FROM activities ORDER BY date DESC")
    suspend fun getAllActivitiesList(): List<ActivityEntry>

    @Query("DELETE FROM activities WHERE date = :date")
    suspend fun deleteActivityForDate(date: LocalDate)
}

