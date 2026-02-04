package com.theblankstate.epmanager.sms

import android.content.Context
import android.net.Uri
import android.util.Log
import com.theblankstate.epmanager.data.model.BankRegistry
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Represents a bank discovered from SMS scanning
 * May have multiple sender IDs if the same bank uses different IDs
 */
data class DiscoveredBank(
    val senderIds: List<String>,  // All discovered sender IDs for this bank
    val senderId: String,  // Primary sender ID (for compatibility)
    val newSenderIds: List<String>,  // Sender IDs NOT already in registry patterns
    val bankInfo: BankRegistry.BankInfo?,
    val bankName: String,
    val transactionCount: Int,
    val sampleSms: String?,
    val lastTransactionDate: Long,
    val isKnownBank: Boolean = bankInfo != null,
    val hasNewSenderIds: Boolean = newSenderIds.isNotEmpty()  // True if we found new IDs
)

/**
 * Result of bank discovery scan
 */
data class BankDiscoveryResult(
    val scannedCount: Int,
    val detectedBanks: List<DiscoveredBank>,
    val unknownSenders: List<DiscoveredBank>,
    val isComplete: Boolean = false
)

/**
 * Scanner that discovers banks from SMS inbox by looking for transaction patterns.
 * Unlike SmsInboxScanner which only processes linked accounts, this scans ALL SMS
 * to find potential banks the user has.
 * 
 * Detection criteria:
 * - Contains debit/credit keywords (debited, credited, paid, received, etc.)
 * - Contains account references (A/C, account, ac, card)
 * - Contains amount patterns (Rs, INR, ₹)
 */
