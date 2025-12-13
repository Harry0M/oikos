package com.theblankstate.epmanager.ui.debt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.theblankstate.epmanager.data.model.Debt
import com.theblankstate.epmanager.data.model.DebtPayment
import com.theblankstate.epmanager.data.model.DebtType
import com.theblankstate.epmanager.data.model.Friend
import com.theblankstate.epmanager.data.model.Transaction
import com.theblankstate.epmanager.data.model.TransactionType
import com.theblankstate.epmanager.data.repository.AccountRepository
import com.theblankstate.epmanager.data.repository.DebtRepository
import com.theblankstate.epmanager.data.repository.FriendsRepository
import com.theblankstate.epmanager.data.repository.TransactionRepository
import com.theblankstate.epmanager.data.sync.DebtSyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DebtUiState(
    val debts: List<Debt> = emptyList(),
    val credits: List<Debt> = emptyList(),
    val totalDebtOwed: Double = 0.0,
    val totalCreditOwed: Double = 0.0,
    val friends: List<Friend> = emptyList(),
    val isLoading: Boolean = true,
    val selectedDebt: Debt? = null,
    val selectedDebtPayments: List<DebtPayment> = emptyList(),
    val showAddSheet: Boolean = false,
    val showDetailSheet: Boolean = false,
    val showPaymentSheet: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class DebtViewModel @Inject constructor(
    private val debtRepository: DebtRepository,
    private val friendsRepository: FriendsRepository,
    private val debtSyncManager: DebtSyncManager,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(DebtUiState())
    val uiState: StateFlow<DebtUiState> = _uiState.asStateFlow()
    
    init {
        loadData()
    }
    
    private fun loadData() {
        // Load debts
        viewModelScope.launch {
            debtRepository.getActiveDebtsByType(DebtType.DEBT)
                .collect { debts ->
                    _uiState.update { it.copy(debts = debts, isLoading = false) }
                }
        }
        
        // Load credits
        viewModelScope.launch {
            debtRepository.getActiveDebtsByType(DebtType.CREDIT)
                .collect { credits ->
                    _uiState.update { it.copy(credits = credits) }
                }
        }
        
        // Load totals
        viewModelScope.launch {
            debtRepository.getTotalDebtOwed()
                .collect { total ->
                    _uiState.update { it.copy(totalDebtOwed = total ?: 0.0) }
                }
        }
        
        viewModelScope.launch {
            debtRepository.getTotalCreditOwed()
                .collect { total ->
                    _uiState.update { it.copy(totalCreditOwed = total ?: 0.0) }
                }
        }
        
        // Load friends
        viewModelScope.launch {
            friendsRepository.getFriends()
                .catch { /* Ignore if not logged in */ }
                .collect { friends ->
                    _uiState.update { it.copy(friends = friends) }
                }
        }
    }
    
    fun showAddSheet() {
        _uiState.update { it.copy(showAddSheet = true) }
    }
    
    fun hideAddSheet() {
        _uiState.update { it.copy(showAddSheet = false) }
    }
    
    fun showDebtDetail(debt: Debt) {
        viewModelScope.launch {
            debtRepository.getPaymentsForDebt(debt.id)
                .collect { payments ->
                    _uiState.update { 
                        it.copy(
                            selectedDebt = debt, 
                            selectedDebtPayments = payments,
                            showDetailSheet = true
                        ) 
                    }
                }
        }
    }
    
    fun hideDetailSheet() {
        _uiState.update { it.copy(showDetailSheet = false, selectedDebt = null) }
    }
    
    fun showPaymentSheet() {
        _uiState.update { it.copy(showPaymentSheet = true) }
    }
    
    fun hidePaymentSheet() {
        _uiState.update { it.copy(showPaymentSheet = false) }
    }
    
    fun addDebt(
        type: DebtType,
        personName: String,
        amount: Double,
        dueDate: Long?,
        friendId: String?,
        notes: String?
    ) {
        viewModelScope.launch {
            val debt = Debt(
                type = type,
                personName = personName,
                totalAmount = amount,
                remainingAmount = amount,
                linkedFriendId = friendId,
                dueDate = dueDate,
                notes = notes
            )
            debtRepository.createDebt(debt)
            
            // Sync to friend if linked
            if (friendId != null) {
                val friend = _uiState.value.friends.find { it.odiserId == friendId }
                val myDisplayName = FirebaseAuth.getInstance().currentUser?.displayName 
                    ?: FirebaseAuth.getInstance().currentUser?.email 
                    ?: "Someone"
                
                // odiserId is the friend's Firebase UID
                if (friend != null) {
                    debtSyncManager.syncDebtToFriend(debt, friend.odiserId, myDisplayName)
                }
            }
            
            hideAddSheet()
        }
    }
    
    fun addPayment(amount: Double, transactionId: String? = null, note: String? = null) {
        val debt = _uiState.value.selectedDebt ?: return
        
        viewModelScope.launch {
            // Create a global transaction for the payment
            val categoryId = if (debt.type == DebtType.DEBT) "debt_payment" else "credit_received"
            val transactionType = if (debt.type == DebtType.DEBT) TransactionType.EXPENSE else TransactionType.INCOME
            val transactionNote = note ?: if (debt.type == DebtType.DEBT) 
                "Payment to ${debt.personName}" 
            else 
                "Received from ${debt.personName}"
            
            // Get default account
            val defaultAccount = accountRepository.getDefaultAccount()
            val accountId = defaultAccount?.id ?: "cash"
            
            // Create the transaction
            val transaction = Transaction(
                amount = amount,
                categoryId = categoryId,
                accountId = accountId,
                type = transactionType,
                date = System.currentTimeMillis(),
                note = transactionNote
            )
            transactionRepository.insertTransaction(transaction)
            
            // Update account balance
            val balanceChange = if (transactionType == TransactionType.EXPENSE) -amount else amount
            accountRepository.updateBalance(accountId, balanceChange)
            
            // Record the debt payment with linked transaction ID
            debtRepository.addPayment(debt.id, amount, transaction.id, note)
            
            // Refresh the debt detail
            val updatedDebt = debtRepository.getDebtById(debt.id)
            if (updatedDebt != null) {
                showDebtDetail(updatedDebt)
            }
            hidePaymentSheet()
        }
    }
    
    fun settleDebt() {
        val debt = _uiState.value.selectedDebt ?: return
        
        viewModelScope.launch {
            // Only create transaction if there's remaining balance
            if (debt.remainingAmount > 0) {
                // Create a global transaction for the settlement
                val categoryId = if (debt.type == DebtType.DEBT) "debt_payment" else "credit_received"
                val transactionType = if (debt.type == DebtType.DEBT) TransactionType.EXPENSE else TransactionType.INCOME
                val transactionNote = if (debt.type == DebtType.DEBT) 
                    "Final settlement to ${debt.personName}" 
                else 
                    "Final receipt from ${debt.personName}"
                
                // Get default account
                val defaultAccount = accountRepository.getDefaultAccount()
                val accountId = defaultAccount?.id ?: "cash"
                
                // Create the transaction
                val transaction = Transaction(
                    amount = debt.remainingAmount,
                    categoryId = categoryId,
                    accountId = accountId,
                    type = transactionType,
                    date = System.currentTimeMillis(),
                    note = transactionNote
                )
                transactionRepository.insertTransaction(transaction)
                
                // Update account balance
                val balanceChange = if (transactionType == TransactionType.EXPENSE) -debt.remainingAmount else debt.remainingAmount
                accountRepository.updateBalance(accountId, balanceChange)
                
                // Record the debt payment with linked transaction ID
                debtRepository.addPayment(debt.id, debt.remainingAmount, transaction.id, "Settlement")
            }
            
            debtRepository.settleDebt(debt.id)
            hideDetailSheet()
        }
    }
    
    fun deleteDebt() {
        val debt = _uiState.value.selectedDebt ?: return
        
        viewModelScope.launch {
            debtRepository.deleteDebt(debt)
            hideDetailSheet()
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
