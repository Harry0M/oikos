package com.theblankstate.epmanager.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.theblankstate.epmanager.data.model.Transaction
import com.theblankstate.epmanager.data.model.TransactionType
import com.theblankstate.epmanager.data.repository.AccountRepository
import com.theblankstate.epmanager.data.repository.CategoryRepository
import com.theblankstate.epmanager.data.repository.TransactionRepository
import com.theblankstate.epmanager.ui.transactions.TransactionWithCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val totalBalance: Double = 0.0,
    val monthlyExpenses: Double = 0.0,
    val monthlyIncome: Double = 0.0,
    val todaySpending: Double = 0.0,
    val recentTransactions: List<TransactionWithCategory> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    init {
        initializeData()
        loadDashboardData()
    }
    
    private fun initializeData() {
        viewModelScope.launch {
            // Initialize default categories and accounts if first run
            categoryRepository.initializeDefaultCategories()
            accountRepository.initializeDefaultAccounts()
        }
    }
    
    private fun loadDashboardData() {
        // Load recent transactions with category info
        viewModelScope.launch {
            combine(
                transactionRepository.getRecentTransactions(5),
                categoryRepository.getAllCategories()
            ) { transactions, categories ->
                val categoryMap = categories.associateBy { it.id }
                transactions.map { transaction ->
                    val category = categoryMap[transaction.categoryId]
                    TransactionWithCategory(
                        transaction = transaction,
                        categoryName = category?.name ?: "Unknown",
                        categoryColor = category?.color ?: 0xFF9CA3AF
                    )
                }
            }.collect { enrichedTransactions ->
                _uiState.update { it.copy(recentTransactions = enrichedTransactions) }
            }
        }
        
        // Load monthly expenses
        viewModelScope.launch {
            transactionRepository.getMonthlyExpenses()
                .collect { expenses ->
                    _uiState.update { it.copy(monthlyExpenses = expenses ?: 0.0) }
                }
        }
        
        // Load monthly income
        viewModelScope.launch {
            transactionRepository.getMonthlyIncome()
                .collect { income ->
                    _uiState.update { it.copy(monthlyIncome = income ?: 0.0) }
                }
        }
        
        // Load today's spending
        viewModelScope.launch {
            transactionRepository.getTodayTransactions()
                .map { transactions ->
                    transactions
                        .filter { it.type == TransactionType.EXPENSE }
                        .sumOf { it.amount }
                }
                .collect { todayTotal ->
                    _uiState.update { it.copy(todaySpending = todayTotal, isLoading = false) }
                }
        }
        
        // Get total balance from ACCOUNTS table (source of truth)
        // This ensures dashboard and accounts screen show same balance
        viewModelScope.launch {
            accountRepository.getTotalBalance()
                .collect { balance ->
                    _uiState.update { it.copy(totalBalance = balance ?: 0.0) }
                }
        }
    }
    
    fun refresh() {
        _uiState.update { it.copy(isLoading = true) }
        loadDashboardData()
    }
}
