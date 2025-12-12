package com.theblankstate.epmanager.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.theblankstate.epmanager.data.local.ExpenseDatabase
import com.theblankstate.epmanager.data.model.Transaction
import com.theblankstate.epmanager.data.model.TransactionType
import com.theblankstate.epmanager.data.model.Account
import com.theblankstate.epmanager.data.model.PendingSms
import com.theblankstate.epmanager.data.model.PendingSmsStatus
import com.theblankstate.epmanager.data.repository.AccountRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Broadcast receiver that listens for incoming SMS and parses bank transactions
 * 
 * Features:
 * 1. Immediate SMS parsing on receive
 * 2. AI-powered parsing with fallback to regex
 * 3. Custom bank template support
 * 4. Instant database insertion for immediate UI update
 */
class SmsBroadcastReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "SmsBroadcastReceiver"
    }
    
    private val parser = SmsParser()
    private val aiParser = AiSmsParser()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return
        
        // Use goAsync() to extend broadcast receiver execution time
        val pendingResult = goAsync()
        
        scope.launch {
            try {
                for (smsMessage in messages) {
                    val sender = smsMessage.displayOriginatingAddress ?: continue
                    val body = smsMessage.messageBody ?: continue
                    
                    Log.d(TAG, "SMS received from: $sender")
                    
                    // First check if this looks like a financial SMS
                    if (!isLikelyFinancialSms(body, sender)) {
                        Log.d(TAG, "Not a financial SMS, skipping")
                        continue
                    }
                    
                    // Process the SMS immediately
                    processSmsFast(context, body, sender)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing SMS", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
    
    /**
     * Quick check if SMS might be financial (runs immediately)
     */
    private fun isLikelyFinancialSms(body: String, sender: String): Boolean {
        // Check sender patterns first (fastest)
        if (parser.isBankSms(sender)) return true
        
        // Check for common transaction keywords
        val lowerBody = body.lowercase()
        val hasAmountIndicator = lowerBody.contains("rs.") || 
                                  lowerBody.contains("rs ") ||
                                  lowerBody.contains("inr") || 
                                  lowerBody.contains("₹") ||
                                  lowerBody.contains("rupees")
        
        val hasTransactionKeyword = lowerBody.contains("debited") ||
                                     lowerBody.contains("credited") ||
                                     lowerBody.contains("spent") ||
                                     lowerBody.contains("received") ||
                                     lowerBody.contains("paid") ||
                                     lowerBody.contains("transferred") ||
                                     lowerBody.contains("payment") ||
                                     lowerBody.contains("transaction")
        
        return hasAmountIndicator && hasTransactionKeyword
    }
    
    /**
     * Fast processing path - parse with regex first, then enhance with AI in background
     */
    private suspend fun processSmsFast(context: Context, body: String, sender: String) {
        val db = ExpenseDatabase.getInstance(context)
        
        // Step 1: Quick regex parse (immediate)
        val regexParsed = parser.parse(body, sender)
        
        if (regexParsed != null && regexParsed.amount > 0) {
            // We got a valid parse, save immediately
            saveTransactionFromRegex(context, db, regexParsed, sender)
        } else {
            // Regex failed, save as pending and try AI
            savePendingSms(db, body, sender)
            
            // Try AI parsing in background (don't block)
            tryAiParsingInBackground(context, db, body, sender)
        }
    }
    
    /**
     * Save transaction from regex parsing (fast path)
     */
    private suspend fun saveTransactionFromRegex(
        context: Context,
        db: ExpenseDatabase,
        parsed: SmsParser.ParsedTransaction,
        sender: String
    ) {
        val accountRepository = AccountRepository(db.accountDao())
        
        // Find matching account
        val matchedAccountId = findMatchingAccountId(accountRepository, parsed, sender)
        
        // Determine category based on merchant if possible
        val categoryId = tryDetectCategory(db, parsed.merchantName)
        
        val transaction = Transaction(
            amount = parsed.amount,
            type = if (parsed.isDebit) TransactionType.EXPENSE else TransactionType.INCOME,
            categoryId = categoryId,
            accountId = matchedAccountId,
            date = System.currentTimeMillis(),
            note = buildNote(parsed, sender, matchedAccountId != null),
            isSynced = false
        )
        
        db.transactionDao().insertTransaction(transaction)
        Log.d(TAG, "Transaction saved immediately: ${parsed.amount}")
        
        // Save pending account info if no match
        if (matchedAccountId == null && parsed.accountHint != null) {
            savePendingAccountInfo(context, parsed, sender)
        }
    }
    
    /**
     * Save as pending SMS for later processing
     */
    private suspend fun savePendingSms(db: ExpenseDatabase, body: String, sender: String) {
        val pendingSms = PendingSms(
            senderId = sender,
            messageBody = body,
            status = PendingSmsStatus.PENDING
        )
        db.smsTemplateDao().insertPendingSms(pendingSms)
        Log.d(TAG, "Saved as pending SMS")
    }
    
    /**
     * Try AI parsing in background (for complex SMS)
     */
    private fun tryAiParsingInBackground(
        context: Context,
        db: ExpenseDatabase,
        body: String,
        sender: String
    ) {
        scope.launch {
            try {
                val aiResult = aiParser.parseSms(body, sender)
                
                if (aiResult != null && aiResult.amount != null && aiResult.amount > 0) {
                    // AI parsed successfully
                    val accountRepository = AccountRepository(db.accountDao())
                    
                    val matchedAccountId = findMatchingAccountIdFromAi(
                        accountRepository, aiResult, sender
                    )
                    
                    val categoryId = tryDetectCategory(db, aiResult.merchantName)
                    
                    val transaction = Transaction(
                        amount = aiResult.amount,
                        type = if (aiResult.isDebit) TransactionType.EXPENSE else TransactionType.INCOME,
                        categoryId = categoryId,
                        accountId = matchedAccountId,
                        date = System.currentTimeMillis(),
                        note = buildNoteFromAi(aiResult, sender, matchedAccountId != null),
                        isSynced = false
                    )
                    
                    db.transactionDao().insertTransaction(transaction)
                    
                    // Update pending SMS status
                    updatePendingSmsStatus(db, body, PendingSmsStatus.AUTO_PROCESSED)
                    
                    Log.d(TAG, "AI parsed and saved: ${aiResult.amount}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "AI parsing failed", e)
            }
        }
    }
    
    /**
     * Try to detect category from merchant name
     */
    private suspend fun tryDetectCategory(db: ExpenseDatabase, merchantName: String?): String? {
        if (merchantName.isNullOrBlank()) return null
        
        // Simple keyword matching for common merchants
        val lowerMerchant = merchantName.lowercase()
        
        return when {
            // Food
            lowerMerchant.contains("swiggy") || 
            lowerMerchant.contains("zomato") ||
            lowerMerchant.contains("restaurant") ||
            lowerMerchant.contains("cafe") -> "food"
            
            // Shopping
            lowerMerchant.contains("amazon") ||
            lowerMerchant.contains("flipkart") ||
            lowerMerchant.contains("myntra") -> "shopping"
            
            // Transport
            lowerMerchant.contains("uber") ||
            lowerMerchant.contains("ola") ||
            lowerMerchant.contains("rapido") ||
            lowerMerchant.contains("petrol") ||
            lowerMerchant.contains("fuel") -> "transportation"
            
            // Bills
            lowerMerchant.contains("electricity") ||
            lowerMerchant.contains("water") ||
            lowerMerchant.contains("gas") ||
            lowerMerchant.contains("bill") -> "bills"
            
            // Entertainment
            lowerMerchant.contains("netflix") ||
            lowerMerchant.contains("spotify") ||
            lowerMerchant.contains("hotstar") ||
            lowerMerchant.contains("prime") -> "entertainment"
            
            else -> null
        }
    }
    
    private suspend fun updatePendingSmsStatus(
        db: ExpenseDatabase, 
        body: String, 
        status: PendingSmsStatus
    ) {
        // Find and update the pending SMS
        val pendingSms = db.smsTemplateDao().getAllPendingSms().first()
            .find { it.messageBody == body }
        
        if (pendingSms != null) {
            db.smsTemplateDao().updatePendingSms(pendingSms.copy(status = status))
        }
    }
    
    private suspend fun findMatchingAccountId(
        accountRepository: AccountRepository,
        parsed: SmsParser.ParsedTransaction,
        sender: String
    ): String? {
        val accounts = accountRepository.getAllAccounts().first()
        val linkedAccounts = accounts.filter { it.isLinked }
        
        if (linkedAccounts.isEmpty()) return null
        
        var bestAccount: Account? = null
        var bestScore = 0
        
        for (account in linkedAccounts) {
            val score = account.getMatchScore(sender, parsed.accountHint)
            if (score > bestScore) {
                bestScore = score
                bestAccount = account
            }
        }
        
        return if (bestScore >= 50) bestAccount?.id else null
    }
    
    private suspend fun findMatchingAccountIdFromAi(
        accountRepository: AccountRepository,
        parsed: com.theblankstate.epmanager.data.model.SmsParseResult,
        sender: String
    ): String? {
        val accounts = accountRepository.getAllAccounts().first()
        val linkedAccounts = accounts.filter { it.isLinked }
        
        if (linkedAccounts.isEmpty()) return null
        
        var bestAccount: Account? = null
        var bestScore = 0
        
        for (account in linkedAccounts) {
            val score = account.getMatchScore(sender, parsed.accountHint)
            if (score > bestScore) {
                bestScore = score
                bestAccount = account
            }
        }
        
        return if (bestScore >= 50) bestAccount?.id else null
    }
    
    private fun savePendingAccountInfo(
        context: Context,
        parsed: SmsParser.ParsedTransaction,
        sender: String
    ) {
        val prefs = context.getSharedPreferences("pending_accounts", Context.MODE_PRIVATE)
        val key = "${parsed.detectedBankCode ?: sender}_${parsed.accountHint}"
        
        val existingCount = prefs.getInt("${key}_count", 0)
        prefs.edit()
            .putString("${key}_sender", sender)
            .putString("${key}_bank", parsed.detectedBankCode)
            .putString("${key}_bankName", parsed.detectedBankName)
            .putString("${key}_accountHint", parsed.accountHint)
            .putInt("${key}_count", existingCount + 1)
            .putLong("${key}_lastSeen", System.currentTimeMillis())
            .apply()
    }
    
    private fun buildNote(
        parsed: SmsParser.ParsedTransaction, 
        sender: String,
        accountMatched: Boolean
    ): String {
        return buildString {
            if (accountMatched) {
                append("[Auto] ")
            } else {
                append("[SMS] ")
            }
            
            if (!parsed.merchantName.isNullOrBlank()) {
                append(parsed.merchantName)
            } else {
                append(if (parsed.isDebit) "Payment" else "Received")
            }
            
            if (!parsed.detectedBankName.isNullOrBlank()) {
                append(" via ${parsed.detectedBankName}")
            }
            
            if (!parsed.accountHint.isNullOrBlank()) {
                append(" (****${parsed.accountHint})")
            }
        }
    }
    
    private fun buildNoteFromAi(
        parsed: com.theblankstate.epmanager.data.model.SmsParseResult, 
        sender: String,
        accountMatched: Boolean
    ): String {
        return buildString {
            if (accountMatched) {
                append("[Auto] ")
            } else {
                append("[AI] ")
            }
            
            if (!parsed.merchantName.isNullOrBlank()) {
                append(parsed.merchantName)
            } else {
                append(if (parsed.isDebit) "Payment" else "Received")
            }
            
            if (!parsed.bankName.isNullOrBlank()) {
                append(" via ${parsed.bankName}")
            }
            
            if (!parsed.accountHint.isNullOrBlank()) {
                append(" (****${parsed.accountHint})")
            }
            
            if (!parsed.referenceNumber.isNullOrBlank()) {
                append(" Ref:${parsed.referenceNumber.take(8)}")
            }
        }
    }
}
