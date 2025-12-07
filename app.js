// Streak Tracker App

const STORAGE_KEYS = {
    STREAK: 'streak_count',
    LAST_DATE: 'last_activity_date',
    HISTORY: 'activity_history',
    NOTIFICATIONS: 'notifications_enabled'
};

// Get today's date as YYYY-MM-DD string
function getToday() {
    return new Date().toISOString().split('T')[0];
}

// Get yesterday's date as YYYY-MM-DD string
function getYesterday() {
    const yesterday = new Date();
    yesterday.setDate(yesterday.getDate() - 1);
    return yesterday.toISOString().split('T')[0];
}

// Load data from localStorage
function loadData() {
    return {
        streak: parseInt(localStorage.getItem(STORAGE_KEYS.STREAK) || '0', 10),
        lastDate: localStorage.getItem(STORAGE_KEYS.LAST_DATE) || null,
        history: JSON.parse(localStorage.getItem(STORAGE_KEYS.HISTORY) || '[]'),
        notificationsEnabled: localStorage.getItem(STORAGE_KEYS.NOTIFICATIONS) === 'true'
    };
}

// Save data to localStorage
function saveData(data) {
    localStorage.setItem(STORAGE_KEYS.STREAK, data.streak.toString());
    if (data.lastDate) {
        localStorage.setItem(STORAGE_KEYS.LAST_DATE, data.lastDate);
    }
    localStorage.setItem(STORAGE_KEYS.HISTORY, JSON.stringify(data.history));
    localStorage.setItem(STORAGE_KEYS.NOTIFICATIONS, data.notificationsEnabled.toString());
}

// Check and update streak based on dates
function checkStreak(data) {
    const today = getToday();
    const yesterday = getYesterday();
    
    // If already logged today, streak is maintained
    if (data.lastDate === today) {
        return data;
    }
    
    // If last activity was yesterday, streak continues
    if (data.lastDate === yesterday) {
        return data;
    }
    
    // If last activity was before yesterday, streak is broken
    if (data.lastDate && data.lastDate < yesterday) {
        data.streak = 0;
    }
    
    return data;
}

// Log an activity
function logActivity(activity) {
    const data = loadData();
    const today = getToday();
    
    // Check if already logged today
    if (data.lastDate === today) {
        showToast('Already logged today! 💪');
        return;
    }
    
    const yesterday = getYesterday();
    
    // Calculate new streak
    if (data.lastDate === yesterday) {
        // Continue streak
        data.streak += 1;
    } else if (data.lastDate === today) {
        // Already logged today (shouldn't reach here)
        return;
    } else {
        // Start new streak (either first time or streak was broken)
        data.streak = 1;
    }
    
    data.lastDate = today;
    
    // Add to history (keep last 10 entries)
    data.history.unshift({
        date: today,
        activity: activity,
        timestamp: Date.now()
    });
    data.history = data.history.slice(0, 10);
    
    saveData(data);
    updateUI();
    showToast(`${capitalize(activity)} logged! 🎉`);
}

// Capitalize first letter
function capitalize(str) {
    return str.charAt(0).toUpperCase() + str.slice(1);
}

// Update the UI
function updateUI() {
    const data = checkStreak(loadData());
    saveData(data); // Save potentially updated streak
    
    const today = getToday();
    const loggedToday = data.lastDate === today;
    
    // Update streak count
    document.getElementById('streakCount').textContent = data.streak;
    
    // Update today's status
    const statusEl = document.getElementById('todayStatus');
    if (loggedToday) {
        statusEl.className = 'today-status completed';
        const todayActivity = data.history.find(h => h.date === today);
        statusEl.innerHTML = `<span>✅</span><span>${todayActivity ? capitalize(todayActivity.activity) : 'Activity'} completed</span>`;
    } else {
        statusEl.className = 'today-status pending';
        statusEl.innerHTML = `<span>⏳</span><span>Log today's activity</span>`;
    }
    
    // Update buttons
    const buttons = document.querySelectorAll('.activity-btn');
    buttons.forEach(btn => {
        if (loggedToday) {
            btn.classList.add('completed');
        } else {
            btn.classList.remove('completed');
        }
    });
    
    // Update history
    updateHistory(data.history);
    
    // Update notification toggle
    document.getElementById('notificationToggle').checked = data.notificationsEnabled;
}

// Update history list
function updateHistory(history) {
    const historyEl = document.getElementById('historyList');
    
    if (history.length === 0) {
        historyEl.innerHTML = '<div class="empty-history">No activity logged yet</div>';
        return;
    }
    
    const activityIcons = {
        running: '🏃',
        aerobic: '💪',
        swimming: '🏊'
    };
    
    historyEl.innerHTML = history.map(item => {
        const date = new Date(item.date);
        const formattedDate = date.toLocaleDateString('en-US', { 
            weekday: 'short', 
            month: 'short', 
            day: 'numeric' 
        });
        
        return `
            <div class="history-item">
                <span class="history-date">${formattedDate}</span>
                <span class="history-activity ${item.activity}">
                    ${activityIcons[item.activity] || '🏃'} ${capitalize(item.activity)}
                </span>
            </div>
        `;
    }).join('');
}

