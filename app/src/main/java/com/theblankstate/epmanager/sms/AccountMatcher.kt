package com.theblankstate.epmanager.sms

import android.util.Log
import com.theblankstate.epmanager.data.model.Account
import com.theblankstate.epmanager.data.model.AccountType
import com.theblankstate.epmanager.data.model.BankRegistry
import com.theblankstate.epmanager.data.repository.AccountRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service to match parsed SMS transactions to user accounts
 * 
 * Matching Strategy:
 * 1. Exact match: Account number (last 4 digits) + Bank code matches
 * 2. Bank match: Bank code matches with linked account
 * 3. Type match: If UPI transaction, match to UPI account
 * 4. Create new: Suggest creating a new linked account
 */
@Singleton
class AccountMatcher @Inject constructor(
    private val accountRepository: AccountRepository
) {
    
    data class MatchResult(
        val matchedAccount: Account?,
        val confidence: MatchConfidence,
        val suggestedNewAccount: SuggestedAccount?,
        val allCandidates: List<AccountCandidate>
    )
    
    data class AccountCandidate(
        val account: Account,
        val score: Int,
        val matchReason: String
    )
    
    data class SuggestedAccount(
        val name: String,
        val type: AccountType,
        val bankCode: String?,
        val bankName: String?,
        val accountNumber: String?,
        val senderIds: List<String>,
        val color: Long
    )
    
    enum class MatchConfidence {
        HIGH,      // >80 score - Very confident match
        MEDIUM,    // 50-80 score - Likely match but verify
        LOW,       // 20-50 score - Possible match
        NONE       // <20 or no linked accounts found
    }
    
    /**
     * Find the best matching account for a parsed transaction
     */
    suspend fun findMatchingAccount(
        parsed: SmsParser.ParsedTransaction,
        senderId: String
    ): MatchResult {
        val accounts = accountRepository.getAllAccounts().first()
        val linkedAccounts = accounts.filter { it.isLinked }
        
        Log.d("AccountMatcher", "Finding match for sender=$senderId, accountHint=${parsed.accountHint}, bank=${parsed.detectedBankCode}")
        Log.d("AccountMatcher", "Found ${linkedAccounts.size} linked accounts")
        
        // Score all linked accounts
        val candidates = linkedAccounts.map { account ->
            val score = account.getMatchScore(senderId, parsed.accountHint)
            val reason = buildMatchReason(account, senderId, parsed.accountHint)
            AccountCandidate(account, score, reason)
        }.sortedByDescending { it.score }
        
        // Find best match
        val bestCandidate = candidates.firstOrNull()
        
        val (matchedAccount, confidence) = when {
            bestCandidate == null -> null to MatchConfidence.NONE
            bestCandidate.score >= 80 -> bestCandidate.account to MatchConfidence.HIGH
            bestCandidate.score >= 50 -> bestCandidate.account to MatchConfidence.MEDIUM
            bestCandidate.score >= 20 -> bestCandidate.account to MatchConfidence.LOW
            else -> null to MatchConfidence.NONE
        }
        
        // Generate suggestion for new account if no good match
        val suggestedAccount = if (confidence == MatchConfidence.NONE || confidence == MatchConfidence.LOW) {
            generateSuggestedAccount(parsed, senderId)
        } else null
        
        return MatchResult(
            matchedAccount = matchedAccount,
            confidence = confidence,
            suggestedNewAccount = suggestedAccount,
            allCandidates = candidates
        )
    }
    
    /**
     * Find account by exact match on bank code and account number
     */
    suspend fun findExactMatch(
        bankCode: String,
        accountNumber: String
    ): Account? {
        val accounts = accountRepository.getAllAccounts().first()
        return accounts.find { account ->
            account.isLinked &&
            account.bankCode?.equals(bankCode, ignoreCase = true) == true &&
            account.accountNumber == accountNumber
        }
    }
    
    /**
     * Find accounts by bank
     */
    suspend fun findAccountsByBank(bankCode: String): List<Account> {
        val accounts = accountRepository.getAllAccounts().first()
        return accounts.filter { account ->
            account.isLinked &&
            (account.bankCode?.equals(bankCode, ignoreCase = true) == true ||
             account.linkedSenderIds?.contains(bankCode, ignoreCase = true) == true)
        }
    }
    
    private fun buildMatchReason(account: Account, senderId: String, accountHint: String?): String {
        val reasons = mutableListOf<String>()
        val normalizedSender = senderId.uppercase().replace("-", "")
        
        if (account.accountNumber != null && account.accountNumber == accountHint) {
            reasons.add("Account number matched (****$accountHint)")
        }
        
        if (account.bankCode?.let { normalizedSender.contains(it.uppercase()) } == true) {
            reasons.add("Bank code matched (${account.bankCode})")
        }
        
        if (account.linkedSenderIds?.split(",")?.any { normalizedSender.contains(it.trim().uppercase()) } == true) {
            reasons.add("Sender ID matched")
        }
        
        return if (reasons.isEmpty()) "No specific match" else reasons.joinToString(", ")
    }
    
    private fun generateSuggestedAccount(
        parsed: SmsParser.ParsedTransaction,
        senderId: String
    ): SuggestedAccount? {
        val detectedBank = BankRegistry.findBankBySender(senderId)
        
        // Determine account type based on card type or bank
        val accountType = when {
            parsed.cardType == SmsParser.CardType.CREDIT_CARD -> AccountType.CREDIT_CARD
            parsed.upiId != null -> AccountType.UPI
            detectedBank != null -> AccountType.BANK
            else -> AccountType.OTHER
        }
        
        // Generate name suggestion
        val nameSuggestion = buildString {
            if (detectedBank != null) {
                append(detectedBank.name)
            } else {
                append("Unknown Bank")
            }
            when (parsed.cardType) {
                SmsParser.CardType.CREDIT_CARD -> append(" Credit Card")
                SmsParser.CardType.DEBIT_CARD -> append(" Debit Card")
                else -> if (parsed.accountHint != null) append(" ****${parsed.accountHint}")
            }
        }
        
        return SuggestedAccount(
            name = nameSuggestion,
            type = accountType,
            bankCode = detectedBank?.code ?: parsed.detectedBankCode,
            bankName = detectedBank?.name ?: parsed.detectedBankName,
            accountNumber = parsed.accountHint,
            senderIds = detectedBank?.senderPatterns ?: listOf(senderId),
            color = detectedBank?.color ?: 0xFF6B7280
        )
    }
    
    /**
     * Create a new linked account from suggestion
     */
    fun createAccountFromSuggestion(suggestion: SuggestedAccount): Account {
        return Account(
            name = suggestion.name,
            type = suggestion.type,
            icon = when (suggestion.type) {
                AccountType.CREDIT_CARD -> "CreditCard"
                AccountType.UPI -> "PhoneAndroid"
                AccountType.BANK -> "AccountBalance"
                AccountType.WALLET -> "AccountBalanceWallet"
                else -> "AccountBalance"
            },
            color = suggestion.color,
            bankCode = suggestion.bankCode,
            accountNumber = suggestion.accountNumber,
            linkedSenderIds = suggestion.senderIds.joinToString(","),
            isLinked = true
        )
    }
}
