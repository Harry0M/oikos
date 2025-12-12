package com.theblankstate.epmanager.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Represents a transaction category (expense or income)
 */
@Entity(tableName = "categories")
data class Category(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val icon: String, // Material icon name
    val color: Long, // Color as Long for Room storage
    val type: CategoryType,
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

enum class CategoryType {
    EXPENSE,
    INCOME
}

// Default expense categories
object DefaultCategories {
    val expenseCategories = listOf(
        Category(id = "food", name = "Food & Dining", icon = "Restaurant", color = 0xFFFF6B6B, type = CategoryType.EXPENSE, isDefault = true),
        Category(id = "transport", name = "Transport", icon = "DirectionsCar", color = 0xFF4ECDC4, type = CategoryType.EXPENSE, isDefault = true),
        Category(id = "shopping", name = "Shopping", icon = "ShoppingBag", color = 0xFFFFE66D, type = CategoryType.EXPENSE, isDefault = true),
        Category(id = "entertainment", name = "Entertainment", icon = "Movie", color = 0xFF95E1D3, type = CategoryType.EXPENSE, isDefault = true),
        Category(id = "bills", name = "Bills & Utilities", icon = "Receipt", color = 0xFFF38181, type = CategoryType.EXPENSE, isDefault = true),
        Category(id = "healthcare", name = "Healthcare", icon = "LocalHospital", color = 0xFFAA96DA, type = CategoryType.EXPENSE, isDefault = true),
        Category(id = "education", name = "Education", icon = "School", color = 0xFF74B9FF, type = CategoryType.EXPENSE, isDefault = true),
        Category(id = "groceries", name = "Groceries", icon = "ShoppingCart", color = 0xFF55A3FF, type = CategoryType.EXPENSE, isDefault = true),
        Category(id = "subscriptions", name = "Subscriptions", icon = "Subscriptions", color = 0xFFA29BFE, type = CategoryType.EXPENSE, isDefault = true),
        Category(id = "travel", name = "Travel", icon = "Flight", color = 0xFFFD79A8, type = CategoryType.EXPENSE, isDefault = true),
        Category(id = "other_expense", name = "Other", icon = "MoreHoriz", color = 0xFF9CA3AF, type = CategoryType.EXPENSE, isDefault = true)
    )
    
    val incomeCategories = listOf(
        Category(id = "salary", name = "Salary", icon = "Work", color = 0xFF22C55E, type = CategoryType.INCOME, isDefault = true),
        Category(id = "freelance", name = "Freelance", icon = "Computer", color = 0xFF14B8A6, type = CategoryType.INCOME, isDefault = true),
        Category(id = "investment", name = "Investment", icon = "TrendingUp", color = 0xFF3B82F6, type = CategoryType.INCOME, isDefault = true),
        Category(id = "gift", name = "Gift", icon = "CardGiftcard", color = 0xFF8B5CF6, type = CategoryType.INCOME, isDefault = true),
        Category(id = "refund", name = "Refund", icon = "Replay", color = 0xFFF59E0B, type = CategoryType.INCOME, isDefault = true),
        Category(id = "adjustment", name = "Adjustment", icon = "SwapVert", color = 0xFF6B7280, type = CategoryType.INCOME, isDefault = true),
        Category(id = "other_income", name = "Other", icon = "MoreHoriz", color = 0xFF9CA3AF, type = CategoryType.INCOME, isDefault = true)
    )
    
    val all = expenseCategories + incomeCategories
}
