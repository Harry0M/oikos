package com.theblankstate.epmanager.ui.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.theblankstate.epmanager.data.model.Account
import com.theblankstate.epmanager.data.model.AccountType
import com.theblankstate.epmanager.data.model.BankRegistry
import com.theblankstate.epmanager.data.model.SmsTemplate
import com.theblankstate.epmanager.data.model.Transaction
import com.theblankstate.epmanager.data.model.TransactionType
import com.theblankstate.epmanager.data.repository.AccountRepository
import com.theblankstate.epmanager.data.repository.TransactionRepository
import com.theblankstate.epmanager.data.local.dao.SmsTemplateDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs

/**
 * UI state for Accounts screen
 */
data class AccountsUiState(
    val accounts: List<Account> = emptyList(),
    val linkedAccounts: List<Account> = emptyList(),
    val unlinkedAccounts: List<Account> = emptyList(),
    val totalBalance: Double = 0.0,
    val isLoading: Boolean = true,
    val showAddDialog: Boolean = false,
    val showLinkDialog: Boolean = false,
    val editingAccount: Account? = null,
    val linkingAccount: Account? = null,
    val bankSuggestions: List<BankSuggestion> = emptyList(),
    val customTemplates: List<SmsTemplate> = emptyList()
)

/**
 * Unified bank suggestion that can be either from registry or custom template
 */
data class BankSuggestion(
    val name: String,
    val code: String,
    val senderPatterns: List<String>,
    val color: Long,
    val isCustom: Boolean = false,
    val templateId: String? = null
)

