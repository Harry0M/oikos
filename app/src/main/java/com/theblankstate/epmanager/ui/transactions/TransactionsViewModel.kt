package com.theblankstate.epmanager.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.theblankstate.epmanager.data.model.Transaction
import com.theblankstate.epmanager.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TransactionsUiState(
    val transactions: List<Transaction> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true
)

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository
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
                searchQuery
            ) { transactions, query ->
                if (query.isBlank()) {
                    transactions
                } else {
                    transactions.filter { 
                        it.note?.contains(query, ignoreCase = true) == true ||
                        it.categoryId?.contains(query, ignoreCase = true) == true
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
