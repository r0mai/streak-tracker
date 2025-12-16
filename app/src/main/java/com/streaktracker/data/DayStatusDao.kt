package com.streaktracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface DayStatusDao {
    @Query("SELECT * FROM day_status WHERE date = :date LIMIT 1")
    suspend fun getStatus(date: LocalDate): DayStatus?

    @Query("SELECT * FROM day_status WHERE date = :date LIMIT 1")
    fun getStatusFlow(date: LocalDate): Flow<DayStatus?>

    @Query("SELECT * FROM day_status WHERE finalized = 0 AND date < :date ORDER BY date ASC")
    suspend fun getUnfinalizedBefore(date: LocalDate): List<DayStatus>

    @Query("SELECT * FROM day_status WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    fun getStatusesInRange(startDate: LocalDate, endDate: LocalDate): Flow<List<DayStatus>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(status: DayStatus)

    @Query("SELECT MIN(date) FROM day_status")
    suspend fun getEarliestDate(): LocalDate?
}
