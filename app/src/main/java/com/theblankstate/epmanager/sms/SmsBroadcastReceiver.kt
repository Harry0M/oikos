package com.theblankstate.epmanager.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import android.location.Location
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
import com.theblankstate.epmanager.util.LocationHelper

class SmsBroadcastReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "SmsBroadcastReceiver"
    }
    
    private val parser = SmsParser()
    private val aiParser = AiSmsParser()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // Lazy initialized helper (context needed)
    private lateinit var locationHelper: LocationHelper
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        
        // Initialize location helper
        if (!::locationHelper.isInitialized) {
            locationHelper = LocationHelper(context.applicationContext)
        }
        
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
     * IMPORTANT: Only processes SMS from banks that are linked to user accounts
     */
    private suspend fun processSmsFast(context: Context, body: String, sender: String) {
        val db = ExpenseDatabase.getInstance(context)
        val accountRepository = AccountRepository(db.accountDao())
        
        // CRITICAL: Check if this sender has a linked account
        // If no linked account matches, skip processing entirely
        val hasLinkedAccount = checkSenderHasLinkedAccount(accountRepository, sender)
        if (!hasLinkedAccount) {
            Log.d(TAG, "No linked account for sender: $sender - skipping SMS parsing")
            return
        }
        
        Log.d(TAG, "Found linked account for sender: $sender - processing...")
        
        // Try to get location
        val location = locationHelper.getCurrentLocation()
        val locationName = location?.let { locationHelper.getLocationName(it) }
        
        // Step 1: Quick regex parse (immediate)
        val regexParsed = parser.parse(body, sender)
        
        if (regexParsed != null && regexParsed.amount > 0) {
            // We got a valid parse, save immediately
            saveTransactionFromRegex(context, db, regexParsed, sender, location, locationName)
        } else {
            // Regex failed, save as pending and try AI
            savePendingSms(db, body, sender)
            
            // Try AI parsing in background (don't block)
            tryAiParsingInBackground(context, db, body, sender, location, locationName)
        }
    }
    
    /**
     * Check if the SMS sender matches any linked account
     * Returns true only if user has an account linked to this bank/sender
     */
    private suspend fun checkSenderHasLinkedAccount(
        accountRepository: AccountRepository,
        sender: String
    ): Boolean {
        val linkedAccounts = accountRepository.getLinkedAccounts().first()
        if (linkedAccounts.isEmpty()) return false
        
        val normalizedSender = sender.uppercase().replace("-", "")
        
        return linkedAccounts.any { account ->
            // Check if any linked sender IDs match
            val senderMatches = account.linkedSenderIds?.split(",")
                ?.any { normalizedSender.contains(it.trim().uppercase()) } ?: false
            
            // Check if bank code matches
            val bankMatches = account.bankCode?.let { 
                normalizedSender.contains(it.uppercase()) 
            } ?: false
            
            senderMatches || bankMatches
        }
    }
    
    /**
     * Save transaction from regex parsing (fast path)
     * Also updates the linked account balance
     * 
     * Smart duplicate detection:
     * - Checks if SMS matches an existing recurring expense
     * - If found, updates existing transaction with SMS details instead of creating duplicate
     */
    private suspend fun saveTransactionFromRegex(
        context: Context,
        db: ExpenseDatabase,
        parsed: SmsParser.ParsedTransaction,
        sender: String,
        location: Location?,
        locationName: String?
    ) {
        val accountRepository = AccountRepository(db.accountDao())
        
        // Find matching account
        val matchedAccountId = findMatchingAccountId(accountRepository, parsed, sender)
        
        // ========== SMART DUPLICATE DETECTION ==========
        // Check if this SMS matches an existing recurring expense
        if (matchedAccountId != null) {
            val matchedRecurring = findMatchingRecurringExpense(
                db = db,
                accountId = matchedAccountId,
                amount = parsed.amount,
                isDebit = parsed.isDebit
            )
            
            if (matchedRecurring != null) {
                // Found matching recurring expense - check if transaction already exists
                val twoDaysMs = 2 * 24 * 60 * 60 * 1000L
                val now = System.currentTimeMillis()
                
                val existingTransaction = db.transactionDao().findRecurringTransaction(
                    recurringId = matchedRecurring.id,
                    startDate = now - twoDaysMs,
                    endDate = now + twoDaysMs
                )
                
                if (existingTransaction != null) {
                    // Transaction already exists from recurring - just add SMS details
                    db.transactionDao().updateTransactionWithSmsDetails(
                        transactionId = existingTransaction.id,
                        smsSender = sender,
                        originalSms = parsed.originalMessage,
                        refNumber = parsed.referenceNumber,
                        upiId = parsed.upiId,
                        merchantName = parsed.merchantName,
                        senderName = parsed.senderName,
                        receiverName = parsed.receiverName
                    )
                    Log.d(TAG, "SMS matched recurring expense '${matchedRecurring.name}' - updated existing transaction with SMS details")
                    return
                } else {
                    // No existing transaction - create new one linked to recurring
                    val categoryId = tryDetectCategory(db, parsed.merchantName) ?: matchedRecurring.categoryId
                    
                    val transaction = Transaction(
                        amount = parsed.amount,
                        type = if (parsed.isDebit) TransactionType.EXPENSE else TransactionType.INCOME,
                        categoryId = categoryId,
                        accountId = matchedAccountId,
                        date = System.currentTimeMillis(),
                        note = buildNote(parsed, sender, true),
                        isRecurring = true,
                        recurringId = matchedRecurring.id,
                        isSynced = false,
                        smsSender = sender,
                        merchantName = parsed.merchantName,
                        refNumber = parsed.referenceNumber,
                        upiId = parsed.upiId,
                        senderName = parsed.senderName,
                        receiverName = parsed.receiverName,
                        originalSms = parsed.originalMessage,
                        latitude = location?.latitude,
                        longitude = location?.longitude,
                        locationName = locationName
                    )
                    
                    db.transactionDao().insertTransaction(transaction)
                    
                    // Update recurring expense due date
                    val nextDue = calculateNextDueDate(matchedRecurring)
                    db.recurringExpenseDao().updateDueDate(
                        id = matchedRecurring.id,
                        nextDueDate = nextDue,
                        processedDate = System.currentTimeMillis()
                    )
                    
                    Log.d(TAG, "SMS matched recurring expense '${matchedRecurring.name}' - created linked transaction")
                    
                    // Update account balance
                    val balanceChange = if (parsed.isDebit) -parsed.amount else parsed.amount
                    accountRepository.updateBalance(matchedAccountId, balanceChange)
                    
                    return
                }
            }
        }
        // ========== END SMART DUPLICATE DETECTION ==========
        
        // No recurring match found - create normal transaction
        val categoryId = tryDetectCategory(db, parsed.merchantName)
        
        val transaction = Transaction(
            amount = parsed.amount,
            type = if (parsed.isDebit) TransactionType.EXPENSE else TransactionType.INCOME,
            categoryId = categoryId,
            accountId = matchedAccountId,
            date = System.currentTimeMillis(),
            note = buildNote(parsed, sender, matchedAccountId != null),
            isSynced = false,
            // SMS metadata
            smsSender = sender,
            merchantName = parsed.merchantName,
            refNumber = parsed.referenceNumber,
            upiId = parsed.upiId,
            senderName = parsed.senderName,
            receiverName = parsed.receiverName,
            originalSms = parsed.originalMessage,
            // Location metadata
            latitude = location?.latitude,
            longitude = location?.longitude,
            locationName = locationName
        )
        
        db.transactionDao().insertTransaction(transaction)
        Log.d(TAG, "Transaction saved immediately: ${parsed.amount}")
        
        // Update account balance if matched
        if (matchedAccountId != null) {
            val balanceChange = if (parsed.isDebit) -parsed.amount else parsed.amount
            accountRepository.updateBalance(matchedAccountId, balanceChange)
            Log.d(TAG, "Account $matchedAccountId balance updated by: $balanceChange")
        }
        
        // Save pending account info if no match
        if (matchedAccountId == null && parsed.accountHint != null) {
            savePendingAccountInfo(context, parsed, sender)
        }
    }
    
    /**
     * Find a recurring expense that matches the SMS transaction
     * Criteria: same account, amount within ±5, due date within ±2 days
     */
    private suspend fun findMatchingRecurringExpense(
        db: ExpenseDatabase,
        accountId: String,
        amount: Double,
        isDebit: Boolean
    ): com.theblankstate.epmanager.data.model.RecurringExpense? {
        val amountTolerance = 5.0
        val dayTolerance = 2 * 24 * 60 * 60 * 1000L // 2 days in ms
        val now = System.currentTimeMillis()
        
        val matches = db.recurringExpenseDao().findMatchingRecurringExpense(
            accountId = accountId,
            minAmount = amount - amountTolerance,
            maxAmount = amount + amountTolerance,
            startDate = now - dayTolerance,
            endDate = now + dayTolerance
        )
        
        // Filter by transaction type (expense/income should match)
        val expectedType = if (isDebit) 
            com.theblankstate.epmanager.data.model.TransactionType.EXPENSE 
        else 
            com.theblankstate.epmanager.data.model.TransactionType.INCOME
            
        return matches.firstOrNull { it.type == expectedType }
    }
    
    /**
     * Calculate next due date based on recurring frequency
     */
    private fun calculateNextDueDate(recurring: com.theblankstate.epmanager.data.model.RecurringExpense): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = recurring.nextDueDate
        
        when (recurring.frequency) {
            com.theblankstate.epmanager.data.model.RecurringFrequency.DAILY -> calendar.add(java.util.Calendar.DAY_OF_MONTH, 1)
            com.theblankstate.epmanager.data.model.RecurringFrequency.WEEKLY -> calendar.add(java.util.Calendar.WEEK_OF_YEAR, 1)
            com.theblankstate.epmanager.data.model.RecurringFrequency.BIWEEKLY -> calendar.add(java.util.Calendar.WEEK_OF_YEAR, 2)
            com.theblankstate.epmanager.data.model.RecurringFrequency.MONTHLY -> calendar.add(java.util.Calendar.MONTH, 1)
            com.theblankstate.epmanager.data.model.RecurringFrequency.QUARTERLY -> calendar.add(java.util.Calendar.MONTH, 3)
            com.theblankstate.epmanager.data.model.RecurringFrequency.YEARLY -> calendar.add(java.util.Calendar.YEAR, 1)
        }
        
        return calendar.timeInMillis
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
     * Also updates the linked account balance
     */
    private fun tryAiParsingInBackground(
        context: Context,
        db: ExpenseDatabase,
        body: String,
        sender: String,
        location: Location?,
        locationName: String?
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
                        isSynced = false,
                        // Location metadata
                        latitude = location?.latitude,
                        longitude = location?.longitude,
                        locationName = locationName
                    )
                    
                    db.transactionDao().insertTransaction(transaction)
                    
                    // Update account balance if matched
                    if (matchedAccountId != null) {
                        val balanceChange = if (aiResult.isDebit) -aiResult.amount else aiResult.amount
                        accountRepository.updateBalance(matchedAccountId, balanceChange)
                        Log.d(TAG, "AI: Account $matchedAccountId balance updated by: $balanceChange")
                    }
                    
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
     * Defaults to "bills" for all bank SMS transactions
     */
    private suspend fun tryDetectCategory(db: ExpenseDatabase, merchantName: String?): String {
        if (merchantName.isNullOrBlank()) return "bills" // Default for bank SMS
        
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
            
            // Default to bills for all bank SMS
            else -> "bills"
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
