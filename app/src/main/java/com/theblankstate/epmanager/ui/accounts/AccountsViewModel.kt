package com.theblankstate.epmanager.ui.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.theblankstate.epmanager.data.model.Account
import com.theblankstate.epmanager.data.model.AccountType
import com.theblankstate.epmanager.data.model.BankRegistry
import com.theblankstate.epmanager.data.repository.AccountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AccountsUiState(
    val accounts: List<Account> = emptyList(),
    val totalBalance: Double = 0.0,
    val isLoading: Boolean = true,
    val showAddDialog: Boolean = false,
    val showLinkDialog: Boolean = false,
    val editingAccount: Account? = null,
    val linkingAccount: Account? = null,
    val bankSuggestions: List<BankRegistry.BankInfo> = emptyList()
)

@HiltViewModel
class AccountsViewModel @Inject constructor(
    private val accountRepository: AccountRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AccountsUiState())
    val uiState: StateFlow<AccountsUiState> = _uiState.asStateFlow()
    
    init {
        loadAccounts()
        loadBankSuggestions()
    }
    
    private fun loadAccounts() {
        viewModelScope.launch {
            accountRepository.getAllAccounts()
                .collect { accounts ->
                    val total = accounts.sumOf { it.balance }
                    _uiState.update { 
                        it.copy(
                            accounts = accounts,
                            totalBalance = total,
                            isLoading = false
                        ) 
                    }
                }
        }
    }
    
    private fun loadBankSuggestions() {
        _uiState.update { 
            it.copy(bankSuggestions = accountRepository.getBankSuggestions()) 
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
                // Update existing
                val updated = existingAccount.copy(
                    name = name,
                    type = type,
                    icon = icon,
                    color = color,
                    balance = balance
                )
                accountRepository.updateAccount(updated)
            } else {
                // Create new
                val newAccount = Account(
                    name = name,
                    type = type,
                    icon = icon,
                    color = color,
                    balance = balance,
                    isDefault = false
                )
                accountRepository.insertAccount(newAccount)
            }
            hideDialog()
        }
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
     * Create a new linked account directly
     */
    fun createLinkedAccount(
        name: String,
        bankCode: String,
        accountNumber: String?,
        type: AccountType,
        balance: Double
    ) {
        viewModelScope.launch {
            accountRepository.createLinkedAccount(
                name = name,
                bankCode = bankCode,
                accountNumber = accountNumber,
                type = type
            )
            hideDialog()
        }
    }
    
    fun deleteAccount(account: Account) {
        if (account.isDefault) return // Can't delete default accounts
        
        viewModelScope.launch {
            accountRepository.deleteAccount(account)
        }
    }
}
