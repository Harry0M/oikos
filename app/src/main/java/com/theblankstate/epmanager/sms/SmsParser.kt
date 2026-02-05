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
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Parses bank SMS messages to extract transaction details
 * 
 * Key features:
 * - Extracts amount, transaction type (debit/credit)
 * - Detects bank from sender ID
 * - Extracts last 4 digits of account/card for matching
 * - Extracts merchant/description
 */
@Singleton
class SmsParser @Inject constructor() {
    
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
        val referenceNumber: String? = null, // Transaction reference if available
        val senderName: String? = null, // Who sent money (for credits)
        val receiverName: String? = null // Who received money (for debits)
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
        
        // SMS types to IGNORE - these are NOT transactions
        private val exclusionKeywords = listOf(
            "auto-pay activated",
            "auto-pay cancelled",
            "autopay activated",
            "autopay cancelled", 
            "autopay registered",
            "autopay deregistered",
            "mandate registered",
            "mandate created",
            "standing instruction",
            "si created",
            "si registered",
            "otp is",
            "otp for",
            "one time password",
            "verification code",
            "login otp",
            "transaction otp",
            "alert:",
            "reminder:",
            "emi due",
            "bill due",
            "payment due",
            "minimum amount due",
            "statement generated",
            "statement ready",
            "reward points",
            "loyalty points",
            "upgrade your",
            "offer:",
            "exclusive offer",
            "pre-approved",
            "preapproved",
            "apply now",
            "activate now",
            "link your",
            "kyc update",
            "kyc pending"
        )
        
        // Merchant to category mapping for auto-categorization
        val merchantCategoryMap = mapOf(
            // Food & Dining
            "swiggy" to "food",
            "zomato" to "food",
            "uber eats" to "food",
            "dominos" to "food",
            "pizza hut" to "food",
            "mcdonalds" to "food",
            "starbucks" to "food",
            "cafe coffee day" to "food",
            "dunkin" to "food",
            "restaurant" to "food",
            "cafe" to "food",
            "bakery" to "food",
            "food" to "food",
            
            // Shopping
            "amazon" to "shopping",
            "flipkart" to "shopping",
            "myntra" to "shopping",
            "ajio" to "shopping",
            "meesho" to "shopping",
            "nykaa" to "shopping",
            "bigbasket" to "shopping",
            "blinkit" to "shopping",
            "zepto" to "shopping",
            "instamart" to "shopping",
            "dmart" to "shopping",
            "reliance" to "shopping",
            "shoppers stop" to "shopping",
            "lifestyle" to "shopping",
            
            // Transportation
            "uber" to "transportation",
            "ola" to "transportation",
            "rapido" to "transportation",
            "meru" to "transportation",
            "irctc" to "transportation",
            "redbus" to "transportation",
            "makemytrip" to "transportation",
            "goibibo" to "transportation",
            "petrol" to "transportation",
            "fuel" to "transportation",
            "bp" to "transportation",
            "indian oil" to "transportation",
            "hindustan petroleum" to "transportation",
            "hp petrol" to "transportation",
            "metro" to "transportation",
            "toll" to "transportation",
            "fastag" to "transportation",
            "parking" to "transportation",
            
            // Bills & Utilities
            "electricity" to "bills",
            "water bill" to "bills",
            "gas bill" to "bills",
            "broadband" to "bills",
            "wifi" to "bills",
            "jio" to "bills",
            "airtel" to "bills",
            "vodafone" to "bills",
            "vi postpaid" to "bills",
            "bsnl" to "bills",
            "tata play" to "bills",
            "dish tv" to "bills",
            "dth" to "bills",
            
            // Entertainment
            "netflix" to "entertainment",
            "hotstar" to "entertainment",
            "disney" to "entertainment",
            "amazon prime" to "entertainment",
            "spotify" to "entertainment",
            "youtube" to "entertainment",
            "gaana" to "entertainment",
            "wynk" to "entertainment",
            "pvr" to "entertainment",
            "inox" to "entertainment",
            "bookmyshow" to "entertainment",
            "cinema" to "entertainment",
            "multiplex" to "entertainment",
            "gaming" to "entertainment",
            "playstation" to "entertainment",
            "xbox" to "entertainment",
            "steam" to "entertainment",
            
            // Technology & Software
            "google cloud" to "technology",
            "google ads" to "technology",
            "google" to "technology",
            "microsoft" to "technology",
            "azure" to "technology",
            "aws" to "technology",
            "apple" to "technology",
            "canva" to "technology",
            "adobe" to "technology",
            "github" to "technology",
            "figma" to "technology",
            "notion" to "technology",
            "slack" to "technology",
            "zoom" to "technology",
            "chatgpt" to "technology",
            "openai" to "technology",
            
            // Health & Medical
            "pharmacy" to "health",
            "medical" to "health",
            "hospital" to "health",
            "clinic" to "health",
            "doctor" to "health",
            "apollo" to "health",
            "medplus" to "health",
            "netmeds" to "health",
            "pharmeasy" to "health",
            "1mg" to "health",
            "practo" to "health",
            "gym" to "health",
            "fitness" to "health",
            "cult.fit" to "health",
            
            // Education
            "school" to "education",
            "college" to "education",
            "university" to "education",
            "udemy" to "education",
            "coursera" to "education",
            "unacademy" to "education",
            "byju" to "education",
            "vedantu" to "education",
            "upgrad" to "education",
            "books" to "education",
            
            // Insurance
            "insurance" to "insurance",
            "lic" to "insurance",
            "hdfc life" to "insurance",
            "icici prudential" to "insurance",
            "sbi life" to "insurance",
            "policy" to "insurance",
            
            // Investment
            "zerodha" to "investment",
            "groww" to "investment",
            "upstox" to "investment",
            "kite" to "investment",
            "mutual fund" to "investment",
            "mf" to "investment",
            "sip" to "investment"
        )
        
