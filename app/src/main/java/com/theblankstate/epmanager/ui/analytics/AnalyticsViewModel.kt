package com.theblankstate.epmanager.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.theblankstate.epmanager.data.model.Category
import com.theblankstate.epmanager.data.model.CategoryType
import com.theblankstate.epmanager.data.model.TransactionType
import com.theblankstate.epmanager.data.repository.CategoryRepository
import com.theblankstate.epmanager.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class CategorySpending(
    val category: Category,
    val amount: Double,
    val percentage: Float
)

data class MonthlyData(
    val month: String,
    val expense: Double,
    val income: Double
)

data class AnalyticsUiState(
    val totalExpenses: Double = 0.0,
    val totalIncome: Double = 0.0,
    val categorySpending: List<CategorySpending> = emptyList(),
    val monthlyData: List<MonthlyData> = emptyList(),
    val topCategory: CategorySpending? = null,
    val averageDailySpend: Double = 0.0,
    val selectedPeriod: AnalyticsPeriod = AnalyticsPeriod.THIS_MONTH,
    val isLoading: Boolean = true
)

enum class AnalyticsPeriod(val label: String) {
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    LAST_3_MONTHS("3 Months"),
    THIS_YEAR("This Year")
}

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()
    
    init {
        loadAnalytics()
    }
    
    fun selectPeriod(period: AnalyticsPeriod) {
        _uiState.update { it.copy(selectedPeriod = period, isLoading = true) }
        loadAnalytics()
    }
    
    private fun loadAnalytics() {
        val period = _uiState.value.selectedPeriod
        val (startDate, endDate) = getDateRange(period)
        
        // Load transactions for the period
        viewModelScope.launch {
            transactionRepository.getTransactionsByDateRange(startDate, endDate)
                .combine(categoryRepository.getCategoriesByType(CategoryType.EXPENSE)) { transactions, categories ->
                    // Calculate totals
                    val expenses = transactions
                        .filter { it.type == TransactionType.EXPENSE }
                        .sumOf { it.amount }
                    
                    val income = transactions
                        .filter { it.type == TransactionType.INCOME }
                        .sumOf { it.amount }
                    
                    // Calculate category spending
                    val categoryMap = categories.associateBy { it.id }
                    val categoryTotals = transactions
                        .filter { it.type == TransactionType.EXPENSE && it.categoryId != null }
                        .groupBy { it.categoryId }
                        .mapNotNull { (categoryId, txns) ->
                            categoryMap[categoryId]?.let { category ->
                                val amount = txns.sumOf { it.amount }
                                CategorySpending(
                                    category = category,
                                    amount = amount,
                                    percentage = if (expenses > 0) (amount / expenses * 100).toFloat() else 0f
                                )
                            }
                        }
                        .sortedByDescending { it.amount }
                    
                    // Calculate average daily spend
                    val daysDiff = ((endDate - startDate) / (1000 * 60 * 60 * 24)).coerceAtLeast(1)
                    val avgDaily = expenses / daysDiff
                    
                    AnalyticsUiState(
                        totalExpenses = expenses,
                        totalIncome = income,
                        categorySpending = categoryTotals,
                        topCategory = categoryTotals.firstOrNull(),
                        averageDailySpend = avgDaily,
                        selectedPeriod = period,
                        isLoading = false
                    )
                }
                .collect { state ->
                    _uiState.update { state }
                }
        }
        
        // Load monthly comparison data
        viewModelScope.launch {
            val monthlyData = mutableListOf<MonthlyData>()
            val calendar = Calendar.getInstance()
            
            repeat(6) { monthsAgo ->
                calendar.timeInMillis = System.currentTimeMillis()
                calendar.add(Calendar.MONTH, -monthsAgo)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                val monthStart = calendar.timeInMillis
                
                calendar.add(Calendar.MONTH, 1)
                val monthEnd = calendar.timeInMillis
                
                val monthName = java.text.SimpleDateFormat("MMM", java.util.Locale.getDefault())
                    .format(java.util.Date(monthStart))
                
                transactionRepository.getTransactionsByDateRange(monthStart, monthEnd)
                    .first()
                    .let { transactions ->
                        val expense = transactions
                            .filter { it.type == TransactionType.EXPENSE }
                            .sumOf { it.amount }
                        val income = transactions
                            .filter { it.type == TransactionType.INCOME }
                            .sumOf { it.amount }
                        
                        monthlyData.add(MonthlyData(monthName, expense, income))
                    }
            }
            
            _uiState.update { it.copy(monthlyData = monthlyData.reversed()) }
        }
    }
    
    private fun getDateRange(period: AnalyticsPeriod): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        val endDate = calendar.timeInMillis
        
        when (period) {
            AnalyticsPeriod.THIS_WEEK -> {
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
            }
            AnalyticsPeriod.THIS_MONTH -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
            }
            AnalyticsPeriod.LAST_3_MONTHS -> {
                calendar.add(Calendar.MONTH, -3)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
            }
            AnalyticsPeriod.THIS_YEAR -> {
                calendar.set(Calendar.DAY_OF_YEAR, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
            }
        }
        
        return calendar.timeInMillis to endDate
    }
}
