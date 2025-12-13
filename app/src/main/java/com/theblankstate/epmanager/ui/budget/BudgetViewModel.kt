package com.theblankstate.epmanager.ui.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.theblankstate.epmanager.data.model.*
import com.theblankstate.epmanager.data.model.CategoryType
import com.theblankstate.epmanager.data.repository.BudgetRepository
import com.theblankstate.epmanager.data.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BudgetUiState(
    val budgets: List<BudgetWithSpending> = emptyList(),
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = true,
    val showAddBudgetSheet: Boolean = false,
    val showSelectCategorySheet: Boolean = false,
    val showAddCategorySheet: Boolean = false,
    val selectedCategory: Category? = null,
    val totalBudget: Double = 0.0,
    val totalSpent: Double = 0.0
)

@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(BudgetUiState())
    val uiState: StateFlow<BudgetUiState> = _uiState.asStateFlow()
    
    init {
        loadBudgets()
        loadCategories()
    }
    
    private fun loadBudgets() {
        viewModelScope.launch {
            budgetRepository.getBudgetsWithSpending()
                .collect { budgets ->
                    val totalBudget = budgets.sumOf { it.budget.amount }
                    val totalSpent = budgets.sumOf { it.spent }
                    
                    _uiState.update { 
                        it.copy(
                            budgets = budgets,
                            totalBudget = totalBudget,
                            totalSpent = totalSpent,
                            isLoading = false
                        ) 
                    }
                }
        }
    }
    
    private fun loadCategories() {
        viewModelScope.launch {
            categoryRepository.getExpenseCategories()
                .collect { categories ->
                    _uiState.update { it.copy(categories = categories) }
                }
        }
    }
    
    // Bottom Sheet Management
    fun showAddBudgetSheet() {
        _uiState.update { it.copy(showAddBudgetSheet = true, selectedCategory = null) }
    }
    
    fun hideAddBudgetSheet() {
        _uiState.update { it.copy(showAddBudgetSheet = false, selectedCategory = null) }
    }
    
    fun showSelectCategorySheet() {
        _uiState.update { it.copy(showSelectCategorySheet = true) }
    }
    
    fun hideSelectCategorySheet() {
        _uiState.update { it.copy(showSelectCategorySheet = false) }
    }
    
    fun showAddCategorySheet() {
        _uiState.update { 
            it.copy(
                showAddCategorySheet = true,
                showSelectCategorySheet = false
            ) 
        }
    }
    
    fun hideAddCategorySheet() {
        _uiState.update { it.copy(showAddCategorySheet = false) }
    }
    
    fun selectCategory(category: Category) {
        _uiState.update { 
            it.copy(
                selectedCategory = category,
                showSelectCategorySheet = false
            ) 
        }
    }
    
    // Budget Operations
    fun addBudget(categoryId: String, amount: Double, period: BudgetPeriod) {
        viewModelScope.launch {
            val budget = Budget(
                categoryId = categoryId,
                amount = amount,
                period = period
            )
            budgetRepository.insertBudget(budget)
            hideAddBudgetSheet()
        }
    }
    
    fun addCategory(name: String, icon: String, color: Long, type: CategoryType) {
        viewModelScope.launch {
            val category = Category(
                name = name,
                icon = icon,
                color = color,
                type = type
            )
            categoryRepository.insertCategory(category)
            // Select the newly created category
            _uiState.update { 
                it.copy(
                    selectedCategory = category,
                    showAddCategorySheet = false
                ) 
            }
        }
    }
    
    fun deleteBudget(budget: Budget) {
        viewModelScope.launch {
            budgetRepository.deleteBudget(budget)
        }
    }
}
