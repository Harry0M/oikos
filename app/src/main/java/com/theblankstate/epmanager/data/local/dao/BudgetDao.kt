package com.theblankstate.epmanager.data.local.dao

import androidx.room.*
import com.theblankstate.epmanager.data.model.Budget
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    
    @Query("SELECT * FROM budgets WHERE isActive = 1 ORDER BY createdAt DESC")
    fun getActiveBudgets(): Flow<List<Budget>>
    
    @Query("SELECT * FROM budgets ORDER BY createdAt DESC")
    fun getAllBudgets(): Flow<List<Budget>>
    
    @Query("SELECT * FROM budgets WHERE id = :id")
    suspend fun getBudgetById(id: String): Budget?
    
    @Query("SELECT * FROM budgets WHERE categoryId = :categoryId AND isActive = 1")
    suspend fun getBudgetByCategory(categoryId: String): Budget?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: Budget)
    
    @Update
    suspend fun updateBudget(budget: Budget)
    
    @Delete
    suspend fun deleteBudget(budget: Budget)
    
    @Query("UPDATE budgets SET isActive = :isActive WHERE id = :id")
    suspend fun setBudgetActive(id: String, isActive: Boolean)
}
