package com.theblankstate.epmanager.data.repository

import com.theblankstate.epmanager.data.local.dao.TransactionDao
import com.theblankstate.epmanager.data.model.Transaction
import com.theblankstate.epmanager.data.model.TransactionType
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao
) {
    // ========== READ OPERATIONS ==========
    
    fun getAllTransactions(): Flow<List<Transaction>> = 
        transactionDao.getAllTransactions()
    
    fun getRecentTransactions(limit: Int = 10): Flow<List<Transaction>> = 
        transactionDao.getRecentTransactions(limit)
    
    fun getTransactionsByType(type: TransactionType): Flow<List<Transaction>> = 
        transactionDao.getTransactionsByType(type)
    
    fun getTransactionsByCategory(categoryId: String): Flow<List<Transaction>> = 
        transactionDao.getTransactionsByCategory(categoryId)
    
    fun getTransactionsByAccount(accountId: String): Flow<List<Transaction>> = 
        transactionDao.getTransactionsByAccount(accountId)

    fun getTransactionsByGoal(goalId: String): Flow<List<Transaction>> = 
        transactionDao.getTransactionsByGoal(goalId)

    fun getTransactionsByDebt(debtId: String): Flow<List<Transaction>> = 
        transactionDao.getTransactionsByDebt(debtId)

    fun getTransactionsByRecurring(recurringId: String): Flow<List<Transaction>> = 
        transactionDao.getTransactionsByRecurring(recurringId)
    
    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>> = 
        transactionDao.getTransactionsByDateRange(startDate, endDate)
    
    fun searchTransactions(query: String): Flow<List<Transaction>> = 
        transactionDao.searchTransactions(query)
    
    suspend fun getTransactionById(id: String): Transaction? = 
        transactionDao.getTransactionById(id)
    
    // ========== TODAY'S TRANSACTIONS ==========
    
    fun getTodayTransactions(): Flow<List<Transaction>> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return transactionDao.getTransactionsFromDate(calendar.timeInMillis)
    }
    
    // ========== AGGREGATIONS ==========
    
    fun getTotalExpenses(): Flow<Double?> = 
        transactionDao.getTotalByType(TransactionType.EXPENSE)
    
    fun getTotalIncome(): Flow<Double?> = 
        transactionDao.getTotalIncomeExcludingAdjustments()
    
    fun getMonthlyExpenses(): Flow<Double?> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfMonth = calendar.timeInMillis
        calendar.add(Calendar.MONTH, 1)
        val endOfMonth = calendar.timeInMillis
        
        return transactionDao.getTotalByTypeInRange(TransactionType.EXPENSE, startOfMonth, endOfMonth)
    }
    
    fun getMonthlyIncome(): Flow<Double?> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfMonth = calendar.timeInMillis
        calendar.add(Calendar.MONTH, 1)
        val endOfMonth = calendar.timeInMillis
        
        return transactionDao.getTotalIncomeExcludingAdjustmentsInRange(startOfMonth, endOfMonth)
    }
    
    fun getWeeklyExpenses(): Flow<Double?> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfWeek = calendar.timeInMillis
        calendar.add(Calendar.WEEK_OF_YEAR, 1)
        val endOfWeek = calendar.timeInMillis
        
        return transactionDao.getTotalByTypeInRange(TransactionType.EXPENSE, startOfWeek, endOfWeek)
    }
    
    fun getWeeklyIncome(): Flow<Double?> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfWeek = calendar.timeInMillis
        calendar.add(Calendar.WEEK_OF_YEAR, 1)
        val endOfWeek = calendar.timeInMillis
        
        return transactionDao.getTotalIncomeExcludingAdjustmentsInRange(startOfWeek, endOfWeek)
    }
    
    suspend fun getCategorySpendingThisMonth(categoryId: String): Double {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfMonth = calendar.timeInMillis
        calendar.add(Calendar.MONTH, 1)
        val endOfMonth = calendar.timeInMillis
        
        return transactionDao.getCategorySpendingInRange(categoryId, startOfMonth, endOfMonth) ?: 0.0
    }
    
    // ========== WRITE OPERATIONS ==========
    
    suspend fun insertTransaction(transaction: Transaction) {
        try {
            transactionDao.insertTransaction(transaction)
        } catch (e: android.database.sqlite.SQLiteConstraintException) {
            // Log error or ignore if FK violation (e.g. category deleted during sync)
            e.printStackTrace()
        }
    }
    
    suspend fun updateTransaction(transaction: Transaction) {
        transactionDao.updateTransaction(transaction.copy(updatedAt = System.currentTimeMillis()))
    }
    
    suspend fun deleteTransaction(transaction: Transaction) {
        transactionDao.deleteTransaction(transaction)
    }
    
    suspend fun deleteTransactionById(id: String) {
        transactionDao.deleteTransactionById(id)
    }
    
    suspend fun deleteAllTransactions() {
        transactionDao.deleteAllTransactions()
    }
    
    // ========== SYNC OPERATIONS ==========
    
    suspend fun getUnsyncedTransactions(): List<Transaction> = 
        transactionDao.getUnsyncedTransactions()
    
    suspend fun markAsSynced(id: String) {
        transactionDao.markAsSynced(id)
    }
}
