package com.theblankstate.epmanager.data.local.dao

import androidx.room.*
import com.theblankstate.epmanager.data.model.SavingsGoal
import kotlinx.coroutines.flow.Flow

@Dao
interface SavingsGoalDao {
    
    @Query("SELECT * FROM savings_goals WHERE isCompleted = 0 ORDER BY createdAt DESC")
    fun getActiveGoals(): Flow<List<SavingsGoal>>
    
    @Query("SELECT * FROM savings_goals ORDER BY createdAt DESC")
    fun getAllGoals(): Flow<List<SavingsGoal>>
    
    @Query("SELECT * FROM savings_goals WHERE isCompleted = 1 ORDER BY updatedAt DESC")
    fun getCompletedGoals(): Flow<List<SavingsGoal>>
    
    @Query("SELECT * FROM savings_goals WHERE id = :id")
    suspend fun getGoalById(id: String): SavingsGoal?
    
    @Query("SELECT SUM(savedAmount) FROM savings_goals WHERE isCompleted = 0")
    fun getTotalSaved(): Flow<Double?>
    
    @Query("SELECT SUM(targetAmount) FROM savings_goals WHERE isCompleted = 0")
    fun getTotalTarget(): Flow<Double?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: SavingsGoal)
    
    @Update
    suspend fun updateGoal(goal: SavingsGoal)
    
    @Delete
    suspend fun deleteGoal(goal: SavingsGoal)
    
    @Query("UPDATE savings_goals SET savedAmount = savedAmount + :amount, updatedAt = :updateTime WHERE id = :goalId")
    suspend fun addToGoal(goalId: String, amount: Double, updateTime: Long = System.currentTimeMillis())
    
    @Query("UPDATE savings_goals SET isCompleted = 1, updatedAt = :updateTime WHERE id = :goalId")
    suspend fun markCompleted(goalId: String, updateTime: Long = System.currentTimeMillis())
}
