# Streak Tracker - Architecture Summary

## Overview

Streak Tracker is a native Android app for tracking daily exercise streaks. Users log activities with durations throughout the day to meet a configurable daily goal. Consecutive days of meeting the goal build a streak. The app is designed for a single user with local-only data storage.

## Tech Stack

| Layer | Technology |
|-------|------------|
| Language | Kotlin 2.0 |
| UI Framework | Jetpack Compose with Material 3 |
| Architecture | MVVM (Model-View-ViewModel) |
| Local Database | Room (SQLite abstraction) |
| Settings Storage | DataStore Preferences |
| Notifications | AlarmManager + BroadcastReceiver |
| Async | Kotlin Coroutines + Flow |
| Min SDK | API 26 (Android 8.0) |

## Project Structure

```
app/src/main/java/com/streaktracker/
├── MainActivity.kt              # Single Activity, hosts Compose UI
├── StreakTrackerApplication.kt  # Application class, initializes DB & notifications
├── data/                        # Data layer
│   ├── ActivityType.kt          # Enum: RUNNING, AEROBIC, SWIMMING
│   ├── ActivityEntry.kt         # Room @Entity for activity logs
│   ├── ActivityDao.kt           # Room @Dao for activities
│   ├── DayStatus.kt             # Room @Entity for daily goal status
│   ├── DayStatusDao.kt          # Room @Dao for day status
│   ├── ActivityDatabase.kt      # Room database singleton
│   ├── Migrations.kt            # Database migration definitions
│   └── SettingsDataStore.kt     # DataStore for user preferences
├── repository/
│   └── ActivityRepository.kt    # Data operations + streak calculation
├── ui/
│   ├── theme/                   # Compose theme (Color.kt, Type.kt, Theme.kt)
│   ├── MainScreen.kt            # Main UI composable with settings
│   ├── CalendarView.kt          # Calendar component with day popups
│   └── MainViewModel.kt         # ViewModel with UI state
└── notification/
    ├── NotificationHelper.kt    # Notification channel & display
    ├── ReminderScheduler.kt     # AlarmManager scheduling logic
    ├── AlarmReceiver.kt         # BroadcastReceiver for alarm events
    └── BootReceiver.kt          # Reschedules alarms after device boot
```

## Data Layer

### Entity: `ActivityEntry`

Defined in `data/ActivityEntry.kt`. Multiple activities can be logged per day, each with a duration in minutes. The `id` is auto-generated, allowing multiple entries for the same date. Also contains the `Converters` class for Room type conversion.

### Entity: `DayStatus`

Defined in `data/DayStatus.kt`. Tracks whether each day's goal was met, storing total duration, the goal that was active, and completion status. Past days are "finalized" - their status is locked. Today's status is recalculated as activities are added.

### Type Converters

Room cannot store `LocalDate` or enums directly. The `Converters` class (in `ActivityEntry.kt`) handles:
- `LocalDate` ↔ `Long` (epoch day)
- `ActivityType` ↔ `String` (enum name)

### DAO: `ActivityDao`

Defined in `data/ActivityDao.kt`. Key queries:
- `getActivitiesForDate(date)` - Get all activities for a specific day
- `getActivitiesForDateFlow(date)` - Observable Flow for today's activities
- `getTotalDurationForDate(date)` - Sum of durations for a day
- `getActivitiesInRange(start, end)` - For calendar display
- `insertActivity(activity)` - Insert new activity entry
- `deleteActivity(id)` - Delete specific activity

### DAO: `DayStatusDao`

Defined in `data/DayStatusDao.kt`. Key queries:
- `getStatus(date)` - Get day status for specific date
- `getStatusFlow(date)` - Observable Flow for day status
- `getStatusesInRange(start, end)` - For calendar display
- `upsert(status)` - Insert or replace day status (REPLACE strategy)
- `getUnfinalizedBefore(date)` - Find days needing finalization

### Database: `ActivityDatabase`

Defined in `data/ActivityDatabase.kt`. Contains two entities (`ActivityEntry`, `DayStatus`) at version 2. Uses singleton pattern with `@Volatile` + `synchronized` for thread-safe lazy initialization. Migrations are defined in `data/Migrations.kt`.

