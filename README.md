# Streak Tracker

A simple Android app to track your daily exercise streak. Log activities with durations throughout the day to meet your daily goal and maintain your streak.

## Features

- **Activity Logging with Durations**: Log Walking, Workout, or Swimming activities with customizable durations (+5 min, +15 min, +1 h increments)
- **Daily Goal Tracking**: Set a daily goal (15, 30, 60, 90, or 120 minutes) and track progress throughout the day
- **Streak Tracking**: See your current streak count - consecutive days of meeting your daily goal
- **Calendar View**: Visual history with color-coded days showing complete (goal met) vs partial (some activity) progress
- **Tap Day for Details**: Tap any calendar day to see an activity summary popup
- **Configurable Reminders**: Set your preferred reminder time (default 8 PM) - notification only shows if daily goal not yet met
- **Multi-Language Support**: English, German, and Hungarian translations with per-app language selector
- **Offline-First**: All data stored locally on your device

## Screenshots

The app features:
- 🔥 Fire-themed streak counter
- 🏃 Walking (green)
- 🏋️ Workout (blue)
- 🏊 Swimming (cyan)
- Calendar with complete/partial day indicators
- Notifications with motivational messages

## Tech Stack

- **Language**: Kotlin 2.0
- **UI**: Jetpack Compose with Material 3
- **Local Storage**: Room Database (SQLite)
- **Settings**: DataStore Preferences
- **Notifications**: AlarmManager with BroadcastReceiver
- **Architecture**: MVVM with Repository pattern
- **Minimum SDK**: API 26 (Android 8.0)

## Building the App

### Prerequisites

- Android Studio Ladybug (2024.2.1) or later
- JDK 17 or 21
- Android SDK 34
- Gradle 8.14

### Build Debug APK

```bash
./gradlew assembleDebug
```

The APK will be at `app/build/outputs/apk/debug/app-debug.apk`

### Build Release APK

For a signed release APK, first create a signing key:

```bash
keytool -genkey -v -keystore streak-tracker.keystore -alias streak-tracker -keyalg RSA -keysize 2048 -validity 10000
```

Then add to `local.properties`:

```properties
RELEASE_STORE_FILE=streak-tracker.keystore
RELEASE_STORE_PASSWORD=your_password
RELEASE_KEY_ALIAS=streak-tracker
RELEASE_KEY_PASSWORD=your_password
```

Build the release:

```bash
./gradlew assembleRelease
```

## Installation

1. Build the APK (debug or release)
2. Transfer to your Android device
3. Enable "Install from unknown sources" in settings
4. Open the APK to install

## Permissions

- **POST_NOTIFICATIONS**: For daily reminder notifications (Android 13+)
- **SCHEDULE_EXACT_ALARM**: For precise reminder timing (Android 12+)
- **RECEIVE_BOOT_COMPLETED**: To reschedule reminders after device restart

## Project Structure

```
app/src/main/java/com/streaktracker/
├── MainActivity.kt              # Main entry point
├── StreakTrackerApplication.kt  # Application class
├── data/
│   ├── ActivityType.kt          # Enum for activity types
│   ├── ActivityEntry.kt         # Room entity for activities
│   ├── ActivityDao.kt           # Data access object for activities
│   ├── DayStatus.kt             # Room entity for daily goal tracking
│   ├── DayStatusDao.kt          # Data access object for day status
│   ├── ActivityDatabase.kt      # Room database
│   ├── Migrations.kt            # Database migrations
│   └── SettingsDataStore.kt     # DataStore for user preferences
├── repository/
│   └── ActivityRepository.kt    # Data repository with streak logic
├── ui/
│   ├── theme/                   # Compose theme files
│   ├── MainScreen.kt            # Main UI screen with settings
│   ├── CalendarView.kt          # Calendar component with day popups
│   └── MainViewModel.kt         # ViewModel
└── notification/
    ├── NotificationHelper.kt    # Notification channel & display
    ├── ReminderScheduler.kt     # AlarmManager scheduling
    ├── AlarmReceiver.kt         # BroadcastReceiver for alarms
    └── BootReceiver.kt          # Reschedule on device boot
```

## Streak Logic

- A streak is the count of consecutive days where the daily goal was met
- Multiple activities can be logged per day; their durations are summed
- The day resets at midnight local time (app auto-refreshes)
- If today's goal is not yet met, today is not yet included in the streak
- Missing the goal on any day breaks the streak

## License

MIT License

