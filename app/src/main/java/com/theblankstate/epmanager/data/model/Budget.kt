package com.theblankstate.epmanager.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Represents a budget limit for a category
 */
@Entity(
    tableName = "budgets",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("categoryId")]
)
data class Budget(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val categoryId: String,
    val amount: Double,
    val period: BudgetPeriod,
    val alertThreshold: Float = 0.8f, // Alert when 80% spent
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

enum class BudgetPeriod(val label: String) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    BIWEEKLY("Bi-weekly"),
    MONTHLY("Monthly"),
    QUARTERLY("Quarterly"),
    YEARLY("Yearly")
}

/**
 * Budget with spending info for display
 */
data class BudgetWithSpending(
    val budget: Budget,
    val category: Category?,
    val spent: Double,
    val remaining: Double,
    val percentage: Float
)
