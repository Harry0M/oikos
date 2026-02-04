package com.theblankstate.epmanager.data.local.dao

import androidx.paging.PagingSource
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
    
    // ========== PAGING OPERATIONS ==========
    
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactionsPaged(): PagingSource<Int, Transaction>
    
    @Query("SELECT * FROM transactions WHERE categoryId = :categoryId ORDER BY date DESC")
    fun getTransactionsByCategoryPaged(categoryId: String): PagingSource<Int, Transaction>
    
    @Query("SELECT * FROM transactions WHERE accountId = :accountId ORDER BY date DESC")
    fun getTransactionsByAccountPaged(accountId: String): PagingSource<Int, Transaction>
    
    @Query("SELECT * FROM transactions WHERE goalId = :goalId ORDER BY date DESC")
    fun getTransactionsByGoalPaged(goalId: String): PagingSource<Int, Transaction>
    
    @Query("SELECT * FROM transactions WHERE debtId = :debtId ORDER BY date DESC")
    fun getTransactionsByDebtPaged(debtId: String): PagingSource<Int, Transaction>
    
    @Query("SELECT * FROM transactions WHERE recurringId = :recurringId ORDER BY date DESC")
    fun getTransactionsByRecurringPaged(recurringId: String): PagingSource<Int, Transaction>
    
    @Query("""
        SELECT * FROM transactions 
        WHERE note LIKE '%' || :query || '%' 
        ORDER BY date DESC
    """)
    fun searchTransactionsPaged(query: String): PagingSource<Int, Transaction>
    
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
    
    // ========== DUPLICATE CHECKING ==========
    
    @Query("""
        SELECT * FROM transactions 
        WHERE smsSender = :sender 
        AND amount = :amount 
        AND date BETWEEN :startTime AND :endTime
    """)
    suspend fun findPotentialDuplicates(sender: String, amount: Double, startTime: Long, endTime: Long): List<Transaction>

    @Query("SELECT * FROM transactions WHERE refNumber = :refNumber LIMIT 1")
    suspend fun findByReferenceNumber(refNumber: String): Transaction?
    
    /**
     * Find existing transaction linked to a recurring expense within a date range
     * Used to avoid duplicates when SMS confirms an existing recurring payment
     */
    @Query("""
        SELECT * FROM transactions 
        WHERE recurringId = :recurringId 
        AND date BETWEEN :startDate AND :endDate
        LIMIT 1
    """)
    suspend fun findRecurringTransaction(
        recurringId: String,
        startDate: Long,
        endDate: Long
    ): Transaction?
    
    /**
     * Update an existing transaction with SMS details when SMS confirms a recurring payment
     */
    @Query("""
        UPDATE transactions SET 
            smsSender = :smsSender,
            originalSms = :originalSms,
            refNumber = :refNumber,
            upiId = :upiId,
            merchantName = :merchantName,
            senderName = :senderName,
            receiverName = :receiverName,
            updatedAt = :updatedAt
        WHERE id = :transactionId
    """)
    suspend fun updateTransactionWithSmsDetails(
        transactionId: String,
        smsSender: String?,
        originalSms: String?,
        refNumber: String?,
        upiId: String?,
        merchantName: String?,
        senderName: String?,
        receiverName: String?,
        updatedAt: Long = System.currentTimeMillis()
    )
    
    /**
     * Get distinct SMS sender IDs that are not linked to any account
     * Used to show suggestions when adding custom banks
     */
    @Query("""
        SELECT DISTINCT smsSender FROM transactions 
        WHERE smsSender IS NOT NULL 
        AND smsSender != ''
        AND accountId IS NULL
        ORDER BY smsSender ASC
    """)
    fun getUncategorizedSmsSenders(): Flow<List<String>>
}
