package com.theblankstate.epmanager.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Represents a financial transaction (expense or income)
 */
@Entity(
    tableName = "transactions",
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
        Index("date"),
        Index("type")
    ]
)
data class Transaction(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val amount: Double,
    val type: TransactionType,
    val categoryId: String?,
    val accountId: String?,
    val date: Long, // Timestamp in millis
    val note: String? = null,
    val isRecurring: Boolean = false,
    val recurringId: String? = null, // Link to recurring expense template
    val goalId: String? = null, // Link to savings goal
    val debtId: String? = null, // Link to debt/credit
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false, // For cloud sync tracking
    
    // Location data
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationName: String? = null, // Human-readable location name
    
    // SMS parsed metadata
    val smsSender: String? = null, // SMS sender ID (e.g., "VM-HDFC")
    val senderName: String? = null, // Who sent money (for credits)
    val receiverName: String? = null, // Who received money (for debits)
    val refNumber: String? = null, // Transaction reference number
    val merchantName: String? = null, // Merchant or payee name
    val upiId: String? = null, // UPI ID if available
    val originalSms: String? = null // Complete original SMS message
)

enum class TransactionType {
    EXPENSE,
    INCOME
}

/**
 * Transaction with related category and account info
 */
data class TransactionWithDetails(
    val transaction: Transaction,
    val category: Category?,
    val account: Account?
)
