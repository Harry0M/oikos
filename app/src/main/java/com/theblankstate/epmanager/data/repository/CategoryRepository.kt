package com.theblankstate.epmanager.data.repository

import com.theblankstate.epmanager.data.local.dao.CategoryDao
import com.theblankstate.epmanager.data.model.Category
import com.theblankstate.epmanager.data.model.CategoryType
import com.theblankstate.epmanager.data.model.DefaultCategories
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao
) {
    fun getAllCategories(): Flow<List<Category>> = 
        categoryDao.getAllCategories()
    
    fun getCategoriesByType(type: CategoryType): Flow<List<Category>> = 
        categoryDao.getCategoriesByType(type)
    
    fun getExpenseCategories(): Flow<List<Category>> = 
        categoryDao.getCategoriesByType(CategoryType.EXPENSE)
    
    fun getIncomeCategories(): Flow<List<Category>> = 
        categoryDao.getCategoriesByType(CategoryType.INCOME)
    
    suspend fun getCategoryById(id: String): Category? = 
        categoryDao.getCategoryById(id)
    
    suspend fun insertCategory(category: Category) {
        categoryDao.insertCategory(category)
    }
    
    suspend fun updateCategory(category: Category) {
        categoryDao.updateCategory(category)
    }
    
    suspend fun deleteCategory(category: Category) {
        categoryDao.deleteCategory(category)
    }
    
    suspend fun initializeDefaultCategories() {
        val count = categoryDao.getCategoryCount()
        if (count == 0) {
            categoryDao.insertCategories(DefaultCategories.all)
        }
    }
    
    /**
     * Ensure split expense categories exist (for existing databases)
     */
    suspend fun ensureSplitCategoriesExist() {
        // Check if split_expense category exists
        if (categoryDao.getCategoryById("split_expense") == null) {
            val splitExpense = Category(
                id = "split_expense",
                name = "Split Expense",
                icon = "Groups",
                color = 0xFF10B981,
                type = CategoryType.EXPENSE,
                isDefault = true
            )
            categoryDao.insertCategory(splitExpense)
        }
        
        // Check if split_payoff category exists
        if (categoryDao.getCategoryById("split_payoff") == null) {
            val splitPayoff = Category(
                id = "split_payoff",
                name = "Split Payoff",
                icon = "Handshake",
                color = 0xFF10B981,
                type = CategoryType.INCOME,
                isDefault = true
            )
            categoryDao.insertCategory(splitPayoff)
        }
    }
}