### Settings: `SettingsDataStore`

Defined in `data/SettingsDataStore.kt`. Preferences stored via DataStore:
- `dailyGoalMinutes` - User's daily goal (default: 30 minutes)
- `reminderHour` / `reminderMinute` - Reminder time (default: 20:00 / 8 PM)

Available goal options: 15, 30, 60, 90, 120 minutes.

## Repository Layer

### `ActivityRepository`

Defined in `repository/ActivityRepository.kt`. Abstracts data access and contains business logic. Takes three dependencies: `ActivityDao`, `DayStatusDao`, and `SettingsDataStore`.

Key operations:
- **Activity logging** - `logActivity()` inserts an activity and updates today's `DayStatus`
- **Today's progress** - `getTodayProgress()` returns a Flow of (current minutes, goal minutes)
- **Streak calculation** - `calculateStreak()` counts consecutive completed days
- **Day finalization** - `finalizePastDays()` locks in past days' completion status
- **Settings** - get/set daily goal and reminder time

### Streak Calculation

The `calculateStreak()` method first ensures past days are finalized, then:
1. Checks if today's total duration meets the goal (live calculation)
2. If yes, starts streak at 1; if no, starts at 0
3. Walks backwards through `DayStatus` records, counting consecutive completed days
4. Stops when it finds an incomplete or missing day

**Key behavior:**
- Streak counts consecutive days where daily goal was met
- Today's progress is calculated live from activities
- Past days use the finalized `DayStatus` table
- Missing the goal on any day breaks the streak

### Day Finalization

Past days are "finalized" to lock in their goal completion status:
- When the app opens or at midnight, `finalizePastDays()` runs
- Each unfinalized past day gets its total duration summed
- The day's goal (at that time) determines if it was "completed"
- Finalized days are immutable - changing the goal doesn't affect them

## UI Layer

### State Management

`MainUiState` (defined in `ui/MainViewModel.kt`) is an immutable data class containing:
- Current streak count and today's progress
- Today's activity list and daily goal
- Current month and its activities/day statuses for calendar
- Activity input panel state (selected type, pending duration)
- Settings panel visibility
- Calendar day selection
- Reminder time and language settings

### ViewModel: `MainViewModel`

Defined in `ui/MainViewModel.kt`. Key responsibilities:
- Exposes `StateFlow<MainUiState>` to Compose
- Observes today's progress and recalculates streak when it changes
- Handles month navigation for calendar
- Manages activity input panel state
- Manages settings panel state
- Auto-refreshes at midnight via `startMidnightTimer()`

### UI Components

1. **`MainScreen`** (`ui/MainScreen.kt`) - Root composable, scrollable column with:
   - `StreakDisplay` - Fire emoji + animated streak count + motivational message
   - `TodayProgress` - Progress bar showing current vs goal minutes
   - `ActivityButtons` - Three tappable cards (Walking/Workout/Swimming)
   - `DurationInputPanel` - Slide-up panel for adding duration (+5, +15, +60 min)
   - `CalendarView` - Month calendar
   - `SettingsPanel` - Daily goal, reminder time, language selection

2. **`CalendarView`** (`ui/CalendarView.kt`) - Shows:
   - Month/year header with navigation arrows
   - Day grid with Monday start
   - Color-coded indicators: green (complete), yellow (partial), empty (none)
   - Tap day to show activity summary popup

### Theme

Defined in `ui/theme/`. Primary color: Fire Orange (`#FF6B35`). Activity colors: Green (Walking), Blue (Workout), Cyan (Swimming). Supports light/dark mode via `isSystemInDarkTheme()`. Navigation bar matches theme.

## Notification System

### Notification Channel

Created in `StreakTrackerApplication.onCreate()` via `NotificationHelper.createNotificationChannel()`:
- Channel ID: `streak_reminder_channel`
- Importance: Default (sound, no heads-up)

### AlarmManager Scheduling

