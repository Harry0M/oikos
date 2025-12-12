package com.theblankstate.epmanager.data.local.dao

import androidx.room.*
import com.theblankstate.epmanager.data.model.Category
import com.theblankstate.epmanager.data.model.CategoryType
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    
    @Query("SELECT * FROM categories ORDER BY isDefault DESC, name ASC")
    fun getAllCategories(): Flow<List<Category>>
    
    @Query("SELECT * FROM categories WHERE type = :type ORDER BY isDefault DESC, name ASC")
    fun getCategoriesByType(type: CategoryType): Flow<List<Category>>
    
    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: String): Category?
    
    @Query("SELECT * FROM categories WHERE isDefault = 1")
    suspend fun getDefaultCategories(): List<Category>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<Category>)
    
    @Update
    suspend fun updateCategory(category: Category)
    
    @Delete
    suspend fun deleteCategory(category: Category)
    
    @Query("DELETE FROM categories WHERE isDefault = 0")
    suspend fun deleteCustomCategories()
    
    @Query("SELECT COUNT(*) FROM categories")
    suspend fun getCategoryCount(): Int
}
