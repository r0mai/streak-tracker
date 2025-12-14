# Streak Tracker - Architecture Summary

## Overview

Streak Tracker is a native Android app for tracking daily exercise streaks. Users log one activity per day (Running, Aerobic, or Swimming) to maintain their streak. The app is designed for a single user with local-only data storage.

## Tech Stack

| Layer | Technology |
|-------|------------|
| Language | Kotlin |
| UI Framework | Jetpack Compose with Material 3 |
| Architecture | MVVM (Model-View-ViewModel) |
| Local Database | Room (SQLite abstraction) |
| Background Tasks | WorkManager |
| Async | Kotlin Coroutines + Flow |
| Min SDK | API 26 (Android 8.0) |

## Project Structure

```
app/src/main/java/com/streaktracker/
├── MainActivity.kt              # Single Activity, hosts Compose UI
├── StreakTrackerApplication.kt  # Application class, initializes DB & notifications
├── data/                        # Data layer
│   ├── ActivityType.kt          # Enum: RUNNING, AEROBIC, SWIMMING
│   ├── ActivityEntry.kt         # Room @Entity + TypeConverters
│   ├── ActivityDao.kt           # Room @Dao interface
│   └── ActivityDatabase.kt      # Room database singleton
├── repository/
│   └── ActivityRepository.kt    # Data operations + streak calculation
├── ui/
│   ├── theme/                   # Compose theme (Color.kt, Type.kt, Theme.kt)
│   ├── MainScreen.kt            # Main UI composable
│   ├── CalendarView.kt          # Calendar component
│   └── MainViewModel.kt         # ViewModel with UI state
└── notification/
    ├── NotificationHelper.kt    # Notification channel & display
    └── ReminderWorker.kt        # WorkManager Worker for daily reminders
```

## Data Layer

### Entity: `ActivityEntry`

```kotlin
@Entity(tableName = "activities")
data class ActivityEntry(
    @PrimaryKey val date: LocalDate,    // One entry per day
    val activityType: ActivityType,      // RUNNING, AEROBIC, or SWIMMING
    val timestamp: Long                  // When logged (epoch millis)
)
```

The `date` is the primary key, ensuring only one activity per day. If a user taps a different activity button, it replaces the existing entry for that day (`OnConflictStrategy.REPLACE`).

### Type Converters

Room cannot store `LocalDate` or enums directly. `Converters` class handles:
- `LocalDate` ↔ `Long` (epoch day)
- `ActivityType` ↔ `String` (enum name)

### DAO: `ActivityDao`

Key queries:
- `getActivityForDate(date)` - Get single day's activity
- `getActivityForDateFlow(date)` - Observable Flow for today's activity
- `getActivitiesInRange(start, end)` - For calendar display
- `getAllActivitiesList()` - For streak calculation (returns all, sorted by date DESC)
- `insertActivity(activity)` - Upsert with REPLACE strategy

### Database: `ActivityDatabase`

Singleton pattern with `@Volatile` + `synchronized` for thread-safe lazy initialization.

## Repository Layer

### `ActivityRepository`

Abstracts data access and contains business logic:

```kotlin
class ActivityRepository(private val activityDao: ActivityDao) {
    fun getTodayActivity(): Flow<ActivityEntry?>
    suspend fun logActivity(activityType: ActivityType)
    suspend fun calculateStreak(): Int
    suspend fun isStreakAtRisk(): Boolean
    fun getActivitiesForMonth(year: Int, month: Int): Flow<List<ActivityEntry>>
}
```

### Streak Calculation Algorithm

```kotlin
suspend fun calculateStreak(): Int {
    val activities = activityDao.getAllActivitiesList()
    val activityDates = activities.map { it.date }.toSet()
    val today = LocalDate.now()
    
    var streak = 0
    var currentDate = today
    
    // If today has activity, count it and start checking from yesterday
    if (activityDates.contains(today)) {
        streak = 1
        currentDate = today.minusDays(1)
    } else {
        // No activity today, start from yesterday
        currentDate = today.minusDays(1)
    }
    
    // Count consecutive days backwards
    while (activityDates.contains(currentDate)) {
        streak++
        currentDate = currentDate.minusDays(1)
    }
    
    return streak
}
```

**Key behavior:**
- Streak counts consecutive days with logged activity
- If today has no activity yet, streak still shows yesterday's count
- Missing any day breaks the streak (resets to 0)

## UI Layer

### State Management

```kotlin
data class MainUiState(
    val currentStreak: Int = 0,
    val todayActivity: ActivityEntry? = null,
    val currentMonth: YearMonth = YearMonth.now(),
    val monthActivities: Map<LocalDate, ActivityEntry> = emptyMap(),
    val isLoading: Boolean = true
)
```

### ViewModel: `MainViewModel`