// Show toast notification
function showToast(message) {
    const toast = document.getElementById('toast');
    toast.textContent = message;
    toast.classList.add('show');
    
    setTimeout(() => {
        toast.classList.remove('show');
    }, 3000);
}

// Request notification permission
async function requestNotificationPermission() {
    if (!('Notification' in window)) {
        showToast('Notifications not supported on this browser');
        return { granted: false, reason: 'unsupported' };
    }
    
    if (Notification.permission === 'granted') {
        return { granted: true };
    }
    
    if (Notification.permission === 'denied') {
        showToast('Notifications blocked. Please enable in browser settings.');
        return { granted: false, reason: 'denied' };
    }
    
    // Permission is 'default' - ask the user
    try {
        const permission = await Notification.requestPermission();
        if (permission === 'granted') {
            return { granted: true };
        } else {
            showToast('Notification permission not granted');
            return { granted: false, reason: 'not_granted' };
        }
    } catch (error) {
        console.error('Error requesting notification permission:', error);
        showToast('Could not request notification permission');
        return { granted: false, reason: 'error' };
    }
}

// Schedule daily notification
async function scheduleNotification() {
    if (!('serviceWorker' in navigator)) {
        showToast('Service Worker not supported');
        document.getElementById('notificationToggle').checked = false;
        return;
    }
    
    try {
        // Check if we have permission first (before waiting for SW)
        const permissionResult = await requestNotificationPermission();
        if (!permissionResult.granted) {
            document.getElementById('notificationToggle').checked = false;
            // Toast already shown by requestNotificationPermission
            return;
        }
        
        const registration = await navigator.serviceWorker.ready;
        
        // Store that notifications are enabled
        const data = loadData();
        data.notificationsEnabled = true;
        saveData(data);
        
        // Send message to service worker to schedule notification
        if (registration.active) {
            registration.active.postMessage({
                type: 'SCHEDULE_NOTIFICATION'
            });
        }
        
        showToast('Reminders enabled! 🔔');
    } catch (error) {
        console.error('Error scheduling notification:', error);
        showToast('Could not enable reminders');
        document.getElementById('notificationToggle').checked = false;
    }
}

// Cancel notifications
async function cancelNotifications() {
    const data = loadData();
    data.notificationsEnabled = false;
    saveData(data);
    
    if ('serviceWorker' in navigator) {
        const registration = await navigator.serviceWorker.ready;
        if (registration.active) {
            registration.active.postMessage({
                type: 'CANCEL_NOTIFICATION'
            });
        }
    }
    
    showToast('Reminders disabled');
}

// Check if running in a context that supports PWA features
function checkPWASupport() {
    const isSecureContext = window.isSecureContext;
    const hasServiceWorker = 'serviceWorker' in navigator;
    const hasNotifications = 'Notification' in window;
    const isFileProtocol = window.location.protocol === 'file:';
    
    console.log('PWA Support Check:', {
        isSecureContext,
        hasServiceWorker,
        hasNotifications,
        isFileProtocol,
        protocol: window.location.protocol
    });
    
    return {
        canUseServiceWorker: hasServiceWorker && !isFileProtocol,
        canUseNotifications: hasNotifications && isSecureContext,
        isFileProtocol
    };
}

// Initialize app
async function init() {
    const pwaSupport = checkPWASupport();
    
    // Register service worker (only if supported)
    if (pwaSupport.canUseServiceWorker) {
        try {
            const registration = await navigator.serviceWorker.register('sw.js');
            console.log('Service Worker registered:', registration.scope);
        } catch (error) {
            console.error('Service Worker registration failed:', error);
        }
    } else {
        console.log('Service Worker not available (file:// protocol or unsupported browser)');
    }
    
    // Set up button listeners
    document.querySelectorAll('.activity-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            const activity = btn.dataset.activity;
            logActivity(activity);
        });
    });
    
    // Set up notification toggle
    const notificationToggle = document.getElementById('notificationToggle');
    
    // Disable toggle if notifications aren't supported
    if (!pwaSupport.canUseNotifications) {
        notificationToggle.disabled = true;
        notificationToggle.parentElement.parentElement.style.opacity = '0.5';
        notificationToggle.parentElement.parentElement.title = 
            pwaSupport.isFileProtocol 
                ? 'Notifications require serving via HTTP(S). Use a local server or deploy to GitHub Pages.'
                : 'Notifications not supported in this browser';
    }
    
    notificationToggle.addEventListener('change', (e) => {
        if (e.target.checked) {
            if (pwaSupport.isFileProtocol) {
                showToast('Serve via HTTP for notifications (try: npx serve .)');
                e.target.checked = false;
                return;
            }
            scheduleNotification();
        } else {
            cancelNotifications();
        }
    });
    
    // Initial UI update
    updateUI();
}

// Run when DOM is ready
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
} else {
    init();
}

