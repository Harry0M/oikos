package com.theblankstate.epmanager.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Represents an available bank for the user on this device.
 * 
 * This entity combines:
 * - Scanned banks (discovered from SMS)
 * - Custom banks (manually added by user)
 * - Unrecognized sender IDs (detected but not matched to known banks)
 * 
 * This replaces the global BankRegistry with a per-device bank list.
 * Also incorporates SmsTemplate functionality for parsing patterns.
 */
@Entity(tableName = "available_banks")
data class AvailableBank(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    
    // Bank identification
    val bankCode: String,                    // e.g., "HDFC", "SBI", or custom ID
    val bankName: String,                    // Display name (e.g., "HDFC Bank")
    val senderIds: String,                   // Comma-separated sender IDs (e.g., "HDFCBK,HDFC")
    val color: Long = 0xFF3B82F6,            // Bank color for UI (default blue)
    
    // Discovery info
    val transactionCount: Int = 0,           // Number of transactions detected
    val lastTransactionDate: Long = 0,       // Timestamp of last transaction
    val sampleSms: String? = null,           // Sample SMS for reference
    
    // Source tracking
    val source: AvailableBankSource = AvailableBankSource.SCANNED,
    val isKnownBank: Boolean = false,        // True if matched to global BankRegistry
    
    // SMS parsing patterns (merged from SmsTemplate)
    val amountPattern: String? = null,       // Regex for amount extraction
    val accountPattern: String? = null,      // Regex for account number extraction
    val merchantPattern: String? = null,     // Regex for merchant extraction
    val debitKeywords: String? = null,       // Comma-separated debit keywords
    val creditKeywords: String? = null,      // Comma-separated credit keywords
    
    // Account linking
    val linkedAccountId: String? = null,     // Auto-assign to this account
    
    // Status
    val isActive: Boolean = true,
    val usageCount: Int = 0,                 // How many times used for parsing
    val lastUsedAt: Long? = null,
    
    // Timestamps
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Get list of sender IDs
     */
    fun getSenderIdList(): List<String> = 
        senderIds.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    
    /**
     * Check if this bank handles the given sender ID
     */
    fun matchesSender(senderId: String): Boolean {
        val normalizedSender = senderId.uppercase().replace("-", "")
        return getSenderIdList().any { pattern ->
            normalizedSender.contains(pattern.uppercase())
        }
    }
    
    companion object {
        /**
         * Create from a discovered bank (from BankDiscoveryScanner)
         */
        fun fromDiscoveredBank(
            bankCode: String,
            bankName: String,
            senderIds: List<String>,
            color: Long,
            transactionCount: Int,
            lastTransactionDate: Long,
            sampleSms: String?,
            isKnownBank: Boolean
        ): AvailableBank = AvailableBank(
            bankCode = bankCode,
            bankName = bankName,
            senderIds = senderIds.joinToString(","),
            color = color,
            transactionCount = transactionCount,
            lastTransactionDate = lastTransactionDate,
            sampleSms = sampleSms,
            source = if (isKnownBank) AvailableBankSource.SCANNED else AvailableBankSource.UNKNOWN_SENDER,
            isKnownBank = isKnownBank
        )
        
        /**
         * Create a custom bank (manually added by user)
         */
        fun createCustom(
            bankName: String,
            senderIds: String,
            linkedAccountId: String? = null
        ): AvailableBank = AvailableBank(
            bankCode = bankName.uppercase().replace(" ", "_").take(10),
            bankName = bankName,
            senderIds = senderIds,
            source = AvailableBankSource.CUSTOM,
            isKnownBank = false,
            linkedAccountId = linkedAccountId
        )
    }
}

/**
 * Source of how the bank was added to the user's available banks
 */
enum class AvailableBankSource {
    SCANNED,        // Discovered from SMS scan (known bank)
    CUSTOM,         // Manually added by user
    UNKNOWN_SENDER  // Discovered from SMS but not matched to known bank registry
}
