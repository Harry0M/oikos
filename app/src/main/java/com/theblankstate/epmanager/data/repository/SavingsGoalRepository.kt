package com.theblankstate.epmanager.data.repository

import com.theblankstate.epmanager.data.local.dao.SavingsGoalDao
import com.theblankstate.epmanager.data.model.SavingsGoal
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SavingsGoalRepository @Inject constructor(
    private val savingsGoalDao: SavingsGoalDao
) {
    fun getActiveGoals(): Flow<List<SavingsGoal>> =
        savingsGoalDao.getActiveGoals()
    
    fun getAllGoals(): Flow<List<SavingsGoal>> =
        savingsGoalDao.getAllGoals()
    
    fun getCompletedGoals(): Flow<List<SavingsGoal>> =
        savingsGoalDao.getCompletedGoals()
    
    fun getTotalSaved(): Flow<Double?> =
        savingsGoalDao.getTotalSaved()
    
    fun getTotalTarget(): Flow<Double?> =
        savingsGoalDao.getTotalTarget()
    
    suspend fun getGoalById(id: String): SavingsGoal? =
        savingsGoalDao.getGoalById(id)
    
    suspend fun createGoal(goal: SavingsGoal) {
        savingsGoalDao.insertGoal(goal)
    }
    
    suspend fun updateGoal(goal: SavingsGoal) {
        savingsGoalDao.updateGoal(goal.copy(updatedAt = System.currentTimeMillis()))
    }
    
    suspend fun deleteGoal(goal: SavingsGoal) {
        savingsGoalDao.deleteGoal(goal)
    }
    
    suspend fun deleteAllGoals() {
        savingsGoalDao.deleteAllGoals()
    }
    
    suspend fun addContribution(goalId: String, amount: Double) {
        val goal = savingsGoalDao.getGoalById(goalId) ?: return
        val newSaved = goal.savedAmount + amount
        
        if (newSaved >= goal.targetAmount) {
            // Goal completed!
            savingsGoalDao.updateGoal(
                goal.copy(
                    savedAmount = goal.targetAmount,
                    isCompleted = true,
                    updatedAt = System.currentTimeMillis()
                )
            )
        } else {
            savingsGoalDao.addToGoal(goalId, amount)
        }
    }
    
    suspend fun withdrawFromGoal(goalId: String, amount: Double) {
        val goal = savingsGoalDao.getGoalById(goalId) ?: return
        val newSaved = (goal.savedAmount - amount).coerceAtLeast(0.0)
        
        savingsGoalDao.updateGoal(
            goal.copy(
                savedAmount = newSaved,
                isCompleted = false,
                updatedAt = System.currentTimeMillis()
            )
        )
    }
}
