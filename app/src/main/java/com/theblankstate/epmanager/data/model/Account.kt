package com.theblankstate.epmanager.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Represents a payment account (Cash, Bank, UPI, Credit Card, etc.)
 * 
 * For SMS auto-detection, accounts can be linked to real bank accounts using:
 * - accountNumber: Last 4 digits of account/card number (for matching)
 * - bankCode: Bank identifier code from SMS sender (e.g., HDFCBK, SBIINB)
 * - linkedSenderIds: Comma-separated SMS sender IDs that should map to this account
 */
@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: AccountType,
    val icon: String, // Material icon name
    val color: Long,
    val balance: Double = 0.0,
    val isDefault: Boolean = false,
    
    // Bank linking fields for SMS parsing
    val accountNumber: String? = null, // Last 4 digits for matching (e.g., "1234")
    val bankCode: String? = null, // Bank identifier (e.g., "HDFC", "SBI", "ICICI")
    val linkedSenderIds: String? = null, // Comma-separated sender IDs (e.g., "HDFCBK,HDFC")
    val isLinked: Boolean = false, // Whether this account is linked for auto-detection
    
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * Check if this account matches the given SMS sender and account hint
     */
    fun matchesSms(senderId: String, accountHint: String?): Boolean {
        // If not linked, can't auto-match
        if (!isLinked) return false
        
        val normalizedSender = senderId.uppercase().replace("-", "")
        
        // Check sender ID match
        val senderMatches = linkedSenderIds?.split(",")
            ?.any { normalizedSender.contains(it.trim().uppercase()) } ?: false
        
        // Check bank code match
        val bankMatches = bankCode?.let { normalizedSender.contains(it.uppercase()) } ?: false
        
        // Check account number match (last 4 digits)
        val accountMatches = accountNumber?.let { it == accountHint } ?: false
        
        // Match logic:
        // 1. If account number matches AND (sender or bank matches) -> strong match
        // 2. If only sender/bank matches and no account number stored -> weak match
        return when {
            accountMatches && (senderMatches || bankMatches) -> true
            accountNumber == null && (senderMatches || bankMatches) -> true
            accountMatches -> true // Account number alone is strong identifier
            else -> false
        }
    }
    
    /**
     * Get match confidence score (0-100)
     */
    fun getMatchScore(senderId: String, accountHint: String?): Int {
        if (!isLinked) return 0
        
        var score = 0
        val normalizedSender = senderId.uppercase().replace("-", "")
        
        // Sender ID match: +40 points
        if (linkedSenderIds?.split(",")?.any { normalizedSender.contains(it.trim().uppercase()) } == true) {
            score += 40
        }
        
        // Bank code match: +30 points
        if (bankCode?.let { normalizedSender.contains(it.uppercase()) } == true) {
            score += 30
        }
        
        // Account number match: +50 points (strongest indicator)
        if (accountNumber != null && accountNumber == accountHint) {
            score += 50
        }
        
        return score.coerceAtMost(100)
    }
}

enum class AccountType {
    CASH,
    BANK,
    UPI,
    CREDIT_CARD,
    WALLET,
    OTHER
}

/**
 * Known Indian banks with their SMS sender ID patterns
 */
object BankRegistry {
    data class BankInfo(
        val name: String,
        val code: String,
        val senderPatterns: List<String>,
        val color: Long
    )
    
