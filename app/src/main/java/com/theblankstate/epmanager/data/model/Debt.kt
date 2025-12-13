package com.theblankstate.epmanager.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Type of debt record
 */
enum class DebtType {
    DEBT,   // Money I owe to someone
    CREDIT  // Money someone owes to me
}

/**
 * Represents a debt or credit record
 */
@Entity(
    tableName = "debts",
    indices = [Index("linkedFriendId")]
)
data class Debt(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val type: DebtType,
    val personName: String,
    val totalAmount: Double,
    val remainingAmount: Double,
    val linkedFriendId: String? = null, // Link to friend if they use the app
    val dueDate: Long? = null, // Optional due date
    val notes: String? = null,
    val isSettled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val paidAmount: Double
        get() = totalAmount - remainingAmount
    
    val progress: Float
        get() = if (totalAmount > 0) (paidAmount / totalAmount).toFloat().coerceIn(0f, 1f) else 0f
    
    val isOverdue: Boolean
        get() = dueDate != null && !isSettled && System.currentTimeMillis() > dueDate
    
    val daysUntilDue: Int?
        get() = dueDate?.let {
            ((it - System.currentTimeMillis()) / (24 * 60 * 60 * 1000)).toInt()
        }
}

/**
 * Payment record for a debt
 */
@Entity(
    tableName = "debt_payments",
    foreignKeys = [
        ForeignKey(
            entity = Debt::class,
            parentColumns = ["id"],
            childColumns = ["debtId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("debtId")]
)
data class DebtPayment(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val debtId: String,
    val amount: Double,
    val linkedTransactionId: String? = null, // Link to actual transaction if recorded
    val note: String? = null,
    val paidAt: Long = System.currentTimeMillis()
)
