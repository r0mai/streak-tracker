// Service Worker for Streak Tracker

const CACHE_NAME = 'streak-tracker-v1';
const ASSETS_TO_CACHE = [
    './',
    './index.html',
    './app.js',
    './manifest.json'
];

// Install event - cache assets
self.addEventListener('install', (event) => {
    event.waitUntil(
        caches.open(CACHE_NAME)
            .then((cache) => {
                console.log('Caching app assets');
                return cache.addAll(ASSETS_TO_CACHE);
            })
            .then(() => self.skipWaiting())
    );
});

// Activate event - clean up old caches
self.addEventListener('activate', (event) => {
    event.waitUntil(
        caches.keys()
            .then((cacheNames) => {
                return Promise.all(
                    cacheNames
                        .filter((name) => name !== CACHE_NAME)
                        .map((name) => caches.delete(name))
                );
            })
            .then(() => self.clients.claim())
    );
});

// Fetch event - serve from cache, fallback to network
self.addEventListener('fetch', (event) => {
    event.respondWith(
        caches.match(event.request)
            .then((response) => {
                if (response) {
                    return response;
                }
                return fetch(event.request);
            })
    );
});

// Handle messages from main app
self.addEventListener('message', (event) => {
    if (event.data.type === 'SCHEDULE_NOTIFICATION') {
        scheduleNotificationCheck();
    } else if (event.data.type === 'CANCEL_NOTIFICATION') {
        // Clear any scheduled checks
        // Note: In a real app, you'd want to track and clear specific timers
    }
});

// Check if notification should be sent
function scheduleNotificationCheck() {
    // Check every hour if user hasn't logged activity today
    // In production, you might want to use a background sync or push notification service
    setInterval(() => {
        checkAndNotify();
    }, 60 * 60 * 1000); // Every hour
    
    // Also check immediately
    checkAndNotify();
}

async function checkAndNotify() {
    // Only notify in the evening (after 6 PM local time)
    const hour = new Date().getHours();
    if (hour < 18 || hour > 22) {
        return;
    }
    
    // Check if already notified today
    const today = new Date().toISOString().split('T')[0];
    const lastNotified = await getFromCache('lastNotifiedDate');
    
    if (lastNotified === today) {
        return;
    }
    
    // Check if user has logged activity today
    const lastActivityDate = await getFromCache('lastActivityDate');
    
    if (lastActivityDate !== today) {
        // User hasn't logged today - send notification
        self.registration.showNotification('Streak Tracker 🔥', {
            body: "Don't forget to log your activity today!",
            icon: 'data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><text y=".9em" font-size="90">🔥</text></svg>',
            badge: 'data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><text y=".9em" font-size="90">🔥</text></svg>',
            tag: 'streak-reminder',
            requireInteraction: true,
            actions: [
                { action: 'open', title: 'Log Activity' }
            ]
        });
        
        // Mark as notified
        await saveToCache('lastNotifiedDate', today);
    }
}

// Simple cache-based storage for service worker
async function getFromCache(key) {
    try {
        const cache = await caches.open('streak-data');
        const response = await cache.match(key);
        if (response) {
            return await response.text();
        }
    } catch (e) {
        console.error('Cache read error:', e);
    }
    return null;
}

async function saveToCache(key, value) {
    try {
        const cache = await caches.open('streak-data');
        await cache.put(key, new Response(value));
    } catch (e) {
        console.error('Cache write error:', e);
    }
}

// Handle notification clicks
self.addEventListener('notificationclick', (event) => {
    event.notification.close();
    
    event.waitUntil(
        clients.matchAll({ type: 'window', includeUncontrolled: true })
            .then((clientList) => {
                // If app is already open, focus it
                for (const client of clientList) {
                    if (client.url.includes('streak-tracker') && 'focus' in client) {
                        return client.focus();
                    }
                }
                // Otherwise open new window
                if (clients.openWindow) {
                    return clients.openWindow('./');
                }
            })
    );
});

// Periodic background sync (if supported)
self.addEventListener('periodicsync', (event) => {
    if (event.tag === 'streak-check') {
        event.waitUntil(checkAndNotify());
    }
});

