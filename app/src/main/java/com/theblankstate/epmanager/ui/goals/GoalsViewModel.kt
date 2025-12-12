package com.theblankstate.epmanager.ui.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.theblankstate.epmanager.data.model.GoalPresets
import com.theblankstate.epmanager.data.model.SavingsGoal
import com.theblankstate.epmanager.data.repository.SavingsGoalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GoalsUiState(
    val activeGoals: List<SavingsGoal> = emptyList(),
    val completedGoals: List<SavingsGoal> = emptyList(),
    val totalSaved: Double = 0.0,
    val totalTarget: Double = 0.0,
    val isLoading: Boolean = true,
    val showAddDialog: Boolean = false,
    val showContributeDialog: SavingsGoal? = null
)

@HiltViewModel
class GoalsViewModel @Inject constructor(
    private val repository: SavingsGoalRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(GoalsUiState())
    val uiState: StateFlow<GoalsUiState> = _uiState.asStateFlow()
    
    init {
        loadGoals()
    }
    
    private fun loadGoals() {
        viewModelScope.launch {
            repository.getActiveGoals().collect { goals ->
                _uiState.update { 
                    it.copy(activeGoals = goals, isLoading = false) 
                }
            }
        }
        
        viewModelScope.launch {
            repository.getCompletedGoals().collect { goals ->
                _uiState.update { it.copy(completedGoals = goals) }
            }
        }
        
        viewModelScope.launch {
            combine(
                repository.getTotalSaved(),
                repository.getTotalTarget()
            ) { saved, target ->
                Pair(saved ?: 0.0, target ?: 0.0)
            }.collect { (saved, target) ->
                _uiState.update { 
                    it.copy(totalSaved = saved, totalTarget = target) 
                }
            }
        }
    }
    
    fun showAddDialog() {
        _uiState.update { it.copy(showAddDialog = true) }
    }
    
    fun hideAddDialog() {
        _uiState.update { it.copy(showAddDialog = false) }
    }
    
    fun showContributeDialog(goal: SavingsGoal) {
        _uiState.update { it.copy(showContributeDialog = goal) }
    }
    
    fun hideContributeDialog() {
        _uiState.update { it.copy(showContributeDialog = null) }
    }
    
    fun createGoal(
        name: String,
        targetAmount: Double,
        presetIndex: Int,
        targetDate: Long?
    ) {
        viewModelScope.launch {
            val preset = GoalPresets.presets.getOrNull(presetIndex) 
                ?: GoalPresets.presets.last()
            
            val goal = SavingsGoal(
                name = name,
                targetAmount = targetAmount,
                icon = preset.icon,
                color = preset.color,
                targetDate = targetDate
            )
            repository.createGoal(goal)
            hideAddDialog()
        }
    }
    
    fun addContribution(goalId: String, amount: Double) {
        viewModelScope.launch {
            repository.addContribution(goalId, amount)
            hideContributeDialog()
        }
    }
    
    fun withdrawFromGoal(goalId: String, amount: Double) {
        viewModelScope.launch {
            repository.withdrawFromGoal(goalId, amount)
        }
    }
    
    fun deleteGoal(goal: SavingsGoal) {
        viewModelScope.launch {
            repository.deleteGoal(goal)
        }
    }
}
