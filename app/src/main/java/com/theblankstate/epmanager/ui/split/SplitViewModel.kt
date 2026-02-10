package com.theblankstate.epmanager.ui.split

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.theblankstate.epmanager.data.model.*
import com.theblankstate.epmanager.data.repository.AccountRepository
import com.theblankstate.epmanager.data.repository.CategoryRepository
import com.theblankstate.epmanager.data.repository.FriendsRepository
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
    val friends: List<Friend> = emptyList(), // Linked friends for quick-add
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
    private val categoryRepository: CategoryRepository,
    private val friendsRepository: FriendsRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SplitUiState())
    val uiState: StateFlow<SplitUiState> = _uiState.asStateFlow()
    
    init {
        loadGroups()
        loadAccounts()
        loadFriends()
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
    
    private fun loadFriends() {
        viewModelScope.launch {
            friendsRepository.getFriends().collect { friends ->
                _uiState.update { it.copy(friends = friends) }
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
    fun createGroup(
        name: String, 
        emoji: String,
        planType: PlanType,
        memberNames: List<String>, 
        linkedFriends: List<Friend>,
        budget: Double?
    ) {
        viewModelScope.launch {
            val group = SplitGroup(name = name, emoji = emoji, planType = planType, budget = budget)
            splitRepository.insertGroup(group)

            
            // Add "You" as the first member
            val youMember = GroupMember(
                groupId = group.id,
                name = "You",
                isCurrentUser = true
            )
            splitRepository.insertMember(youMember)
            
            // Add manual members (offline, no linked account)
            val manualMembers = memberNames.filter { it.isNotBlank() }.map { memberName ->
                GroupMember(
                    groupId = group.id,
                    name = memberName
                )
            }
            splitRepository.insertMembers(manualMembers)
            
            // Add linked friends (with their Firebase userId)
            val linkedMembers = linkedFriends.map { friend ->
                GroupMember(
                    groupId = group.id,
                    name = friend.displayName ?: friend.email.substringBefore("@"),
                    email = friend.email,
                    linkedUserId = friend.odiserId
                )
            }
            splitRepository.insertMembers(linkedMembers)
            
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
    
    fun addLinkedMember(friend: Friend) {
        val groupId = _uiState.value.selectedGroup?.id ?: return
        val existingMembers = _uiState.value.groupMembers
        
        // Check if already in group (by linkedUserId)
        if (existingMembers.any { it.linkedUserId == friend.odiserId }) {
            // Already in group, don't add again
            hideAddMemberSheet()
            return
        }
        
        viewModelScope.launch {
            val member = GroupMember(
                groupId = groupId,
                name = friend.displayName ?: friend.email.substringBefore("@"),
                email = friend.email,
                linkedUserId = friend.odiserId
            )
            splitRepository.insertMember(member)
            hideAddMemberSheet()
        }
    }
    
    fun addExpense(
        description: String, 
        amount: Double, 
        paidById: String, 
        accountId: String?,
        enableSplit: Boolean = true,
        includedMemberIds: List<String>? = null,
        customShares: Map<String, Double>? = null,  // Changed to Map for all members
        instantSettle: Boolean = false
    ) {
        val groupId = _uiState.value.selectedGroup?.id ?: return
        val currentUser = _uiState.value.groupMembers.find { it.isCurrentUser }
        val isPaidByMe = paidById == currentUser?.id
        
        viewModelScope.launch {
            // Create the split expense with new fields
            val expense = SplitExpense(
                groupId = groupId,
                description = description,
                totalAmount = amount,
                paidById = paidById,
                enableSplit = enableSplit,
                includedMemberIds = includedMemberIds?.joinToString(","),
                customUserShare = customShares?.get(currentUser?.id)  // Store current user's custom share
            )
            splitRepository.insertExpense(expense)
            
            // Create ExpenseShare records for balance calculation
            if (enableSplit) {
                val memberIds = includedMemberIds ?: _uiState.value.groupMembers.map { it.id }
                val equalShare = amount / memberIds.size
                
                val shares = memberIds.map { memberId ->
                    ExpenseShare(
                        expenseId = expense.id,
                        memberId = memberId,
                        // Use custom share if provided for this member, otherwise equal share
                        shareAmount = customShares?.get(memberId) ?: equalShare
                    )
                }
                splitRepository.insertShares(shares)
            }

            
            // If paid by "You" and account is selected, create a transaction for the full amount
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
            } else if (!isPaidByMe && accountId != null && enableSplit && currentUser != null) {
                // Someone else paid - record YOUR share as an expense from the repayment account
                val memberIds = includedMemberIds ?: _uiState.value.groupMembers.map { it.id }
                val userIncluded = currentUser.id in memberIds
                
                if (userIncluded) {
                    // Calculate user's share
                    val userShare = customShares?.get(currentUser.id) ?: (amount / memberIds.size)
                    val payer = _uiState.value.groupMembers.find { it.id == paidById }
                    val payerName = payer?.name ?: "Someone"
                    
                    val transaction = Transaction(
                        amount = userShare,
                        type = TransactionType.EXPENSE,
                        categoryId = "split_expense",
                        accountId = accountId,
                        date = System.currentTimeMillis(),
                        note = "Split Share: $description - paid by $payerName (${_uiState.value.selectedGroup?.name})"
                    )
                    transactionRepository.insertTransaction(transaction)
                    
                    // Update account balance
                    accountRepository.updateBalance(accountId, -userShare)
                    
                    // If instant settle is enabled, create settlement record to clear the balance
                    if (instantSettle && paidById != null) {
                        val settlement = Settlement(
                            groupId = groupId,
                            fromMemberId = currentUser.id,  // I paid back
                            toMemberId = paidById,          // To the person who paid
                            amount = userShare,
                            note = "Instant settle: $description"
                        )
                        splitRepository.insertSettlement(settlement)
                    }
                }
            }
            
            hideAddExpenseSheet()
            
            // Refresh balances
            val balances = splitRepository.calculateBalances(groupId)
            _uiState.update { it.copy(memberBalances = balances) }
        }
    }
    
    fun settleUp(amount: Double, accountId: String?) {
        val groupId = _uiState.value.selectedGroup?.id ?: return
        val groupName = _uiState.value.selectedGroup?.name ?: "Split Group"
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
            
            // Send notification to linked friend if applicable
            settleWith.linkedUserId?.let { linkedUserId ->
                friendsRepository.sendSettlementNotification(
                    toUserId = linkedUserId,
                    amount = amount,
                    groupId = groupId,
                    groupName = groupName
                )
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
