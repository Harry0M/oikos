package com.theblankstate.epmanager.data.repository

import com.theblankstate.epmanager.data.local.dao.AccountDao
import com.theblankstate.epmanager.data.model.Account
import com.theblankstate.epmanager.data.model.AccountType
import com.theblankstate.epmanager.data.model.DefaultAccounts
import com.theblankstate.epmanager.data.model.BankRegistry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepository @Inject constructor(
    private val accountDao: AccountDao
) {
    fun getAllAccounts(): Flow<List<Account>> = 
        accountDao.getAllAccounts()
    
    fun getTotalBalance(): Flow<Double?> = 
        accountDao.getTotalBalance()
    
    suspend fun getAccountById(id: String): Account? = 
        accountDao.getAccountById(id)
    
    suspend fun getDefaultAccount(): Account? = 
        accountDao.getDefaultAccount()
    
    suspend fun getDefaultAccountSync(): Account? = 
        accountDao.getDefaultAccount()
    
    suspend fun insertAccount(account: Account) {
        accountDao.insertAccount(account)
    }
    
    suspend fun updateAccount(account: Account) {
        accountDao.updateAccount(account)
    }
    
    suspend fun updateBalance(accountId: String, amount: Double) {
        accountDao.updateBalance(accountId, amount)
    }
    
    suspend fun deleteAccount(account: Account) {
        accountDao.deleteAccount(account)
    }
    
    suspend fun initializeDefaultAccounts() {
        val count = accountDao.getAccountCount()
        if (count == 0) {
            accountDao.insertAccounts(DefaultAccounts.accounts)
        }
    }
    
    // ========== LINKED ACCOUNT OPERATIONS ==========
    
    /**
     * Get all linked accounts (accounts set up for SMS auto-detection)
     */
    fun getLinkedAccounts(): Flow<List<Account>> = 
        accountDao.getAllAccounts().map { accounts ->
            accounts.filter { it.isLinked }
        }
    
    /**
     * Get accounts by bank code
     */
    suspend fun getAccountsByBank(bankCode: String): List<Account> {
        val accounts = accountDao.getAllAccounts().first()
        return accounts.filter { account ->
            account.bankCode?.equals(bankCode, ignoreCase = true) == true
        }
    }
    
    /**
     * Find account by bank code and account number (last 4 digits)
     */
    suspend fun findLinkedAccount(bankCode: String?, accountNumber: String?): Account? {
        if (bankCode == null && accountNumber == null) return null
        
        val accounts = accountDao.getAllAccounts().first()
        return accounts.find { account ->
            account.isLinked &&
            (bankCode == null || account.bankCode?.equals(bankCode, ignoreCase = true) == true) &&
            (accountNumber == null || account.accountNumber == accountNumber)
        }
    }
    
    /**
     * Link an existing account to a real bank account
     */
    suspend fun linkAccount(
        accountId: String,
        bankCode: String?,
        accountNumber: String?,
        senderIds: List<String>
    ) {
        val account = accountDao.getAccountById(accountId) ?: return
        val updatedAccount = account.copy(
            bankCode = bankCode,
            accountNumber = accountNumber,
            linkedSenderIds = senderIds.joinToString(","),
            isLinked = true
        )
        accountDao.updateAccount(updatedAccount)
    }
    
    /**
     * Unlink an account from SMS auto-detection
     */
    suspend fun unlinkAccount(accountId: String) {
        val account = accountDao.getAccountById(accountId) ?: return
        val updatedAccount = account.copy(
            isLinked = false
        )
        accountDao.updateAccount(updatedAccount)
    }
    
    /**
     * Create a new linked bank account
     */
    suspend fun createLinkedAccount(
        name: String,
        bankCode: String,
        accountNumber: String?,
        type: AccountType = AccountType.BANK,
        senderIds: List<String> = emptyList()
    ): Account {
        val bankInfo = BankRegistry.findBankByCode(bankCode)
        
        val icon = when (type) {
            AccountType.CREDIT_CARD -> "CreditCard"
            AccountType.UPI -> "PhoneAndroid"
            AccountType.WALLET -> "AccountBalanceWallet"
            else -> "AccountBalance"
        }
        
        val color = bankInfo?.color ?: 0xFF6B7280
        
        val finalSenderIds = if (senderIds.isEmpty()) {
            bankInfo?.senderPatterns ?: listOf(bankCode)
        } else {
            senderIds
        }
        
        val account = Account(
            name = name,
            type = type,
            icon = icon,
            color = color,
            bankCode = bankCode,
            accountNumber = accountNumber,
            linkedSenderIds = finalSenderIds.joinToString(","),
            isLinked = true
        )
        
        accountDao.insertAccount(account)
        return account
    }
    
    /**
     * Get suggestions for accounts that could be linked based on bank registry
     */
    fun getBankSuggestions(): List<BankRegistry.BankInfo> {
        return BankRegistry.banks + BankRegistry.upiProviders
    }
}
