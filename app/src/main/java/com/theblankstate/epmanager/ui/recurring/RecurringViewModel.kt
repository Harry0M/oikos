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
        autoAdd: Boolean
    ) {
        viewModelScope.launch {
            val recurring = RecurringExpense(
                name = name,
                amount = amount,
                type = TransactionType.EXPENSE,
                categoryId = categoryId,
                accountId = null,
                frequency = frequency,
                startDate = System.currentTimeMillis(),
                nextDueDate = System.currentTimeMillis(),
                autoAdd = autoAdd
            )
            recurringRepository.insertRecurringExpense(recurring)
            hideAddDialog()
        }
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