@HiltViewModel
class AccountsViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val smsTemplateDao: SmsTemplateDao
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AccountsUiState())
    val uiState: StateFlow<AccountsUiState> = _uiState.asStateFlow()
    
    // Category ID for adjustment transactions
    private val ADJUSTMENT_CATEGORY = "adjustment"
    
    init {
        loadAccounts()
        loadBankSuggestions()
    }
    
    private fun loadAccounts() {
        viewModelScope.launch {
            accountRepository.getAllAccounts()
                .collect { accounts ->
                    val total = accounts.sumOf { it.balance }
                    val linked = accounts.filter { it.isLinked }
                    val unlinked = accounts.filter { !it.isLinked }
                    
                    _uiState.update { 
                        it.copy(
                            accounts = accounts,
                            linkedAccounts = linked,
                            unlinkedAccounts = unlinked,
                            totalBalance = total,
                            isLoading = false
                        ) 
                    }
                }
        }
    }
    
    private fun loadBankSuggestions() {
        viewModelScope.launch {
            // Get registry banks
            val registryBanks = (BankRegistry.banks + BankRegistry.upiProviders).map { bank ->
                BankSuggestion(
                    name = bank.name,
                    code = bank.code,
                    senderPatterns = bank.senderPatterns,
                    color = bank.color,
                    isCustom = false
                )
            }
            
            // Add custom templates
            smsTemplateDao.getAllActiveTemplates()
                .collect { templates ->
                    val customBanks = templates.map { template ->
                        BankSuggestion(
                            name = template.bankName,
                            code = template.bankName.uppercase().take(6),
                            senderPatterns = template.senderIds.split(",").map { it.trim() },
                            color = 0xFF6B7280, // Gray for custom
                            isCustom = true,
                            templateId = template.id
                        )
                    }
                    
                    _uiState.update { 
                        it.copy(
                            bankSuggestions = registryBanks + customBanks,
                            customTemplates = templates
                        ) 
                    }
                }
        }
    }
    
    fun showAddDialog() {
        _uiState.update { it.copy(showAddDialog = true, editingAccount = null) }
    }
    
    fun showEditDialog(account: Account) {
        _uiState.update { it.copy(showAddDialog = true, editingAccount = account) }
    }
    
    fun hideDialog() {
        _uiState.update { it.copy(showAddDialog = false, editingAccount = null) }
    }
    
    fun showLinkDialog(account: Account) {
        _uiState.update { it.copy(showLinkDialog = true, linkingAccount = account) }
    }
    
    fun hideLinkDialog() {
        _uiState.update { it.copy(showLinkDialog = false, linkingAccount = null) }
    }
    
    fun saveAccount(
        name: String,
        type: AccountType,
        icon: String,
        color: Long,
        balance: Double
    ) {
        viewModelScope.launch {
            val existingAccount = _uiState.value.editingAccount
            if (existingAccount != null) {
                // Calculate balance difference for adjustment transaction
                val balanceDiff = balance - existingAccount.balance
                
                // Update existing account
                val updated = existingAccount.copy(
                    name = name,
                    type = type,
                    icon = icon,
                    color = color,
                    balance = balance
                )
                accountRepository.updateAccount(updated)
                
                // Record adjustment transaction if balance changed
                if (balanceDiff != 0.0) {
                    recordAdjustmentTransaction(
                        accountId = existingAccount.id,
                        accountName = name,
                        amount = balanceDiff,
                        note = "Manual balance adjustment"
                    )
                }
            } else {
                // Create new account
                val newAccount = Account(
                    name = name,
                    type = type,
                    icon = icon,
                    color = color,
                    balance = balance,
                    isDefault = false
                )
                accountRepository.insertAccount(newAccount)
                
                // Record initial balance as adjustment if not zero
                if (balance != 0.0) {
                    recordAdjustmentTransaction(
                        accountId = newAccount.id,
                        accountName = name,
                        amount = balance,
                        note = "Initial balance"
                    )
                }
            }
            hideDialog()
        }
    }
    
    /**
     * Record an adjustment transaction for audit trail
     */
    private suspend fun recordAdjustmentTransaction(
        accountId: String,
        accountName: String,
        amount: Double,
        note: String
    ) {
        val transaction = Transaction(
            amount = abs(amount),
            type = if (amount >= 0) TransactionType.INCOME else TransactionType.EXPENSE,
            categoryId = ADJUSTMENT_CATEGORY,
            accountId = accountId,
            date = System.currentTimeMillis(),
            note = "[$accountName] $note",
            isSynced = false
        )
        transactionRepository.insertTransaction(transaction)
    }
    
    /**
     * Link an account to a bank for SMS auto-detection
     */
    fun linkAccount(
        accountId: String,
        bankCode: String,
        accountNumber: String?,
        senderIds: List<String>
    ) {
        viewModelScope.launch {
            accountRepository.linkAccount(
                accountId = accountId,
                bankCode = bankCode,
                accountNumber = accountNumber,
                senderIds = senderIds
            )
            hideLinkDialog()
        }
    }
    
    /**
     * Unlink an account from SMS auto-detection
     */
    fun unlinkAccount(account: Account) {
        viewModelScope.launch {
            accountRepository.unlinkAccount(account.id)
        }
    }
    
    /**
     * Create a new linked bank account
     */
    fun createLinkedAccount(
        name: String,
        bankSuggestion: BankSuggestion,
        accountNumber: String?,
        type: AccountType,
        balance: Double
    ) {
        viewModelScope.launch {
            val account = accountRepository.createLinkedAccount(
                name = name,
                bankCode = bankSuggestion.code,
                accountNumber = accountNumber,
                type = type,
                senderIds = bankSuggestion.senderPatterns
            )
            
            // Record initial balance if not zero
            if (balance != 0.0) {
                // Update the balance
                accountRepository.updateBalance(account.id, balance)
                
                recordAdjustmentTransaction(
                    accountId = account.id,
                    accountName = name,
                    amount = balance,
                    note = "Initial balance"
                )
            }
            
            hideDialog()
        }
    }
    
    fun deleteAccount(account: Account) {
        if (account.isDefault) return // Can't delete default accounts
        
        viewModelScope.launch {
            // Record closing transaction if account has balance
            if (account.balance != 0.0) {
                recordAdjustmentTransaction(
                    accountId = account.id,
                    accountName = account.name,
                    amount = -account.balance, // Reverse the balance
                    note = "Account closed"
                )
            }
            
            accountRepository.deleteAccount(account)
        }
    }
}
