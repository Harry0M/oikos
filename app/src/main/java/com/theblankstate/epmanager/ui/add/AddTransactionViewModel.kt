package com.theblankstate.epmanager.ui.add

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.theblankstate.epmanager.data.model.*
import com.theblankstate.epmanager.data.repository.AccountRepository
import com.theblankstate.epmanager.data.repository.CategoryRepository
import com.theblankstate.epmanager.data.repository.DebtRepository
import com.theblankstate.epmanager.data.repository.FriendsRepository
import com.theblankstate.epmanager.data.repository.SavingsGoalRepository
import com.theblankstate.epmanager.data.repository.SplitRepository
import com.theblankstate.epmanager.data.repository.TransactionRepository
import com.theblankstate.epmanager.data.local.dao.SmsTemplateDao
import com.theblankstate.epmanager.data.repository.AvailableBankRepository
import com.theblankstate.epmanager.ui.accounts.BankSuggestion
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Data class representing someone who owes you or you owe them
 */
data class OwedBalance(
    val groupId: String,
    val groupName: String,
    val groupEmoji: String,
    val member: GroupMember,
    val amount: Double, // Positive = they owe you, Negative = you owe them
    val isLinkedFriend: Boolean = false
)

data class AddTransactionUiState(
    val amount: String = "",
    val note: String = "",
    val transactionType: TransactionType = TransactionType.EXPENSE,
    val selectedCategory: Category? = null,
    val selectedAccount: Account? = null,
    val selectedDate: Long = System.currentTimeMillis(),
    val categories: List<Category> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
    val isSaved: Boolean = false,
    // Location state
    val currentLocation: Location? = null,
    val locationName: String? = null,
    val isLocationEnabled: Boolean = true, // User can toggle off
    // Split balance state
    val owedBalances: List<OwedBalance> = emptyList(),
    val showSettleSheet: Boolean = false,
    val selectedSettleBalance: OwedBalance? = null,
    val isSettling: Boolean = false,
    val settleSuccess: String? = null,
    // Quick Split state
    val isSplitEnabled: Boolean = false,
    val friends: List<Friend> = emptyList(),
    val selectedFriends: List<Friend> = emptyList(),
    val manualSplitMembers: List<String> = emptyList(), // Names of non-friend members
    // Goal Contribution state (for income)
    val savingsGoals: List<SavingsGoal> = emptyList(),
    val selectedGoal: SavingsGoal? = null,
    val contributeToGoal: Boolean = false,
    // Debt Payment state
    val activeDebts: List<Debt> = emptyList(),
    val activeCredits: List<Debt> = emptyList(),
    val selectedDebt: Debt? = null,
    val isDebtPayment: Boolean = false,
    // Bank suggestions for adding linked accounts
    val bankSuggestions: List<BankSuggestion> = emptyList()
)

