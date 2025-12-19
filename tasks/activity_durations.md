# Task: Activity Durations

Extend activities with duration tracking and introduce a daily goal system.

## Status: Complete

## Feature Summary

Replace the current "one activity per day" model with a duration-based goal system where users log multiple activity sessions throughout the day, and the day is marked complete when the cumulative duration meets the daily goal.

---

## Decisions Made

| Question | Decision |
|----------|----------|
| Initial duration in panel | Start at 0, OK button disabled until > 0 |
| Reset/decrement | Single "Clear" button to reset to 0 (no decrement) |
| Existing data migration | Assign 30 min default to all existing entries |
| Progress bar overflow | Show actual value (e.g., "45 of 30 mins") with full bar |
| Goal changes & history | DayStatus table with finalized flag - past is immutable |
| Calendar view | Distinguish: no activity / partial / complete |
| Quick-log shortcut | No - prevents accidental entries |
| Animations | Use default Android/Compose animations if easy, no custom work |
| Settings UI | Simple slide-in panel for now |

---

## Architecture

### Schema Design

**ActivityEntry** (updated)
```kotlin
@Entity(tableName = "activities")
data class ActivityEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: LocalDate,
    val activityType: ActivityType,
    val duration: Int,              // Duration in minutes
    val timestamp: Long = System.currentTimeMillis()
)
```

**DayStatus** (new table)
```kotlin
@Entity(tableName = "day_status")
data class DayStatus(
    @PrimaryKey
    val date: LocalDate,
    val totalDuration: Int,         // Sum of all activities that day
    val dailyGoal: Int,             // Goal that was active for this day
    val completed: Boolean,         // Was goal met?
    val finalized: Boolean          // Is this day closed/immutable?
)
```

**Settings** (DataStore)
```kotlin
data class UserSettings(
    val dailyGoalMinutes: Int = 30  // Default 30 min
)
```

### Day Finalization Logic

**When to finalize:**
- Midnight timer fires → finalize yesterday
- App resumes on new day → finalize all past non-finalized days
- First activity logged on new day → finalize all past non-finalized days

**Finalization process:**
```kotlin
suspend fun finalizeDay(date: LocalDate) {
    val existing = dayStatusDao.getStatus(date)
    if (existing?.finalized == true) return  // Already done

    val totalDuration = activityDao.getTotalDurationForDate(date) ?: 0
    val goal = existing?.dailyGoal ?: settings.dailyGoalMinutes

    val status = DayStatus(
        date = date,
        totalDuration = totalDuration,
        dailyGoal = goal,
        completed = totalDuration >= goal,
        finalized = true
    )
    dayStatusDao.upsert(status)
}

suspend fun finalizePastDays() {
    val today = LocalDate.now()
    val unfinalized = dayStatusDao.getUnfinalizedBefore(today)

    // Also fill gaps: find earliest activity date, ensure all days since then exist
    val earliestActivity = activityDao.getEarliestDate()
    if (earliestActivity != null) {
        var date = earliestActivity
        while (date < today) {
            finalizeDay(date)
            date = date.plusDays(1)
        }
    }
}
```

### Streak Calculation

```kotlin
suspend fun calculateStreak(): Int {
    val today = LocalDate.now()

    // Today: live calculation (not finalized yet)
    val todayTotal = activityDao.getTotalDurationForDate(today) ?: 0
    val currentGoal = settings.dailyGoalMinutes
    val todayComplete = todayTotal >= currentGoal

    var streak = if (todayComplete) 1 else 0
    var checkDate = today.minusDays(1)

    // Past days: read from DayStatus (all should be finalized)
    while (true) {
        val dayStatus = dayStatusDao.getStatus(checkDate)
        if (dayStatus == null || !dayStatus.completed) break
        streak++
        checkDate = checkDate.minusDays(1)
    }

    return streak
}
```

### Today's Progress (Live)

```kotlin
suspend fun getTodayProgress(): Pair<Int, Int> {  // (current, goal)
    val todayTotal = activityDao.getTotalDurationForDate(LocalDate.now()) ?: 0
    val goal = settings.dailyGoalMinutes
    return Pair(todayTotal, goal)
}
```

---

## Room Migration (v1 → v2)

```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 1. Create new activities table with auto-generated ID
        database.execSQL("""
            CREATE TABLE activities_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                date INTEGER NOT NULL,
                activityType TEXT NOT NULL,
                duration INTEGER NOT NULL DEFAULT 30,
                timestamp INTEGER NOT NULL
            )
        """)

        // 2. Copy existing data (assign 30 min default duration)
        database.execSQL("""
            INSERT INTO activities_new (date, activityType, duration, timestamp)
            SELECT date, activityType, 30, timestamp FROM activities
        """)

        // 3. Drop old table, rename new
        database.execSQL("DROP TABLE activities")
        database.execSQL("ALTER TABLE activities_new RENAME TO activities")

        // 4. Create day_status table
        database.execSQL("""
            CREATE TABLE day_status (
                date INTEGER PRIMARY KEY NOT NULL,
                totalDuration INTEGER NOT NULL,
                dailyGoal INTEGER NOT NULL,
                completed INTEGER NOT NULL,
                finalized INTEGER NOT NULL
            )
        """)

        // 5. Populate day_status from existing activities (all complete with 30/30)
        database.execSQL("""
            INSERT INTO day_status (date, totalDuration, dailyGoal, completed, finalized)
            SELECT date, 30, 30, 1, 1 FROM activities GROUP BY date
        """)
    }
}
```