        // Improved patterns to extract merchant/business name
        private val merchantPatterns = listOf(
            // Business name before VPA: "to BusinessName (merchant@upi)" or "to BusinessName merchant@upi"
            Pattern.compile("(?:to|paid to)\\s+([A-Za-z][A-Za-z0-9\\s&'.,-]{2,40})\\s*(?:\\(|\\s)[a-zA-Z0-9._-]+@", Pattern.CASE_INSENSITIVE),
            // Generic "at/to/for" pattern
            Pattern.compile("(?:at|to|for|@)\\s+([A-Za-z0-9][A-Za-z0-9\\s&'.,-]*?)(?:\\s+on|\\s+ref|\\.|\\s+VPA|$)", Pattern.CASE_INSENSITIVE),
            // Info: field pattern
            Pattern.compile("Info[:\\s]+([^.]+)", Pattern.CASE_INSENSITIVE),
            // Payee name pattern
            Pattern.compile("payee[:\\s]+([A-Za-z][A-Za-z0-9\\s&'.,-]+)", Pattern.CASE_INSENSITIVE)
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
        
        // Improved UPI ID patterns - handles various formats
        private val upiPatterns = listOf(
            // VPA: user@upi or UPI: user@upi
            Pattern.compile("(?:VPA|UPI)[:\\s]+([a-zA-Z0-9._-]+@[a-zA-Z0-9]+)", Pattern.CASE_INSENSITIVE),
            // (user@upi) in parentheses
            Pattern.compile("\\(([a-zA-Z0-9._-]+@[a-zA-Z0-9]+)\\)", Pattern.CASE_INSENSITIVE),
            // to user@upi or from user@upi
            Pattern.compile("(?:to|from)\\s+([a-zA-Z0-9._-]+@[a-zA-Z0-9]+)", Pattern.CASE_INSENSITIVE),
            // Standalone UPI format
            Pattern.compile("\\b([a-zA-Z0-9._-]{3,}@(?:upi|ybl|paytm|okhdfcbank|okaxis|oksbi|okicici|axl|ibl|apl|fbl|waaxis|wahdfcbank|wasbi|waicici|axisb|hdfcbank|icici|sbi|yesbank|kotak|indus|federal|rbl|bob|citi|pnb|idbi|dbs))\\b", Pattern.CASE_INSENSITIVE)
        )
        
        // Reference number patterns - matches "ref:123456" or "ref: 123456" or "Ref no: 123456"
        private val refPatterns = listOf(
            Pattern.compile("ref[:\\s.#]*([0-9]{8,20})", Pattern.CASE_INSENSITIVE),
            Pattern.compile("reference[:\\s.#]*([0-9]{8,20})", Pattern.CASE_INSENSITIVE),
            Pattern.compile("UTR[:\\s]*([A-Za-z0-9]+)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("txn[:\\s.#]*([A-Za-z0-9]+)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("UPI[:\\s]*([0-9]{12})", Pattern.CASE_INSENSITIVE)
        )
        
        // Sender patterns (for credits - who sent money)
        private val senderPatterns = listOf(
            // Period removed from char class - acts as terminator to stop at sentence boundaries
            Pattern.compile("from\\s+([A-Za-z][A-Za-z0-9\\s&',-]{2,35})(?:\\s*(?:ref|on|via|\\.| |\\(|$))", Pattern.CASE_INSENSITIVE),
            Pattern.compile("from\\s+\"([^\"]+)\"", Pattern.CASE_INSENSITIVE),
            Pattern.compile("credited\\s+by\\s+([A-Za-z][A-Za-z0-9\\s&',-]{2,35})(?:\\s*(?:ref|on|via|\\.| |\\(|$))", Pattern.CASE_INSENSITIVE),
            // Sender before UPI ID: "from SenderName (sender@upi)"
            Pattern.compile("from\\s+([A-Za-z][A-Za-z0-9\\s&',-]{2,35})\\s*(?:\\(|\\s)[a-zA-Z0-9._-]+@", Pattern.CASE_INSENSITIVE)
        )
        
        // Receiver patterns (for debits - who received money)
        private val receiverPatterns = listOf(
            // Period removed from char class - acts as terminator to stop at sentence boundaries
            Pattern.compile("to\\s+([A-Za-z][A-Za-z0-9\\s&',-]{2,35})(?:\\s*(?:ref|on|via|\\.| |\\(|$))", Pattern.CASE_INSENSITIVE),
            Pattern.compile("to\\s+\"([^\"]+)\"", Pattern.CASE_INSENSITIVE),
            Pattern.compile("paid\\s+to\\s+([A-Za-z][A-Za-z0-9\\s&',-]{2,35})(?:\\s*(?:ref|on|via|\\.| |\\(|$))", Pattern.CASE_INSENSITIVE),
            // Receiver before UPI ID: "to ReceiverName (receiver@upi)"
            Pattern.compile("(?:to|paid to)\\s+([A-Za-z][A-Za-z0-9\\s&',-]{2,35})\\s*(?:\\(|\\s)[a-zA-Z0-9._-]+@", Pattern.CASE_INSENSITIVE)
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
        val lowerMessage = message.lowercase()
        
        // FIRST: Check for exclusion keywords - these are NOT transactions
        val isExcluded = exclusionKeywords.any { lowerMessage.contains(it) }
        if (isExcluded) return null
        
        // SECOND: Must have EXPLICIT debit or credit keyword for a transaction
        val hasDebitKeyword = debitKeywords.any { lowerMessage.contains(it) }
        val hasCreditKeyword = creditKeywords.any { lowerMessage.contains(it) }
        
        // If no explicit transaction keyword, this is NOT a transaction
        // (Prevents amount-only SMS like "Auto-Pay for INR 75000" from being logged)
        if (!hasDebitKeyword && !hasCreditKeyword) {
            return null
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
        
        // Extract UPI ID if present (try all patterns)
        // Only store as UPI ID if it contains @ symbol (intelligent detection)
        var upiId: String? = null
        for (pattern in upiPatterns) {
            val matcher = pattern.matcher(message)
            if (matcher.find()) {
                val extracted = matcher.group(1)
                // Validate that it's actually a UPI ID (must contain @)
                if (!extracted.isNullOrBlank() && extracted.contains("@")) {
                    upiId = extracted
                    break
                }
            }
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
        
        // Extract sender name (for credits)
        // Skip values with @ as they are UPI IDs, not banking names
        var senderName: String? = null
        if (!isDebit) {
            for (pattern in senderPatterns) {
                val matcher = pattern.matcher(message)
                if (matcher.find()) {
                    val extracted = matcher.group(1)?.trim()?.take(50)
                    // Only store as sender name if it doesn't contain @ (not a UPI ID)
                    if (!extracted.isNullOrBlank() && !extracted.contains("@")) {
                        senderName = extracted
                        break
                    }
                }
            }
        }
        
        // Extract receiver name (for debits)
        // Skip values with @ as they are UPI IDs, not banking names
        var receiverName: String? = null
        if (isDebit) {
            for (pattern in receiverPatterns) {
                val matcher = pattern.matcher(message)
                if (matcher.find()) {
                    val extracted = matcher.group(1)?.trim()?.take(50)
                    // Only store as receiver name if it doesn't contain @ (not a UPI ID)
                    if (!extracted.isNullOrBlank() && !extracted.contains("@")) {
                        receiverName = extracted
                        break
                    }
                }
            }
            // Fallback to merchant name if no receiver found (only if merchant name is not a UPI ID)
            if (receiverName.isNullOrBlank() && !merchantName.isNullOrBlank() && !merchantName.contains("@")) {
                receiverName = merchantName
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
            referenceNumber = refNumber,
            senderName = senderName,
            receiverName = receiverName
        )
    }
}
