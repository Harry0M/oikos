package com.theblankstate.epmanager.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.theblankstate.epmanager.data.model.Category
import com.theblankstate.epmanager.data.model.CategoryType
import com.theblankstate.epmanager.data.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoriesUiState(
    val expenseCategories: List<Category> = emptyList(),
    val incomeCategories: List<Category> = emptyList(),
    val isLoading: Boolean = true,
    val showAddDialog: Boolean = false,
    val editingCategory: Category? = null,
    val selectedTab: Int = 0
)

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(CategoriesUiState())
    val uiState: StateFlow<CategoriesUiState> = _uiState.asStateFlow()
    
    init {
        loadCategories()
    }
    
    private fun loadCategories() {
        viewModelScope.launch {
            categoryRepository.getExpenseCategories()
                .collect { categories ->
                    _uiState.update { 
                        it.copy(
                            expenseCategories = categories,
                            isLoading = false
                        ) 
                    }
                }
        }
        
        viewModelScope.launch {
            categoryRepository.getIncomeCategories()
                .collect { categories ->
                    _uiState.update { it.copy(incomeCategories = categories) }
                }
        }
    }
    
    fun setSelectedTab(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
    }
    
    fun showAddDialog() {
        _uiState.update { it.copy(showAddDialog = true, editingCategory = null) }
    }
    
    fun showEditDialog(category: Category) {
        _uiState.update { it.copy(showAddDialog = true, editingCategory = category) }
    }
    
    fun hideDialog() {
        _uiState.update { it.copy(showAddDialog = false, editingCategory = null) }
    }
    
    fun saveCategory(
        name: String,
        icon: String,
        color: Long,
        type: CategoryType
    ) {
        viewModelScope.launch {
            val existingCategory = _uiState.value.editingCategory
            if (existingCategory != null) {
                // Update existing
                val updated = existingCategory.copy(
                    name = name,
                    icon = icon,
                    color = color,
                    type = type
                )
                categoryRepository.updateCategory(updated)
            } else {
                // Create new
                val newCategory = Category(
                    name = name,
                    icon = icon,
                    color = color,
                    type = type,
                    isDefault = false
                )
                categoryRepository.insertCategory(newCategory)
            }
            hideDialog()
        }
    }
    
    fun deleteCategory(category: Category) {
        if (category.isDefault) return // Can't delete default categories
        
        viewModelScope.launch {
            categoryRepository.deleteCategory(category)
        }
    }
}
