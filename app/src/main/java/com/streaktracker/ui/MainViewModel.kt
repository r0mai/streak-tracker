package com.streaktracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.streaktracker.data.ActivityEntry
import com.streaktracker.data.ActivityType
import com.streaktracker.repository.ActivityRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.temporal.ChronoUnit

data class MainUiState(
    val currentStreak: Int = 0,
    val todayActivity: ActivityEntry? = null,
    val currentMonth: YearMonth = YearMonth.now(),
    val monthActivities: Map<LocalDate, ActivityEntry> = emptyMap(),
    val isLoading: Boolean = true
)

class MainViewModel(private val repository: ActivityRepository) : ViewModel() {
    
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()
    
    // Track the date we're currently observing
    private var trackedDate: LocalDate = LocalDate.now()
    private var todayObservationJob: Job? = null
    private var midnightTimerJob: Job? = null

    init {
        loadData()
        observeTodayActivity()
        startMidnightTimer()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            val streak = repository.calculateStreak()
            _uiState.value = _uiState.value.copy(
                currentStreak = streak,
                isLoading = false
            )
            
            loadMonthActivities(_uiState.value.currentMonth)
        }
    }

    private fun observeTodayActivity() {
        // Cancel any existing observation
        todayObservationJob?.cancel()
        
        trackedDate = LocalDate.now()
        todayObservationJob = viewModelScope.launch {
            repository.getActivityForDate(trackedDate).collect { activity ->
                _uiState.value = _uiState.value.copy(todayActivity = activity)
                // Recalculate streak when today's activity changes
                val streak = repository.calculateStreak()
                _uiState.value = _uiState.value.copy(currentStreak = streak)
            }
        }
    }
    
    /**
     * Starts a timer that triggers a refresh at midnight.
     * This ensures the UI updates when the day changes while the app is open.
     */
    private fun startMidnightTimer() {
        midnightTimerJob?.cancel()
        midnightTimerJob = viewModelScope.launch {
            while (true) {
                val now = LocalDateTime.now()
                val midnight = LocalDate.now().plusDays(1).atStartOfDay()
                val delayMillis = ChronoUnit.MILLIS.between(now, midnight) + 100 // Add small buffer
                
                delay(delayMillis)
                
                // Day has changed, refresh everything
                onDayChanged()
            }
        }
    }
    
    /**
     * Called when the day changes (either at midnight or when resuming from background).
     * Refreshes all data to reflect the new day.
     */
    private fun onDayChanged() {
        val today = LocalDate.now()
        
        // Check if we need to update the month view
        val currentMonth = YearMonth.from(today)
        val wasViewingCurrentMonth = _uiState.value.currentMonth == YearMonth.from(trackedDate)
        
        // Re-observe today's activity with the new date
        observeTodayActivity()
        
        // Recalculate streak
        viewModelScope.launch {
            val streak = repository.calculateStreak()
            _uiState.value = _uiState.value.copy(currentStreak = streak)
        }
        
        // If user was viewing the "current" month, navigate to the new current month
        if (wasViewingCurrentMonth && _uiState.value.currentMonth != currentMonth) {
            navigateToMonth(currentMonth)
        } else {
            // Just reload the current month's activities in case they changed
            loadMonthActivities(_uiState.value.currentMonth)
        }
    }
    
    /**
     * Called when the app resumes from background.
     * Checks if the day has changed and refreshes data if needed.
     */
    fun refreshForDayChange() {
        val today = LocalDate.now()
        if (today != trackedDate) {
            onDayChanged()
        }
    }

    private fun loadMonthActivities(yearMonth: YearMonth) {
        viewModelScope.launch {
            repository.getActivitiesForMonth(yearMonth.year, yearMonth.monthValue)
                .collect { activities ->
                    val activityMap = activities.associateBy { it.date }
                    _uiState.value = _uiState.value.copy(monthActivities = activityMap)
                }
        }
    }

    fun logActivity(activityType: ActivityType) {
        viewModelScope.launch {
            repository.logActivity(activityType)
        }
    }

    fun navigateToMonth(yearMonth: YearMonth) {
        _uiState.value = _uiState.value.copy(currentMonth = yearMonth)
        loadMonthActivities(yearMonth)
    }

    fun previousMonth() {
        navigateToMonth(_uiState.value.currentMonth.minusMonths(1))
    }

    fun nextMonth() {
        val nextMonth = _uiState.value.currentMonth.plusMonths(1)
        // Don't navigate to future months
        if (nextMonth <= YearMonth.now()) {
            navigateToMonth(nextMonth)
        }
    }

    class Factory(private val repository: ActivityRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                return MainViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

