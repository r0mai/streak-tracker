package com.streaktracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import java.time.LocalDate

@Entity(tableName = "day_status")
@TypeConverters(Converters::class)
data class DayStatus(
    @PrimaryKey
    val date: LocalDate,
    val totalDuration: Int,     // Sum of all activities that day (minutes)
    val dailyGoal: Int,         // Goal that was active for this day (minutes)
    val completed: Boolean,     // Was goal met?
    val finalized: Boolean      // Is this day closed/immutable?
)
