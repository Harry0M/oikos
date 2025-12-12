package com.theblankstate.epmanager.sms

import com.theblankstate.epmanager.data.model.BankRegistry
import java.util.regex.Pattern

/**
 * Parses bank SMS messages to extract transaction details
 * 
 * Key features:
 * - Extracts amount, transaction type (debit/credit)
 * - Detects bank from sender ID
 * - Extracts last 4 digits of account/card for matching
 * - Extracts merchant/description
 */
class SmsParser {
    
    data class ParsedTransaction(
        val amount: Double,
        val merchantName: String?,
        val isDebit: Boolean, // true = expense, false = income
        val accountHint: String?, // Last 4 digits, card type etc.
        val originalMessage: String,
        // Enhanced fields for account matching
        val detectedBankCode: String? = null, // Bank code detected from sender
        val detectedBankName: String? = null, // Human-readable bank name
        val cardType: CardType? = null, // Credit card, debit card, etc.
        val upiId: String? = null, // UPI ID if detected
        val referenceNumber: String? = null // Transaction reference if available
    )
    
    enum class CardType {
        CREDIT_CARD,
        DEBIT_CARD,
        PREPAID_CARD,
        UNKNOWN
    }
    
    companion object {
        // Common patterns for Indian bank SMS
        private val amountPatterns = listOf(
            // Rs.1,234.56 or Rs 1234.56 or INR 1234
            Pattern.compile("(?:Rs\\.?|INR|₹)\\s*([\\d,]+(?:\\.\\d{2})?)"),
            // debited by 1234.56
            Pattern.compile("(?:debited|credited|paid|spent|received)\\s+(?:by\\s+)?(?:Rs\\.?|INR|₹)?\\s*([\\d,]+(?:\\.\\d{2})?)", Pattern.CASE_INSENSITIVE),
            // Amount: 1234.56
            Pattern.compile("(?:amount|amt)[:\\s]+(?:Rs\\.?|INR|₹)?\\s*([\\d,]+(?:\\.\\d{2})?)", Pattern.CASE_INSENSITIVE)
        )
        
        private val debitKeywords = listOf(
            "debited", "debit", "spent", "paid", "purchase", "payment", 
            "withdrawn", "transfer to", "sent", "deducted", "charged"
        )
        
        private val creditKeywords = listOf(
            "credited", "credit", "received", "deposited", "refund",
            "transfer from", "cashback", "added"
        )
        
        // Patterns to extract merchant/description
        private val merchantPatterns = listOf(
            Pattern.compile("(?:at|to|for|@)\\s+([A-Za-z0-9][A-Za-z0-9\\s]*?)(?:\\s+on|\\.|$)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?:VPA|UPI)[:\\s]+([^\\s]+)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("Info[:\\s]+([^.]+)", Pattern.CASE_INSENSITIVE)
        )
        
        // Pattern for account/card last digits
        private val accountPatterns = listOf(
            Pattern.compile("(?:a/c|account|acct|ac)[\\s*:]*[Xx*]*([0-9]{4})", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?:card)[\\s*:]*[Xx*]*([0-9]{4})", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\*{2,}([0-9]{4})"),
            Pattern.compile("[Xx]{2,}([0-9]{4})")
        )
        
        // Card type patterns
        private val creditCardPatterns = listOf(
            Pattern.compile("credit\\s*card", Pattern.CASE_INSENSITIVE),
            Pattern.compile("cc\\s*\\*", Pattern.CASE_INSENSITIVE)
        )
        
        private val debitCardPatterns = listOf(
            Pattern.compile("debit\\s*card", Pattern.CASE_INSENSITIVE),
            Pattern.compile("atm\\s*card", Pattern.CASE_INSENSITIVE)
        )
        
        // UPI ID pattern
        private val upiPattern = Pattern.compile("(?:VPA|UPI)[:\\s]+([a-zA-Z0-9._-]+@[a-zA-Z]+)", Pattern.CASE_INSENSITIVE)
        
        // Reference number patterns
        private val refPatterns = listOf(
            Pattern.compile("(?:ref|reference|txn|transaction)[\\s.:#]*([A-Za-z0-9]+)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("UTR[:\\s]*([A-Za-z0-9]+)", Pattern.CASE_INSENSITIVE)
        )
        
        // Bank sender IDs to identify transaction SMS
        private val bankSenderPrefixes = listOf(
            "HDFCBK", "SBIINB", "ICICIB", "AXISBK", "KOTAKB", "PNBSMS",
            "YESBNK", "ILOYBK", "BOIIND", "CANBNK", "UCOBNK", "CENTBK",
            "PNBSMS", "UNIONB", "BOBSMS", "INDUSB", "FEDERL", "SCBANK",
            "PAYTM", "GPAY", "PHONPE", "AMAZON", "IDFCFB", "RBLBNK"
        )
    }
    
    /**
     * Check if sender ID looks like a bank/payment app
     */
    fun isBankSms(senderId: String): Boolean {
        val normalizedSender = senderId.uppercase().replace("-", "")
        return bankSenderPrefixes.any { normalizedSender.contains(it) } ||
               normalizedSender.matches(Regex("^[A-Z]{2}-[A-Z]{6}$")) ||
               normalizedSender.matches(Regex("^[A-Z]{6}$"))
    }
    
    /**
     * Detect bank from sender ID using BankRegistry
     */
    fun detectBank(senderId: String): BankRegistry.BankInfo? {
        return BankRegistry.findBankBySender(senderId)
    }
    
    /**
     * Parse SMS message to extract transaction details
     * Returns null if not a transaction SMS
     */
    fun parse(message: String, senderId: String = ""): ParsedTransaction? {
        // First, check if this looks like a transaction SMS
        val lowerMessage = message.lowercase()
        val hasDebitKeyword = debitKeywords.any { lowerMessage.contains(it) }
        val hasCreditKeyword = creditKeywords.any { lowerMessage.contains(it) }
        
        if (!hasDebitKeyword && !hasCreditKeyword) {
            // Check for amount patterns as fallback
            val hasAmount = amountPatterns.any { it.matcher(message).find() }
            if (!hasAmount) return null
        }
        
        // Extract amount
        var amount: Double? = null
        for (pattern in amountPatterns) {
            val matcher = pattern.matcher(message)
            if (matcher.find()) {
                val amountStr = matcher.group(1)?.replace(",", "") ?: continue
                amount = amountStr.toDoubleOrNull()
                if (amount != null && amount > 0) break
            }
        }
        
        if (amount == null || amount <= 0) return null
        
        // Determine if debit or credit
        val isDebit = when {
            hasDebitKeyword && !hasCreditKeyword -> true
            hasCreditKeyword && !hasDebitKeyword -> false
            else -> true // Default to expense if unclear
        }
        
        // Extract merchant name
        var merchantName: String? = null
        for (pattern in merchantPatterns) {
            val matcher = pattern.matcher(message)
            if (matcher.find()) {
                merchantName = matcher.group(1)?.trim()?.take(50)
                if (!merchantName.isNullOrBlank()) break
            }
        }
        
        // Extract account hint (last 4 digits)
        var accountHint: String? = null
        for (pattern in accountPatterns) {
            val matcher = pattern.matcher(message)
            if (matcher.find()) {
                accountHint = matcher.group(1)
                break
            }
        }
        
        // Detect bank from sender
        val detectedBank = detectBank(senderId)
        
        // Detect card type
        val cardType = when {
            creditCardPatterns.any { it.matcher(message).find() } -> CardType.CREDIT_CARD
            debitCardPatterns.any { it.matcher(message).find() } -> CardType.DEBIT_CARD
            else -> null
        }
        
        // Extract UPI ID if present
        var upiId: String? = null
        val upiMatcher = upiPattern.matcher(message)
        if (upiMatcher.find()) {
            upiId = upiMatcher.group(1)
        }
        
        // Extract reference number
        var refNumber: String? = null
        for (pattern in refPatterns) {
            val matcher = pattern.matcher(message)
            if (matcher.find()) {
                refNumber = matcher.group(1)
                break
            }
        }
        
        return ParsedTransaction(
            amount = amount,
            merchantName = merchantName?.trim(),
            isDebit = isDebit,
            accountHint = accountHint,
            originalMessage = message,
            detectedBankCode = detectedBank?.code,
            detectedBankName = detectedBank?.name,
            cardType = cardType,
            upiId = upiId,
            referenceNumber = refNumber
        )
    }
}
