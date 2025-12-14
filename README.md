# Streak Tracker

A simple Android app to track your daily exercise streak. Log one of three activities (Running, Aerobic, Swimming) each day to maintain your streak.

## Features

- **Daily Activity Logging**: Tap one of three buttons (Running, Aerobic, Swimming) to log your daily activity
- **Streak Tracking**: See your current streak count prominently displayed
- **Calendar View**: Visual history of your activities with color-coded days
- **Daily Reminders**: Notification at 8 PM if you haven't logged an activity yet
- **Offline-First**: All data stored locally on your device

## Screenshots

The app features:
- 🔥 Fire-themed streak counter
- 🏃 Running (green)
- 🏋️ Aerobic (blue)  
- 🏊 Swimming (cyan)

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose with Material 3
- **Local Storage**: Room Database
- **Notifications**: WorkManager
- **Architecture**: MVVM with Repository pattern
- **Minimum SDK**: API 26 (Android 8.0)

## Building the App

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- Android SDK 34

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
- **RECEIVE_BOOT_COMPLETED**: To reschedule reminders after device restart

## Project Structure

```
app/src/main/java/com/streaktracker/
├── MainActivity.kt              # Main entry point
├── StreakTrackerApplication.kt  # Application class
├── data/
│   ├── ActivityType.kt          # Enum for activity types
│   ├── ActivityEntry.kt         # Room entity
│   ├── ActivityDao.kt           # Data access object
│   └── ActivityDatabase.kt      # Room database
├── repository/
│   └── ActivityRepository.kt    # Data repository with streak logic
├── ui/
│   ├── theme/                   # Compose theme files
│   ├── MainScreen.kt            # Main UI screen
│   ├── CalendarView.kt          # Calendar component
│   └── MainViewModel.kt         # ViewModel
└── notification/
    ├── NotificationHelper.kt    # Notification utilities
    └── ReminderWorker.kt        # WorkManager worker
```

## Streak Logic

- A streak is the count of consecutive days with at least one logged activity
- The day resets at midnight local time
- If today has no activity yet, the streak counts from yesterday
- Missing a single day breaks the streak

## License

MIT License

