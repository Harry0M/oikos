package com.theblankstate.epmanager.ui.transactions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.theblankstate.epmanager.data.model.Account
import com.theblankstate.epmanager.data.model.AccountType
import com.theblankstate.epmanager.data.model.Category
import com.theblankstate.epmanager.data.model.Transaction
import com.theblankstate.epmanager.data.model.TransactionType
import com.theblankstate.epmanager.data.repository.AccountRepository
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
 * Account info for filter chips
 */
data class AccountFilterItem(
    val id: String,
    val name: String,
    val type: AccountType,
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

/**
 * Filter options for payment method (account type)
 */
enum class PaymentMethodFilter(val label: String) {
    ALL("All"),
    CASH("Cash"),
    BANK("Bank"),
    UPI("UPI"),
    CREDIT_CARD("Credit Card"),
    WALLET("Wallet"),
    OTHER("Other")
}

data class TransactionsUiState(
    val transactions: List<TransactionWithCategory> = emptyList(),
    val availableCategories: List<CategoryFilterItem> = emptyList(),
    val availableAccounts: List<AccountFilterItem> = emptyList(),
    val searchQuery: String = "",
    val typeFilter: TypeFilter = TypeFilter.ALL,
    val timeFilter: TimeFilter = TimeFilter.ALL,
    val selectedCategoryId: String? = null,
    val selectedAccountId: String? = null,
    val paymentMethodFilter: PaymentMethodFilter = PaymentMethodFilter.ALL,
    val minAmount: Double? = null,
    val maxAmount: Double? = null,
    val customStartDate: Long? = null,
    val customEndDate: Long? = null,
    val showDatePicker: Boolean = false,
    val isSelectingStartDate: Boolean = true,
    val showFilters: Boolean = false,
    val isLoading: Boolean = true
)

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(TransactionsUiState())
    val uiState: StateFlow<TransactionsUiState> = _uiState.asStateFlow()
    
    private val searchQuery = MutableStateFlow("")
    private val typeFilter = MutableStateFlow(TypeFilter.ALL)
    private val timeFilter = MutableStateFlow(TimeFilter.ALL)
    private val selectedCategoryId = MutableStateFlow<String?>(null)
    private val selectedAccountId = MutableStateFlow<String?>(null)
    private val paymentMethodFilter = MutableStateFlow(PaymentMethodFilter.ALL)
    private val minAmount = MutableStateFlow<Double?>(null)
    private val maxAmount = MutableStateFlow<Double?>(null)
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
        // Combine filter states into a single flow
        val filtersFlow = combine(
            combine(
                searchQuery,
                typeFilter,
                timeFilter,
                selectedCategoryId,
                selectedAccountId
            ) { query, type, time, catId, accId ->
                FilterGroup1(query, type, time, catId, accId)
            },
            combine(
                paymentMethodFilter,
                minAmount,
                maxAmount,
                customStartDate,
                customEndDate
            ) { payMethod, min, max, start, end ->
                FilterGroup2(payMethod, min, max, start, end)
            }
        ) { group1, group2 ->
            FilterState(
                searchQuery = group1.searchQuery,
                typeFilter = group1.typeFilter,
                timeFilter = group1.timeFilter,
                categoryId = group1.categoryId,
                accountId = group1.accountId,
                paymentMethodFilter = group2.paymentMethodFilter,
                minAmount = group2.minAmount,
                maxAmount = group2.maxAmount,
                customStartDate = group2.customStartDate,
                customEndDate = group2.customEndDate
            )
        }


        viewModelScope.launch {
            combine(
                transactionsFlow,
                categoryRepository.getAllCategories(),
                accountRepository.getAllAccounts(),
                filtersFlow
            ) { transactions: List<Transaction>, categories: List<Category>, accounts: List<Account>, filters: FilterState ->
                val categoryMap = categories.associateBy { it.id }
                val accountMap = accounts.associateBy { it.id }
                
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
                
                // Get accounts present in transactions
                val presentAccountIds = transactions.mapNotNull { it.accountId }.distinct()
                val availableAccounts = presentAccountIds.mapNotNull { accId ->
                    accountMap[accId]?.let { acc ->
                        AccountFilterItem(
                            id = acc.id,
                            name = acc.name,
                            type = acc.type,
                            color = acc.color
                        )
                    }
                }.sortedBy { it.name }
                
                // Apply filters
                val filtered = enrichedTransactions
                    .filter { filterByType(it.transaction, filters.typeFilter) }
                    .filter { filterByCategory(it.transaction, filters.categoryId) }
                    .filter { filterByAccount(it.transaction, filters.accountId) }
                    .filter { filterByPaymentMethod(it.transaction, filters.paymentMethodFilter, accountMap) }
                    .filter { filterByAmountRange(it.transaction, filters.minAmount, filters.maxAmount) }
                    .filter { filterByTime(it.transaction, filters.timeFilter, filters.customStartDate, filters.customEndDate) }
                    .filter { filterBySearch(it, filters.searchQuery) }
                
                Triple(filtered, availableCategories, availableAccounts)
            }.collect { (filtered, categories, accounts) ->
                _uiState.update { 
                    it.copy(
                        transactions = filtered,
                        availableCategories = categories,
    
                        availableAccounts = accounts,
                        isLoading = false
                    ) 
                }
            }
        }
    }
    
    
    private data class FilterGroup1(
        val searchQuery: String,
        val typeFilter: TypeFilter,
        val timeFilter: TimeFilter,
        val categoryId: String?,
        val accountId: String?
    )
    
    private data class FilterGroup2(
        val paymentMethodFilter: PaymentMethodFilter,
        val minAmount: Double?,
        val maxAmount: Double?,
        val customStartDate: Long?,
        val customEndDate: Long?
    )
    

    private data class FilterState(
        val searchQuery: String,
        val typeFilter: TypeFilter,
        val timeFilter: TimeFilter,
        val categoryId: String?,
        val accountId: String?,
        val paymentMethodFilter: PaymentMethodFilter,
        val minAmount: Double?,
        val maxAmount: Double?,
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
    
    private fun filterByAccount(transaction: Transaction, accountId: String?): Boolean {
        if (accountId == null) return true
        return transaction.accountId == accountId
    }
    
    private fun filterByPaymentMethod(
        transaction: Transaction, 
        filter: PaymentMethodFilter,
        accountMap: Map<String, Account>
    ): Boolean {
        if (filter == PaymentMethodFilter.ALL) return true
        
        val account = transaction.accountId?.let { accountMap[it] }
        val accountType = account?.type ?: return false
        
        return when (filter) {
            PaymentMethodFilter.ALL -> true
            PaymentMethodFilter.CASH -> accountType == AccountType.CASH
            PaymentMethodFilter.BANK -> accountType == AccountType.BANK
            PaymentMethodFilter.UPI -> accountType == AccountType.UPI
            PaymentMethodFilter.CREDIT_CARD -> accountType == AccountType.CREDIT_CARD
            PaymentMethodFilter.WALLET -> accountType == AccountType.WALLET
            PaymentMethodFilter.OTHER -> accountType == AccountType.OTHER
        }
    }
    
    private fun filterByAmountRange(
        transaction: Transaction,
        minAmount: Double?,
        maxAmount: Double?
    ): Boolean {
        val amount = transaction.amount
        
        return when {
            minAmount != null && maxAmount != null -> amount >= minAmount && amount <= maxAmount
            minAmount != null -> amount >= minAmount
            maxAmount != null -> amount <= maxAmount
            else -> true
        }
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
    
    fun updateAccountFilter(accountId: String?) {
        selectedAccountId.value = accountId
        _uiState.update { it.copy(selectedAccountId = accountId) }
    }
    
    fun updatePaymentMethodFilter(filter: PaymentMethodFilter) {
        paymentMethodFilter.value = filter
        _uiState.update { it.copy(paymentMethodFilter = filter) }
    }
    
    fun updateMinAmount(amount: Double?) {
        minAmount.value = amount
        _uiState.update { it.copy(minAmount = amount) }
    }
    
    fun updateMaxAmount(amount: Double?) {
        maxAmount.value = amount
        _uiState.update { it.copy(maxAmount = amount) }
    }
    
    fun clearAllFilters() {
        searchQuery.value = ""
        typeFilter.value = TypeFilter.ALL
        timeFilter.value = TimeFilter.ALL
        selectedCategoryId.value = null
        selectedAccountId.value = null
        paymentMethodFilter.value = PaymentMethodFilter.ALL
        minAmount.value = null
        maxAmount.value = null
        customStartDate.value = null
        customEndDate.value = null
        
        _uiState.update {
            it.copy(
                searchQuery = "",
                typeFilter = TypeFilter.ALL,
                timeFilter = TimeFilter.ALL,
                selectedCategoryId = null,
                selectedAccountId = null,
                paymentMethodFilter = PaymentMethodFilter.ALL,
                minAmount = null,
                maxAmount = null,
                customStartDate = null,
                customEndDate = null,
                showDatePicker = false
            )
        }
    }

    fun toggleFilters() {
        _uiState.update { it.copy(showFilters = !it.showFilters) }
    }

    
    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            transactionRepository.deleteTransaction(transaction)
        }
    }
}
