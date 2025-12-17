package com.streaktracker.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Create new activities table with auto-generated ID and duration
        db.execSQL("""
            CREATE TABLE activities_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                date INTEGER NOT NULL,
                activityType TEXT NOT NULL,
                duration INTEGER NOT NULL DEFAULT 30,
                timestamp INTEGER NOT NULL
            )
        """.trimIndent())

        // 2. Copy existing data (assign 30 min default duration)
        db.execSQL("""
            INSERT INTO activities_new (date, activityType, duration, timestamp)
            SELECT date, activityType, 30, timestamp FROM activities
        """.trimIndent())

        // 3. Drop old table, rename new
        db.execSQL("DROP TABLE activities")
        db.execSQL("ALTER TABLE activities_new RENAME TO activities")

        // 4. Create day_status table
        db.execSQL("""
            CREATE TABLE day_status (
                date INTEGER PRIMARY KEY NOT NULL,
                totalDuration INTEGER NOT NULL,
                dailyGoal INTEGER NOT NULL,
                completed INTEGER NOT NULL,
                finalized INTEGER NOT NULL
            )
        """.trimIndent())

        // 5. Populate day_status from existing activities
        // Each migrated entry has 30 min duration, goal is 30 min, so all are complete
        db.execSQL("""
            INSERT INTO day_status (date, totalDuration, dailyGoal, completed, finalized)
            SELECT date, SUM(duration), 30, 1, 1 FROM activities GROUP BY date
        """.trimIndent())
    }
}