- Exposes `StateFlow<MainUiState>` to Compose
- Observes today's activity via `repository.getTodayActivity().collect { ... }`
- Recalculates streak whenever today's activity changes
- Handles month navigation for calendar

### UI Components

1. **`MainScreen`** - Root composable, scrollable column with:
   - `StreakDisplay` - Fire emoji + animated streak count
   - `TodayStatus` - Shows "Today: Running" or "No activity logged"
   - `ActivityButtons` - Three tappable cards (Running/Aerobic/Swimming)
   - `CalendarView` - Month calendar

2. **`CalendarView`** - Shows:
   - Month/year header with navigation arrows
   - Day grid with color-coded activity dots
   - Legend for activity colors

### Theme

- Primary color: Fire Orange (`#FF6B35`)
- Activity colors: Green (Running), Blue (Aerobic), Cyan (Swimming)
- Supports light/dark mode via `isSystemInDarkTheme()`

## Notification System

### Notification Channel

Created in `StreakTrackerApplication.onCreate()`:
- Channel ID: `streak_reminder_channel`
- Importance: Default (sound, no heads-up)

### WorkManager Scheduling

`ReminderWorker` is a `CoroutineWorker` that:
1. Checks if today's activity is logged via `repository.isStreakAtRisk()`
2. Shows notification only if no activity logged
3. Reschedules itself for tomorrow at 8 PM

**Scheduling logic:**
```kotlin
val REMINDER_TIME = LocalTime.of(20, 0)  // 8 PM

fun scheduleReminder(context: Context) {
    val now = LocalDateTime.now()
    var nextReminder = now.toLocalDate().atTime(REMINDER_TIME)
    
    if (now.isAfter(nextReminder)) {
        nextReminder = nextReminder.plusDays(1)
    }
    
    val delay = Duration.between(now, nextReminder)
    // Schedule OneTimeWorkRequest with delay
}
```

Uses `ExistingWorkPolicy.REPLACE` to avoid duplicate workers.

### Permission Handling

Android 13+ requires `POST_NOTIFICATIONS` permission. `MainActivity` requests this on launch using `ActivityResultContracts.RequestPermission()`.

## Application Lifecycle

### Initialization (`StreakTrackerApplication`)

```kotlin
override fun onCreate() {
    super.onCreate()
    NotificationHelper.createNotificationChannel(this)
    ReminderWorker.scheduleReminder(this)
}
```

### Activity Lifecycle (`MainActivity`)

- `onCreate`: Request notification permission, initialize ViewModel, set up Compose UI
- `onResume`: Reschedule reminder (ensures it persists after app restarts)

## Data Flow Diagram

```
┌─────────────────┐      ┌──────────────────┐      ┌─────────────────┐
│   MainActivity  │      │   MainViewModel  │      │ ActivityRepository│
│   (Compose UI)  │◄────►│                  │◄────►│                 │
└─────────────────┘      └──────────────────┘      └─────────────────┘
        │                        │                         │
        │ collectAsState()       │ viewModelScope          │ suspend functions
        │                        │ .launch { }             │ + Flow
        ▼                        ▼                         ▼
┌─────────────────┐      ┌──────────────────┐      ┌─────────────────┐
│  MainUiState    │      │ StateFlow<State> │      │   ActivityDao   │
│  (immutable)    │      │                  │      │   (Room)        │
└─────────────────┘      └──────────────────┘      └─────────────────┘
                                                           │
                                                           ▼
                                                   ┌─────────────────┐
                                                   │  SQLite DB      │
                                                   │  (activities)   │
                                                   └─────────────────┘
```

## Key Patterns

1. **Single Activity Architecture** - One `MainActivity` hosting all Compose UI
2. **Unidirectional Data Flow** - UI → ViewModel → Repository → DB, state flows back up
3. **Repository Pattern** - `ActivityRepository` abstracts Room from ViewModel
4. **Singleton Database** - Thread-safe lazy initialization
5. **Immutable UI State** - `data class MainUiState` with `copy()` for updates

## File Locations Quick Reference

| Need to modify... | File |
|-------------------|------|
| Activity types | `data/ActivityType.kt` |
| Database schema | `data/ActivityEntry.kt` |
| Database queries | `data/ActivityDao.kt` |
| Streak logic | `repository/ActivityRepository.kt` |
| UI state shape | `ui/MainViewModel.kt` |
| Main UI layout | `ui/MainScreen.kt` |
| Calendar UI | `ui/CalendarView.kt` |
| Colors/theme | `ui/theme/Color.kt`, `ui/theme/Theme.kt` |
| Notification time | `notification/ReminderWorker.kt` (REMINDER_TIME) |
| Notification content | `res/values/strings.xml` |
| App permissions | `AndroidManifest.xml` |
| Dependencies | `app/build.gradle.kts` |

