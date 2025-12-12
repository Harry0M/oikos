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
}
