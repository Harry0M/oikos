package com.theblankstate.epmanager.data.repository

import com.theblankstate.epmanager.data.local.dao.BudgetDao
import com.theblankstate.epmanager.data.local.dao.TransactionDao
import com.theblankstate.epmanager.data.model.Budget
import com.theblankstate.epmanager.data.model.BudgetPeriod
import com.theblankstate.epmanager.data.model.BudgetWithSpending
import com.theblankstate.epmanager.data.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BudgetRepository @Inject constructor(
    private val budgetDao: BudgetDao,
    private val transactionDao: TransactionDao,
    private val categoryRepository: CategoryRepository
) {
    fun getActiveBudgets(): Flow<List<Budget>> =
        budgetDao.getActiveBudgets()
    
    fun getAllBudgets(): Flow<List<Budget>> =
        budgetDao.getAllBudgets()
    
    suspend fun getBudgetById(id: String): Budget? =
        budgetDao.getBudgetById(id)
    
    suspend fun getBudgetByCategory(categoryId: String): Budget? =
        budgetDao.getBudgetByCategory(categoryId)
    
    suspend fun insertBudget(budget: Budget) {
        budgetDao.insertBudget(budget)
    }
    
    suspend fun updateBudget(budget: Budget) {
        budgetDao.updateBudget(budget)
    }
    
    suspend fun deleteBudget(budget: Budget) {
        budgetDao.deleteBudget(budget)
    }
    
    /**
     * Get budgets with current spending calculations
     */
    fun getBudgetsWithSpending(): Flow<List<BudgetWithSpending>> {
        return budgetDao.getActiveBudgets().map { budgets ->
            budgets.mapNotNull { budget ->
                val category = categoryRepository.getCategoryById(budget.categoryId)
                val (startDate, endDate) = getDateRangeForPeriod(budget.period)
                
                val spent = transactionDao.getCategorySpendingInRange(
                    budget.categoryId, 
                    startDate, 
                    endDate
                ) ?: 0.0
                
                val remaining = (budget.amount - spent).coerceAtLeast(0.0)
                val percentage = if (budget.amount > 0) {
                    (spent / budget.amount * 100).toFloat().coerceIn(0f, 100f)
                } else 0f
                
                BudgetWithSpending(
                    budget = budget,
                    category = category,
                    spent = spent,
                    remaining = remaining,
                    percentage = percentage
                )
            }
        }
    }
    
    private fun getDateRangeForPeriod(period: BudgetPeriod): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        val endDate = calendar.timeInMillis
        
        when (period) {
            BudgetPeriod.DAILY -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
            }
            BudgetPeriod.WEEKLY -> {
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
            }
            BudgetPeriod.BIWEEKLY -> {
                calendar.add(Calendar.DAY_OF_YEAR, -14)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
            }
            BudgetPeriod.MONTHLY -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
            }
            BudgetPeriod.QUARTERLY -> {
                calendar.add(Calendar.MONTH, -3)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
            }
            BudgetPeriod.YEARLY -> {
                calendar.set(Calendar.DAY_OF_YEAR, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
            }
        }
        
        return calendar.timeInMillis to endDate
    }
}