---

## UI Components

### Main Screen Changes

```
┌─────────────────────────────────────────┐
│  [Settings ⚙️]                          │
│                                         │
│         🔥 12 day streak 🔥             │
│                                         │
│  ┌───────────────────────────────────┐  │
│  │ ████████████░░░░░░  20 of 30 min  │  │  ← Progress bar
│  └───────────────────────────────────┘  │
│                                         │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐   │
│  │ 🏃 Run  │ │ 🏋️ Aero │ │ 🏊 Swim │   │  ← Activity buttons
│  └─────────┘ └─────────┘ └─────────┘   │
│                                         │
│  ┌───────────────────────────────────┐  │
│  │         Calendar View             │  │
│  └───────────────────────────────────┘  │
└─────────────────────────────────────────┘
```

### Activity Input Panel (replaces buttons when active)

```
┌─────────────────────────────────────────┐
│                                         │
│         + 15 min Swimming 🏊            │  ← Label (updates live)
│                                         │
│   ┌───────┐  ┌────────┐  ┌────────┐    │
│   │ +5min │  │ +15min │  │  +1h   │    │  ← Increment buttons
│   └───────┘  └────────┘  └────────┘    │
│                                         │
│   ┌────────┐ ┌─────────┐ ┌──────────┐  │
│   │ Cancel │ │  Clear  │ │    OK    │  │  ← OK disabled if duration=0
│   └────────┘ └─────────┘ └──────────┘  │
└─────────────────────────────────────────┘
```

### Settings Panel (slide-in)

```
┌─────────────────────────────────────────┐
│  Daily Goal                             │
│                                         │
│  ○ 15 min                               │
│  ● 30 min  ← selected                   │
│  ○ 60 min                               │
│  ○ 90 min                               │
│  ○ 120 min                              │
│                                         │
│              ┌─────────┐                │
│              │  Done   │                │
│              └─────────┘                │
└─────────────────────────────────────────┘
```

### Calendar View Updates

| State | Visual |
|-------|--------|
| No activity | Empty / no indicator |
| Partial (0 < total < goal) | Half-filled dot or different color |
| Complete (total >= goal) | Filled dot (current behavior) |

---

## Implementation Checklist

### Phase 1: Data Layer
- [x] Update `ActivityEntry`: add `id` (auto-gen PK), add `duration` field
- [x] Create `DayStatus` entity
- [x] Create `DayStatusDao` with queries: getStatus, getUnfinalizedBefore, upsert
- [x] Update `ActivityDao`: getTotalDurationForDate, getEarliestDate, remove date-as-PK constraint
- [x] Add type converters for new fields if needed
- [x] Write Room migration (v1 → v2)
- [x] Update `ActivityDatabase` version and add migration

### Phase 2: Settings
- [x] Add DataStore dependency
- [x] Create `SettingsDataStore` class
- [x] Expose `dailyGoalMinutes` as Flow
- [x] Update `StreakTrackerApplication` to provide settings

### Phase 3: Repository Layer
- [x] Update `ActivityRepository`:
  - [x] `logActivity(type, duration)` - new signature
  - [x] `getTodayProgress(): Flow<Pair<Int, Int>>`
  - [x] `calculateStreak()` - new logic using DayStatus
  - [x] `isStreakAtRisk()` - check if today's total < goal
- [x] Create day finalization logic (in repository)
- [x] Call `finalizePastDays()` on app start and day change

### Phase 4: ViewModel Updates
- [x] Update `MainUiState`:
  - [x] Add `todayProgress: Int`
  - [x] Add `dailyGoal: Int`
  - [x] Add `selectedActivityType: ActivityType?` (null = buttons visible, non-null = panel visible)
  - [x] Add `pendingDuration: Int`
  - [x] Add `showSettings: Boolean`
- [x] Add actions: selectActivity, addDuration, clearDuration, confirmActivity, cancelInput
- [x] Add settings actions: openSettings, closeSettings, setDailyGoal
- [x] Update day-change handling to trigger finalization

### Phase 5: UI - Main Screen
- [x] Add progress bar composable
- [x] Add settings button (cogwheel icon)
- [x] Conditionally show activity buttons OR input panel based on state

### Phase 6: UI - Activity Input Panel
- [x] Create `ActivityInputPanel` composable
- [x] Duration label with activity icon
- [x] Increment buttons (+5m, +15m, +1h)
- [x] Clear button
- [x] OK button (disabled when duration = 0)
- [x] Cancel button

### Phase 7: UI - Settings Panel
- [x] Create `SettingsPanel` composable (slide-in from right)
- [x] Radio buttons for goal options (15, 30, 60, 90, 120)
- [x] Done button to close

### Phase 8: UI - Calendar Updates
- [x] Update calendar day rendering to show three states
- [x] Update legend (shows Complete/Partial indicators)

### Phase 9: Notifications
- [x] Update `ReminderWorker` to check today's total vs goal
- [ ] Update notification text if needed (optional)

### Phase 10: Basic testing
- [x] Test migration with existing data
- [x] Test goal changes don't affect past days
- [x] Manual testing of full flow

### Phase 11: Test edge cases (optional)
- [ ] Test streak calculation edge cases
- [ ] Test day finalization after multi-day gap

---

## Notes

- DataStore preferred over SharedPreferences for async + Flow support
- `LocalDate` is timezone-agnostic; date determined by device timezone at creation time
- No gaps in DayStatus after earliest activity - simplifies streak calculation
- Today's DayStatus exists but with `finalized = false` until day ends
