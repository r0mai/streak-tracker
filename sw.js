// Service Worker for Streak Tracker - Notifications Only

// Skip caching - just activate immediately
self.addEventListener('install', () => {
    self.skipWaiting();
});

self.addEventListener('activate', (event) => {
    event.waitUntil(self.clients.claim());
});

// Handle messages from main app
self.addEventListener('message', (event) => {
    if (event.data.type === 'SCHEDULE_NOTIFICATION') {
        scheduleNotificationCheck();
    }
});

// Check if notification should be sent
function scheduleNotificationCheck() {
    // Check every hour if user hasn't logged activity today
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
    
    // Check localStorage via client to see if user logged today
    const clients = await self.clients.matchAll({ type: 'window' });
    
    // If no clients open, check our cached notification date
    const today = new Date().toISOString().split('T')[0];
    const lastNotified = await getFromCache('lastNotifiedDate');
    
    if (lastNotified === today) {
        return;
    }
    
    // Try to get last activity date from a client
    let lastActivityDate = null;
    for (const client of clients) {
        try {
            // Request the last activity date from the client
            client.postMessage({ type: 'GET_LAST_ACTIVITY' });
        } catch (e) {
            // Client might not be responding
        }
    }
    
    // If no clients are open, we should notify (user hasn't opened app)
    if (clients.length === 0) {
        await sendNotification();
        await saveToCache('lastNotifiedDate', today);
    }
}

async function sendNotification() {
    await self.registration.showNotification('Streak Tracker 🔥', {
        body: "Don't forget to log your activity today!",
        icon: 'icons/icon-192.svg',
        badge: 'icons/icon-192.svg',
        tag: 'streak-reminder',
        requireInteraction: true,
        actions: [
            { action: 'open', title: 'Log Activity' }
        ]
    });
}

// Simple cache-based storage for service worker state
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
        self.clients.matchAll({ type: 'window', includeUncontrolled: true })
            .then((clientList) => {
                // If app is already open, focus it
                for (const client of clientList) {
                    if ('focus' in client) {
                        return client.focus();
                    }
                }
                // Otherwise open new window
                if (self.clients.openWindow) {
                    return self.clients.openWindow('./');
                }
            })
    );
});

// Periodic background sync (if supported by browser)
self.addEventListener('periodicsync', (event) => {
    if (event.tag === 'streak-check') {
        event.waitUntil(checkAndNotify());
    }
});
