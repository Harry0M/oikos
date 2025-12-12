package com.theblankstate.epmanager.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.theblankstate.epmanager.data.model.Category
import com.theblankstate.epmanager.data.model.Transaction
import com.theblankstate.epmanager.data.repository.CategoryRepository
import com.theblankstate.epmanager.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Transaction with its associated category info for display
 */
data class TransactionWithCategory(
    val transaction: Transaction,
    val categoryName: String,
    val categoryColor: Long
)

data class TransactionsUiState(
    val transactions: List<TransactionWithCategory> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true
)

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(TransactionsUiState())
    val uiState: StateFlow<TransactionsUiState> = _uiState.asStateFlow()
    
    private val searchQuery = MutableStateFlow("")
    
    init {
        loadTransactions()
    }
    
    private fun loadTransactions() {
        viewModelScope.launch {
            combine(
                transactionRepository.getAllTransactions(),
                categoryRepository.getAllCategories(),
                searchQuery
            ) { transactions, categories, query ->
                val categoryMap = categories.associateBy { it.id }
                
                val enrichedTransactions = transactions.map { transaction ->
                    val category = categoryMap[transaction.categoryId]
                    TransactionWithCategory(
                        transaction = transaction,
                        categoryName = category?.name ?: "Unknown",
                        categoryColor = category?.color ?: 0xFF9CA3AF
                    )
                }
                
                if (query.isBlank()) {
                    enrichedTransactions
                } else {
                    enrichedTransactions.filter { 
                        it.transaction.note?.contains(query, ignoreCase = true) == true ||
                        it.categoryName.contains(query, ignoreCase = true)
                    }
                }
            }.collect { filtered ->
                _uiState.update { 
                    it.copy(
                        transactions = filtered,
                        isLoading = false
                    ) 
                }
            }
        }
    }
    
    fun updateSearchQuery(query: String) {
        searchQuery.value = query
        _uiState.update { it.copy(searchQuery = query) }
    }
    
    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            transactionRepository.deleteTransaction(transaction)
        }
    }
}
