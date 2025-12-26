package com.theblankstate.epmanager.data.local.dao

import androidx.room.*
import com.theblankstate.epmanager.data.model.Transaction
import com.theblankstate.epmanager.data.model.TransactionType
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    
    // ========== READ OPERATIONS ==========
    
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<Transaction>>
    
    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: String): Transaction?
    
    @Query("SELECT * FROM transactions WHERE type = :type ORDER BY date DESC")
    fun getTransactionsByType(type: TransactionType): Flow<List<Transaction>>
    
    @Query("SELECT * FROM transactions WHERE categoryId = :categoryId ORDER BY date DESC")
    fun getTransactionsByCategory(categoryId: String): Flow<List<Transaction>>
    
    @Query("SELECT * FROM transactions WHERE accountId = :accountId ORDER BY date DESC")
    fun getTransactionsByAccount(accountId: String): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE goalId = :goalId ORDER BY date DESC")
    fun getTransactionsByGoal(goalId: String): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE debtId = :debtId ORDER BY date DESC")
    fun getTransactionsByDebt(debtId: String): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE recurringId = :recurringId ORDER BY date DESC")
    fun getTransactionsByRecurring(recurringId: String): Flow<List<Transaction>>
    
    @Query("SELECT * FROM transactions WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>>
    
    @Query("SELECT * FROM transactions WHERE date >= :startDate ORDER BY date DESC")
    fun getTransactionsFromDate(startDate: Long): Flow<List<Transaction>>
    
    @Query("""
        SELECT * FROM transactions 
        WHERE note LIKE '%' || :query || '%' 
        ORDER BY date DESC
    """)
    fun searchTransactions(query: String): Flow<List<Transaction>>
    
    @Query("SELECT * FROM transactions ORDER BY date DESC LIMIT :limit")
    fun getRecentTransactions(limit: Int): Flow<List<Transaction>>
    
    // ========== AGGREGATIONS ==========
    
    @Query("SELECT SUM(amount) FROM transactions WHERE type = :type")
    fun getTotalByType(type: TransactionType): Flow<Double?>
    
    @Query("SELECT SUM(amount) FROM transactions WHERE type = :type AND date BETWEEN :startDate AND :endDate")
    fun getTotalByTypeInRange(type: TransactionType, startDate: Long, endDate: Long): Flow<Double?>
    
    // Income excluding adjustment category
    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'INCOME' AND (categoryId IS NULL OR categoryId != 'adjustment')")
    fun getTotalIncomeExcludingAdjustments(): Flow<Double?>
    
    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'INCOME' AND (categoryId IS NULL OR categoryId != 'adjustment') AND date BETWEEN :startDate AND :endDate")
    fun getTotalIncomeExcludingAdjustmentsInRange(startDate: Long, endDate: Long): Flow<Double?>
    
    @Query("""
        SELECT SUM(amount) FROM transactions 
        WHERE type = 'EXPENSE' AND categoryId = :categoryId 
        AND date BETWEEN :startDate AND :endDate
    """)
    suspend fun getCategorySpendingInRange(categoryId: String, startDate: Long, endDate: Long): Double?
    
    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun getTransactionCount(): Int
    
    @Query("SELECT COUNT(*) FROM transactions WHERE date >= :startDate")
    suspend fun getTransactionCountFromDate(startDate: Long): Int
    
    // ========== WRITE OPERATIONS ==========
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<Transaction>)
    
    @Update
    suspend fun updateTransaction(transaction: Transaction)
    
    @Delete
    suspend fun deleteTransaction(transaction: Transaction)
    
    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: String)
    
    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()
    
    // ========== SYNC OPERATIONS ==========
    
    @Query("SELECT * FROM transactions WHERE isSynced = 0")
    suspend fun getUnsyncedTransactions(): List<Transaction>
    
    @Query("UPDATE transactions SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)
    
    @Query("SELECT * FROM transactions WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getTransactionsBetweenDatesSync(startDate: Long, endDate: Long): List<Transaction>
}
