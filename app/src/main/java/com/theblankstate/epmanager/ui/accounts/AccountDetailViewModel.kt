package com.theblankstate.epmanager.ui.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.theblankstate.epmanager.data.model.Account
import com.theblankstate.epmanager.data.model.Transaction
import com.theblankstate.epmanager.data.model.TransactionType
import com.theblankstate.epmanager.data.repository.AccountRepository
import com.theblankstate.epmanager.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

enum class DateRange(val label: String, val days: Int) {
    ONE_WEEK("1W", 7),
    ONE_MONTH("1M", 30),
    THREE_MONTHS("3M", 90),
    CUSTOM("Custom", 0),
    ALL("All", -1)
}

enum class ChartMode(val label: String) {
    NET_FLOW("Net Flow"),
    INCOME("Income"),
    EXPENSE("Expense")
}

enum class TransactionFilter(val label: String) {
    ALL("All"),
    INCOME("Income"),
    EXPENSE("Expense")
}

enum class SortOption(val label: String) {
    NEWEST("Newest"),
    OLDEST("Oldest"),
    HIGHEST_AMOUNT("High Amount"),
    LOWEST_AMOUNT("Low Amount")
}

data class AccountDetailUiState(
    val account: Account? = null,
    val transactions: List<Transaction> = emptyList(),
    val filteredTransactions: List<Transaction> = emptyList(),
    val chartData: List<Float> = emptyList(),
    val selectedRange: DateRange = DateRange.ONE_MONTH,
    val customStartDate: Long? = null,
    val customEndDate: Long? = null,
    val chartMode: ChartMode = ChartMode.NET_FLOW,
    val transactionFilter: TransactionFilter = TransactionFilter.ALL,
    val searchQuery: String = "",
    val sortOption: SortOption = SortOption.NEWEST,
    val showInformatics: Boolean = false,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val netFlow: Double = 0.0,
    val dailyAverage: Double = 0.0,
    val isLoading: Boolean = true
)

