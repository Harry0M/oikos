package com.theblankstate.epmanager.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.theblankstate.epmanager.data.model.Transaction
import com.theblankstate.epmanager.data.model.TransactionType
import com.theblankstate.epmanager.data.model.Debt
import com.theblankstate.epmanager.data.model.DebtType
import com.theblankstate.epmanager.data.model.RecurringExpense
import com.theblankstate.epmanager.data.model.SavingsGoal
import com.theblankstate.epmanager.data.repository.AccountRepository
import com.theblankstate.epmanager.data.repository.CategoryRepository
import com.theblankstate.epmanager.data.repository.TransactionRepository
import com.theblankstate.epmanager.data.repository.RecurringExpenseRepository
import com.theblankstate.epmanager.data.repository.DebtRepository
import com.theblankstate.epmanager.data.repository.SavingsGoalRepository
import com.theblankstate.epmanager.data.repository.BudgetRepository
import com.theblankstate.epmanager.data.model.BudgetWithSpending
import com.theblankstate.epmanager.ui.transactions.TransactionWithCategory
import com.theblankstate.epmanager.util.LocationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Represents an upcoming due item that can be displayed on the home screen
 */
sealed class UpcomingDue(
    open val id: String,
    open val title: String,
    open val amount: Double,
    open val dueDate: Long,
    open val type: UpcomingDueType
) {
    data class DebtDue(
        override val id: String,
        override val title: String,
        override val amount: Double,
        override val dueDate: Long,
        val isCredit: Boolean,
        val personName: String
    ) : UpcomingDue(id, title, amount, dueDate, if (isCredit) UpcomingDueType.CREDIT else UpcomingDueType.DEBT)
    
    data class ExpenseDue(
        override val id: String,
        override val title: String,
        override val amount: Double,
        override val dueDate: Long,
        val isSettled: Boolean = false
    ) : UpcomingDue(id, title, amount, dueDate, UpcomingDueType.EXPENSE)
    
    data class SubscriptionDue(
        override val id: String,
        override val title: String,
        override val amount: Double,
        override val dueDate: Long,
        val isSettled: Boolean = false
    ) : UpcomingDue(id, title, amount, dueDate, UpcomingDueType.SUBSCRIPTION)
    
    data class GoalDue(
        override val id: String,
        override val title: String,
        override val amount: Double,
        override val dueDate: Long,
        val progress: Float
    ) : UpcomingDue(id, title, amount, dueDate, UpcomingDueType.GOAL)
}

data class AccountCardData(
    val id: String,
    val name: String,
    val type: String,
    val balance: Double,
    val spendingGraphData: List<Float>, // Daily spending (expense)
    val incomeGraphData: List<Float>,   // Daily income
    val todayIncome: Double,
    val todayExpense: Double
)

enum class UpcomingDueType {
    DEBT, CREDIT, EXPENSE, SUBSCRIPTION, GOAL
}

