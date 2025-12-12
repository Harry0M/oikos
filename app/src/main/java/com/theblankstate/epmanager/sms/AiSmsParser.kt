package com.theblankstate.epmanager.sms

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.generationConfig
import com.theblankstate.epmanager.data.model.SmsParseResult
import com.theblankstate.epmanager.data.model.SuggestedPatterns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI-powered SMS parser using Firebase AI (Gemini)
 * 
 * This parser can:
 * 1. Parse any bank SMS format without hardcoded patterns
 * 2. Learn patterns from sample SMS for future parsing
 * 3. Handle edge cases that regex might miss
 */
@Singleton
class AiSmsParser @Inject constructor() {
    
    companion object {
        private const val TAG = "AiSmsParser"
        
        // Prompt for parsing SMS
        private val PARSE_SMS_PROMPT = """
            You are a financial SMS parser. Parse the following bank/payment SMS and extract transaction details.
            
            SMS: "{SMS_BODY}"
            Sender: "{SENDER_ID}"
            
            Extract the following information and respond ONLY with a valid JSON object (no markdown, no explanation):
            {
                "amount": <number or null>,
                "isDebit": <true if money was spent/debited, false if received/credited>,
                "merchantName": "<merchant/payee name or null>",
                "accountHint": "<last 4 digits of account/card or null>",
                "bankName": "<detected bank name or null>",
                "transactionDate": "<date string if found or null>",
                "referenceNumber": "<transaction reference/UTR or null>",
                "balance": "<available balance after transaction or null>",
                "confidence": <0.0 to 1.0 confidence score>,
                "isTransaction": <true if this is a financial transaction SMS, false if promotional/OTP/other>
            }
            
            Important rules:
            - Amount should be a number without currency symbols
            - isDebit = true for: debited, spent, paid, purchase, withdrawn, sent, charged
            - isDebit = false for: credited, received, deposited, refund, cashback
            - Only extract accountHint if you find last 4 digits (like XX1234, **5678)
            - Set confidence based on how clearly you can identify the transaction
            - If this is not a transaction SMS (OTP, promotional, etc), set isTransaction=false
        """.trimIndent()
        
        // Prompt for learning patterns from sample SMS
        private val LEARN_PATTERNS_PROMPT = """
            Analyze this bank SMS and create regex patterns for future parsing.
            
            SMS: "{SMS_BODY}"
            Sender: "{SENDER_ID}"
            
            Create regex patterns to extract:
            1. Amount (capture group for the number)
            2. Account/Card last 4 digits
            3. Merchant name
            4. Keywords that indicate debit (money going out)
            5. Keywords that indicate credit (money coming in)
            
            Respond ONLY with a valid JSON object (no markdown):
            {
                "amountPattern": "<regex pattern with capture group for amount>",
                "accountPattern": "<regex pattern with capture group for last 4 digits>",
                "merchantPattern": "<regex pattern with capture group for merchant>",
                "debitKeywords": ["keyword1", "keyword2"],
                "creditKeywords": ["keyword1", "keyword2"],
                "bankName": "<detected bank name>",
                "senderPatterns": ["<pattern1>", "<pattern2>"]
            }
            
            Make patterns flexible enough to work with similar SMS formats from the same bank.
        """.trimIndent()
    }
    
    private val generativeModel by lazy {
        try {
            Firebase.ai(backend = GenerativeBackend.googleAI())
                .generativeModel(
                    modelName = "gemini-2.0-flash",
                    generationConfig = generationConfig {
                        temperature = 0.1f  // Low temperature for more deterministic output
                        maxOutputTokens = 500
                    }
                )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Firebase AI model", e)
            null
        }
    }
    
    /**
     * Parse SMS using AI
     * Falls back to regex parser if AI fails
     */
    suspend fun parseSms(smsBody: String, senderId: String): SmsParseResult? {
        return withContext(Dispatchers.IO) {
            try {
                val model = generativeModel ?: return@withContext fallbackParse(smsBody, senderId)
                
                val prompt = PARSE_SMS_PROMPT
                    .replace("{SMS_BODY}", smsBody)
                    .replace("{SENDER_ID}", senderId)
                
                val response = model.generateContent(prompt)
                val responseText = response.text?.trim() ?: return@withContext fallbackParse(smsBody, senderId)
                
                Log.d(TAG, "AI Response: $responseText")
                
                // Parse JSON response
                parseAiResponse(responseText)
            } catch (e: Exception) {
                Log.e(TAG, "AI parsing failed, using fallback", e)
                fallbackParse(smsBody, senderId)
            }
        }
    }
    