@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val availableBankRepository: AvailableBankRepository,
    private val splitRepository: SplitRepository,
    private val friendsRepository: FriendsRepository,
    private val savingsGoalRepository: SavingsGoalRepository,
    private val debtRepository: DebtRepository,
    private val smsTemplateDao: SmsTemplateDao
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AddTransactionUiState())
    val uiState: StateFlow<AddTransactionUiState> = _uiState.asStateFlow()
    
    init {
        // Ensure all required categories exist (including Goals)
        viewModelScope.launch {
            categoryRepository.ensureSplitCategoriesExist()
        }
        loadData()
        loadOwedBalances()
        loadBankSuggestions()
    }
    
    private fun loadData() {
        // Load expense categories by default
        viewModelScope.launch {
            categoryRepository.getExpenseCategories()
                .collect { categories ->
                    _uiState.update { state ->
                        state.copy(
                            categories = categories,
                            selectedCategory = state.selectedCategory ?: categories.firstOrNull()
                        )
                    }
                }
        }
        
        // Load accounts
        viewModelScope.launch {
            accountRepository.getAllAccounts()
                .collect { accounts ->
                    _uiState.update { state ->
                        state.copy(
                            accounts = accounts,
                            selectedAccount = state.selectedAccount ?: accounts.firstOrNull { it.isDefault } ?: accounts.firstOrNull(),
                            isLoading = false
                        )
                    }
                }
        }
        
        // Load friends for quick split
        viewModelScope.launch {
            friendsRepository.getFriends()
                .catch { /* Ignore - user may not be logged in */ }
                .collect { friends ->
                    _uiState.update { it.copy(friends = friends) }
                }
        }
        
        // Load active savings goals for goal contribution
        viewModelScope.launch {
            savingsGoalRepository.getActiveGoals()
                .collect { goals ->
                    _uiState.update { it.copy(savingsGoals = goals) }
                }
        }
        
        // Load active debts for debt payment
        viewModelScope.launch {
            debtRepository.getActiveDebtsByType(DebtType.DEBT)
                .collect { debts ->
                    _uiState.update { it.copy(activeDebts = debts) }
                }
        }
        
        // Load active credits for credit receipt
        viewModelScope.launch {
            debtRepository.getActiveDebtsByType(DebtType.CREDIT)
                .collect { credits ->
                    _uiState.update { it.copy(activeCredits = credits) }
                }
        }
    }
    
    private fun loadOwedBalances() {
        viewModelScope.launch {
            try {
                val groups = splitRepository.getGroupsWithSummary()
                val allOwedBalances = mutableListOf<OwedBalance>()
                
                groups.forEach { groupWithMembers ->
                    val group = groupWithMembers.group
                    val balances = splitRepository.calculateBalances(group.id)
                    
                    balances.forEach { memberBalance ->
                        // Skip if balance is essentially zero
                        if (kotlin.math.abs(memberBalance.balance) > 0.01) {
                            // Skip the current user
                            if (!memberBalance.member.isCurrentUser) {
                                allOwedBalances.add(
                                    OwedBalance(
                                        groupId = group.id,
                                        groupName = group.name,
                                        groupEmoji = group.emoji,
                                        member = memberBalance.member,
                                        amount = memberBalance.balance,
                                        isLinkedFriend = memberBalance.member.linkedUserId != null
                                    )
                                )
                            }
                        }
                    }
                }
                
                // Sort: who owes you first (positive), then who you owe (negative)
                val sortedBalances = allOwedBalances.sortedByDescending { it.amount }
                _uiState.update { it.copy(owedBalances = sortedBalances) }
            } catch (e: Exception) {
                // Silently fail
            }
        }
    }
    
    // Settle Sheet management
    fun showSettleSheet(owedBalance: OwedBalance) {
        _uiState.update { it.copy(showSettleSheet = true, selectedSettleBalance = owedBalance) }
    }
    
    fun hideSettleSheet() {
        _uiState.update { it.copy(showSettleSheet = false, selectedSettleBalance = null) }
    }
    
    /**
     * Settle with a member - called from the bottom sheet
     */
    fun settleBalance(amount: Double, accountId: String?) {
        val owedBalance = _uiState.value.selectedSettleBalance ?: return
        val finalAccountId = accountId ?: _uiState.value.selectedAccount?.id ?: return
        val isOwedToYou = owedBalance.amount > 0
        
        _uiState.update { it.copy(isSettling = true) }
        
        viewModelScope.launch {
            try {
                // Get current user member
                val members = splitRepository.getMembersByGroupSync(owedBalance.groupId)
                val currentUser = members.find { it.isCurrentUser } ?: return@launch
                
                // Create settlement record
                val settlement = if (isOwedToYou) {
                    // They owe me - they are paying me
                    Settlement(
                        groupId = owedBalance.groupId,
                        fromMemberId = owedBalance.member.id,
                        toMemberId = currentUser.id,
                        amount = amount
                    )
                } else {
                    // I owe them - I am paying them
                    Settlement(
                        groupId = owedBalance.groupId,
                        fromMemberId = currentUser.id,
                        toMemberId = owedBalance.member.id,
                        amount = amount
                    )
                }
                splitRepository.insertSettlement(settlement)
                
                // Create transaction
                if (isOwedToYou) {
                    // I'm receiving money - INCOME
                    val transaction = Transaction(
                        amount = amount,
                        type = TransactionType.INCOME,
                        categoryId = "split_payoff",
                        accountId = finalAccountId,
                        date = System.currentTimeMillis(),
                        note = "Split Payoff from ${owedBalance.member.name} (${owedBalance.groupName})"
                    )
                    transactionRepository.insertTransaction(transaction)
                    accountRepository.updateBalance(finalAccountId, amount)
                } else {
                    // I'm paying money - EXPENSE
                    val transaction = Transaction(
                        amount = amount,
                        type = TransactionType.EXPENSE,
                        categoryId = "split_expense",
                        accountId = finalAccountId,
                        date = System.currentTimeMillis(),
                        note = "Split Payoff to ${owedBalance.member.name} (${owedBalance.groupName})"
                    )
                    transactionRepository.insertTransaction(transaction)
                    accountRepository.updateBalance(finalAccountId, -amount)
                }
                
                // Send notification to linked friend if applicable
                owedBalance.member.linkedUserId?.let { linkedUserId ->
                    friendsRepository.sendSettlementNotification(
                        toUserId = linkedUserId,
                        amount = amount,
                        groupId = owedBalance.groupId,
                        groupName = owedBalance.groupName
                    )
                }
                
                hideSettleSheet()
                
                _uiState.update { 
                    it.copy(
                        isSettling = false,
                        settleSuccess = "Settled ₹${String.format("%.0f", amount)} with ${owedBalance.member.name}"
                    ) 
                }
                
                // Reload balances
                loadOwedBalances()
                
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(isSettling = false, error = "Failed to settle: ${e.message}") 
                }
            }
        }
    }
    
    fun clearSettleSuccess() {
        _uiState.update { it.copy(settleSuccess = null) }
    }
    
    fun updateAmount(amount: String) {
        // Only allow valid number input
        val filtered = amount.filter { it.isDigit() || it == '.' }
        if (filtered.count { it == '.' } <= 1) {
            _uiState.update { it.copy(amount = filtered) }
        }
    }
    
    fun updateNote(note: String) {
        _uiState.update { it.copy(note = note) }
    }
    
    fun updateTransactionType(type: TransactionType) {
        _uiState.update { it.copy(transactionType = type, selectedCategory = null) }
        
        // Load categories for the selected type
        viewModelScope.launch {
            val flow = if (type == TransactionType.EXPENSE) {
                categoryRepository.getExpenseCategories()
            } else {
                categoryRepository.getIncomeCategories()
            }
            
            flow.collect { categories ->
                _uiState.update { state ->
                    state.copy(
                        categories = categories,
                        selectedCategory = categories.firstOrNull()
                    )
                }
            }
        }
    }
    
    fun selectCategory(category: Category) {
        _uiState.update { it.copy(selectedCategory = category) }
    }
    
    fun createCategory(name: String) {
        viewModelScope.launch {
            val categoryType = when (_uiState.value.transactionType) {
                TransactionType.EXPENSE -> CategoryType.EXPENSE
                TransactionType.INCOME -> CategoryType.INCOME
            }
            val newCategory = Category(
                id = java.util.UUID.randomUUID().toString(),
                name = name,
                icon = "Category", // Default icon
                color = 0xFF6366F1, // Default indigo color
                type = categoryType
            )
            categoryRepository.insertCategory(newCategory)
            // Refresh categories and select the new one
            val updatedCategories = categoryRepository.getCategoriesByType(categoryType).first()
            _uiState.update { it.copy(
                categories = updatedCategories,
                selectedCategory = newCategory
            ) }
        }
    }
    
    fun addCategory(name: String, icon: String, color: Long, type: CategoryType) {
        viewModelScope.launch {
            val newCategory = Category(
                id = java.util.UUID.randomUUID().toString(),
                name = name,
                icon = icon,
                color = color,
                type = type
            )
            categoryRepository.insertCategory(newCategory)
            // Refresh categories and select the new one
            val updatedCategories = categoryRepository.getCategoriesByType(type).first()
            _uiState.update { it.copy(
                categories = updatedCategories,
                selectedCategory = newCategory
            ) }
        }
    }
    
    fun selectAccount(account: Account) {
        _uiState.update { it.copy(selectedAccount = account) }
    }
    
    fun addAccount(
        name: String,
        type: AccountType,
        icon: String,
        color: Long,
        balance: Double,
        bankCode: String?,
        accountNumber: String?
    ) {
        viewModelScope.launch {
            val newAccount = Account(
                id = java.util.UUID.randomUUID().toString(),
                name = name,
                type = type,
                icon = icon,
                color = color,
                balance = balance,
                bankCode = bankCode,
                accountNumber = accountNumber,
                linkedSenderIds = bankCode, // Use bank code as default sender ID
                isLinked = bankCode != null
            )
            accountRepository.insertAccount(newAccount)
            // Refresh accounts and select the new one
            val updatedAccounts = accountRepository.getAllAccounts().first()
            _uiState.update { it.copy(
                accounts = updatedAccounts,
                selectedAccount = newAccount
            ) }
        }
    }
    
    /**
     * Create a new linked bank account with proper sender patterns
     */
    fun createLinkedAccount(
        name: String,
        bankSuggestion: BankSuggestion,
        accountNumber: String?,
        type: AccountType,
        balance: Double
    ) {
        viewModelScope.launch {
            val newAccount = accountRepository.createLinkedAccount(
                name = name,
                bankCode = bankSuggestion.code,
                accountNumber = accountNumber,
                type = type,
                senderIds = bankSuggestion.senderPatterns
            )
            
            // Update balance if not zero
            if (balance != 0.0) {
                accountRepository.updateBalance(newAccount.id, balance)
            }
            
            // Refresh accounts and select the new one
            val updatedAccounts = accountRepository.getAllAccounts().first()
            _uiState.update { it.copy(
                accounts = updatedAccounts,
                selectedAccount = newAccount.copy(balance = balance)
            ) }
        }
    }
    
    private fun loadBankSuggestions() {
        viewModelScope.launch {
            // Combine linkable banks from repository and custom templates
            combine(
                availableBankRepository.getLinkableBanks(),
                smsTemplateDao.getAllActiveTemplates()
            ) { availableBanks, templates ->
                val bankSuggestions = availableBanks.map { bank ->
                    BankSuggestion(
                        name = bank.bankName,
                        code = bank.bankCode,
                        senderPatterns = bank.getSenderIdList(),
                        color = bank.color,
                        isCustom = bank.source == com.theblankstate.epmanager.data.model.AvailableBankSource.CUSTOM,
                        templateId = bank.id
                    )
                }
                
                // Also include custom templates that aren't in available_banks yet
                val templateSuggestions = templates
                    .filter { template -> 
                        availableBanks.none { 
                            it.bankName.equals(template.bankName, ignoreCase = true) ||
                            it.bankCode.equals(template.bankName.uppercase().replace(" ", "_").take(10), ignoreCase = true)
                        }
                    }
                    .map { template ->
                        BankSuggestion(
                            name = template.bankName,
                            code = template.bankName.uppercase().replace(" ", "_").take(10),
                            senderPatterns = template.senderIds.split(",").map { it.trim() },
                            color = 0xFF6B7280, // Gray for custom templates
                            isCustom = true,
                            templateId = template.id
                        )
                    }
                
                bankSuggestions + templateSuggestions
            }.collect { suggestions ->
                _uiState.update { 
                    it.copy(bankSuggestions = suggestions) 
                }
            }
        }
    }

    
    fun updateDate(timestamp: Long) {
        _uiState.update { it.copy(selectedDate = timestamp) }
    }
    
    /**
     * Update current location
     */
    fun updateLocation(location: Location?, locationName: String? = null) {
        _uiState.update { 
            it.copy(
                currentLocation = location,
                locationName = locationName
            ) 
        }
    }
    
    /**
     * Toggle location recording
     */
    fun toggleLocationRecording(enabled: Boolean) {
        _uiState.update { it.copy(isLocationEnabled = enabled) }
    }
    
    // ========== Quick Split Functions ==========
    
    fun toggleSplit(enabled: Boolean) {
        _uiState.update { it.copy(isSplitEnabled = enabled) }
    }
    
    fun toggleFriendSelection(friend: Friend) {
        _uiState.update { state ->
            val currentSelected = state.selectedFriends.toMutableList()
            if (currentSelected.any { it.odiserId == friend.odiserId }) {
                currentSelected.removeAll { it.odiserId == friend.odiserId }
            } else {
                currentSelected.add(friend)
            }
            state.copy(selectedFriends = currentSelected)
        }
    }
    
    fun addManualMember(name: String) {
        if (name.isBlank()) return
        _uiState.update { state ->
            val members = state.manualSplitMembers.toMutableList()
            if (!members.contains(name)) {
                members.add(name)
            }
            state.copy(manualSplitMembers = members)
        }
    }
    
    fun removeManualMember(name: String) {
        _uiState.update { state ->
            state.copy(manualSplitMembers = state.manualSplitMembers.filter { it != name })
        }
    }
    
    fun saveTransaction() {
        val state = _uiState.value
        
        // Validate
        val amount = state.amount.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            _uiState.update { it.copy(error = "Please enter a valid amount") }
            return
        }
        
        if (state.selectedCategory == null) {
            _uiState.update { it.copy(error = "Please select a category") }
            return
        }
        
        if (state.selectedAccount == null) {
            _uiState.update { it.copy(error = "Please select an account") }
            return
        }
        
        // Validate split if enabled
        if (state.isSplitEnabled && state.selectedFriends.isEmpty() && state.manualSplitMembers.isEmpty()) {
            _uiState.update { it.copy(error = "Please add at least one person to split with") }
            return
        }
        
        _uiState.update { it.copy(isSaving = true, error = null) }
        
        viewModelScope.launch {
            try {
                val transaction = Transaction(
                    amount = amount,
                    type = state.transactionType,
                    categoryId = state.selectedCategory.id,
                    accountId = state.selectedAccount.id,
                    date = state.selectedDate,
                    note = state.note.takeIf { it.isNotBlank() },
                    // Location data (only if enabled and available)
                    latitude = if (state.isLocationEnabled) state.currentLocation?.latitude else null,
                    longitude = if (state.isLocationEnabled) state.currentLocation?.longitude else null,
                    locationName = if (state.isLocationEnabled) state.locationName else null
                )
                
                // Save the transaction
                transactionRepository.insertTransaction(transaction)
                
                // Update account balance
                // Expense = subtract, Income = add
                val balanceChange = if (state.transactionType == TransactionType.EXPENSE) {
                    -amount
                } else {
                    amount
                }
                accountRepository.updateBalance(state.selectedAccount.id, balanceChange)
                
                // Handle quick split if enabled
                if (state.isSplitEnabled && state.transactionType == TransactionType.EXPENSE) {
                    createQuickSplit(state, amount)
                }
                
                // Handle goal contribution if enabled (only for income)
                if (state.contributeToGoal && state.selectedGoal != null && state.transactionType == TransactionType.INCOME) {
                    savingsGoalRepository.addContribution(state.selectedGoal.id, amount)
                }
                
                // Handle debt payment if enabled
                if (state.isDebtPayment && state.selectedDebt != null) {
                    debtRepository.addPayment(
                        debtId = state.selectedDebt.id,
                        amount = amount,
                        transactionId = transaction.id,
                        note = state.note
                    )
                }
                
                _uiState.update { it.copy(isSaving = false, isSaved = true) }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(isSaving = false, error = e.message ?: "Failed to save transaction") 
                }
            }
        }
    }
    
    private suspend fun createQuickSplit(state: AddTransactionUiState, amount: Double) {
        // Use note as group name, or generate a default name
        val groupName = state.note.takeIf { it.isNotBlank() } 
            ?: "Split ${java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault()).format(java.util.Date())}"
        
        // Create the quick split group
        val group = SplitGroup(
            name = groupName,
            emoji = "💸",
            description = "Quick split expense"
        )
        splitRepository.insertGroup(group)
        
        // Create "You" member
        val youMember = GroupMember(
            groupId = group.id,
            name = "You",
            isCurrentUser = true
        )
        splitRepository.insertMember(youMember)
        
        // Create members for selected friends
        val friendMembers = state.selectedFriends.map { friend ->
            GroupMember(
                groupId = group.id,
                name = friend.displayName ?: friend.email,
                email = friend.email,
                linkedUserId = friend.odiserId
            )
        }
        friendMembers.forEach { splitRepository.insertMember(it) }
        
        // Create members for manual entries
        val manualMembers = state.manualSplitMembers.map { name ->
            GroupMember(
                groupId = group.id,
                name = name
            )
        }
        manualMembers.forEach { splitRepository.insertMember(it) }
        
        // All members including you
        val allMembers = listOf(youMember) + friendMembers + manualMembers
        val totalMembers = allMembers.size
        val sharePerPerson = amount / totalMembers
        
        // Create the split expense (you paid)
        val expense = SplitExpense(
            groupId = group.id,
            description = groupName,
            totalAmount = amount,
            paidById = youMember.id,
            splitType = SplitType.EQUAL
        )
        splitRepository.insertExpense(expense)
        
        // Create shares for each member
        val shares = allMembers.map { member ->
            ExpenseShare(
                expenseId = expense.id,
                memberId = member.id,
                shareAmount = sharePerPerson
            )
        }
        splitRepository.insertShares(shares)
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    // Goal contribution functions
    fun toggleGoalContribution(enabled: Boolean) {
        _uiState.update { state ->
            val goalToSelect = if (enabled && state.selectedGoal == null) 
                state.savingsGoals.firstOrNull() 
            else 
                state.selectedGoal
            
            // Find the goals category
            val goalsCategory = state.categories.find { it.id == "goals" }
            
            state.copy(
                contributeToGoal = enabled,
                selectedGoal = goalToSelect,
                // Auto-select the Goals category when enabling
                selectedCategory = if (enabled && goalsCategory != null) goalsCategory else state.selectedCategory,
                // Auto-set note to "Toward [Goal Name]" when enabling
                note = if (enabled && goalToSelect != null) "Toward ${goalToSelect.name}" else state.note
            )
        }
    }
    
    fun selectGoal(goal: SavingsGoal) {
        _uiState.update { state ->
            state.copy(
                selectedGoal = goal,
                // Update note when goal is changed
                note = if (state.contributeToGoal) "Toward ${goal.name}" else state.note
            )
        }
    }
    
    // Debt payment functions
    fun toggleDebtPayment(enabled: Boolean) {
        _uiState.update { state ->
            val debtsOrCredits = if (state.transactionType == TransactionType.EXPENSE) 
                state.activeDebts 
            else 
                state.activeCredits
            
            val debtToSelect = if (enabled && state.selectedDebt == null) 
                debtsOrCredits.firstOrNull() 
            else 
                state.selectedDebt
            
            // Find the appropriate category
            val categoryId = if (state.transactionType == TransactionType.EXPENSE) "debt_payment" else "credit_received"
            val debtCategory = state.categories.find { it.id == categoryId }
            
            state.copy(
                isDebtPayment = enabled,
                selectedDebt = if (enabled) debtToSelect else null,
                // Auto-select the debt/credit category
                selectedCategory = if (enabled && debtCategory != null) debtCategory else state.selectedCategory,
                // Auto-set note
                note = if (enabled && debtToSelect != null) {
                    if (state.transactionType == TransactionType.EXPENSE) 
                        "Payment to ${debtToSelect.personName}" 
                    else 
                        "Received from ${debtToSelect.personName}"
                } else state.note
            )
        }
    }
    
    fun selectDebt(debt: Debt) {
        _uiState.update { state ->
            val notePrefix = if (state.transactionType == TransactionType.EXPENSE) "Payment to" else "Received from"
            state.copy(
                selectedDebt = debt,
                note = if (state.isDebtPayment) "$notePrefix ${debt.personName}" else state.note
            )
        }
    }
}

