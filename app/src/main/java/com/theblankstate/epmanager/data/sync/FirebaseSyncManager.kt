package com.theblankstate.epmanager.data.sync

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.theblankstate.epmanager.data.model.*
import com.theblankstate.epmanager.data.repository.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages synchronization between local Room database and Firebase Realtime Database.
 * 
 * Sync Strategy:
 * - Settings data (budgets, recurring, goals, accounts, categories, splits) auto-syncs
 * - Transaction history requires manual backup/restore (user decision)
 * - Offline-first: App always uses local data, sync runs in background
 */
@Singleton
class FirebaseSyncManager @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val budgetRepository: BudgetRepository,
    private val recurringExpenseRepository: RecurringExpenseRepository,
    private val savingsGoalRepository: SavingsGoalRepository,
    private val splitRepository: SplitRepository
) {
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    
    private val userId: String?
        get() = auth.currentUser?.uid
    
    val isLoggedIn: Boolean
        get() = userId != null
    
    private fun getUserRef(): DatabaseReference? {
        return userId?.let { database.reference.child("users").child(it) }
    }
    
    // ========== AUTO-SYNC (Settings Data) ==========
    
    /**
     * Backup all settings data to Firebase (excludes transactions)
     * Called automatically in background
     */
    suspend fun backupSettingsToCloud(): Result<Unit> {
        val userRef = getUserRef() ?: return Result.failure(Exception("Not logged in"))
        
        return try {
            // Backup budgets
            val budgets = budgetRepository.getAllBudgets().first()
            val budgetsMap = budgets.associate { it.id to budgetToMap(it) }
            userRef.child("budgets").setValue(budgetsMap).await()
            
            // Backup recurring expenses
            val recurring = recurringExpenseRepository.getAllRecurringExpenses().first()
            val recurringMap = recurring.associate { it.id to recurringToMap(it) }
            userRef.child("recurring_expenses").setValue(recurringMap).await()
            
            // Backup savings goals
            val goals = savingsGoalRepository.getAllGoals().first()
            val goalsMap = goals.associate { it.id to goalToMap(it) }
            userRef.child("savings_goals").setValue(goalsMap).await()
            
            // Backup custom categories (not default ones)
            val categories = categoryRepository.getAllCategories().first()
                .filter { !it.isDefault }
            val categoriesMap = categories.associate { it.id to categoryToMap(it) }
            userRef.child("categories").setValue(categoriesMap).await()
            
            // Backup custom accounts (not default ones)
            val accounts = accountRepository.getAllAccounts().first()
                .filter { !it.isDefault }
            val accountsMap = accounts.associate { it.id to accountToMap(it) }
            userRef.child("accounts").setValue(accountsMap).await()
            
            // Backup split groups, members, expenses, shares, settlements
            backupSplitData(userRef)
            
            // Update last sync time
            userRef.child("lastSettingsSync").setValue(System.currentTimeMillis()).await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun backupSplitData(userRef: DatabaseReference) {
        val groups = splitRepository.getAllGroups().first()
        val groupsMap = groups.associate { it.id to splitGroupToMap(it) }
        userRef.child("split_groups").setValue(groupsMap).await()
        
        // Backup members for each group
        val allMembers = mutableMapOf<String, Map<String, Any?>>()
        val allExpenses = mutableMapOf<String, Map<String, Any?>>()
        val allShares = mutableMapOf<String, Map<String, Any?>>()
        val allSettlements = mutableMapOf<String, Map<String, Any?>>()
        
        groups.forEach { group ->
            val members = splitRepository.getMembersByGroup(group.id).first()
            members.forEach { member ->
                allMembers[member.id] = memberToMap(member)
            }
            
            val expenses = splitRepository.getExpensesByGroup(group.id).first()
            expenses.forEach { expense ->
                allExpenses[expense.id] = splitExpenseToMap(expense)
                
                val shares = splitRepository.getSharesByExpense(expense.id)
                shares.forEach { share ->
                    allShares[share.id] = shareToMap(share)
                }
            }
            
            val settlements = splitRepository.getSettlementsByGroup(group.id).first()
            settlements.forEach { settlement ->
                allSettlements[settlement.id] = settlementToMap(settlement)
            }
        }
        
        userRef.child("group_members").setValue(allMembers).await()
        userRef.child("split_expenses").setValue(allExpenses).await()
        userRef.child("expense_shares").setValue(allShares).await()
        userRef.child("settlements").setValue(allSettlements).await()
    }
    
    /**
     * Restore settings data from Firebase (excludes transactions)
     * Called when user reinstalls app and chooses to restore
     */
    suspend fun restoreSettingsFromCloud(): Result<Unit> {
        val userRef = getUserRef() ?: return Result.failure(Exception("Not logged in"))
        
        return try {
            // Restore budgets
            val budgetsSnapshot = userRef.child("budgets").get().await()
            budgetsSnapshot.children.forEach { snapshot ->
                val map = snapshot.value as? Map<*, *>
                if (map != null) {
                    val budget = mapToBudget(map)
                    budgetRepository.insertBudget(budget)
                }
            }
            
            // Restore recurring expenses
            val recurringSnapshot = userRef.child("recurring_expenses").get().await()
            recurringSnapshot.children.forEach { snapshot ->
                val map = snapshot.value as? Map<*, *>
                if (map != null) {
                    val recurring = mapToRecurring(map)
                    recurringExpenseRepository.insertRecurringExpense(recurring)
                }
            }
            
            // Restore savings goals
            val goalsSnapshot = userRef.child("savings_goals").get().await()
            goalsSnapshot.children.forEach { snapshot ->
                val map = snapshot.value as? Map<*, *>
                if (map != null) {
                    val goal = mapToGoal(map)
                    savingsGoalRepository.createGoal(goal)
                }
            }
            
            // Restore custom categories
            val categoriesSnapshot = userRef.child("categories").get().await()
            categoriesSnapshot.children.forEach { snapshot ->
                val map = snapshot.value as? Map<*, *>
                if (map != null) {
                    val category = mapToCategory(map)
                    categoryRepository.insertCategory(category)
                }
            }
            
            // Restore custom accounts
            val accountsSnapshot = userRef.child("accounts").get().await()
            accountsSnapshot.children.forEach { snapshot ->
                val map = snapshot.value as? Map<*, *>
                if (map != null) {
                    val account = mapToAccount(map)
                    accountRepository.insertAccount(account)
                }
            }
            
            // Restore split data
            restoreSplitData(userRef)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun restoreSplitData(userRef: DatabaseReference) {
        // Restore split groups
        val groupsSnapshot = userRef.child("split_groups").get().await()
        groupsSnapshot.children.forEach { snapshot ->
            val map = snapshot.value as? Map<*, *>
            if (map != null) {
                val group = mapToSplitGroup(map)
                splitRepository.insertGroup(group)
            }
        }
        
        // Restore group members
        val membersSnapshot = userRef.child("group_members").get().await()
        membersSnapshot.children.forEach { snapshot ->
            val map = snapshot.value as? Map<*, *>
            if (map != null) {
                val member = mapToMember(map)
                splitRepository.insertMember(member)
            }
        }
        
        // Restore split expenses
        val expensesSnapshot = userRef.child("split_expenses").get().await()
        expensesSnapshot.children.forEach { snapshot ->
            val map = snapshot.value as? Map<*, *>
            if (map != null) {
                val expense = mapToSplitExpense(map)
                splitRepository.insertExpense(expense)
            }
        }
        
        // Restore expense shares
        val sharesSnapshot = userRef.child("expense_shares").get().await()
        val shares = mutableListOf<ExpenseShare>()
        sharesSnapshot.children.forEach { snapshot ->
            val map = snapshot.value as? Map<*, *>
            if (map != null) {
                shares.add(mapToShare(map))
            }
        }
        if (shares.isNotEmpty()) {
            splitRepository.insertShares(shares)
        }
        
        // Restore settlements
        val settlementsSnapshot = userRef.child("settlements").get().await()
        settlementsSnapshot.children.forEach { snapshot ->
            val map = snapshot.value as? Map<*, *>
            if (map != null) {
                val settlement = mapToSettlement(map)
                splitRepository.insertSettlement(settlement)
            }
        }
    }
    
    // ========== MANUAL SYNC (Transactions) ==========
    
    /**
     * Manual backup of all transactions (user-initiated)
     */
    suspend fun backupTransactionsToCloud(): Result<Unit> {
        val userRef = getUserRef() ?: return Result.failure(Exception("Not logged in"))
        
        return try {
            val transactions = transactionRepository.getAllTransactions().first()
            val transactionsMap = transactions.associate { it.id to transactionToMap(it) }
            userRef.child("transactions").setValue(transactionsMap).await()
            
            // Mark all as synced
            transactions.forEach { 
                transactionRepository.markAsSynced(it.id)
            }
            
            userRef.child("lastTransactionBackup").setValue(System.currentTimeMillis()).await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Manual restore of transactions (user-initiated)
     */
    suspend fun restoreTransactionsFromCloud(): Result<Int> {
        val userRef = getUserRef() ?: return Result.failure(Exception("Not logged in"))
        
        return try {
            var count = 0
            val transactionsSnapshot = userRef.child("transactions").get().await()
            transactionsSnapshot.children.forEach { snapshot ->
                val map = snapshot.value as? Map<*, *>
                if (map != null) {
                    val transaction = mapToTransaction(map)
                    transactionRepository.insertTransaction(transaction)
                    count++
                }
            }
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get last sync timestamps
     */
    suspend fun getLastSyncTimes(): Pair<Long?, Long?> {
        val userRef = getUserRef() ?: return null to null
        
        return try {
            val settingsSync = userRef.child("lastSettingsSync").get().await()
                .getValue(Long::class.java)
            val transactionSync = userRef.child("lastTransactionBackup").get().await()
                .getValue(Long::class.java)
            settingsSync to transactionSync
        } catch (e: Exception) {
            null to null
        }
    }
    
    // ========== CONVERSION FUNCTIONS ==========
    
    // Budget
    private fun budgetToMap(budget: Budget): Map<String, Any?> = mapOf(
        "id" to budget.id,
        "categoryId" to budget.categoryId,
        "amount" to budget.amount,
        "period" to budget.period.name,
        "alertThreshold" to budget.alertThreshold,
        "isActive" to budget.isActive,
        "createdAt" to budget.createdAt
    )
    
    private fun mapToBudget(map: Map<*, *>): Budget = Budget(
        id = map["id"] as? String ?: "",
        categoryId = map["categoryId"] as? String ?: "",
        amount = (map["amount"] as? Number)?.toDouble() ?: 0.0,
        period = try { 
            BudgetPeriod.valueOf(map["period"] as? String ?: "MONTHLY") 
        } catch (e: Exception) { BudgetPeriod.MONTHLY },
        alertThreshold = (map["alertThreshold"] as? Number)?.toFloat() ?: 0.8f,
        isActive = map["isActive"] as? Boolean ?: true,
        createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
    )
    
    // RecurringExpense
    private fun recurringToMap(recurring: RecurringExpense): Map<String, Any?> = mapOf(
        "id" to recurring.id,
        "name" to recurring.name,
        "amount" to recurring.amount,
        "type" to recurring.type.name,
        "categoryId" to recurring.categoryId,
        "accountId" to recurring.accountId,
        "frequency" to recurring.frequency.name,
        "startDate" to recurring.startDate,
        "endDate" to recurring.endDate,
        "nextDueDate" to recurring.nextDueDate,
        "lastProcessedDate" to recurring.lastProcessedDate,
        "note" to recurring.note,
        "isActive" to recurring.isActive,
        "autoAdd" to recurring.autoAdd,
        "reminderDaysBefore" to recurring.reminderDaysBefore,
        "createdAt" to recurring.createdAt
    )
    
    private fun mapToRecurring(map: Map<*, *>): RecurringExpense = RecurringExpense(
        id = map["id"] as? String ?: "",
        name = map["name"] as? String ?: "",
        amount = (map["amount"] as? Number)?.toDouble() ?: 0.0,
        type = try { 
            TransactionType.valueOf(map["type"] as? String ?: "EXPENSE") 
        } catch (e: Exception) { TransactionType.EXPENSE },
        categoryId = map["categoryId"] as? String,
        accountId = map["accountId"] as? String,
        frequency = try { 
            RecurringFrequency.valueOf(map["frequency"] as? String ?: "MONTHLY") 
        } catch (e: Exception) { RecurringFrequency.MONTHLY },
        startDate = (map["startDate"] as? Number)?.toLong() ?: System.currentTimeMillis(),
        endDate = (map["endDate"] as? Number)?.toLong(),
        nextDueDate = (map["nextDueDate"] as? Number)?.toLong() ?: System.currentTimeMillis(),
        lastProcessedDate = (map["lastProcessedDate"] as? Number)?.toLong(),
        note = map["note"] as? String,
        isActive = map["isActive"] as? Boolean ?: true,
        autoAdd = map["autoAdd"] as? Boolean ?: false,
        reminderDaysBefore = (map["reminderDaysBefore"] as? Number)?.toInt() ?: 1,
        createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
    )
    
    // SavingsGoal
    private fun goalToMap(goal: SavingsGoal): Map<String, Any?> = mapOf(
        "id" to goal.id,
        "name" to goal.name,
        "targetAmount" to goal.targetAmount,
        "savedAmount" to goal.savedAmount,
        "icon" to goal.icon,
        "color" to goal.color,
        "targetDate" to goal.targetDate,
        "isCompleted" to goal.isCompleted,
        "createdAt" to goal.createdAt,
        "updatedAt" to goal.updatedAt
    )
    
    private fun mapToGoal(map: Map<*, *>): SavingsGoal = SavingsGoal(
        id = map["id"] as? String ?: "",
        name = map["name"] as? String ?: "",
        targetAmount = (map["targetAmount"] as? Number)?.toDouble() ?: 0.0,
        savedAmount = (map["savedAmount"] as? Number)?.toDouble() ?: 0.0,
        icon = map["icon"] as? String ?: "Savings",
        color = (map["color"] as? Number)?.toLong() ?: 0xFF22C55E,
        targetDate = (map["targetDate"] as? Number)?.toLong(),
        isCompleted = map["isCompleted"] as? Boolean ?: false,
        createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
        updatedAt = (map["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
    )
    
    // Split entities
    private fun splitGroupToMap(group: SplitGroup): Map<String, Any?> = mapOf(
        "id" to group.id,
        "name" to group.name,
        "description" to group.description,
        "emoji" to group.emoji,
        "createdAt" to group.createdAt
    )
    
    private fun mapToSplitGroup(map: Map<*, *>): SplitGroup = SplitGroup(
        id = map["id"] as? String ?: "",
        name = map["name"] as? String ?: "",
        description = map["description"] as? String,
        emoji = map["emoji"] as? String ?: "👥",
        createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
    )
    
    private fun memberToMap(member: GroupMember): Map<String, Any?> = mapOf(
        "id" to member.id,
        "groupId" to member.groupId,
        "name" to member.name,
        "phone" to member.phone,
        "email" to member.email,
        "isCurrentUser" to member.isCurrentUser,
        "createdAt" to member.createdAt
    )
    
    private fun mapToMember(map: Map<*, *>): GroupMember = GroupMember(
        id = map["id"] as? String ?: "",
        groupId = map["groupId"] as? String ?: "",
        name = map["name"] as? String ?: "",
        phone = map["phone"] as? String,
        email = map["email"] as? String,
        isCurrentUser = map["isCurrentUser"] as? Boolean ?: false,
        createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
    )
    
    private fun splitExpenseToMap(expense: SplitExpense): Map<String, Any?> = mapOf(
        "id" to expense.id,
        "groupId" to expense.groupId,
        "description" to expense.description,
        "totalAmount" to expense.totalAmount,
        "paidById" to expense.paidById,
        "splitType" to expense.splitType.name,
        "date" to expense.date,
        "createdAt" to expense.createdAt
    )
    
    private fun mapToSplitExpense(map: Map<*, *>): SplitExpense = SplitExpense(
        id = map["id"] as? String ?: "",
        groupId = map["groupId"] as? String ?: "",
        description = map["description"] as? String ?: "",
        totalAmount = (map["totalAmount"] as? Number)?.toDouble() ?: 0.0,
        paidById = map["paidById"] as? String,
        splitType = try { 
            SplitType.valueOf(map["splitType"] as? String ?: "EQUAL") 
        } catch (e: Exception) { SplitType.EQUAL },
        date = (map["date"] as? Number)?.toLong() ?: System.currentTimeMillis(),
        createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
    )
    
    private fun shareToMap(share: ExpenseShare): Map<String, Any?> = mapOf(
        "id" to share.id,
        "expenseId" to share.expenseId,
        "memberId" to share.memberId,
        "shareAmount" to share.shareAmount,
        "sharePercentage" to share.sharePercentage,
        "shareCount" to share.shareCount
    )
    
    private fun mapToShare(map: Map<*, *>): ExpenseShare = ExpenseShare(
        id = map["id"] as? String ?: "",
        expenseId = map["expenseId"] as? String ?: "",
        memberId = map["memberId"] as? String ?: "",
        shareAmount = (map["shareAmount"] as? Number)?.toDouble() ?: 0.0,
        sharePercentage = (map["sharePercentage"] as? Number)?.toDouble(),
        shareCount = (map["shareCount"] as? Number)?.toInt()
    )
    
    private fun settlementToMap(settlement: Settlement): Map<String, Any?> = mapOf(
        "id" to settlement.id,
        "groupId" to settlement.groupId,
        "fromMemberId" to settlement.fromMemberId,
        "toMemberId" to settlement.toMemberId,
        "amount" to settlement.amount,
        "note" to settlement.note,
        "date" to settlement.date
    )
    
    private fun mapToSettlement(map: Map<*, *>): Settlement = Settlement(
        id = map["id"] as? String ?: "",
        groupId = map["groupId"] as? String ?: "",
        fromMemberId = map["fromMemberId"] as? String ?: "",
        toMemberId = map["toMemberId"] as? String ?: "",
        amount = (map["amount"] as? Number)?.toDouble() ?: 0.0,
        note = map["note"] as? String,
        date = (map["date"] as? Number)?.toLong() ?: System.currentTimeMillis()
    )
    
    // Transaction (for manual backup)
    private fun transactionToMap(transaction: Transaction): Map<String, Any?> = mapOf(
        "id" to transaction.id,
        "amount" to transaction.amount,
        "type" to transaction.type.name,
        "categoryId" to transaction.categoryId,
        "accountId" to transaction.accountId,
        "date" to transaction.date,
        "note" to transaction.note,
        "isRecurring" to transaction.isRecurring,
        "recurringId" to transaction.recurringId,
        "latitude" to transaction.latitude,
        "longitude" to transaction.longitude,
        "locationName" to transaction.locationName,
        "createdAt" to transaction.createdAt,
        "updatedAt" to transaction.updatedAt
    )
    
    private fun mapToTransaction(map: Map<*, *>): Transaction = Transaction(
        id = map["id"] as? String ?: "",
        amount = (map["amount"] as? Number)?.toDouble() ?: 0.0,
        type = try { 
            TransactionType.valueOf(map["type"] as? String ?: "EXPENSE") 
        } catch (e: Exception) { TransactionType.EXPENSE },
        categoryId = map["categoryId"] as? String,
        accountId = map["accountId"] as? String,
        date = (map["date"] as? Number)?.toLong() ?: System.currentTimeMillis(),
        note = map["note"] as? String,
        isRecurring = map["isRecurring"] as? Boolean ?: false,
        recurringId = map["recurringId"] as? String,
        latitude = (map["latitude"] as? Number)?.toDouble(),
        longitude = (map["longitude"] as? Number)?.toDouble(),
        locationName = map["locationName"] as? String,
        createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
        updatedAt = (map["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
        isSynced = true
    )
    
    // Category (existing)
    private fun categoryToMap(category: Category): Map<String, Any?> = mapOf(
        "id" to category.id,
        "name" to category.name,
        "icon" to category.icon,
        "color" to category.color,
        "type" to category.type.name,
        "isDefault" to category.isDefault,
        "createdAt" to category.createdAt
    )
    
    private fun mapToCategory(map: Map<*, *>): Category = Category(
        id = map["id"] as? String ?: "",
        name = map["name"] as? String ?: "",
        icon = map["icon"] as? String ?: "",
        color = (map["color"] as? Number)?.toLong() ?: 0xFF9CA3AF,
        type = try { 
            CategoryType.valueOf(map["type"] as? String ?: "EXPENSE") 
        } catch (e: Exception) { CategoryType.EXPENSE },
        isDefault = map["isDefault"] as? Boolean ?: false,
        createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
    )
    
    // Account (existing)
    private fun accountToMap(account: Account): Map<String, Any?> = mapOf(
        "id" to account.id,
        "name" to account.name,
        "type" to account.type.name,
        "icon" to account.icon,
        "color" to account.color,
        "balance" to account.balance,
        "isDefault" to account.isDefault,
        "createdAt" to account.createdAt
    )
    
    private fun mapToAccount(map: Map<*, *>): Account = Account(
        id = map["id"] as? String ?: "",
        name = map["name"] as? String ?: "",
        type = try { 
            AccountType.valueOf(map["type"] as? String ?: "CASH") 
        } catch (e: Exception) { AccountType.CASH },
        icon = map["icon"] as? String ?: "",
        color = (map["color"] as? Number)?.toLong() ?: 0xFF22C55E,
        balance = (map["balance"] as? Number)?.toDouble() ?: 0.0,
        isDefault = map["isDefault"] as? Boolean ?: false,
        createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
    )
}
