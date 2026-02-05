package com.theblankstate.epmanager.domain

import com.theblankstate.epmanager.data.local.dao.CategorizationRuleDao
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionCategorizer @Inject constructor(
    private val ruleDao: CategorizationRuleDao
) {
    /**
     * Determine the category for a transaction based on its metadata.
     * Priority:
     * 1. User-defined rules (CategorizationRules)
     * 2. Hardcoded keyword matching (Fallback)
     * 
     * Returns null if no category is detected.
     */
    suspend fun categorize(
        merchantName: String? = null,
        upiId: String? = null,
        sender: String? = null,
        originalSms: String? = null
    ): String? {
        // 1. Check User Defined Rules
        // Priority: UPI ID (most specific) -> Merchant Name -> Sender (least specific)
        val textsToCheck = listOfNotNull(upiId, merchantName, sender)
        
        for (text in textsToCheck) {
            if (text.isNotBlank()) {
                val rule = ruleDao.findRuleForText(text)
                if (rule != null) {
                    return rule.categoryId
                }
            }
        }

        // 2. Hardcoded Fallback Logic
        // Use merchant name primarily for fallback logic
        val textToAnalyze = merchantName ?: sender ?: ""
        if (textToAnalyze.isBlank()) return null
        
        val lowerText = textToAnalyze.lowercase()
        
        return when {
            // Food
            lowerText.contains("swiggy") || 
            lowerText.contains("zomato") ||
            lowerText.contains("restaurant") ||
            lowerText.contains("cafe") ||
            lowerText.contains("food") -> "food"
            
            // Shopping
            lowerText.contains("amazon") ||
            lowerText.contains("flipkart") ||
            lowerText.contains("myntra") ||
            lowerText.contains("shop") ||
            lowerText.contains("store") -> "shopping"
            
            // Transport
            lowerText.contains("uber") ||
            lowerText.contains("ola") ||
            lowerText.contains("rapido") ||
            lowerText.contains("petrol") ||
            lowerText.contains("fuel") ||
            lowerText.contains("pump") -> "transportation"
            
            // Bills
            lowerText.contains("electricity") ||
            lowerText.contains("water") ||
            lowerText.contains("gas") ||
            lowerText.contains("bill") ||
            lowerText.contains("recharge") ||
            lowerText.contains("mobile") ||
            lowerText.contains("broadband") -> "bills"
            
            // Entertainment
            lowerText.contains("netflix") ||
            lowerText.contains("spotify") ||
            lowerText.contains("hotstar") ||
            lowerText.contains("prime") ||
            lowerText.contains("movie") ||
            lowerText.contains("cinema") -> "entertainment"
            
            // Groceries
            lowerText.contains("grocery") ||
            lowerText.contains("supermarket") ||
            lowerText.contains("mart") ||
            lowerText.contains("basket") ||
            lowerText.contains("blinkit") ||
            lowerText.contains("zepto") ||
            lowerText.contains("instamart") -> "groceries"
            
            else -> null
        }
    }
}