data class HomeUiState(
    val totalBalance: Double = 0.0,
    val monthlyExpenses: Double = 0.0,
    val monthlyIncome: Double = 0.0,
    val todaySpending: Double = 0.0,
    val recentTransactions: List<TransactionWithCategory> = emptyList(),
    val upcomingDues: List<UpcomingDue> = emptyList(),
    val selectedDue: UpcomingDue? = null,
    val accounts: List<com.theblankstate.epmanager.data.model.Account> = emptyList(),
    val accountCards: List<AccountCardData> = emptyList(),
    val budgetAlerts: List<BudgetWithSpending> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val debtRepository: DebtRepository,
    private val recurringRepository: RecurringExpenseRepository,
    private val savingsGoalRepository: SavingsGoalRepository,
    private val budgetRepository: BudgetRepository,
    private val locationHelper: LocationHelper
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    init {
        initializeData()
        loadDashboardData()
        loadUpcomingDues()
        loadBudgetAlerts()
        loadAccounts()
    }
    
    private fun initializeData() {
        viewModelScope.launch {
            // Initialize default categories and accounts if first run
            categoryRepository.initializeDefaultCategories()
            accountRepository.initializeDefaultAccounts()
        }
    }
    
    private fun loadDashboardData() {
        // Load recent transactions with category info
        viewModelScope.launch {
            combine(
                transactionRepository.getRecentTransactions(5),
                categoryRepository.getAllCategories()
            ) { transactions, categories ->
                val categoryMap = categories.associateBy { it.id }
                transactions.map { transaction ->
                    val category = categoryMap[transaction.categoryId]
                    TransactionWithCategory(
                        transaction = transaction,
                        categoryName = category?.name ?: "Unknown",
                        categoryColor = category?.color ?: 0xFF9CA3AF
                    )
                }
            }.collect { enrichedTransactions ->
                _uiState.update { it.copy(recentTransactions = enrichedTransactions) }
            }
        }
        
        // Load monthly expenses and calculate graph data
        viewModelScope.launch {
            val calendar = java.util.Calendar.getInstance()
            calendar.set(java.util.Calendar.DAY_OF_MONTH, 1)
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
            calendar.set(java.util.Calendar.MINUTE, 0)
            calendar.set(java.util.Calendar.SECOND, 0)
            calendar.set(java.util.Calendar.MILLISECOND, 0)
            
            val startOfMonth = calendar.timeInMillis
            val daysInMonth = calendar.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
            val monthRange = startOfMonth..(startOfMonth + (daysInMonth * 24L * 60 * 60 * 1000))
            
            transactionRepository.getTransactionsByDateRange(monthRange.first, monthRange.last)
                .collect { transactions ->
                    val expenses = transactions.filter { it.type == TransactionType.EXPENSE }
                    val incomes = transactions.filter { it.type == TransactionType.INCOME }
                    
                    // Total Monthly Stats
                    val totalExpense = expenses.sumOf { it.amount }
                    val totalIncome = incomes.sumOf { it.amount }
                    _uiState.update { it.copy(monthlyExpenses = totalExpense, monthlyIncome = totalIncome) }
                    
                    // Calculate Global Graph Data & Today's Stats
                    val daysInMonth = calendar.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
                    val dailyExpenses = FloatArray(daysInMonth) { 0f }
                    val dailyIncomes = FloatArray(daysInMonth) { 0f }
                    
                    // Get Today's Index
                    val todayCal = java.util.Calendar.getInstance()
                    val todayDayIndex = if (todayCal.get(java.util.Calendar.MONTH) == calendar.get(java.util.Calendar.MONTH)) 
                        todayCal.get(java.util.Calendar.DAY_OF_MONTH) - 1 
                    else -1

                    var totalTodayExpense = 0.0
                    var totalTodayIncome = 0.0
                    
                    transactions.forEach { txn ->
                        val dayIndex = java.util.Calendar.getInstance().apply { timeInMillis = txn.date }
                            .get(java.util.Calendar.DAY_OF_MONTH) - 1
                        
                        if (dayIndex in 0 until daysInMonth) {
                            if (txn.type == TransactionType.EXPENSE) {
                                dailyExpenses[dayIndex] += txn.amount.toFloat()
                                if (dayIndex == todayDayIndex) totalTodayExpense += txn.amount
                            } else if (txn.type == TransactionType.INCOME) {
                                dailyIncomes[dayIndex] += txn.amount.toFloat()
                                if (dayIndex == todayDayIndex) totalTodayIncome += txn.amount
                            }
                        }
                    }
                    
                    // Calculate Per-Account Data
                    val accountCards = _uiState.value.accounts.map { account ->
                        val accTransactions = transactions.filter { it.accountId == account.id }
                        val accDailyExpenses = FloatArray(daysInMonth) { 0f }
                        val accDailyIncomes = FloatArray(daysInMonth) { 0f }
                        var accTodayExpense = 0.0
                        var accTodayIncome = 0.0
                        
                        accTransactions.forEach { txn ->
                            val dayIndex = java.util.Calendar.getInstance().apply { timeInMillis = txn.date }
                                .get(java.util.Calendar.DAY_OF_MONTH) - 1
                            
                            if (dayIndex in 0 until daysInMonth) {
                                if (txn.type == TransactionType.EXPENSE) {
                                    accDailyExpenses[dayIndex] += txn.amount.toFloat()
                                    if (dayIndex == todayDayIndex) accTodayExpense += txn.amount
                                } else if (txn.type == TransactionType.INCOME) {
                                    accDailyIncomes[dayIndex] += txn.amount.toFloat()
                                    if (dayIndex == todayDayIndex) accTodayIncome += txn.amount
                                }
                            }
                        }
                        
                        AccountCardData(
                            id = account.id,
                            name = account.name,
                            type = account.type.name,
                            balance = account.balance,
                            spendingGraphData = accDailyExpenses.toList(),
                            incomeGraphData = accDailyIncomes.toList(),
                            todayIncome = accTodayIncome,
                            todayExpense = accTodayExpense
                        )
                    }
                    
                    // Create "All Accounts" card (Total Balance)
                    val totalCard = AccountCardData(
                        id = "all",
                        name = "Total Balance",
                        type = "TOTAL",
                        balance = _uiState.value.totalBalance,
                        spendingGraphData = dailyExpenses.toList(),
                        incomeGraphData = dailyIncomes.toList(),
                        todayIncome = totalTodayIncome,
                        todayExpense = totalTodayExpense
                    )
                    
                    _uiState.update { 
                        it.copy(accountCards = listOf(totalCard) + accountCards) 
                    }
                }
        }
        
        // Load monthly income
        viewModelScope.launch {
            transactionRepository.getMonthlyIncome()
                .collect { income ->
                    _uiState.update { it.copy(monthlyIncome = income ?: 0.0) }
                }
        }
        
        // Load today's spending
        viewModelScope.launch {
            transactionRepository.getTodayTransactions()
                .map { transactions ->
                    transactions
                        .filter { it.type == TransactionType.EXPENSE }
                        .sumOf { it.amount }
                }
                .collect { todayTotal ->
                    _uiState.update { it.copy(todaySpending = todayTotal, isLoading = false) }
                }
        }
        
        // Get total balance from ACCOUNTS table (source of truth)
        viewModelScope.launch {
            accountRepository.getTotalBalance()
                .collect { balance ->
                    _uiState.update { it.copy(totalBalance = balance ?: 0.0) }
                }
        }
    }
    
    private fun loadAccounts() {
        viewModelScope.launch {
            accountRepository.getAllAccounts().collect { accounts ->
                _uiState.update { it.copy(accounts = accounts) }
                // Trigger reload of dashboard data to update account cards
                loadDashboardData()
            }
        }
    }
    
    private fun loadUpcomingDues() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            
            // Combine all sources of upcoming dues
            combine(
                debtRepository.getActiveDebts(),
                recurringRepository.getAllRecurringExpenses(),
                savingsGoalRepository.getAllGoals()
            ) { debts, recurring, goals ->
                val upcomingList = mutableListOf<UpcomingDue>()
                val thirtyDaysLater = now + (30L * 24 * 60 * 60 * 1000)
                
                // Add debts/credits with due dates
                debts.filter { !it.isSettled && it.dueDate != null && it.dueDate in now..thirtyDaysLater }
                    .forEach { debt ->
                        upcomingList.add(
                            UpcomingDue.DebtDue(
                                id = debt.id,
                                title = if (debt.type == DebtType.DEBT) "Pay ${debt.personName}" else "Collect from ${debt.personName}",
                                amount = debt.remainingAmount,
                                dueDate = debt.dueDate!!,
                                isCredit = debt.type == DebtType.CREDIT,
                                personName = debt.personName
                            )
                        )
                    }
                
                // Add recurring expenses with frequency-based visibility logic
                recurring.filter { it.isActive }.forEach { expense ->
                    val timeDiff = expense.nextDueDate - now
                    val daysUntil = (timeDiff / (24 * 60 * 60 * 1000)).toInt()
                    
                    var shouldShow = false
                    var isSettledForToday = false
                    
                    when (expense.frequency) {
                        com.theblankstate.epmanager.data.model.RecurringFrequency.YEARLY -> {
                            shouldShow = daysUntil <= 30
                        }
                        com.theblankstate.epmanager.data.model.RecurringFrequency.MONTHLY,
                        com.theblankstate.epmanager.data.model.RecurringFrequency.QUARTERLY -> {
                            shouldShow = daysUntil <= 7
                        }
                        com.theblankstate.epmanager.data.model.RecurringFrequency.WEEKLY,
                        com.theblankstate.epmanager.data.model.RecurringFrequency.BIWEEKLY -> {
                            shouldShow = daysUntil <= 2
                        }
                        com.theblankstate.epmanager.data.model.RecurringFrequency.DAILY -> {
                            shouldShow = true
                            // Check if processed today
                            val lastProcessed = expense.lastProcessedDate
                            if (lastProcessed != null) {
                                val procCal = java.util.Calendar.getInstance().apply { timeInMillis = lastProcessed }
                                val nowCal = java.util.Calendar.getInstance().apply { timeInMillis = now }
                                
                                if (procCal.get(java.util.Calendar.YEAR) == nowCal.get(java.util.Calendar.YEAR) &&
                                    procCal.get(java.util.Calendar.DAY_OF_YEAR) == nowCal.get(java.util.Calendar.DAY_OF_YEAR)) {
                                    isSettledForToday = true
                                }
                            }
                        }
                    }
                    
                    if (daysUntil < 0 && !isSettledForToday) shouldShow = true
                    
                    if (shouldShow) {
                        val isSubscription = expense.frequency in listOf(
                            com.theblankstate.epmanager.data.model.RecurringFrequency.MONTHLY,
                            com.theblankstate.epmanager.data.model.RecurringFrequency.WEEKLY,
                            com.theblankstate.epmanager.data.model.RecurringFrequency.YEARLY,
                            com.theblankstate.epmanager.data.model.RecurringFrequency.QUARTERLY,
                            com.theblankstate.epmanager.data.model.RecurringFrequency.BIWEEKLY
                        )
                        
                        if (isSubscription) {
                            upcomingList.add(
                                UpcomingDue.SubscriptionDue(
                                    id = expense.id,
                                    title = expense.name,
                                    amount = expense.amount,
                                    dueDate = expense.nextDueDate,
                                    isSettled = isSettledForToday
                                )
                            )
                        } else {
                            upcomingList.add(
                                UpcomingDue.ExpenseDue(
                                    id = expense.id,
                                    title = expense.name,
                                    amount = expense.amount,
                                    dueDate = expense.nextDueDate,
                                    isSettled = isSettledForToday
                                )
                            )
                        }
                    }
                }
                
                // Add goals with target dates
                goals.filter { !it.isCompleted && it.targetDate != null && it.targetDate in now..thirtyDaysLater }
                    .forEach { goal ->
                        upcomingList.add(
                            UpcomingDue.GoalDue(
                                id = goal.id,
                                title = goal.name,
                                amount = goal.remaining,
                                dueDate = goal.targetDate!!,
                                progress = goal.progress
                            )
                        )
                    }
                
                // Sort by due date
                upcomingList.sortedBy { it.dueDate }
            }.collect { upcomingDues ->
                _uiState.update { it.copy(upcomingDues = upcomingDues) }
            }
        }
    }
    
    private fun loadBudgetAlerts() {
        viewModelScope.launch {
            budgetRepository.getBudgetsWithSpending()
                .collect { budgets ->
                    // Filter budgets that exceed the alert threshold (e.g. > 80%)
                    val alerts = budgets.filter { 
                        it.percentage >= (it.budget.alertThreshold * 100) 
                    }.sortedByDescending { it.percentage }
                    
                    _uiState.update { it.copy(budgetAlerts = alerts) }
                }
        }
    }
    
    fun selectDue(due: UpcomingDue) {
        _uiState.update { it.copy(selectedDue = due) }
    }
    
    fun dismissDueSheet() {
        _uiState.update { it.copy(selectedDue = null) }
    }
    
    /**
     * Pay subscription/recurring expense early
     * Creates a transaction and updates the next due date
     */
    fun paySubscriptionEarly(due: UpcomingDue, accountId: String? = null) {
        viewModelScope.launch {
            when (due) {
                is UpcomingDue.SubscriptionDue, is UpcomingDue.ExpenseDue -> {
                    // Get the recurring expense
                    val expense = recurringRepository.getRecurringExpenseById(due.id)
                    if (expense != null) {
                        // Use provided accountId or fall back to default account
                        val finalAccountId = accountId ?: accountRepository.getDefaultAccount()?.id ?: "cash"
                        
                        // Try to get location
                        val location = locationHelper.getCurrentLocation()
                        val locationName = location?.let { locationHelper.getLocationName(it) }
                        
                        val transaction = Transaction(
                            amount = due.amount,
                            categoryId = expense.categoryId ?: "expense",
                            accountId = finalAccountId,
                            type = TransactionType.EXPENSE,
                            date = System.currentTimeMillis(),
                            note = "Early payment: ${due.title}",
                            recurringId = due.id,
                            latitude = location?.latitude,
                            longitude = location?.longitude,
                            locationName = locationName
                        )
                        transactionRepository.insertTransaction(transaction)
                        
                        // Update account balance
                        accountRepository.updateBalance(finalAccountId, -due.amount)
                        
                        // Calculate and update next due date
                        val calendar = java.util.Calendar.getInstance()
                        when (expense.frequency) {
                            com.theblankstate.epmanager.data.model.RecurringFrequency.DAILY -> 
                                calendar.add(java.util.Calendar.DAY_OF_MONTH, 1)
                            com.theblankstate.epmanager.data.model.RecurringFrequency.WEEKLY -> 
                                calendar.add(java.util.Calendar.WEEK_OF_YEAR, 1)
                            com.theblankstate.epmanager.data.model.RecurringFrequency.BIWEEKLY -> 
                                calendar.add(java.util.Calendar.WEEK_OF_YEAR, 2)
                            com.theblankstate.epmanager.data.model.RecurringFrequency.MONTHLY -> 
                                calendar.add(java.util.Calendar.MONTH, 1)
                            com.theblankstate.epmanager.data.model.RecurringFrequency.QUARTERLY -> 
                                calendar.add(java.util.Calendar.MONTH, 3)
                            com.theblankstate.epmanager.data.model.RecurringFrequency.YEARLY -> 
                                calendar.add(java.util.Calendar.YEAR, 1)
                        }
                        
                        // Set to schedule day
                        if (expense.frequency in listOf(
                            com.theblankstate.epmanager.data.model.RecurringFrequency.MONTHLY,
                            com.theblankstate.epmanager.data.model.RecurringFrequency.QUARTERLY
                        )) {
                            calendar.set(java.util.Calendar.DAY_OF_MONTH, 
                                minOf(expense.scheduleDay, calendar.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)))
                        }
                        
                        recurringRepository.updateNextDueDate(due.id, calendar.timeInMillis)
                    }
                }
                else -> { }
            }
            dismissDueSheet()
            loadUpcomingDues()
        }
    }
    
    /**
     * Record debt payment
     */
    fun payDebt(dueId: String, amount: Double, note: String?, accountId: String? = null) {
        viewModelScope.launch {
            val debt = debtRepository.getDebtById(dueId)
            if (debt != null) {
                val categoryId = if (debt.type == DebtType.DEBT) "debt_payment" else "credit_received"
                val transactionType = if (debt.type == DebtType.DEBT) TransactionType.EXPENSE else TransactionType.INCOME
                val transactionNote = note ?: if (debt.type == DebtType.DEBT) 
                    "Payment to ${debt.personName}" 
                else 
                    "Received from ${debt.personName}"
                
                // Use provided accountId or fall back to default account
                val finalAccountId = accountId ?: accountRepository.getDefaultAccount()?.id ?: "cash"
                
                // Try to get location
                val location = locationHelper.getCurrentLocation()
                val locationName = location?.let { locationHelper.getLocationName(it) }
                
                val transaction = Transaction(
                    amount = amount,
                    categoryId = categoryId,
                    accountId = finalAccountId,
                    type = transactionType,
                    date = System.currentTimeMillis(),
                    note = transactionNote,
                    latitude = location?.latitude,
                    longitude = location?.longitude,
                    locationName = locationName
                )
                transactionRepository.insertTransaction(transaction)
                
                val balanceChange = if (transactionType == TransactionType.EXPENSE) -amount else amount
                accountRepository.updateBalance(finalAccountId, balanceChange)
                
                debtRepository.addPayment(debt.id, amount, transaction.id, note)
            }
            dismissDueSheet()
            loadUpcomingDues()
        }
    }
    
    fun refresh() {
        _uiState.update { it.copy(isLoading = true) }
        loadDashboardData()
        loadUpcomingDues()
    }
}
