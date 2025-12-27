package com.theblankstate.epmanager.ui.transactions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.theblankstate.epmanager.data.model.Category
import com.theblankstate.epmanager.data.model.Transaction
import com.theblankstate.epmanager.data.model.TransactionType
import com.theblankstate.epmanager.data.repository.CategoryRepository
import com.theblankstate.epmanager.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

/**
 * Transaction with its associated category info for display
 */
data class TransactionWithCategory(
    val transaction: Transaction,
    val categoryName: String,
    val categoryColor: Long
)

/**
 * Category info for filter chips
 */
data class CategoryFilterItem(
    val id: String,
    val name: String,
    val color: Long
)

/**
 * Filter options for transaction type
 */
enum class TypeFilter(val label: String) {
    ALL("All"),
    EXPENSE("Expense"),
    INCOME("Income")
}

/**
 * Filter options for time period
 */
enum class TimeFilter(val label: String) {
    ALL("All Time"),
    TODAY("Today"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    THIS_YEAR("This Year"),
    CUSTOM("Custom")
}

data class TransactionsUiState(
    val transactions: List<TransactionWithCategory> = emptyList(),
    val availableCategories: List<CategoryFilterItem> = emptyList(),
    val searchQuery: String = "",
    val typeFilter: TypeFilter = TypeFilter.ALL,
    val timeFilter: TimeFilter = TimeFilter.ALL,
    val selectedCategoryId: String? = null,
    val customStartDate: Long? = null,
    val customEndDate: Long? = null,
    val showDatePicker: Boolean = false,
    val isSelectingStartDate: Boolean = true,
    val isLoading: Boolean = true
)

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(TransactionsUiState())
    val uiState: StateFlow<TransactionsUiState> = _uiState.asStateFlow()
    
    private val searchQuery = MutableStateFlow("")
    private val typeFilter = MutableStateFlow(TypeFilter.ALL)
    private val timeFilter = MutableStateFlow(TimeFilter.ALL)
    private val selectedCategoryId = MutableStateFlow<String?>(null)
    private val customStartDate = MutableStateFlow<Long?>(null)
    private val customEndDate = MutableStateFlow<Long?>(null)
    
    init {
        loadTransactions()
    }
    
    private fun loadTransactions() {
        // Get filter arguments from navigation
        val filterType = savedStateHandle.get<String>("type")
        val filterId = savedStateHandle.get<String>("id") ?: ""
        
        // Select the appropriate data source
        val transactionsFlow: Flow<List<Transaction>> = when (filterType) {
            "GOAL" -> transactionRepository.getTransactionsByGoal(filterId)
            "DEBT" -> transactionRepository.getTransactionsByDebt(filterId)
            "RECURRING" -> transactionRepository.getTransactionsByRecurring(filterId)
            "CATEGORY" -> transactionRepository.getTransactionsByCategory(filterId)
            "ACCOUNT" -> transactionRepository.getTransactionsByAccount(filterId)
            else -> transactionRepository.getAllTransactions()
        }
        
        // Combine filter states into a single flow
        val filtersFlow = combine(
            searchQuery,
            typeFilter,
            timeFilter,
            selectedCategoryId,
            combine(customStartDate, customEndDate) { start, end -> Pair(start, end) }
        ) { query, type, time, catId, customDates ->
            FilterState(query, type, time, catId, customDates.first, customDates.second)
        }

        viewModelScope.launch {
            combine(
                transactionsFlow,
                categoryRepository.getAllCategories(),
                filtersFlow
            ) { transactions: List<Transaction>, categories: List<Category>, filters: FilterState ->
                val categoryMap = categories.associateBy { it.id }
                
                val enrichedTransactions = transactions.map { transaction ->
                    val category = categoryMap[transaction.categoryId]
                    TransactionWithCategory(
                        transaction = transaction,
                        categoryName = category?.name ?: "Unknown",
                        categoryColor = category?.color ?: 0xFF9CA3AF
                    )
                }
                
                // Get categories present in transactions
                val presentCategoryIds = transactions.mapNotNull { it.categoryId }.distinct()
                val availableCategories = presentCategoryIds.mapNotNull { catId ->
                    categoryMap[catId]?.let { cat ->
                        CategoryFilterItem(
                            id = cat.id,
                            name = cat.name,
                            color = cat.color
                        )
                    }
                }.sortedBy { it.name }
                
                // Apply filters
                val filtered = enrichedTransactions
                    .filter { filterByType(it.transaction, filters.typeFilter) }
                    .filter { filterByCategory(it.transaction, filters.categoryId) }
                    .filter { filterByTime(it.transaction, filters.timeFilter, filters.customStartDate, filters.customEndDate) }
                    .filter { filterBySearch(it, filters.searchQuery) }
                
                Pair(filtered, availableCategories)
            }.collect { (filtered, categories) ->
                _uiState.update { 
                    it.copy(
                        transactions = filtered,
                        availableCategories = categories,
                        isLoading = false
                    ) 
                }
            }
        }
    }
    
