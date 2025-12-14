package com.streaktracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import java.time.LocalDate

@Entity(tableName = "activities")
@TypeConverters(Converters::class)
data class ActivityEntry(
    @PrimaryKey
    val date: LocalDate,
    val activityType: ActivityType,
    val timestamp: Long = System.currentTimeMillis()
)

class Converters {
    @TypeConverter
    fun fromLocalDate(date: LocalDate): Long {
        return date.toEpochDay()
    }

    @TypeConverter
    fun toLocalDate(epochDay: Long): LocalDate {
        return LocalDate.ofEpochDay(epochDay)
    }

    @TypeConverter
    fun fromActivityType(type: ActivityType): String {
        return type.name
    }

    @TypeConverter
    fun toActivityType(name: String): ActivityType {
        return ActivityType.valueOf(name)
    }
}

