package com.theblankstate.epmanager.data.repository

import com.theblankstate.epmanager.data.local.dao.RecurringExpenseDao
import com.theblankstate.epmanager.data.local.dao.TransactionDao
import com.theblankstate.epmanager.data.model.*
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecurringExpenseRepository @Inject constructor(
    private val recurringExpenseDao: RecurringExpenseDao,
    private val transactionDao: TransactionDao,
    private val categoryRepository: CategoryRepository
) {
    fun getActiveRecurringExpenses(): Flow<List<RecurringExpense>> =
        recurringExpenseDao.getActiveRecurringExpenses()
    
    fun getAllRecurringExpenses(): Flow<List<RecurringExpense>> =
        recurringExpenseDao.getAllRecurringExpenses()
    
    suspend fun getRecurringExpenseById(id: String): RecurringExpense? =
        recurringExpenseDao.getRecurringExpenseById(id)
    
    suspend fun insertRecurringExpense(recurringExpense: RecurringExpense) {
        recurringExpenseDao.insertRecurringExpense(recurringExpense)
    }
    
    suspend fun updateRecurringExpense(recurringExpense: RecurringExpense) {
        recurringExpenseDao.updateRecurringExpense(recurringExpense)
    }
    
    suspend fun deleteRecurringExpense(recurringExpense: RecurringExpense) {
        recurringExpenseDao.deleteRecurringExpense(recurringExpense)
    }
    
    suspend fun toggleActive(id: String, isActive: Boolean) {
        recurringExpenseDao.setActiveStatus(id, isActive)
    }
    
    /**
     * Get recurring expenses with their categories for display
     */
    suspend fun getRecurringExpensesWithCategories(): List<RecurringExpenseWithCategory> {
        val recurring = recurringExpenseDao.getActiveRecurringExpenses()
        val result = mutableListOf<RecurringExpenseWithCategory>()
        
        // This is a simplified version - in production, use a JOIN query
        recurring.collect { list ->
            list.forEach { expense ->
                val category = expense.categoryId?.let { categoryRepository.getCategoryById(it) }
                result.add(RecurringExpenseWithCategory(expense, category))
            }
        }
        
        return result
    }
    
    /**
     * Process due recurring expenses - create transactions for them
     */
    suspend fun processDueExpenses(): Int {
        val now = System.currentTimeMillis()
        val dueExpenses = recurringExpenseDao.getDueRecurringExpenses(now)
        var processed = 0
        
        dueExpenses.forEach { recurring ->
            if (recurring.autoAdd) {
                // Create transaction
                val transaction = Transaction(
                    amount = recurring.amount,
                    type = recurring.type,
                    categoryId = recurring.categoryId,
                    accountId = recurring.accountId,
                    date = recurring.nextDueDate,
                    note = "${recurring.name} (Recurring)",
                    isRecurring = true,
                    recurringId = recurring.id
                )
                transactionDao.insertTransaction(transaction)
            }
            
            // Calculate next due date
            val nextDueDate = calculateNextDueDate(recurring.nextDueDate, recurring.frequency)
            
            // Check if we've passed the end date
            val shouldDeactivate = recurring.endDate?.let { nextDueDate > it } ?: false
            
            if (shouldDeactivate) {
                recurringExpenseDao.setActiveStatus(recurring.id, false)
            } else {
                recurringExpenseDao.updateDueDate(recurring.id, nextDueDate, now)
            }
            
            processed++
        }
        
        return processed
    }
    
    /**
     * Get upcoming expenses that need reminders
     */
    suspend fun getUpcomingReminders(): List<RecurringExpense> {
        val reminderDate = System.currentTimeMillis() + (2 * 24 * 60 * 60 * 1000) // 2 days ahead
        return recurringExpenseDao.getUpcomingDueExpenses(reminderDate)
    }
    
    private fun calculateNextDueDate(currentDueDate: Long, frequency: RecurringFrequency): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = currentDueDate
        }
        
        when (frequency) {
            RecurringFrequency.DAILY -> calendar.add(Calendar.DAY_OF_YEAR, 1)
            RecurringFrequency.WEEKLY -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
            RecurringFrequency.BIWEEKLY -> calendar.add(Calendar.WEEK_OF_YEAR, 2)
            RecurringFrequency.MONTHLY -> calendar.add(Calendar.MONTH, 1)
            RecurringFrequency.QUARTERLY -> calendar.add(Calendar.MONTH, 3)
            RecurringFrequency.YEARLY -> calendar.add(Calendar.YEAR, 1)
        }
        
        return calendar.timeInMillis
    }
}