    /**
     * Learn patterns from a sample SMS using AI
     */
    suspend fun learnPatternsFromSample(smsBody: String, senderId: String): SuggestedPatterns? {
        return withContext(Dispatchers.IO) {
            try {
                val model = generativeModel ?: return@withContext null
                
                val prompt = LEARN_PATTERNS_PROMPT
                    .replace("{SMS_BODY}", smsBody)
                    .replace("{SENDER_ID}", senderId)
                
                val response = model.generateContent(prompt)
                val responseText = response.text?.trim() ?: return@withContext null
                
                Log.d(TAG, "Pattern Learning Response: $responseText")
                
                parsePatternResponse(responseText)
            } catch (e: Exception) {
                Log.e(TAG, "Pattern learning failed", e)
                null
            }
        }
    }
    
    private fun parseAiResponse(responseText: String): SmsParseResult? {
        return try {
            // Clean JSON if wrapped in markdown
            val cleanJson = responseText
                .replace("```json", "")
                .replace("```", "")
                .trim()
            
            val json = JSONObject(cleanJson)
            
            // Check if it's actually a transaction
            val isTransaction = json.optBoolean("isTransaction", true)
            if (!isTransaction) {
                Log.d(TAG, "Not a transaction SMS")
                return null
            }
            
            SmsParseResult(
                amount = if (json.has("amount") && !json.isNull("amount")) json.getDouble("amount") else null,
                isDebit = json.optBoolean("isDebit", true),
                merchantName = json.optString("merchantName", "").takeIf { it.isNotEmpty() && it != "null" },
                accountHint = json.optString("accountHint", "").takeIf { it.isNotEmpty() && it != "null" },
                bankName = json.optString("bankName", "").takeIf { it.isNotEmpty() && it != "null" },
                transactionDate = json.optString("transactionDate", "").takeIf { it.isNotEmpty() && it != "null" },
                referenceNumber = json.optString("referenceNumber", "").takeIf { it.isNotEmpty() && it != "null" },
                balance = if (json.has("balance") && !json.isNull("balance")) json.optDouble("balance") else null,
                confidence = json.optDouble("confidence", 0.5).toFloat(),
                suggestedPatterns = null
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse AI response: $responseText", e)
            null
        }
    }
    
    private fun parsePatternResponse(responseText: String): SuggestedPatterns? {
        return try {
            val cleanJson = responseText
                .replace("```json", "")
                .replace("```", "")
                .trim()
            
            val json = JSONObject(cleanJson)
            
            val debitKeywords = mutableListOf<String>()
            val creditKeywords = mutableListOf<String>()
            
            json.optJSONArray("debitKeywords")?.let { arr ->
                for (i in 0 until arr.length()) {
                    debitKeywords.add(arr.getString(i))
                }
            }
            
            json.optJSONArray("creditKeywords")?.let { arr ->
                for (i in 0 until arr.length()) {
                    creditKeywords.add(arr.getString(i))
                }
            }
            
            SuggestedPatterns(
                amountPattern = json.optString("amountPattern", "").takeIf { it.isNotEmpty() },
                accountPattern = json.optString("accountPattern", "").takeIf { it.isNotEmpty() },
                merchantPattern = json.optString("merchantPattern", "").takeIf { it.isNotEmpty() },
                debitKeywords = debitKeywords,
                creditKeywords = creditKeywords
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse pattern response", e)
            null
        }
    }
    
    /**
     * Fallback to regex-based parsing when AI is unavailable
     */
    private fun fallbackParse(smsBody: String, senderId: String): SmsParseResult? {
        val regexParser = SmsParser()
        val parsed = regexParser.parse(smsBody, senderId) ?: return null
        
        return SmsParseResult(
            amount = parsed.amount,
            isDebit = parsed.isDebit,
            merchantName = parsed.merchantName,
            accountHint = parsed.accountHint,
            bankName = parsed.detectedBankName,
            transactionDate = null,
            referenceNumber = parsed.referenceNumber,
            balance = null,
            confidence = 0.7f, // Lower confidence for regex parsing
            suggestedPatterns = null
        )
    }
}
