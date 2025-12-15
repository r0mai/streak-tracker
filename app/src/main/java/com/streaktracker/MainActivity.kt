package com.streaktracker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.streaktracker.notification.NotificationHelper
import com.streaktracker.notification.ReminderWorker
import com.streaktracker.ui.MainScreen
import com.streaktracker.ui.MainViewModel
import com.streaktracker.ui.theme.StreakTrackerTheme

class MainActivity : ComponentActivity() {
    
    private lateinit var viewModel: MainViewModel
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted, reschedule reminder
            ReminderWorker.scheduleReminder(this)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Request notification permission for Android 13+
        requestNotificationPermission()
        
        // Cancel any existing notification when app is opened
        NotificationHelper.cancelReminderNotification(this)
        
        // Initialize ViewModel
        val application = application as StreakTrackerApplication
        viewModel = ViewModelProvider(
            this,
            MainViewModel.Factory(application.repository)
        )[MainViewModel::class.java]
        
        setContent {
            StreakTrackerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val uiState by viewModel.uiState.collectAsState()
                    
                    MainScreen(
                        uiState = uiState,
                        onActivityClick = { activityType ->
                            viewModel.logActivity(activityType)
                            // Cancel notification since activity is now logged
                            NotificationHelper.cancelReminderNotification(this)
                        },
                        onPreviousMonth = { viewModel.previousMonth() },
                        onNextMonth = { viewModel.nextMonth() }
                    )
                }
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Reschedule reminder when app comes to foreground
        ReminderWorker.scheduleReminder(this)
        // Check if day changed while app was in background and refresh if needed
        viewModel.refreshForDayChange()
    }
    
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permission already granted
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // Show explanation if needed, then request
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> {
                    // Request permission directly
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }
}