@Singleton
class BankDiscoveryScanner @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "BankDiscoveryScanner"
        
        // Transaction indicator patterns
        private val DEBIT_KEYWORDS = listOf(
            "debited", "debit", "spent", "paid", "purchase", "withdrawn", 
            "payment", "transferred", "sent", "txn"
        )
        
        private val CREDIT_KEYWORDS = listOf(
            "credited", "credit", "received", "deposited", "refund", 
            "cashback", "reversed", "added"
        )
        
        // Account reference patterns
        private val ACCOUNT_PATTERNS = listOf(
            "a/c", "ac ", "acct", "account", "card", "xx", "**"
        )
        
        // Amount patterns
        private val AMOUNT_REGEX = Regex(
            """(?:Rs\.?|INR|₹)\s*[\d,]+(?:\.\d{1,2})?""",
            RegexOption.IGNORE_CASE
        )
    }
    
    /**
     * Scan SMS inbox to discover banks/financial institutions
     * @param startTime Only scan SMS from this timestamp onwards
     * @return Flow of discovery results with progress updates
     */
    fun discoverBanks(startTime: Long): Flow<BankDiscoveryResult> = flow {
        val senderMap = mutableMapOf<String, MutableList<SmsData>>()
        var scannedCount = 0
        
        val uri = Uri.parse("content://sms/inbox")
        val projection = arrayOf("_id", "address", "body", "date")
        val selection = "date >= ?"
        val selectionArgs = arrayOf(startTime.toString())
        val sortOrder = "date DESC"
        
        try {
            val cursor = context.contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )
            
            cursor?.use {
                val senderCol = it.getColumnIndex("address")
                val bodyCol = it.getColumnIndex("body")
                val dateCol = it.getColumnIndex("date")
                
                while (it.moveToNext()) {
                    val sender = it.getString(senderCol) ?: continue
                    val body = it.getString(bodyCol) ?: continue
                    val date = it.getLong(dateCol)
                    
                    scannedCount++
                    
                    // Check if this looks like a financial/transaction SMS
                    if (isTransactionSms(body)) {
                        val normalizedSender = normalizeSenderId(sender)
                        
                        if (!senderMap.containsKey(normalizedSender)) {
                            senderMap[normalizedSender] = mutableListOf()
                        }
                        senderMap[normalizedSender]?.add(SmsData(body, date))
                    }
                    
                    // Emit progress every 50 messages
                    if (scannedCount % 50 == 0) {
                        emit(buildResult(senderMap, scannedCount, false))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning SMS", e)
        }
        
        // Emit final result
        emit(buildResult(senderMap, scannedCount, true))
    }.flowOn(Dispatchers.IO)
    
    /**
     * Check if an SMS body looks like a financial transaction
     */
    private fun isTransactionSms(body: String): Boolean {
        val lowerBody = body.lowercase()
        
        // Must have an amount pattern
        if (!AMOUNT_REGEX.containsMatchIn(body)) {
            return false
        }
        
        // Must have debit or credit keywords
        val hasTransactionKeyword = DEBIT_KEYWORDS.any { lowerBody.contains(it) } ||
                CREDIT_KEYWORDS.any { lowerBody.contains(it) }
        
        if (!hasTransactionKeyword) {
            return false
        }
        
        // Should have account reference (but not required for UPI)
        val hasAccountRef = ACCOUNT_PATTERNS.any { lowerBody.contains(it) }
        val isUpiRelated = lowerBody.contains("upi") || lowerBody.contains("@")
        
        return hasAccountRef || isUpiRelated
    }
    
    /**
     * Normalize sender ID for grouping
     * e.g., "VM-HDFCBK" -> "HDFCBK", "JD-SBIINB" -> "SBIINB"
     */
    private fun normalizeSenderId(sender: String): String {
        // Remove common prefixes like VM-, VK-, JD-, etc.
        val cleaned = sender.uppercase()
            .replace(Regex("^[A-Z]{2}-"), "")
            .replace("-", "")
            .trim()
        
        return cleaned
    }
    
    /**
     * Build result from collected sender data
     * Groups senders by detected bank (for known banks)
     */
    private fun buildResult(
        senderMap: Map<String, List<SmsData>>,
        scannedCount: Int,
        isComplete: Boolean
    ): BankDiscoveryResult {
        // Group senders by bank code for known banks
        val bankGroupMap = mutableMapOf<String, MutableList<Pair<String, List<SmsData>>>>()
        val unknownSenders = mutableListOf<DiscoveredBank>()
        
        for ((senderId, smsList) in senderMap) {
            if (smsList.isEmpty()) continue
            
            // Try to match to known bank
            val bankInfo = BankRegistry.findBankBySender(senderId)
            
            if (bankInfo != null) {
                // Group by bank code
                val bankCode = bankInfo.code
                if (!bankGroupMap.containsKey(bankCode)) {
                    bankGroupMap[bankCode] = mutableListOf()
                }
                bankGroupMap[bankCode]?.add(senderId to smsList)
            } else if (smsList.size >= 2) {
                // Unknown sender with at least 2 transactions
                unknownSenders.add(
                    DiscoveredBank(
                        senderIds = listOf(senderId),
                        senderId = senderId,
                        newSenderIds = listOf(senderId),  // All IDs are "new" for unknown senders
                        bankInfo = null,
                        bankName = inferBankName(senderId),
                        transactionCount = smsList.size,
                        sampleSms = smsList.firstOrNull()?.body?.take(200),
                        lastTransactionDate = smsList.maxOfOrNull { it.date } ?: 0L,
                        isKnownBank = false
                    )
                )
            }
        }
        
        // Build detected banks list (grouped by bank)
        val detectedBanks = bankGroupMap.map { (bankCode, senderDataList) ->
            val bankInfo = BankRegistry.findBankByCode(bankCode)
            val allSenderIds = senderDataList.map { it.first }
            val allSmsList = senderDataList.flatMap { it.second }
            
            // Find sender IDs that are NOT already in registry patterns
            val registryPatterns = bankInfo?.senderPatterns?.map { it.uppercase() } ?: emptyList()
            val newIds = allSenderIds.filter { senderId ->
                // Check if this sender ID is covered by any registry pattern
                !registryPatterns.any { pattern -> senderId.uppercase().contains(pattern) }
            }
            
            DiscoveredBank(
                senderIds = allSenderIds,
                senderId = allSenderIds.joinToString(","),  // Combined for display
                newSenderIds = newIds,  // Only IDs not in registry
                bankInfo = bankInfo,
                bankName = bankInfo?.name ?: bankCode,
                transactionCount = allSmsList.size,
                sampleSms = allSmsList.maxByOrNull { it.date }?.body?.take(200),
                lastTransactionDate = allSmsList.maxOfOrNull { it.date } ?: 0L,
                isKnownBank = true
            )
        }
        
        // Sort by transaction count
        return BankDiscoveryResult(
            scannedCount = scannedCount,
            detectedBanks = detectedBanks.sortedByDescending { it.transactionCount },
            unknownSenders = unknownSenders.sortedByDescending { it.transactionCount },
            isComplete = isComplete
        )
    }
    
    /**
     * Try to infer a readable bank name from sender ID
     */
    private fun inferBankName(senderId: String): String {
        // Try to make it more readable
        val cleaned = senderId
            .replace(Regex("[0-9]"), "")
            .replace("BK", " Bank")
            .replace("BNK", " Bank")
            .replace("INB", "")
            .replace("SMS", "")
            .trim()
        
        return if (cleaned.length >= 3) {
            cleaned.take(15) + if (cleaned.length > 15) "..." else ""
        } else {
            senderId
        }
    }
    
    private data class SmsData(val body: String, val date: Long)
}