    val banks = listOf(
        // Major Private Banks
        BankInfo("HDFC Bank", "HDFC", listOf("HDFCBK", "HDFC", "HDFCBN"), 0xFF004B8D),
        BankInfo("ICICI Bank", "ICICI", listOf("ICICIB", "ICICI", "ICICIP"), 0xFFF58220),
        BankInfo("Axis Bank", "AXIS", listOf("AXISBK", "AXIS", "UTIB"), 0xFF97144D),
        BankInfo("Kotak Mahindra Bank", "KOTAK", listOf("KOTAKB", "KOTAK", "KKBK"), 0xFFED1C24),
        BankInfo("Yes Bank", "YES", listOf("YESBNK", "YES", "YESBK"), 0xFF0066B3),
        BankInfo("IndusInd Bank", "INDUSIND", listOf("INDUSB", "INDUS", "INDB"), 0xFF98272A),
        BankInfo("IDFC First Bank", "IDFC", listOf("IDFCFB", "IDFC", "IDFCBK"), 0xFF9C1D26),
        BankInfo("Federal Bank", "FEDERAL", listOf("FEDERL", "FEDERA", "FDRL"), 0xFF00529B),
        BankInfo("RBL Bank", "RBL", listOf("RBLBNK", "RBL", "RATNAKAR"), 0xFFDA251D),
        BankInfo("South Indian Bank", "SIB", listOf("SIBANK", "SIB", "SOUTHIN"), 0xFF0066B3),
        BankInfo("Karnataka Bank", "KARNATAKA", listOf("KRNTKB", "KARNBK", "KB"), 0xFF00529B),
        BankInfo("City Union Bank", "CUB", listOf("CITYUB", "CUB", "CIUB"), 0xFF0072BC),
        BankInfo("Karur Vysya Bank", "KVB", listOf("KVBANK", "KVB", "KARURV"), 0xFFF26522),
        BankInfo("Bandhan Bank", "BANDHAN", listOf("BANDHN", "BANDHAN", "BDBL"), 0xFFE34424),
        BankInfo("DCB Bank", "DCB", listOf("DCBBK", "DCB", "DCBL"), 0xFF0072AA),
        BankInfo("Dhanlaxmi Bank", "DHANLAXMI", listOf("DHANLA", "DLB", "DLXMI"), 0xFF97144D),
        BankInfo("Jammu & Kashmir Bank", "JK", listOf("JKBANK", "JKB", "JKBNK"), 0xFF004B8D),
        BankInfo("Tamilnad Mercantile Bank", "TMB", listOf("TMBANK", "TMB", "TNMB"), 0xFFE42529),
        BankInfo("CSB Bank", "CSB", listOf("CSBBNK", "CSB", "CATHOL"), 0xFF22409A),
        BankInfo("Nainital Bank", "NAINITAL", listOf("NNITAL", "NAINIT", "NBNK"), 0xFF0072BC),
        
        // Public Sector Banks
        BankInfo("State Bank of India", "SBI", listOf("SBIINB", "SBIPSG", "SBI", "SBISMS"), 0xFF22409A),
        BankInfo("Punjab National Bank", "PNB", listOf("PNBSMS", "PNB", "PUNBNK"), 0xFFE42529),
        BankInfo("Bank of Baroda", "BOB", listOf("BOBSMS", "BOB", "BARODAB"), 0xFFE34424),
        BankInfo("Canara Bank", "CANARA", listOf("CANBNK", "CANARA", "CNRBK"), 0xFFFFCC00),
        BankInfo("Union Bank of India", "UNION", listOf("UNIONB", "UNION", "UBOI"), 0xFFF26522),
        BankInfo("Bank of India", "BOI", listOf("BOIIND", "BOI", "BKID"), 0xFF0072BC),
        BankInfo("Indian Bank", "INDIAN", listOf("INDBK", "INDIAN", "IDIB"), 0xFF004B8D),
        BankInfo("Central Bank of India", "CENTRAL", listOf("CBIBNK", "CENBNK", "CBI"), 0xFFE42529),
        BankInfo("Bank of Maharashtra", "BOM", listOf("BOMBNK", "BOM", "MAHB"), 0xFFF26522),
        BankInfo("Punjab & Sind Bank", "PSB", listOf("PSBBNK", "PSB", "PSIB"), 0xFF22409A),
        BankInfo("Indian Overseas Bank", "IOB", listOf("IOBBNK", "IOB", "IOBA"), 0xFF0072BC),
        BankInfo("UCO Bank", "UCO", listOf("UCOBK", "UCO", "UCOBNK"), 0xFF007749),
        BankInfo("IDBI Bank", "IDBI", listOf("IDBIBK", "IDBI", "IDBLIK"), 0xFF007749),
        
        // Small Finance Banks
        BankInfo("AU Small Finance Bank", "AU", listOf("AUBANK", "AUSFB", "AUSF"), 0xFF5F259F),
        BankInfo("Equitas Small Finance Bank", "EQUITAS", listOf("EQITAS", "EQUITA", "ESFB"), 0xFFDA251D),
        BankInfo("Ujjivan Small Finance Bank", "UJJIVAN", listOf("UJJIVA", "UJJIV", "USFB"), 0xFF0066B3),
        BankInfo("ESAF Small Finance Bank", "ESAF", listOf("ESAFBK", "ESAF", "ESAFB"), 0xFF007749),
        BankInfo("Suryoday Small Finance Bank", "SURYODAY", listOf("SURYOD", "SURYO", "SSFB"), 0xFFF58220),
        BankInfo("Jana Small Finance Bank", "JANA", listOf("JANABK", "JANA", "JSFB"), 0xFF22409A),
        BankInfo("Fincare Small Finance Bank", "FINCARE", listOf("FNCBK", "FINCAR", "FSFB"), 0xFF5F259F),
        BankInfo("Shivalik Small Finance Bank", "SHIVALIK", listOf("SHVLK", "SHIVAL", "SHSFB"), 0xFF0072BC),
        
        // Payment Banks
        BankInfo("Airtel Payments Bank", "AIRTEL", listOf("AIRTEL", "APTB", "AIRPB"), 0xFFED1C24),
        BankInfo("India Post Payments Bank", "IPPB", listOf("IPPBNK", "IPPB", "INDPOST"), 0xFFE42529),
        BankInfo("Jio Payments Bank", "JIO", listOf("JIOPAY", "JIOBNK", "JPB"), 0xFF0066B3),
        BankInfo("NSDL Payments Bank", "NSDL", listOf("NSDLPB", "NSDL", "NPB"), 0xFF004B8D),
        BankInfo("Fino Payments Bank", "FINO", listOf("FINOPB", "FINO", "FPB"), 0xFF0072AA),
        
        // Foreign Banks
        BankInfo("Standard Chartered", "SCB", listOf("SCBANK", "SCB", "STCHART"), 0xFF0072AA),
        BankInfo("HSBC", "HSBC", listOf("HSBCIN", "HSBC", "HSBCBK"), 0xFFDB0011),
        BankInfo("Citibank", "CITI", listOf("CITIBK", "CITI", "CITBNK"), 0xFF0066B3),
        BankInfo("Deutsche Bank", "DB", listOf("DEUTBK", "DB", "DEUTSC"), 0xFF0018A8),
        BankInfo("DBS Bank", "DBS", listOf("DBSBNK", "DBS", "DBSS"), 0xFFED1C24),
        BankInfo("American Express", "AMEX", listOf("AMEXIN", "AMEX", "AMXIN"), 0xFF006FCF),
        
        // Cooperative Banks
        BankInfo("Saraswat Bank", "SARASWAT", listOf("SARBNK", "SARASW", "SBL"), 0xFF97144D),
        BankInfo("Cosmos Bank", "COSMOS", listOf("COSMOS", "COSMOB", "CBL"), 0xFF0072BC),
        BankInfo("TJSB Bank", "TJSB", listOf("TJSBNK", "TJSB", "TJSBK"), 0xFF22409A)
    )
    
