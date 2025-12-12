package com.theblankstate.epmanager.ui.subscriptions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.theblankstate.epmanager.data.model.Category
import com.theblankstate.epmanager.data.model.RecurringExpense
import com.theblankstate.epmanager.data.model.RecurringFrequency
import com.theblankstate.epmanager.data.model.TransactionType
import com.theblankstate.epmanager.data.repository.CategoryRepository
import com.theblankstate.epmanager.data.repository.RecurringExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SubscriptionsUiState(
    val subscriptions: List<RecurringExpense> = emptyList(),
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = true,
    val showAddDialog: Boolean = false,
    val editingSubscription: RecurringExpense? = null,
    val totalMonthlyAmount: Double = 0.0,
    val totalYearlyAmount: Double = 0.0,
    val upcomingRenewals: List<RecurringExpense> = emptyList()
)

@HiltViewModel
class SubscriptionsViewModel @Inject constructor(
    private val recurringRepository: RecurringExpenseRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SubscriptionsUiState())
    val uiState: StateFlow<SubscriptionsUiState> = _uiState.asStateFlow()
    
    init {
        loadSubscriptions()
        loadCategories()
    }
    
    private fun loadSubscriptions() {
        viewModelScope.launch {
            recurringRepository.getActiveRecurringExpenses()
                .collect { all ->
                    // Filter for subscription-like recurring expenses (monthly or more frequent)
                    val subscriptions = all.filter { 
                        it.type == TransactionType.EXPENSE && 
                        it.frequency in listOf(
                            RecurringFrequency.MONTHLY, 
                            RecurringFrequency.WEEKLY,
                            RecurringFrequency.BIWEEKLY,
                            RecurringFrequency.YEARLY,
                            RecurringFrequency.QUARTERLY
                        )
                    }
                    
                    val monthlyTotal = calculateMonthlyTotal(subscriptions)
                    val yearlyTotal = monthlyTotal * 12
                    
                    // Get upcoming renewals (within next 7 days)
                    val now = System.currentTimeMillis()
                    val sevenDaysLater = now + (7 * 24 * 60 * 60 * 1000)
                    val upcoming = subscriptions
                        .filter { it.isActive && it.nextDueDate in now..sevenDaysLater }
                        .sortedBy { it.nextDueDate }
                    
                    _uiState.update { 
                        it.copy(
                            subscriptions = subscriptions.sortedBy { s -> s.nextDueDate },
                            totalMonthlyAmount = monthlyTotal,
                            totalYearlyAmount = yearlyTotal,
                            upcomingRenewals = upcoming,
                            isLoading = false
                        ) 
                    }
                }
        }
    }
    
    private fun loadCategories() {
        viewModelScope.launch {
            categoryRepository.getExpenseCategories()
                .collect { categories ->
                    _uiState.update { it.copy(categories = categories) }
                }
        }
    }
    
    private fun calculateMonthlyTotal(subscriptions: List<RecurringExpense>): Double {
        return subscriptions.filter { it.isActive }.sumOf { subscription ->
            when (subscription.frequency) {
                RecurringFrequency.DAILY -> subscription.amount * 30
                RecurringFrequency.WEEKLY -> subscription.amount * 4.33
                RecurringFrequency.BIWEEKLY -> subscription.amount * 2.17
                RecurringFrequency.MONTHLY -> subscription.amount
                RecurringFrequency.QUARTERLY -> subscription.amount / 3
                RecurringFrequency.YEARLY -> subscription.amount / 12
            }
        }
    }
    
    fun showAddDialog() {
        _uiState.update { it.copy(showAddDialog = true, editingSubscription = null) }
    }
    
    fun showEditDialog(subscription: RecurringExpense) {
        _uiState.update { it.copy(showAddDialog = true, editingSubscription = subscription) }
    }
    
    fun hideDialog() {
        _uiState.update { it.copy(showAddDialog = false, editingSubscription = null) }
    }
    
    fun saveSubscription(
        name: String,
        amount: Double,
        categoryId: String?,
        frequency: RecurringFrequency,
        autoAdd: Boolean
    ) {
        viewModelScope.launch {
            val existing = _uiState.value.editingSubscription
            if (existing != null) {
                // Update existing
                val updated = existing.copy(
                    name = name,
                    amount = amount,
                    categoryId = categoryId,
                    frequency = frequency,
                    autoAdd = autoAdd
                )
                recurringRepository.updateRecurringExpense(updated)
            } else {
                // Create new subscription
                val subscription = RecurringExpense(
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
                recurringRepository.insertRecurringExpense(subscription)
            }
            hideDialog()
        }
    }
    
    fun toggleActive(subscription: RecurringExpense) {
        viewModelScope.launch {
            recurringRepository.toggleActive(subscription.id, !subscription.isActive)
        }
    }
    
    fun cancelSubscription(subscription: RecurringExpense) {
        viewModelScope.launch {
            recurringRepository.deleteRecurringExpense(subscription)
        }
    }
}
