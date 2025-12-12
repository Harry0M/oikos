package com.theblankstate.epmanager.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Represents a recurring expense/income template
 */
@Entity(
    tableName = "recurring_expenses",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = Account::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("categoryId"),
        Index("accountId"),
        Index("nextDueDate")
    ]
)
data class RecurringExpense(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val amount: Double,
    val type: TransactionType,
    val categoryId: String?,
    val accountId: String?,
    val frequency: RecurringFrequency,
    val startDate: Long,
    val endDate: Long? = null, // null = indefinite
    val nextDueDate: Long,
    val lastProcessedDate: Long? = null,
    val note: String? = null,
    val isActive: Boolean = true,
    val autoAdd: Boolean = false, // Auto-add transaction on due date
    val reminderDaysBefore: Int = 1, // Send reminder N days before
    val createdAt: Long = System.currentTimeMillis()
)

enum class RecurringFrequency(val label: String, val days: Int) {
    DAILY("Daily", 1),
    WEEKLY("Weekly", 7),
    BIWEEKLY("Bi-weekly", 14),
    MONTHLY("Monthly", 30),
    QUARTERLY("Quarterly", 90),
    YEARLY("Yearly", 365)
}

/**
 * Recurring expense with category info for display
 */
data class RecurringExpenseWithCategory(
    val recurring: RecurringExpense,
    val category: Category?
)