@HiltViewModel
class AccountDetailViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountDetailUiState())
    val uiState: StateFlow<AccountDetailUiState> = _uiState.asStateFlow()

    private var currentTransactionJob: kotlinx.coroutines.Job? = null

    fun loadAccount(accountId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            // Load Account
            val account = accountRepository.getAccountById(accountId)
            
            // Load Transactions based on default range
            updateDataForRange(accountId, _uiState.value.selectedRange)

            _uiState.update { it.copy(account = account) }
        }
    }

    fun onDateRangeSelected(range: DateRange) {
        val accountId = _uiState.value.account?.id ?: return
        if (range != DateRange.CUSTOM) {
             _uiState.update { it.copy(selectedRange = range) }
             updateDataForRange(accountId, range)
        } else {
             _uiState.update { it.copy(selectedRange = range) }
             // Wait for user to pick dates, or default to current month?
             // UI should show date picker if CUSTOM is selected
        }
    }

    fun setCustomDateRange(startDate: Long, endDate: Long) {
        val accountId = _uiState.value.account?.id ?: return
         _uiState.update { it.copy(selectedRange = DateRange.CUSTOM, customStartDate = startDate, customEndDate = endDate) }
         updateDataForRange(accountId, DateRange.CUSTOM, startDate, endDate)
    }

    fun setChartMode(mode: ChartMode) {
        _uiState.update { it.copy(chartMode = mode) }
        recalculateChart()
    }

    fun setTransactionFilter(filter: TransactionFilter) {
        _uiState.update { it.copy(transactionFilter = filter) }
        applyTransactionFilters()
    }
    
    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applyTransactionFilters()
    }
    
    fun setSortOption(option: SortOption) {
        _uiState.update { it.copy(sortOption = option) }
        applyTransactionFilters()
    }
    
    fun toggleInformatics() {
        _uiState.update { it.copy(showInformatics = !it.showInformatics) }
    }

    private fun updateDataForRange(
        accountId: String, 
        range: DateRange, 
        customStart: Long? = null, 
        customEnd: Long? = null
    ) {
        currentTransactionJob?.cancel()
        currentTransactionJob = viewModelScope.launch {
            val endDate = customEnd ?: System.currentTimeMillis()
            val startDate = when {
                range == DateRange.ALL -> 0L
                range == DateRange.CUSTOM -> customStart ?: (endDate - 30L * 24 * 60 * 60 * 1000) // Default 30d if null
                else -> {
                    val calendar = Calendar.getInstance()
                    calendar.timeInMillis = endDate // Ensure relative to 'end' if needed, or simply today
                    calendar.add(Calendar.DAY_OF_YEAR, -range.days)
                    calendar.timeInMillis
                }
            }

            val transactionsFlow = if (range == DateRange.ALL) {
                transactionRepository.getTransactionsByAccount(accountId)
            } else {
                transactionRepository.getTransactionsByAccountAndDateRange(accountId, startDate, endDate)
            }

            transactionsFlow.collect { transactions ->
                _uiState.update {
                    it.copy(
                        transactions = transactions,
                        isLoading = false
                    )
                }
                
                calculateStats(transactions, range, startDate, endDate)
                applyTransactionFilters()
                recalculateChart()
            }
        }
    }

    private fun calculateStats(transactions: List<Transaction>, range: DateRange, startDate: Long, endDate: Long) {
         val income = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
         val expense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
         
         val msDiff = endDate - startDate
         val days = if (range == DateRange.ALL) 30 else (msDiff / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(1)

         _uiState.update {
             it.copy(
                 totalIncome = income,
                 totalExpense = expense,
                 netFlow = income - expense,
                 dailyAverage = if(days > 0) expense / days else 0.0
             )
         }
    }

    private fun recalculateChart() {
        val transactions = _uiState.value.transactions
        val mode = _uiState.value.chartMode
        val points = calculateTrend(transactions, mode)
        _uiState.update { it.copy(chartData = points) }
    }
    
    private fun applyTransactionFilters() {
        val state = _uiState.value
        val filtered = state.transactions.filter { transaction ->
             val matchesType = when(state.transactionFilter) {
                 TransactionFilter.ALL -> true
                 TransactionFilter.INCOME -> transaction.type == TransactionType.INCOME
                 TransactionFilter.EXPENSE -> transaction.type == TransactionType.EXPENSE
             }
             
             val matchesSearch = if (state.searchQuery.isEmpty()) true else {
                 (transaction.merchantName?.contains(state.searchQuery, ignoreCase = true) == true) ||
                 (transaction.note?.contains(state.searchQuery, ignoreCase = true) == true) ||
                 (transaction.amount.toString().contains(state.searchQuery))
             }
             
             matchesType && matchesSearch
        }
        
        val sorted = when(state.sortOption) {
            SortOption.NEWEST -> filtered.sortedByDescending { it.date }
            SortOption.OLDEST -> filtered.sortedBy { it.date }
            SortOption.HIGHEST_AMOUNT -> filtered.sortedByDescending { it.amount }
            SortOption.LOWEST_AMOUNT -> filtered.sortedBy { it.amount }
        }
        
        _uiState.update { it.copy(filteredTransactions = sorted) }
    }
    
    private fun calculateTrend(
        transactions: List<Transaction>, 
        mode: ChartMode
    ): List<Float> {
        val sorted = transactions.sortedBy { it.date }
        if (sorted.isEmpty()) return emptyList()
        
        // Group by day? Using simple point mapping for now as requested by "options to add more variable"
        // Ideally we group by day for a proper time-series
        // Doing a simple transaction-by-transaction plot for smoother verification first
        // Or better: Accumulate?
        
        // Let's do simple mapping:
        return sorted.mapNotNull { 
            when (mode) {
                ChartMode.NET_FLOW -> if (it.type == TransactionType.INCOME) it.amount.toFloat() else -it.amount.toFloat()
                ChartMode.INCOME -> if (it.type == TransactionType.INCOME) it.amount.toFloat() else null
                ChartMode.EXPENSE -> if (it.type == TransactionType.EXPENSE) it.amount.toFloat() else null
            }
        }
    }
}
