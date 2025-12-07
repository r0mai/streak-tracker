# 🔥 Streak Tracker

A simple PWA to track your daily fitness activity streaks. Log Running, Aerobic, or Swimming activities and keep your streak alive!

## Features

- **Three activity types**: Running 🏃, Aerobic 💪, Swimming 🏊
- **Streak tracking**: Your streak continues as long as you log one activity per day
- **Push notifications**: Get daily reminders to not break your streak
- **Offline support**: Works without internet connection
- **Installable**: Add to home screen on Android/iOS for native app experience
- **No account needed**: All data stored locally on your device

## Deployment to GitHub Pages

### Automatic Deployment

1. Push this repository to GitHub
2. Go to your repository **Settings** → **Pages**
3. Under "Build and deployment", select **GitHub Actions** as the source
4. Push any commit to the `main` branch to trigger deployment

### Manual Setup

If the workflow doesn't run automatically:

1. Go to the **Actions** tab in your repository
2. Click on "Deploy to GitHub Pages" workflow
3. Click "Run workflow"

### Access Your App

After deployment, your app will be available at:
```
https://<your-username>.github.io/<repository-name>/
```

## Installing as PWA on Android

1. Open the deployed URL in Chrome
2. Tap the menu (three dots)
3. Select "Add to Home Screen" or "Install App"
4. The app will now appear on your home screen

## Local Development

Simply open `index.html` in a browser. For full PWA features (service worker, notifications), you'll need to serve it via HTTP:

```bash
# Using Python
python -m http.server 8000

# Using Node.js
npx serve .
```

Then open `http://localhost:8000`

## How It Works

- **Streak Logic**: Your streak increases when you log an activity on consecutive days
- **Streak Reset**: If you miss a day (no activity logged), your streak resets to 0
- **One per day**: You can only log one activity per day
- **History**: View your last 10 logged activities
- **Notifications**: When enabled, reminds you between 6-10 PM if you haven't logged

## Tech Stack

- Vanilla HTML/CSS/JavaScript
- localStorage for data persistence
- Service Worker for offline support & notifications
- PWA manifest for installability

## Privacy

All data is stored locally in your browser's localStorage. No data is sent to any server.

