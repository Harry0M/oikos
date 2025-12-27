package com.theblankstate.epmanager.ui.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.theblankstate.epmanager.data.model.GoalPresets
import com.theblankstate.epmanager.data.model.SavingsGoal
import com.theblankstate.epmanager.data.repository.SavingsGoalRepository
import com.theblankstate.epmanager.data.model.Transaction
import com.theblankstate.epmanager.data.model.TransactionType
import com.theblankstate.epmanager.data.repository.AccountRepository
import com.theblankstate.epmanager.data.repository.TransactionRepository
import com.theblankstate.epmanager.data.repository.CategoryRepository
import com.theblankstate.epmanager.util.LocationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GoalsUiState(
    val activeGoals: List<SavingsGoal> = emptyList(),
    val completedGoals: List<SavingsGoal> = emptyList(),
    val accounts: List<com.theblankstate.epmanager.data.model.Account> = emptyList(),
    val totalSaved: Double = 0.0,
    val totalTarget: Double = 0.0,
    val isLoading: Boolean = true,
    val showAddDialog: Boolean = false,
    val showContributeDialog: SavingsGoal? = null
)

@HiltViewModel
class GoalsViewModel @Inject constructor(
    private val repository: SavingsGoalRepository,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val locationHelper: LocationHelper
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(GoalsUiState())
    val uiState: StateFlow<GoalsUiState> = _uiState.asStateFlow()
    
    init {
        loadGoals()
        viewModelScope.launch {
            categoryRepository.ensureSplitCategoriesExist()
        }
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
            accountRepository.getAllAccounts().collect { accounts ->
                _uiState.update { it.copy(accounts = accounts) }
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
    
    fun addContribution(goalId: String, amount: Double, accountId: String? = null) {
        viewModelScope.launch {
            // Get the goal details for the note/category
            val goal = repository.getGoalById(goalId)
            
            if (goal != null) {
                // Use provided accountId or fall back to default account
                val finalAccountId = accountId ?: accountRepository.getDefaultAccount()?.id
                
                // Try to get location
                val location = locationHelper.getCurrentLocation()
                val locationName = location?.let { locationHelper.getLocationName(it) }
                
                // Create Transaction
                val transaction = Transaction(
                    amount = amount,
                    categoryId = "goals",
                    accountId = finalAccountId,
                    type = TransactionType.EXPENSE,
                    date = System.currentTimeMillis(),
                    note = "Contribution to ${goal.name}",
                    latitude = location?.latitude,
                    longitude = location?.longitude,
                    locationName = locationName
                )
                transactionRepository.insertTransaction(transaction)
                
                // Update Account Balance
                if (finalAccountId != null) {
                    accountRepository.updateBalance(finalAccountId, -amount)
                }
                
                // Add to Goal
                repository.addContribution(goalId, amount)
            }
            
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
