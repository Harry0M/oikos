package com.theblankstate.epmanager.data.local.dao

import androidx.room.*
import com.theblankstate.epmanager.data.model.Debt
import com.theblankstate.epmanager.data.model.DebtPayment
import com.theblankstate.epmanager.data.model.DebtType
import kotlinx.coroutines.flow.Flow

@Dao
interface DebtDao {
    
    // Debt operations
    @Query("SELECT * FROM debts WHERE isSettled = 0 ORDER BY dueDate ASC, createdAt DESC")
    fun getActiveDebts(): Flow<List<Debt>>
    
    @Query("SELECT * FROM debts WHERE type = :type AND isSettled = 0 ORDER BY dueDate ASC, createdAt DESC")
    fun getActiveDebtsByType(type: DebtType): Flow<List<Debt>>
    
    @Query("SELECT * FROM debts WHERE isSettled = 1 ORDER BY updatedAt DESC")
    fun getSettledDebts(): Flow<List<Debt>>
    
    @Query("SELECT * FROM debts ORDER BY createdAt DESC")
    fun getAllDebts(): Flow<List<Debt>>
    
    @Query("SELECT * FROM debts WHERE id = :id")
    suspend fun getDebtById(id: String): Debt?
    
    @Query("SELECT * FROM debts WHERE linkedFriendId = :friendId")
    fun getDebtsByFriend(friendId: String): Flow<List<Debt>>
    
    @Query("SELECT SUM(remainingAmount) FROM debts WHERE type = :type AND isSettled = 0")
    fun getTotalRemainingByType(type: DebtType): Flow<Double?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebt(debt: Debt)
    
    @Update
    suspend fun updateDebt(debt: Debt)
    
    @Delete
    suspend fun deleteDebt(debt: Debt)
    
    @Query("UPDATE debts SET remainingAmount = :remaining, isSettled = :isSettled, updatedAt = :updateTime WHERE id = :debtId")
    suspend fun updateDebtBalance(
        debtId: String, 
        remaining: Double, 
        isSettled: Boolean, 
        updateTime: Long = System.currentTimeMillis()
    )
    
    // Payment operations
    @Query("SELECT * FROM debt_payments WHERE debtId = :debtId ORDER BY paidAt DESC")
    fun getPaymentsForDebt(debtId: String): Flow<List<DebtPayment>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: DebtPayment)
    
    @Delete
    suspend fun deletePayment(payment: DebtPayment)
    
    @Query("DELETE FROM debts")
    suspend fun deleteAllDebts()
}
