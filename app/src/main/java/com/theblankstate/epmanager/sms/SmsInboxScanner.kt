package com.theblankstate.epmanager.sms

import android.content.Context
import android.net.Uri
import android.util.Log
import com.theblankstate.epmanager.data.local.ExpenseDatabase
import com.theblankstate.epmanager.data.model.Account
import com.theblankstate.epmanager.data.model.RecurringExpense
import com.theblankstate.epmanager.data.model.Transaction
import com.theblankstate.epmanager.data.model.TransactionType
import com.theblankstate.epmanager.data.repository.AccountRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class SmsInboxScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val smsParser: SmsParser,
    private val accountRepository: AccountRepository,
    private val db: ExpenseDatabase
) {
    
    data class ScanResult(
        val scannedCount: Int,
        val foundCount: Int,
        val newTransactionsCount: Int,
        val isComplete: Boolean = false
    )
    
    fun scanMessages(startTime: Long): Flow<ScanResult> = flow {
        var scannedCount = 0
        var foundCount = 0
        var newTransactionsCount = 0
        
        // Get linked accounts first to filter relevant SMS
        val linkedAccounts = accountRepository.getLinkedAccounts().first()
        if (linkedAccounts.isEmpty()) {
            emit(ScanResult(0, 0, 0, true))
            return@flow
        }
        
        val uri = Uri.parse("content://sms/inbox")
        val projection = arrayOf("_id", "address", "body", "date")
        val selection = "date >= ?"
        val selectionArgs = arrayOf(startTime.toString())
        val sortOrder = "date ASC" // Process oldest first
        
        try {
            val cursor = context.contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )
            
            cursor?.use {
                val totalMessages = it.count
                val senderCol = it.getColumnIndex("address")
                val bodyCol = it.getColumnIndex("body")
                val dateCol = it.getColumnIndex("date")
                
                while (it.moveToNext()) {
                    val sender = it.getString(senderCol) ?: continue
                    val body = it.getString(bodyCol) ?: continue
                    val date = it.getLong(dateCol)
                    
                    scannedCount++
                    
                    // Report progress every 20 messages
                    if (scannedCount % 20 == 0) {
                        emit(ScanResult(scannedCount, foundCount, newTransactionsCount))
                    }
                    
                    // 1. Check if sender is relevant (linked to an account)
                    if (!isRelevantSender(sender, linkedAccounts)) continue
                    
                    // 2. Parse SMS
                    val parsed = smsParser.parse(body, sender) ?: continue
                    foundCount++
                    
                    // 3. Check for duplicates and save
                    if (saveIfUnique(parsed, sender, date, linkedAccounts)) {
                        newTransactionsCount++
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SmsInboxScanner", "Error scanning SMS", e)
        }
        
        emit(ScanResult(scannedCount, foundCount, newTransactionsCount, true))
    }.flowOn(Dispatchers.IO)
    
    private fun isRelevantSender(sender: String, linkedAccounts: List<Account>): Boolean {
        val normalizedSender = sender.uppercase().replace("-", "")
        
        return linkedAccounts.any { account ->
            // Check sender IDs
            val senderMatches = account.linkedSenderIds?.split(",")
                ?.any { normalizedSender.contains(it.trim().uppercase()) } ?: false
            
            // Check bank code
            val bankMatches = account.bankCode?.let { 
                normalizedSender.contains(it.uppercase()) 
            } ?: false
            
            senderMatches || bankMatches
        }
    }
    
    private suspend fun saveIfUnique(
        parsed: SmsParser.ParsedTransaction, 
        sender: String, 
        date: Long,
        linkedAccounts: List<Account>
    ): Boolean {
        val transactionDao = db.transactionDao()
        
        // Check by Reference Number first (strongest match)
        if (!parsed.referenceNumber.isNullOrEmpty()) {
            val existing = transactionDao.findByReferenceNumber(parsed.referenceNumber)
            if (existing != null) return false
        }
        
        // Check fuzzy match (Sender + Amount + Approx Time)
        val window = 24 * 60 * 60 * 1000L
        val potentialDuplicates = transactionDao.findPotentialDuplicates(
            sender = sender,
            amount = parsed.amount,
            startTime = date - window,
            endTime = date + window
        )
        
        if (potentialDuplicates.isNotEmpty()) return false
        
        // Find matching account
        val matchedAccountId = findMatchingAccountId(parsed, sender, linkedAccounts)
        
        // ========== SMART DUPLICATE DETECTION FOR RECURRING ==========
        if (matchedAccountId != null) {
            val matchedRecurring = findMatchingRecurringExpense(
                accountId = matchedAccountId,
                amount = parsed.amount,
                isDebit = parsed.isDebit,
                smsDate = date
            )
            
            if (matchedRecurring != null) {
                val twoDaysMs = 2 * 24 * 60 * 60 * 1000L
                
                val existingTransaction = transactionDao.findRecurringTransaction(
                    recurringId = matchedRecurring.id,
                    startDate = date - twoDaysMs,
                    endDate = date + twoDaysMs
                )
                
                if (existingTransaction != null) {
                    // Update existing recurring transaction with SMS details
                    transactionDao.updateTransactionWithSmsDetails(
                        transactionId = existingTransaction.id,
                        smsSender = sender,
                        originalSms = parsed.originalMessage,
                        refNumber = parsed.referenceNumber,
                        upiId = parsed.upiId,
                        merchantName = parsed.merchantName,
                        senderName = parsed.senderName,
                        receiverName = parsed.receiverName
                    )
                    return false // Not a new transaction
                } else {
                    // Create transaction linked to recurring
                    val categoryId = tryDetectCategory(parsed.merchantName) ?: matchedRecurring.categoryId
                    
                    val transaction = Transaction(
                        amount = parsed.amount,
                        type = if (parsed.isDebit) TransactionType.EXPENSE else TransactionType.INCOME,
                        categoryId = categoryId,
                        accountId = matchedAccountId,
                        date = date,
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
                        locationName = null
                    )
                    
                    transactionDao.insertTransaction(transaction)
                    
                    // Update account balance
                    val balanceChange = if (parsed.isDebit) -parsed.amount else parsed.amount
                    accountRepository.updateBalance(matchedAccountId, balanceChange)
                    
                    return true
                }
            }
        }
        // ========== END SMART DUPLICATE DETECTION ==========
        
        // No recurring match - create normal transaction
        val categoryId = tryDetectCategory(parsed.merchantName)
        
        val transaction = Transaction(
            amount = parsed.amount,
            type = if (parsed.isDebit) TransactionType.EXPENSE else TransactionType.INCOME,
            categoryId = categoryId,
            accountId = matchedAccountId,
            date = date,
            note = buildNote(parsed, sender, matchedAccountId != null),
            isSynced = false,
            smsSender = sender,
            merchantName = parsed.merchantName,
            refNumber = parsed.referenceNumber,
            upiId = parsed.upiId,
            senderName = parsed.senderName,
            receiverName = parsed.receiverName,
            originalSms = parsed.originalMessage,
            locationName = null
        )
        
        transactionDao.insertTransaction(transaction)
        
        // Update balance if matched
        if (matchedAccountId != null) {
            val balanceChange = if (parsed.isDebit) -parsed.amount else parsed.amount
            accountRepository.updateBalance(matchedAccountId, balanceChange)
        }
        
        return true
    }
    
    /**
     * Find a recurring expense that matches the SMS transaction
     */
    private suspend fun findMatchingRecurringExpense(
        accountId: String,
        amount: Double,
        isDebit: Boolean,
        smsDate: Long
    ): RecurringExpense? {
        val amountTolerance = 5.0
        val dayTolerance = 2 * 24 * 60 * 60 * 1000L
        
        val matches = db.recurringExpenseDao().findMatchingRecurringExpense(
            accountId = accountId,
            minAmount = amount - amountTolerance,
            maxAmount = amount + amountTolerance,
            startDate = smsDate - dayTolerance,
            endDate = smsDate + dayTolerance
        )
        
        val expectedType = if (isDebit) TransactionType.EXPENSE else TransactionType.INCOME
        return matches.firstOrNull { it.type == expectedType }
    }
    
    private fun findMatchingAccountId(
        parsed: SmsParser.ParsedTransaction,
        sender: String,
        linkedAccounts: List<Account>
    ): String? {
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
    
    private suspend fun tryDetectCategory(merchantName: String?): String {
        if (merchantName.isNullOrBlank()) return "bills"
        val lowerMerchant = merchantName.lowercase()
        
        return when {
            lowerMerchant.contains("swiggy") || lowerMerchant.contains("zomato") || 
            lowerMerchant.contains("restaurant") || lowerMerchant.contains("cafe") -> "food"
            
            lowerMerchant.contains("amazon") || lowerMerchant.contains("flipkart") || 
            lowerMerchant.contains("myntra") -> "shopping"
            
            lowerMerchant.contains("uber") || lowerMerchant.contains("ola") || 
            lowerMerchant.contains("rapido") || lowerMerchant.contains("petrol") -> "transportation"
            
            lowerMerchant.contains("electricity") || lowerMerchant.contains("water") || 
            lowerMerchant.contains("gas") || lowerMerchant.contains("bill") -> "bills"
            
            lowerMerchant.contains("netflix") || lowerMerchant.contains("hotstar") -> "entertainment"
            
            else -> "bills"
        }
    }
    
    private fun buildNote(
        parsed: SmsParser.ParsedTransaction, 
        sender: String,
        accountMatched: Boolean
    ): String {
        return buildString {
            append(if (accountMatched) "[Auto] " else "[Scan] ")
            
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
}
