package com.theblankstate.epmanager.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.theblankstate.epmanager.data.model.Account
import com.theblankstate.epmanager.data.model.Category
import com.theblankstate.epmanager.data.model.CategoryType
import com.theblankstate.epmanager.data.model.SavingsGoal
import com.theblankstate.epmanager.data.model.Transaction
import com.theblankstate.epmanager.data.model.TransactionType
import com.theblankstate.epmanager.data.repository.AccountRepository
import com.theblankstate.epmanager.data.repository.BudgetRepository
import com.theblankstate.epmanager.data.repository.CategoryRepository
import com.theblankstate.epmanager.data.repository.DebtRepository
import com.theblankstate.epmanager.data.repository.SavingsGoalRepository
import com.theblankstate.epmanager.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

// ========== DATA CLASSES ==========

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

data class AccountBalance(
    val account: Account,
    val balance: Double,
    val percentage: Float
)

data class DailySpending(
    val date: Long,
    val dayLabel: String,
    val amount: Double
)

data class GoalProgress(
    val goal: SavingsGoal,
    val progress: Float
)

data class TopTransaction(
    val transaction: Transaction,
    val categoryName: String?,
    val categoryColor: Long?
)

data class AnalyticsUiState(
    // Basic Stats
    val totalExpenses: Double = 0.0,
    val totalIncome: Double = 0.0,
    val categorySpending: List<CategorySpending> = emptyList(),
    val monthlyData: List<MonthlyData> = emptyList(),
    val topCategory: CategorySpending? = null,
    val averageDailySpend: Double = 0.0,
    val selectedPeriod: AnalyticsPeriod = AnalyticsPeriod.THIS_MONTH,
    val isLoading: Boolean = true,
    
    // Financial Health
    val netWorth: Double = 0.0,
    val savingsRate: Double = 0.0,
    val healthScore: Int = 0,
    
    // Goals Progress
    val activeGoals: List<GoalProgress> = emptyList(),
    val totalGoalsSaved: Double = 0.0,
    val totalGoalsTarget: Double = 0.0,
    val goalsProgress: Float = 0.0f,
    
    // Debt Summary
    val totalDebtOwed: Double = 0.0,
    val totalCreditOwed: Double = 0.0,
    
    // Budget Utilization
    val totalBudget: Double = 0.0,
    val totalBudgetSpent: Double = 0.0,
    val budgetUtilization: Float = 0.0f,
    val overBudgetCategories: Int = 0,
    
    // Account Breakdown
    val accountBalances: List<AccountBalance> = emptyList(),
    val totalAccountBalance: Double = 0.0,
    
    // Spending Patterns
    val dailySpending: List<DailySpending> = emptyList(),
    val topTransactions: List<TopTransaction> = emptyList(),
    val transactionCount: Int = 0
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
    private val categoryRepository: CategoryRepository,
    private val savingsGoalRepository: SavingsGoalRepository,
    private val debtRepository: DebtRepository,
    private val budgetRepository: BudgetRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()
    
    init {
        loadAllAnalytics()
    }
    
    fun selectPeriod(period: AnalyticsPeriod) {
        _uiState.update { it.copy(selectedPeriod = period, isLoading = true) }
        loadAllAnalytics()
    }
    
    private fun loadAllAnalytics() {
        loadBasicAnalytics()
        loadMonthlyData()
        loadFinancialHealth()
        loadGoalsProgress()
        loadDebtSummary()
        loadBudgetUtilization()
        loadAccountBreakdown()
        loadDailySpending()
    }
    
    private fun loadBasicAnalytics() {
        val period = _uiState.value.selectedPeriod
        val (startDate, endDate) = getDateRange(period)
        
        viewModelScope.launch {
            transactionRepository.getTransactionsByDateRange(startDate, endDate)
                .combine(categoryRepository.getCategoriesByType(CategoryType.EXPENSE)) { transactions, categories ->
                    val expenses = transactions
                        .filter { it.type == TransactionType.EXPENSE }
                        .sumOf { it.amount }
                    
                    val income = transactions
                        .filter { it.type == TransactionType.INCOME }
                        .sumOf { it.amount }
                    
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
                    
                    val daysDiff = ((endDate - startDate) / (1000 * 60 * 60 * 24)).coerceAtLeast(1)
                    val avgDaily = expenses / daysDiff
                    
                    // Calculate savings rate
                    val savingsRate = if (income > 0) ((income - expenses) / income * 100) else 0.0
                    
                    // Get top 5 transactions
                    val topTxns = transactions
                        .filter { it.type == TransactionType.EXPENSE }
                        .sortedByDescending { it.amount }
                        .take(5)
                        .map { txn ->
                            val cat = txn.categoryId?.let { categoryMap[it] }
                            TopTransaction(txn, cat?.name, cat?.color)
                        }
                    
                    _uiState.update { current ->
                        current.copy(
                            totalExpenses = expenses,
                            totalIncome = income,
                            categorySpending = categoryTotals,
                            topCategory = categoryTotals.firstOrNull(),
                            averageDailySpend = avgDaily,
                            savingsRate = savingsRate,
                            selectedPeriod = period,
                            topTransactions = topTxns,
                            transactionCount = transactions.size,
                            isLoading = false
                        )
                    }
                }
                .collect { }
        }
    }
    
    private fun loadMonthlyData() {
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
                
                val monthName = SimpleDateFormat("MMM", Locale.getDefault())
                    .format(Date(monthStart))
                
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
    
    private fun loadFinancialHealth() {
        viewModelScope.launch {
            combine(
                accountRepository.getTotalBalance(),
                savingsGoalRepository.getTotalSaved(),
                debtRepository.getTotalDebtOwed(),
                debtRepository.getTotalCreditOwed()
            ) { totalBalance, goalsSaved, debtOwed, creditOwed ->
                val balance = totalBalance ?: 0.0
                val saved = goalsSaved ?: 0.0
                val debt = debtOwed ?: 0.0
                val credit = creditOwed ?: 0.0
                
                // Net Worth = Balance + Goals Saved + Credit Owed to You - Debt
                val netWorth = balance + saved + credit - debt
                
                // Calculate health score (0-100)
                val healthScore = calculateHealthScore(
                    netWorth = netWorth,
                    savingsRate = _uiState.value.savingsRate,
                    debtRatio = if (balance > 0) debt / balance else 0.0
                )
                
                _uiState.update { current ->
                    current.copy(
                        netWorth = netWorth,
                        healthScore = healthScore,
                        totalAccountBalance = balance
                    )
                }
            }.collect { }
        }
    }
    
    private fun calculateHealthScore(netWorth: Double, savingsRate: Double, debtRatio: Double): Int {
        var score = 50 // Base score
        
        // Net worth factor (+/- 20 points)
        score += when {
            netWorth > 100000 -> 20
            netWorth > 50000 -> 15
            netWorth > 10000 -> 10
            netWorth > 0 -> 5
            netWorth > -10000 -> -5
            else -> -15
        }
        
        // Savings rate factor (+/- 20 points)
        score += when {
            savingsRate >= 30 -> 20
            savingsRate >= 20 -> 15
            savingsRate >= 10 -> 10
            savingsRate >= 0 -> 0
            else -> -15
        }
        
        // Debt ratio factor (+/- 10 points)
        score += when {
            debtRatio <= 0.1 -> 10
            debtRatio <= 0.3 -> 5
            debtRatio <= 0.5 -> 0
            else -> -10
        }
        
        return score.coerceIn(0, 100)
    }
    
    private fun loadGoalsProgress() {
        viewModelScope.launch {
            combine(
                savingsGoalRepository.getActiveGoals(),
                savingsGoalRepository.getTotalSaved(),
                savingsGoalRepository.getTotalTarget()
            ) { goals, totalSaved, totalTarget ->
                val saved = totalSaved ?: 0.0
                val target = totalTarget ?: 0.0
                val progress = if (target > 0) (saved / target * 100).toFloat() else 0f
                
                val goalProgressList = goals.map { goal ->
                    val goalProgress = if (goal.targetAmount > 0) {
                        (goal.savedAmount / goal.targetAmount * 100).toFloat()
                    } else 0f
                    GoalProgress(goal, goalProgress)
                }
                
                _uiState.update { current ->
                    current.copy(
                        activeGoals = goalProgressList,
                        totalGoalsSaved = saved,
                        totalGoalsTarget = target,
                        goalsProgress = progress
                    )
                }
            }.collect { }
        }
    }
    
    private fun loadDebtSummary() {
        viewModelScope.launch {
            combine(
                debtRepository.getTotalDebtOwed(),
                debtRepository.getTotalCreditOwed()
            ) { debtOwed, creditOwed ->
                _uiState.update { current ->
                    current.copy(
                        totalDebtOwed = debtOwed ?: 0.0,
                        totalCreditOwed = creditOwed ?: 0.0
                    )
                }
            }.collect { }
        }
    }
    
    private fun loadBudgetUtilization() {
        viewModelScope.launch {
            budgetRepository.getBudgetsWithSpending()
                .collect { budgets ->
                    val totalBudget = budgets.sumOf { it.budget.amount }
                    val totalSpent = budgets.sumOf { it.spent }
                    val utilization = if (totalBudget > 0) {
                        (totalSpent / totalBudget * 100).toFloat()
                    } else 0f
                    val overBudget = budgets.count { it.spent > it.budget.amount }
                    
                    _uiState.update { current ->
                        current.copy(
                            totalBudget = totalBudget,
                            totalBudgetSpent = totalSpent,
                            budgetUtilization = utilization,
                            overBudgetCategories = overBudget
                        )
                    }
                }
        }
    }
    
    private fun loadAccountBreakdown() {
        viewModelScope.launch {
            accountRepository.getAllAccounts()
                .collect { accounts ->
                    val totalBalance = accounts.sumOf { it.balance }
                    val balances = accounts
                        .filter { it.balance != 0.0 }
                        .sortedByDescending { kotlin.math.abs(it.balance) }
                        .map { account ->
                            val percentage = if (totalBalance > 0) {
                                (account.balance / totalBalance * 100).toFloat()
                            } else 0f
                            AccountBalance(account, account.balance, percentage)
                        }
                    
                    _uiState.update { current ->
                        current.copy(
                            accountBalances = balances,
                            totalAccountBalance = totalBalance
                        )
                    }
                }
        }
    }
    
    private fun loadDailySpending() {
        viewModelScope.launch {
            val dailyData = mutableListOf<DailySpending>()
            val calendar = Calendar.getInstance()
            val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
            
            // Get last 7 days
            repeat(7) { daysAgo ->
                calendar.timeInMillis = System.currentTimeMillis()
                calendar.add(Calendar.DAY_OF_YEAR, -daysAgo)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                val dayStart = calendar.timeInMillis
                
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                val dayEnd = calendar.timeInMillis
                
                val dayLabel = dayFormat.format(Date(dayStart))
                
                transactionRepository.getTransactionsByDateRange(dayStart, dayEnd)
                    .first()
                    .let { transactions ->
                        val spending = transactions
                            .filter { it.type == TransactionType.EXPENSE }
                            .sumOf { it.amount }
                        
                        dailyData.add(DailySpending(dayStart, dayLabel, spending))
                    }
            }
            
            _uiState.update { it.copy(dailySpending = dailyData.reversed()) }
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
