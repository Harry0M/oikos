package com.theblankstate.epmanager.data.local.dao

import androidx.room.*
import com.theblankstate.epmanager.data.model.RecurringExpense
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringExpenseDao {
    
    @Query("SELECT * FROM recurring_expenses WHERE isActive = 1 ORDER BY nextDueDate ASC")
    fun getActiveRecurringExpenses(): Flow<List<RecurringExpense>>
    
    @Query("SELECT * FROM recurring_expenses ORDER BY createdAt DESC")
    fun getAllRecurringExpenses(): Flow<List<RecurringExpense>>
    
    @Query("SELECT * FROM recurring_expenses WHERE id = :id")
    suspend fun getRecurringExpenseById(id: String): RecurringExpense?
    
    @Query("SELECT * FROM recurring_expenses WHERE nextDueDate <= :date AND isActive = 1")
    suspend fun getDueRecurringExpenses(date: Long): List<RecurringExpense>
    
    @Query("""
        SELECT * FROM recurring_expenses 
        WHERE nextDueDate <= :reminderDate AND isActive = 1
        AND (lastProcessedDate IS NULL OR lastProcessedDate < nextDueDate - 86400000)
    """)
    suspend fun getUpcomingDueExpenses(reminderDate: Long): List<RecurringExpense>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurringExpense(recurringExpense: RecurringExpense)
    
    @Update
    suspend fun updateRecurringExpense(recurringExpense: RecurringExpense)
    
    @Delete
    suspend fun deleteRecurringExpense(recurringExpense: RecurringExpense)
    
    @Query("DELETE FROM recurring_expenses WHERE id = :id")
    suspend fun deleteRecurringExpenseById(id: String)
    
    @Query("UPDATE recurring_expenses SET isActive = :isActive WHERE id = :id")
    suspend fun setActiveStatus(id: String, isActive: Boolean)
    
    @Query("UPDATE recurring_expenses SET nextDueDate = :nextDueDate, lastProcessedDate = :processedDate WHERE id = :id")
    suspend fun updateDueDate(id: String, nextDueDate: Long, processedDate: Long)
    
    @Query("DELETE FROM recurring_expenses")
    suspend fun deleteAllRecurringExpenses()
}
