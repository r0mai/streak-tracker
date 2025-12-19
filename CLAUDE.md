# Streak Tracker - Claude Code Context

## Project Overview

Streak Tracker is a native Android app for tracking daily exercise streaks. Users log activities with durations (Walking, Workout, Swimming) throughout the day to meet a configurable daily goal and maintain their streak. Single-user, offline-first design with local SQLite storage.

## Documentation

- **[README.md](README.md)** - User-facing documentation: features, build instructions, installation
- **[ARCHITECTURE.md](ARCHITECTURE.md)** - Technical deep-dive: data layer, UI patterns, notification system, data flow diagrams

## Tech Stack

- **Language:** Kotlin 2.0
- **UI:** Jetpack Compose + Material 3
- **Architecture:** MVVM with Repository pattern
- **Database:** Room (SQLite)
- **Settings:** DataStore Preferences
- **Notifications:** AlarmManager + BroadcastReceiver
- **Async:** Kotlin Coroutines + Flow
- **Min SDK:** API 26 (Android 8.0)

## Quick Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK (requires signing config)
./gradlew assembleRelease

# Run tests
./gradlew test

# Clean build
./gradlew clean
```

## Project Structure

```
app/src/main/java/com/streaktracker/
├── MainActivity.kt              # Entry point, hosts Compose UI
├── StreakTrackerApplication.kt  # App singleton, owns DB & Repository
├── data/                        # Room entities, DAOs, database, settings
├── repository/                  # Business logic, streak calculation
├── ui/                          # Compose screens, ViewModel, theme
└── notification/                # AlarmManager-based reminder system
```

## Task Tracking

Development tasks are tracked in the `tasks/` folder. Each `.md` file represents a separate feature, refactoring, or development task with detailed planning and progress tracking.

**Note:** Task files should be loaded individually as needed, not all at once.

## Key Files Reference

| To modify... | Edit |
|--------------|------|
| Activity types | `data/ActivityType.kt` |
| Database schema (activities) | `data/ActivityEntry.kt` |
| Database schema (day status) | `data/DayStatus.kt` |
| User settings | `data/SettingsDataStore.kt` |
| Streak logic | `repository/ActivityRepository.kt` |
| Main UI | `ui/MainScreen.kt` |
| Calendar | `ui/CalendarView.kt` |
| Notifications | `notification/ReminderScheduler.kt` |
| Theme/colors | `ui/theme/Color.kt`, `ui/theme/Theme.kt` |

