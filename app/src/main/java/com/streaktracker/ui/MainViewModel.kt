package com.streaktracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.streaktracker.data.ActivityEntry
import com.streaktracker.data.ActivityType
import com.streaktracker.data.DayStatus
import com.streaktracker.data.SettingsDataStore
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
    val todayProgress: Int = 0,
    val dailyGoal: Int = SettingsDataStore.DEFAULT_DAILY_GOAL,
    val todayActivities: List<ActivityEntry> = emptyList(),
    val currentMonth: YearMonth = YearMonth.now(),
    val monthActivities: List<ActivityEntry> = emptyList(),
    val monthDayStatuses: Map<LocalDate, DayStatus> = emptyMap(),
    val isLoading: Boolean = true,
    // Activity input panel state
    val selectedActivityType: ActivityType? = null,
    val pendingDuration: Int = 0,
    // Settings panel state
    val showSettings: Boolean = false
)

class MainViewModel(private val repository: ActivityRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    // Track the date we're currently observing
    private var trackedDate: LocalDate = LocalDate.now()
    private var todayProgressJob: Job? = null
    private var todayActivitiesJob: Job? = null
    private var dailyGoalJob: Job? = null
    private var midnightTimerJob: Job? = null

    init {
        loadData()
        observeTodayProgress()
        observeTodayActivities()
        observeDailyGoal()
        startMidnightTimer()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            // Finalize any past days that need it
            repository.finalizePastDays()

            val streak = repository.calculateStreak()
            _uiState.value = _uiState.value.copy(
                currentStreak = streak,
                isLoading = false
            )

            loadMonthData(_uiState.value.currentMonth)
        }
    }

    private fun observeTodayProgress() {
        todayProgressJob?.cancel()
        trackedDate = LocalDate.now()

        todayProgressJob = viewModelScope.launch {
            repository.getTodayProgress().collect { (progress, goal) ->
                _uiState.value = _uiState.value.copy(
                    todayProgress = progress,
                    dailyGoal = goal
                )
                // Recalculate streak when progress changes
                val streak = repository.calculateStreak()
                _uiState.value = _uiState.value.copy(currentStreak = streak)
            }
        }
    }

    private fun observeTodayActivities() {
        todayActivitiesJob?.cancel()

        todayActivitiesJob = viewModelScope.launch {
            repository.getTodayActivities().collect { activities ->
                _uiState.value = _uiState.value.copy(todayActivities = activities)
            }
        }
    }

    private fun observeDailyGoal() {
        dailyGoalJob?.cancel()

        dailyGoalJob = viewModelScope.launch {
            repository.getDailyGoal().collect { goal ->
                _uiState.value = _uiState.value.copy(dailyGoal = goal)
            }
        }
    }

    /**
     * Starts a timer that triggers a refresh at midnight.
     */
    private fun startMidnightTimer() {
        midnightTimerJob?.cancel()
        midnightTimerJob = viewModelScope.launch {
            while (true) {
                val now = LocalDateTime.now()
                val midnight = LocalDate.now().plusDays(1).atStartOfDay()
                val delayMillis = ChronoUnit.MILLIS.between(now, midnight) + 100

                delay(delayMillis)
                onDayChanged()
            }
        }
    }

    private fun onDayChanged() {
        val today = LocalDate.now()
        val currentMonth = YearMonth.from(today)
        val wasViewingCurrentMonth = _uiState.value.currentMonth == YearMonth.from(trackedDate)

        // Re-observe today's data with the new date
        observeTodayProgress()
        observeTodayActivities()

        // Finalize past days and recalculate streak
        viewModelScope.launch {
            repository.finalizePastDays()
            val streak = repository.calculateStreak()
            _uiState.value = _uiState.value.copy(currentStreak = streak)
        }

        if (wasViewingCurrentMonth && _uiState.value.currentMonth != currentMonth) {
            navigateToMonth(currentMonth)
        } else {
            loadMonthData(_uiState.value.currentMonth)
        }
    }

    fun refreshForDayChange() {
        val today = LocalDate.now()
        if (today != trackedDate) {
            onDayChanged()
        }
    }

    private fun loadMonthData(yearMonth: YearMonth) {
        viewModelScope.launch {
            // Load activities for the month
            repository.getActivitiesForMonth(yearMonth.year, yearMonth.monthValue)
                .collect { activities ->
                    _uiState.value = _uiState.value.copy(monthActivities = activities)
                }
        }
        viewModelScope.launch {
            // Load day statuses for the month
            repository.getDayStatusesForMonth(yearMonth.year, yearMonth.monthValue)
                .collect { statuses ->
                    val statusMap = statuses.associateBy { it.date }
                    _uiState.value = _uiState.value.copy(monthDayStatuses = statusMap)
                }
        }
    }

    // ===== Activity Input Actions =====

    fun selectActivity(activityType: ActivityType) {
        _uiState.value = _uiState.value.copy(
            selectedActivityType = activityType,
            pendingDuration = 0
        )
    }

    fun addDuration(minutes: Int) {
        _uiState.value = _uiState.value.copy(
            pendingDuration = _uiState.value.pendingDuration + minutes
        )
    }

    fun clearDuration() {
        _uiState.value = _uiState.value.copy(pendingDuration = 0)
    }

    fun confirmActivity() {
        val activityType = _uiState.value.selectedActivityType ?: return
        val duration = _uiState.value.pendingDuration
        if (duration <= 0) return

        viewModelScope.launch {
            repository.logActivity(activityType, duration)
        }

        // Close the panel
        _uiState.value = _uiState.value.copy(
            selectedActivityType = null,
            pendingDuration = 0
        )
    }

    fun cancelInput() {
        _uiState.value = _uiState.value.copy(
            selectedActivityType = null,
            pendingDuration = 0
        )
    }

    // ===== Settings Actions =====

    fun openSettings() {
        _uiState.value = _uiState.value.copy(showSettings = true)
    }

    fun closeSettings() {
        _uiState.value = _uiState.value.copy(showSettings = false)
    }

    fun setDailyGoal(minutes: Int) {
        viewModelScope.launch {
            repository.setDailyGoal(minutes)
        }
    }

    // ===== Navigation =====

    fun navigateToMonth(yearMonth: YearMonth) {
        _uiState.value = _uiState.value.copy(currentMonth = yearMonth)
        loadMonthData(yearMonth)
    }

    fun previousMonth() {
        navigateToMonth(_uiState.value.currentMonth.minusMonths(1))
    }

    fun nextMonth() {
        val nextMonth = _uiState.value.currentMonth.plusMonths(1)
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
