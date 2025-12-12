package com.theblankstate.epmanager.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Custom SMS template for banks not in the default registry
 * Users can add their own bank SMS patterns
 */
@Entity(tableName = "sms_templates")
data class SmsTemplate(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    
    // Bank/Sender identification
    val bankName: String,                    // User-friendly name (e.g., "My Bank")
    val senderIds: String,                   // Comma-separated sender IDs (e.g., "MYBANK,MY-BANK")
    
    // Pattern detection (learned from sample SMS)
    val amountPattern: String? = null,       // Regex for amount extraction
    val accountPattern: String? = null,      // Regex for account number extraction
    val merchantPattern: String? = null,     // Regex for merchant extraction
    val debitKeywords: String? = null,       // Comma-separated debit keywords
    val creditKeywords: String? = null,      // Comma-separated credit keywords
    
    // Sample SMS used for learning
    val sampleSms: String? = null,
    
    // Linking to account
    val linkedAccountId: String? = null,     // Auto-assign to this account
    
    // Status
    val isActive: Boolean = true,
    val isCustom: Boolean = true,            // true if user-created
    val createdAt: Long = System.currentTimeMillis(),
    val lastUsedAt: Long? = null,
    val usageCount: Int = 0
)

/**
 * Result of AI-based SMS parsing/learning
 */
data class SmsParseResult(
    val amount: Double?,
    val isDebit: Boolean,
    val merchantName: String?,
    val accountHint: String?,
    val bankName: String?,
    val transactionDate: String?,
    val referenceNumber: String?,
    val balance: Double?,
    val confidence: Float,           // 0.0 to 1.0
    val suggestedPatterns: SuggestedPatterns?
)

/**
 * Patterns suggested by AI for future parsing
 */
data class SuggestedPatterns(
    val amountPattern: String?,
    val accountPattern: String?,
    val merchantPattern: String?,
    val debitKeywords: List<String>,
    val creditKeywords: List<String>
)

/**
 * Pending SMS that needs user review
 */
@Entity(tableName = "pending_sms")
data class PendingSms(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val senderId: String,
    val messageBody: String,
    val receivedAt: Long = System.currentTimeMillis(),
    val parsedAmount: Double? = null,
    val parsedIsDebit: Boolean? = null,
    val parsedMerchant: String? = null,
    val parsedAccountHint: String? = null,
    val status: PendingSmsStatus = PendingSmsStatus.PENDING,
    val linkedTransactionId: String? = null
)

enum class PendingSmsStatus {
    PENDING,           // Waiting for user review
    APPROVED,          // User approved, transaction created
    REJECTED,          // User rejected, not a transaction
    AUTO_PROCESSED     // Auto-processed successfully
}