    // UPI/Wallet providers
    val upiProviders = listOf(
        BankInfo("Google Pay", "GPAY", listOf("GPAY", "GOOGLEPAY"), 0xFF4285F4),
        BankInfo("PhonePe", "PHONEPE", listOf("PHONPE", "PHONEPE"), 0xFF5F259F),
        BankInfo("Paytm", "PAYTM", listOf("PAYTM", "PYTM"), 0xFF00BAF2),
        BankInfo("Amazon Pay", "AMAZONPAY", listOf("AMAZON", "AMZN"), 0xFFFF9900)
    )
    
    fun findBankByCode(code: String): BankInfo? {
        val upperCode = code.uppercase()
        return banks.find { it.code.uppercase() == upperCode } 
            ?: upiProviders.find { it.code.uppercase() == upperCode }
    }
    
    fun findBankBySender(senderId: String): BankInfo? {
        val normalizedSender = senderId.uppercase().replace("-", "")
        return banks.find { bank -> 
            bank.senderPatterns.any { normalizedSender.contains(it) }
        } ?: upiProviders.find { provider ->
            provider.senderPatterns.any { normalizedSender.contains(it) }
        }
    }
}

// Default accounts (generic, not linked)
object DefaultAccounts {
    val accounts = listOf(
        Account(
            id = "cash", 
            name = "Cash", 
            type = AccountType.CASH, 
            icon = "Money", 
            color = 0xFF22C55E, 
            isDefault = true,
            isLinked = false
        ),
        Account(
            id = "bank", 
            name = "Bank Account", 
            type = AccountType.BANK, 
            icon = "AccountBalance", 
            color = 0xFF3B82F6, 
            isDefault = false,
            isLinked = false
        ),
        Account(
            id = "upi", 
            name = "UPI", 
            type = AccountType.UPI, 
            icon = "PhoneAndroid", 
            color = 0xFF8B5CF6, 
            isDefault = false,
            isLinked = false
        ),
        Account(
            id = "credit_card", 
            name = "Credit Card", 
            type = AccountType.CREDIT_CARD, 
            icon = "CreditCard", 
            color = 0xFFF59E0B, 
            isDefault = false,
            isLinked = false
        )
    )
}
