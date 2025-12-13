package com.theblankstate.epmanager.ui.edit

import androidx.lifecycle.SavedStateHandle
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

data class EditTransactionUiState(
    val transactionId: String = "",
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
    val isSaved: Boolean = false,
    // Original values for balance adjustment
    val originalAmount: Double = 0.0,
    val originalType: TransactionType = TransactionType.EXPENSE,
    val originalAccountId: String? = null
)

@HiltViewModel
class EditTransactionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {
    
    private val transactionId: String = savedStateHandle.get<String>("transactionId") ?: ""
    
    private val _uiState = MutableStateFlow(EditTransactionUiState(transactionId = transactionId))
    val uiState: StateFlow<EditTransactionUiState> = _uiState.asStateFlow()
    
    init {
        loadTransaction()
    }
    
    private fun loadTransaction() {
        viewModelScope.launch {
            try {
                // Load the transaction
                val transaction = transactionRepository.getTransactionById(transactionId)
                if (transaction == null) {
                    _uiState.update { it.copy(isLoading = false, error = "Transaction not found") }
                    return@launch
                }
                
                // Load categories for the transaction type
                val categoriesFlow = if (transaction.type == TransactionType.EXPENSE) {
                    categoryRepository.getExpenseCategories()
                } else {
                    categoryRepository.getIncomeCategories()
                }
                
                // Load accounts
                val accounts = accountRepository.getAllAccounts().first()
                val categories = categoriesFlow.first()
                
                // Find matching category and account
                val selectedCategory = categories.find { it.id == transaction.categoryId }
                val selectedAccount = accounts.find { it.id == transaction.accountId }
                
                _uiState.update { state ->
                    state.copy(
                        amount = transaction.amount.toString(),
                        note = transaction.note ?: "",
                        transactionType = transaction.type,
                        selectedCategory = selectedCategory,
                        selectedAccount = selectedAccount,
                        selectedDate = transaction.date,
                        categories = categories,
                        accounts = accounts,
                        isLoading = false,
                        // Store original values for balance adjustment
                        originalAmount = transaction.amount,
                        originalType = transaction.type,
                        originalAccountId = transaction.accountId
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(isLoading = false, error = e.message ?: "Failed to load transaction") 
                }
            }
        }
    }
    
    fun updateAmount(amount: String) {
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
        val newAmount = state.amount.toDoubleOrNull()
        if (newAmount == null || newAmount <= 0) {
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
                // Calculate balance adjustments
                // First, reverse the original transaction effect
                // Then, apply the new transaction effect
                
                val originalEffect = if (state.originalType == TransactionType.EXPENSE) {
                    -state.originalAmount
                } else {
                    state.originalAmount
                }
                
                val newEffect = if (state.transactionType == TransactionType.EXPENSE) {
                    -newAmount
                } else {
                    newAmount
                }
                
                // Reverse original on original account
                state.originalAccountId?.let { originalAccountId ->
                    accountRepository.updateBalance(originalAccountId, -originalEffect)
                }
                
                // Apply new effect on new account
                accountRepository.updateBalance(state.selectedAccount.id, newEffect)
                
                // Update the transaction
                val updatedTransaction = Transaction(
                    id = state.transactionId,
                    amount = newAmount,
                    type = state.transactionType,
                    categoryId = state.selectedCategory.id,
                    accountId = state.selectedAccount.id,
                    date = state.selectedDate,
                    note = state.note.takeIf { it.isNotBlank() },
                    updatedAt = System.currentTimeMillis()
                )
                
                transactionRepository.updateTransaction(updatedTransaction)
                
                _uiState.update { it.copy(isSaving = false, isSaved = true) }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(isSaving = false, error = e.message ?: "Failed to update transaction") 
                }
            }
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