`ReminderScheduler` (`notification/ReminderScheduler.kt`) is a singleton object that handles alarm scheduling:
- `scheduleReminder()` - Schedule using saved time from settings
- `scheduleReminderAt()` - Schedule at specific hour/minute
- `scheduleNextReminder()` - Schedule for tomorrow
- `cancelReminder()` - Cancel pending alarm

**Scheduling logic:**
- Uses `AlarmManager.setExactAndAllowWhileIdle()` for precise timing
- Falls back to inexact alarm if exact alarm permission not granted (Android 12+)
- Reminder time is user-configurable via settings (stored in DataStore)
- Each alarm schedules the next day's alarm when fired

### Alarm Receiver

`AlarmReceiver` (`notification/AlarmReceiver.kt`) is a `BroadcastReceiver` that:
1. Checks if daily goal is not yet met via `repository.isStreakAtRisk()`
2. Shows notification only if goal not met
3. Schedules next reminder for tomorrow

### Boot Receiver

`BootReceiver` (`notification/BootReceiver.kt`) reschedules the reminder when the device boots, ensuring alarms persist across reboots.

### Permission Handling

- Android 13+ requires `POST_NOTIFICATIONS` permission - requested on launch
- Android 12+ may require `SCHEDULE_EXACT_ALARM` - app handles fallback gracefully

## Application Lifecycle

### Initialization

`StreakTrackerApplication.onCreate()` creates the notification channel and schedules the initial reminder.

### Activity Lifecycle

`MainActivity`:
- `onCreate`: Request notification permission, initialize ViewModel, set up Compose UI
- `onResume`: Reschedule reminder, refresh for day change (handles app sleeping past midnight)

## Data Flow Diagram

```
┌─────────────────┐      ┌──────────────────┐      ┌─────────────────────┐
│   MainActivity  │      │   MainViewModel  │      │  ActivityRepository │
│   (Compose UI)  │◄────►│                  │◄────►│                     │
└─────────────────┘      └──────────────────┘      └─────────────────────┘
        │                        │                          │
        │ collectAsState()       │ viewModelScope           │ suspend functions
        │                        │ .launch { }              │ + Flow
        ▼                        ▼                          ▼
┌─────────────────┐      ┌──────────────────┐      ┌─────────────────────┐
│  MainUiState    │      │ StateFlow<State> │      │ ActivityDao         │
│  (immutable)    │      │                  │      │ DayStatusDao        │
└─────────────────┘      └──────────────────┘      │ SettingsDataStore   │
                                                   └─────────────────────┘
                                                            │
                                                            ▼
                                                   ┌─────────────────────┐
                                                   │  SQLite DB          │
                                                   │  (activities,       │
                                                   │   day_status)       │
                                                   └─────────────────────┘
```

## Key Patterns

1. **Single Activity Architecture** - One `MainActivity` hosting all Compose UI
2. **Unidirectional Data Flow** - UI → ViewModel → Repository → DB, state flows back up
3. **Repository Pattern** - `ActivityRepository` abstracts Room and DataStore from ViewModel
4. **Singleton Database** - Thread-safe lazy initialization
5. **Immutable UI State** - `MainUiState` data class with `copy()` for updates
6. **Day Finalization** - Past days are locked to prevent goal changes affecting history

## File Locations Quick Reference

| Need to modify... | File |
|-------------------|------|
| Activity types | `data/ActivityType.kt` |
| Activity schema | `data/ActivityEntry.kt` |
| Day status schema | `data/DayStatus.kt` |
| Database queries (activities) | `data/ActivityDao.kt` |
| Database queries (day status) | `data/DayStatusDao.kt` |
| Database migrations | `data/Migrations.kt` |
| User settings | `data/SettingsDataStore.kt` |
| Streak logic | `repository/ActivityRepository.kt` |
| UI state shape | `ui/MainViewModel.kt` |
| Main UI layout | `ui/MainScreen.kt` |
| Calendar UI | `ui/CalendarView.kt` |
| Colors/theme | `ui/theme/Color.kt`, `ui/theme/Theme.kt` |
| Reminder scheduling | `notification/ReminderScheduler.kt` |
| Notification content | `res/values/strings.xml` |
| App permissions | `AndroidManifest.xml` |
| Dependencies | `app/build.gradle.kts` |
