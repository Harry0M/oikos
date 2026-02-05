package com.theblankstate.epmanager.ui.transactions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.theblankstate.epmanager.data.model.Account
import com.theblankstate.epmanager.data.model.Category
import com.theblankstate.epmanager.data.model.Transaction
import com.theblankstate.epmanager.data.repository.AccountRepository
import com.theblankstate.epmanager.data.repository.CategoryRepository
import com.theblankstate.epmanager.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TransactionDetailUiState(
    val transaction: Transaction? = null,
    val category: Category? = null,
    val account: Account? = null,
    val linkedAccounts: List<Account> = emptyList(),
    val allCategories: List<Category> = emptyList(),
    val isLoading: Boolean = true,
    val isDeleted: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class TransactionDetailViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val ruleDao: com.theblankstate.epmanager.data.local.dao.CategorizationRuleDao,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    private val transactionId: String = checkNotNull(savedStateHandle["transactionId"])
    
    private val _uiState = MutableStateFlow(TransactionDetailUiState())
    val uiState: StateFlow<TransactionDetailUiState> = _uiState.asStateFlow()
    
    init {
        loadTransaction()
        loadLinkedAccounts()
    }
    
    private fun loadTransaction() {
        viewModelScope.launch {
            try {
                val transaction = transactionRepository.getTransactionById(transactionId)
                
                if (transaction != null) {
                    // Load category
                    val categories = categoryRepository.getAllCategories().first()
                    val category = categories.find { it.id == transaction.categoryId }
                    
                    // Load account
                    val account = transaction.accountId?.let { 
                        accountRepository.getAccountById(it) 
                    }
                    
                    _uiState.update {
                        it.copy(
                            transaction = transaction,
                            category = category,
                            account = account,
                            allCategories = categories,
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Transaction not found"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load transaction"
                    )
                }
            }
        }
    }
    
    private fun loadLinkedAccounts() {
        viewModelScope.launch {
            accountRepository.getLinkedAccounts().collect { accounts ->
                _uiState.update { it.copy(linkedAccounts = accounts) }
            }
        }
    }
    
    /**
     * Link SMS sender ID to selected account
     */
    fun linkSenderToAccount(senderId: String, accountId: String) {
        viewModelScope.launch {
            try {
                val success = accountRepository.addSenderIdToAccount(accountId, senderId)
                if (success) {
                    val account = accountRepository.getAccountById(accountId)
                    _uiState.update { 
                        it.copy(successMessage = "Sender linked to ${account?.name ?: "account"}") 
                    }
                } else {
                    _uiState.update { 
                        it.copy(error = "Sender already linked to this account") 
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to link sender") }
            }
        }
    }
    
    fun clearMessages() {
        _uiState.update { it.copy(error = null, successMessage = null) }
    }
    
    fun deleteTransaction() {
        val transaction = _uiState.value.transaction ?: return
        
        viewModelScope.launch {
            try {
                // Reverse the balance change first
                if (transaction.accountId != null) {
                    val reverseAmount = if (transaction.type == com.theblankstate.epmanager.data.model.TransactionType.EXPENSE) {
                        transaction.amount // Add back
                    } else {
                        -transaction.amount // Subtract back
                    }
                    accountRepository.updateBalance(transaction.accountId, reverseAmount)
                }
                
                transactionRepository.deleteTransaction(transaction)
                _uiState.update { it.copy(isDeleted = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    /**
     * Create a permanent categorization rule
     */
    fun createCategorizationRule(pattern: String, categoryId: String) {
        viewModelScope.launch {
            try {
                ruleDao.insertRule(
                    com.theblankstate.epmanager.data.model.CategorizationRule(
                        pattern = pattern,
                        categoryId = categoryId
                    )
                )
                _uiState.update { it.copy(successMessage = "Rule saved: '$pattern' -> Category") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to save rule: ${e.message}") }
            }
        }
    }
}