    private data class FilterState(
        val searchQuery: String,
        val typeFilter: TypeFilter,
        val timeFilter: TimeFilter,
        val categoryId: String?,
        val customStartDate: Long?,
        val customEndDate: Long?
    )
    
    private fun filterByType(transaction: Transaction, filter: TypeFilter): Boolean {
        return when (filter) {
            TypeFilter.ALL -> true
            TypeFilter.EXPENSE -> transaction.type == TransactionType.EXPENSE
            // Exclude adjustment transactions from income filter
            TypeFilter.INCOME -> transaction.type == TransactionType.INCOME && transaction.categoryId != "adjustment"
        }
    }
    
    private fun filterByCategory(transaction: Transaction, categoryId: String?): Boolean {
        if (categoryId == null) return true
        return transaction.categoryId == categoryId
    }
    
    private fun filterByTime(
        transaction: Transaction, 
        filter: TimeFilter, 
        customStart: Long?, 
        customEnd: Long?
    ): Boolean {
        val now = Calendar.getInstance()
        val transactionDate = Calendar.getInstance().apply { timeInMillis = transaction.date }
        
        return when (filter) {
            TimeFilter.ALL -> true
            TimeFilter.TODAY -> {
                transactionDate.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                transactionDate.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
            }
            TimeFilter.THIS_WEEK -> {
                transactionDate.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                transactionDate.get(Calendar.WEEK_OF_YEAR) == now.get(Calendar.WEEK_OF_YEAR)
            }
            TimeFilter.THIS_MONTH -> {
                transactionDate.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                transactionDate.get(Calendar.MONTH) == now.get(Calendar.MONTH)
            }
            TimeFilter.THIS_YEAR -> {
                transactionDate.get(Calendar.YEAR) == now.get(Calendar.YEAR)
            }
            TimeFilter.CUSTOM -> {
                val start = customStart ?: return true
                val end = customEnd ?: return true
                transaction.date in start..end
            }
        }
    }
    
    private fun filterBySearch(item: TransactionWithCategory, query: String): Boolean {
        if (query.isBlank()) return true
        return item.transaction.note?.contains(query, ignoreCase = true) == true ||
               item.categoryName.contains(query, ignoreCase = true)
    }
    
    fun updateSearchQuery(query: String) {
        searchQuery.value = query
        _uiState.update { it.copy(searchQuery = query) }
    }
    
    fun updateTypeFilter(filter: TypeFilter) {
        typeFilter.value = filter
        _uiState.update { it.copy(typeFilter = filter) }
    }
    
    fun updateTimeFilter(filter: TimeFilter) {
        timeFilter.value = filter
        _uiState.update { it.copy(timeFilter = filter) }
        
        // Show date picker if custom is selected
        if (filter == TimeFilter.CUSTOM) {
            _uiState.update { it.copy(showDatePicker = true, isSelectingStartDate = true) }
        }
    }
    
    fun updateCategoryFilter(categoryId: String?) {
        selectedCategoryId.value = categoryId
        _uiState.update { it.copy(selectedCategoryId = categoryId) }
    }
    
    fun showDatePicker(isStartDate: Boolean) {
        _uiState.update { it.copy(showDatePicker = true, isSelectingStartDate = isStartDate) }
    }
    
    fun hideDatePicker() {
        _uiState.update { it.copy(showDatePicker = false) }
    }
    
    fun setCustomStartDate(date: Long) {
        customStartDate.value = date
        _uiState.update { it.copy(customStartDate = date, isSelectingStartDate = false) }
    }
    
    fun setCustomEndDate(date: Long) {
        // Set end of day for end date
        val endOfDay = Calendar.getInstance().apply {
            timeInMillis = date
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
        
        customEndDate.value = endOfDay
        _uiState.update { it.copy(customEndDate = endOfDay, showDatePicker = false) }
    }
    
    fun clearCustomDates() {
        customStartDate.value = null
        customEndDate.value = null
        _uiState.update { 
            it.copy(
                customStartDate = null, 
                customEndDate = null,
                timeFilter = TimeFilter.ALL
            ) 
        }
        timeFilter.value = TimeFilter.ALL
    }
    
    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            transactionRepository.deleteTransaction(transaction)
        }
    }
}
