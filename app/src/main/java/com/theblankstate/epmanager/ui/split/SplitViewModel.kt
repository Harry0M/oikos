package com.theblankstate.epmanager.ui.split

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.theblankstate.epmanager.data.model.*
import com.theblankstate.epmanager.data.repository.AccountRepository
import com.theblankstate.epmanager.data.repository.CategoryRepository
import com.theblankstate.epmanager.data.repository.SplitRepository
import com.theblankstate.epmanager.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SplitUiState(
    val groups: List<SplitGroupWithMembers> = emptyList(),
    val selectedGroup: SplitGroup? = null,
    val groupMembers: List<GroupMember> = emptyList(),
    val groupExpenses: List<SplitExpense> = emptyList(),
    val memberBalances: List<MemberBalance> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val isLoading: Boolean = true,
    val showCreateGroupSheet: Boolean = false,
    val showAddExpenseSheet: Boolean = false,
    val showAddMemberSheet: Boolean = false,
    val showSettleSheet: Boolean = false,
    val settleWithMember: GroupMember? = null
)

@HiltViewModel
class SplitViewModel @Inject constructor(
    private val splitRepository: SplitRepository,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SplitUiState())
    val uiState: StateFlow<SplitUiState> = _uiState.asStateFlow()
    
    init {
        loadGroups()
        loadAccounts()
        // Ensure split categories exist (for existing databases)
        viewModelScope.launch {
            categoryRepository.ensureSplitCategoriesExist()
        }
    }
    
    private fun loadGroups() {
        viewModelScope.launch {
            splitRepository.getAllGroups()
                .collect { groups ->
                    // Get summary for each group
                    val groupsWithSummary = groups.map { group ->
                        val members = splitRepository.getMembersByGroupSync(group.id)
                        val total = splitRepository.getTotalExpensesByGroup(group.id)
                        SplitGroupWithMembers(
                            group = group,
                            members = members,
                            totalExpenses = total
                        )
                    }
                    _uiState.update { 
                        it.copy(
                            groups = groupsWithSummary,
                            isLoading = false
                        ) 
                    }
                }
        }
    }
    
    private fun loadAccounts() {
        viewModelScope.launch {
            accountRepository.getAllAccounts().collect { accounts ->
                _uiState.update { it.copy(accounts = accounts) }
            }
        }
    }
    
    fun selectGroup(group: SplitGroup) {
        _uiState.update { it.copy(selectedGroup = group) }
        loadGroupDetails(group.id)
    }
    
    fun clearSelection() {
        _uiState.update { 
            it.copy(
                selectedGroup = null,
                groupMembers = emptyList(),
                groupExpenses = emptyList(),
                memberBalances = emptyList()
            ) 
        }
    }
    
    private fun loadGroupDetails(groupId: String) {
        viewModelScope.launch {
            splitRepository.getMembersByGroup(groupId).collect { members ->
                _uiState.update { it.copy(groupMembers = members) }
            }
        }
        
        viewModelScope.launch {
            splitRepository.getExpensesByGroup(groupId).collect { expenses ->
                _uiState.update { it.copy(groupExpenses = expenses) }
            }
        }
        
        viewModelScope.launch {
            val balances = splitRepository.calculateBalances(groupId)
            _uiState.update { it.copy(memberBalances = balances) }
        }
    }
    
    // Sheet visibility - renamed from dialog
    fun showCreateGroupSheet() {
        _uiState.update { it.copy(showCreateGroupSheet = true) }
    }
    
    fun hideCreateGroupSheet() {
        _uiState.update { it.copy(showCreateGroupSheet = false) }
    }
    
    fun showAddExpenseSheet() {
        _uiState.update { it.copy(showAddExpenseSheet = true) }
    }
    
    fun hideAddExpenseSheet() {
        _uiState.update { it.copy(showAddExpenseSheet = false) }
    }
    
    fun showAddMemberSheet() {
        _uiState.update { it.copy(showAddMemberSheet = true) }
    }
    
    fun hideAddMemberSheet() {
        _uiState.update { it.copy(showAddMemberSheet = false) }
    }
    
    fun showSettleSheet(member: GroupMember) {
        _uiState.update { it.copy(showSettleSheet = true, settleWithMember = member) }
    }
    
    fun hideSettleSheet() {
        _uiState.update { it.copy(showSettleSheet = false, settleWithMember = null) }
    }
    
    // Actions
    fun createGroup(name: String, emoji: String, memberNames: List<String>, budget: Double?) {
        viewModelScope.launch {
            val group = SplitGroup(name = name, emoji = emoji, budget = budget)
            splitRepository.insertGroup(group)
            
            // Add "You" as the first member
            val youMember = GroupMember(
                groupId = group.id,
                name = "You",
                isCurrentUser = true
            )
            splitRepository.insertMember(youMember)
            
            // Add other members
            val otherMembers = memberNames.filter { it.isNotBlank() }.map { memberName ->
                GroupMember(
                    groupId = group.id,
                    name = memberName
                )
            }
            splitRepository.insertMembers(otherMembers)
            
            hideCreateGroupSheet()
        }
    }
    
    fun addMember(name: String, phone: String? = null) {
        val groupId = _uiState.value.selectedGroup?.id ?: return
        
        viewModelScope.launch {
            val member = GroupMember(
                groupId = groupId,
                name = name,
                phone = phone
            )
            splitRepository.insertMember(member)
            hideAddMemberSheet()
        }
    }
    
    fun addExpense(description: String, amount: Double, paidById: String, accountId: String?) {
        val groupId = _uiState.value.selectedGroup?.id ?: return
        val currentUser = _uiState.value.groupMembers.find { it.isCurrentUser }
        val isPaidByMe = paidById == currentUser?.id
        
        viewModelScope.launch {
            // Create the split expense
            splitRepository.addSplitExpense(
                groupId = groupId,
                description = description,
                totalAmount = amount,
                paidById = paidById
            )
            
            // If paid by "You" and account is selected, create a transaction
            if (isPaidByMe && accountId != null) {
                val transaction = Transaction(
                    amount = amount,
                    type = TransactionType.EXPENSE,
                    categoryId = "split_expense",
                    accountId = accountId,
                    date = System.currentTimeMillis(),
                    note = "Split: $description (${_uiState.value.selectedGroup?.name})"
                )
                transactionRepository.insertTransaction(transaction)
                
                // Update account balance
                accountRepository.updateBalance(accountId, -amount)
            }
            
            hideAddExpenseSheet()
            
            // Refresh balances
            val balances = splitRepository.calculateBalances(groupId)
            _uiState.update { it.copy(memberBalances = balances) }
        }
    }
    
    fun settleUp(amount: Double, accountId: String?) {
        val groupId = _uiState.value.selectedGroup?.id ?: return
        val settleWith = _uiState.value.settleWithMember ?: return
        val currentUser = splitRepository.getCurrentUserMemberSync(_uiState.value.groupMembers)
            ?: return
        
        val balance = _uiState.value.memberBalances.find { it.member.id == settleWith.id }?.balance ?: 0.0
        val isOwed = balance > 0 // They owe me
        
        viewModelScope.launch {
            // Create settlement record
            val settlement = if (isOwed) {
                // They owe me - they are paying me
                Settlement(
                    groupId = groupId,
                    fromMemberId = settleWith.id,
                    toMemberId = currentUser.id,
                    amount = amount
                )
            } else {
                // I owe them - I am paying them
                Settlement(
                    groupId = groupId,
                    fromMemberId = currentUser.id,
                    toMemberId = settleWith.id,
                    amount = amount
                )
            }
            splitRepository.insertSettlement(settlement)
            
            // Create transaction if account is selected
            if (accountId != null) {
                if (isOwed) {
                    // I'm receiving money - INCOME
                    val transaction = Transaction(
                        amount = amount,
                        type = TransactionType.INCOME,
                        categoryId = "split_payoff",
                        accountId = accountId,
                        date = System.currentTimeMillis(),
                        note = "Split Payoff from ${settleWith.name}"
                    )
                    transactionRepository.insertTransaction(transaction)
                    accountRepository.updateBalance(accountId, amount)
                } else {
                    // I'm paying money - EXPENSE
                    val transaction = Transaction(
                        amount = amount,
                        type = TransactionType.EXPENSE,
                        categoryId = "split_expense",
                        accountId = accountId,
                        date = System.currentTimeMillis(),
                        note = "Split Payoff to ${settleWith.name}"
                    )
                    transactionRepository.insertTransaction(transaction)
                    accountRepository.updateBalance(accountId, -amount)
                }
            }
            
            hideSettleSheet()
            
            // Refresh balances
            val balances = splitRepository.calculateBalances(groupId)
            _uiState.update { it.copy(memberBalances = balances) }
        }
    }
    
    fun deleteGroup(group: SplitGroup) {
        viewModelScope.launch {
            splitRepository.deleteGroup(group)
            clearSelection()
        }
    }
}
