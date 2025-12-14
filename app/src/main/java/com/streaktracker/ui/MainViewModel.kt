package com.streaktracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.streaktracker.data.ActivityEntry
import com.streaktracker.data.ActivityType
import com.streaktracker.repository.ActivityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

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

    init {
        loadData()
        observeTodayActivity()
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
        viewModelScope.launch {
            repository.getTodayActivity().collect { activity ->
                _uiState.value = _uiState.value.copy(todayActivity = activity)
                // Recalculate streak when today's activity changes
                val streak = repository.calculateStreak()
                _uiState.value = _uiState.value.copy(currentStreak = streak)
            }
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

