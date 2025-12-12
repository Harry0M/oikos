package com.theblankstate.epmanager.ui.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.theblankstate.epmanager.data.model.*
import com.theblankstate.epmanager.data.repository.AccountRepository
import com.theblankstate.epmanager.data.repository.CategoryRepository
import com.theblankstate.epmanager.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddTransactionUiState(
    val amount: String = "",
    val note: String = "",
    val transactionType: TransactionType = TransactionType.EXPENSE,
    val selectedCategory: Category? = null,
    val selectedAccount: Account? = null,
    val selectedDate: Long = System.currentTimeMillis(),
    val categories: List<Category> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
    val isSaved: Boolean = false
)

@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AddTransactionUiState())
    val uiState: StateFlow<AddTransactionUiState> = _uiState.asStateFlow()
    
    init {
        loadData()
    }
    
    private fun loadData() {
        // Load expense categories by default
        viewModelScope.launch {
            categoryRepository.getExpenseCategories()
                .collect { categories ->
                    _uiState.update { state ->
                        state.copy(
                            categories = categories,
                            selectedCategory = state.selectedCategory ?: categories.firstOrNull()
                        )
                    }
                }
        }
        
        // Load accounts
        viewModelScope.launch {
            accountRepository.getAllAccounts()
                .collect { accounts ->
                    _uiState.update { state ->
                        state.copy(
                            accounts = accounts,
                            selectedAccount = state.selectedAccount ?: accounts.firstOrNull { it.isDefault } ?: accounts.firstOrNull(),
                            isLoading = false
                        )
                    }
                }
        }
    }
    
    fun updateAmount(amount: String) {
        // Only allow valid number input
        val filtered = amount.filter { it.isDigit() || it == '.' }
        if (filtered.count { it == '.' } <= 1) {
            _uiState.update { it.copy(amount = filtered) }
        }
    }
    
    fun updateNote(note: String) {
        _uiState.update { it.copy(note = note) }
    }
    
    fun updateTransactionType(type: TransactionType) {
        _uiState.update { it.copy(transactionType = type, selectedCategory = null) }
        
        // Load categories for the selected type
        viewModelScope.launch {
            val flow = if (type == TransactionType.EXPENSE) {
                categoryRepository.getExpenseCategories()
            } else {
                categoryRepository.getIncomeCategories()
            }
            
            flow.collect { categories ->
                _uiState.update { state ->
                    state.copy(
                        categories = categories,
                        selectedCategory = categories.firstOrNull()
                    )
                }
            }
        }
    }
    
    fun selectCategory(category: Category) {
        _uiState.update { it.copy(selectedCategory = category) }
    }
    
    fun selectAccount(account: Account) {
        _uiState.update { it.copy(selectedAccount = account) }
    }
    
    fun updateDate(timestamp: Long) {
        _uiState.update { it.copy(selectedDate = timestamp) }
    }
    
    fun saveTransaction() {
        val state = _uiState.value
        
        // Validate
        val amount = state.amount.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            _uiState.update { it.copy(error = "Please enter a valid amount") }
            return
        }
        
        if (state.selectedCategory == null) {
            _uiState.update { it.copy(error = "Please select a category") }
            return
        }
        
        if (state.selectedAccount == null) {
            _uiState.update { it.copy(error = "Please select an account") }
            return
        }
        
        _uiState.update { it.copy(isSaving = true, error = null) }
        
        viewModelScope.launch {
            try {
                val transaction = Transaction(
                    amount = amount,
                    type = state.transactionType,
                    categoryId = state.selectedCategory.id,
                    accountId = state.selectedAccount.id,
                    date = state.selectedDate,
                    note = state.note.takeIf { it.isNotBlank() }
                )
                
                // Save the transaction
                transactionRepository.insertTransaction(transaction)
                
                // Update account balance
                // Expense = subtract, Income = add
                val balanceChange = if (state.transactionType == TransactionType.EXPENSE) {
                    -amount
                } else {
                    amount
                }
                accountRepository.updateBalance(state.selectedAccount.id, balanceChange)
                
                _uiState.update { it.copy(isSaving = false, isSaved = true) }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(isSaving = false, error = e.message ?: "Failed to save transaction") 
                }
            }
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
