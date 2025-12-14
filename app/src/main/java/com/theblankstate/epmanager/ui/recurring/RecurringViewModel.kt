package com.theblankstate.epmanager.ui.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.theblankstate.epmanager.data.model.*
import com.theblankstate.epmanager.data.repository.CategoryRepository
import com.theblankstate.epmanager.data.repository.RecurringExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecurringUiState(
    val recurringExpenses: List<RecurringExpense> = emptyList(),
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = true,
    val showAddDialog: Boolean = false,
    val upcomingCount: Int = 0
)

@HiltViewModel
class RecurringViewModel @Inject constructor(
    private val recurringRepository: RecurringExpenseRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(RecurringUiState())
    val uiState: StateFlow<RecurringUiState> = _uiState.asStateFlow()
    
    init {
        loadData()
    }
    
    private fun loadData() {
        viewModelScope.launch {
            recurringRepository.getAllRecurringExpenses()
                .collect { expenses ->
                    _uiState.update { 
                        it.copy(
                            recurringExpenses = expenses,
                            isLoading = false
                        ) 
                    }
                }
        }
        
        viewModelScope.launch {
            categoryRepository.getExpenseCategories()
                .collect { categories ->
                    _uiState.update { it.copy(categories = categories) }
                }
        }
        
        viewModelScope.launch {
            val upcoming = recurringRepository.getUpcomingReminders()
            _uiState.update { it.copy(upcomingCount = upcoming.size) }
        }
    }
    
    fun showAddDialog() {
        _uiState.update { it.copy(showAddDialog = true) }
    }
    
    fun hideAddDialog() {
        _uiState.update { it.copy(showAddDialog = false) }
    }
    
    fun addRecurringExpense(
        name: String,
        amount: Double,
        categoryId: String?,
        frequency: RecurringFrequency,
        scheduleDay: Int,
        autoAdd: Boolean
    ) {
        viewModelScope.launch {
            val nextDueDate = calculateNextDueDate(frequency, scheduleDay)
            val recurring = RecurringExpense(
                name = name,
                amount = amount,
                type = TransactionType.EXPENSE,
                categoryId = categoryId,
                accountId = null,
                frequency = frequency,
                scheduleDay = scheduleDay,
                startDate = System.currentTimeMillis(),
                nextDueDate = nextDueDate,
                autoAdd = autoAdd
            )
            recurringRepository.insertRecurringExpense(recurring)
            hideAddDialog()
        }
    }
    
    private fun calculateNextDueDate(frequency: RecurringFrequency, scheduleDay: Int): Long {
        val calendar = java.util.Calendar.getInstance()
        when (frequency) {
            RecurringFrequency.DAILY -> {
                // Daily - next occurrence is tomorrow
                calendar.add(java.util.Calendar.DAY_OF_MONTH, 1)
            }
            RecurringFrequency.WEEKLY, RecurringFrequency.BIWEEKLY -> {
                // Weekly/Biweekly - scheduleDay is day of week (1-7, Sunday=1)
                val currentDayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK)
                var daysUntilNext = scheduleDay - currentDayOfWeek
                if (daysUntilNext <= 0) daysUntilNext += 7
                if (frequency == RecurringFrequency.BIWEEKLY && daysUntilNext <= 7) {
                    daysUntilNext += 7
                }
                calendar.add(java.util.Calendar.DAY_OF_MONTH, daysUntilNext)
            }
            RecurringFrequency.MONTHLY, RecurringFrequency.QUARTERLY -> {
                // Monthly/Quarterly - scheduleDay is day of month (1-28)
                val currentDay = calendar.get(java.util.Calendar.DAY_OF_MONTH)
                if (currentDay >= scheduleDay) {
                    // Move to next month
                    val monthsToAdd = if (frequency == RecurringFrequency.QUARTERLY) 3 else 1
                    calendar.add(java.util.Calendar.MONTH, monthsToAdd)
                }
                calendar.set(java.util.Calendar.DAY_OF_MONTH, minOf(scheduleDay, calendar.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)))
            }
            RecurringFrequency.YEARLY -> {
                // Yearly - use start date as base, next year
                calendar.add(java.util.Calendar.YEAR, 1)
            }
        }
        // Set to beginning of day
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
    
    fun toggleActive(recurring: RecurringExpense) {
        viewModelScope.launch {
            recurringRepository.toggleActive(recurring.id, !recurring.isActive)
        }
    }
    
    fun deleteRecurring(recurring: RecurringExpense) {
        viewModelScope.launch {
            recurringRepository.deleteRecurringExpense(recurring)
        }
    }
    
    fun processDueExpenses() {
        viewModelScope.launch {
            recurringRepository.processDueExpenses()
        }
    }
}
