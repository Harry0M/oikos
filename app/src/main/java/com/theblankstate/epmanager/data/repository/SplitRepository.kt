package com.theblankstate.epmanager.data.repository

import com.theblankstate.epmanager.data.local.dao.SplitDao
import com.theblankstate.epmanager.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SplitRepository @Inject constructor(
    private val splitDao: SplitDao
) {
    // Groups
    fun getAllGroups(): Flow<List<SplitGroup>> = splitDao.getAllGroups()
    
    suspend fun getGroupById(id: String): SplitGroup? = splitDao.getGroupById(id)
    
    suspend fun insertGroup(group: SplitGroup) = splitDao.insertGroup(group)
    
    suspend fun updateGroup(group: SplitGroup) = splitDao.updateGroup(group)
    
    suspend fun deleteGroup(group: SplitGroup) = splitDao.deleteGroup(group)
    
    // Members
    fun getMembersByGroup(groupId: String): Flow<List<GroupMember>> = 
        splitDao.getMembersByGroup(groupId)
    
    suspend fun getMembersByGroupSync(groupId: String): List<GroupMember> =
        splitDao.getMembersByGroupSync(groupId)
    
    suspend fun getCurrentUserMember(groupId: String): GroupMember? =
        splitDao.getCurrentUserMember(groupId)
    
    suspend fun insertMember(member: GroupMember) = splitDao.insertMember(member)
    
    suspend fun insertMembers(members: List<GroupMember>) = splitDao.insertMembers(members)
    
    suspend fun deleteMember(member: GroupMember) = splitDao.deleteMember(member)
    
    // Expenses
    fun getExpensesByGroup(groupId: String): Flow<List<SplitExpense>> = 
        splitDao.getExpensesByGroup(groupId)
    
    suspend fun getTotalExpensesByGroup(groupId: String): Double =
        splitDao.getTotalExpensesByGroup(groupId) ?: 0.0
    
    suspend fun insertExpense(expense: SplitExpense) = splitDao.insertExpense(expense)
    
    suspend fun deleteExpense(expense: SplitExpense) {
        splitDao.deleteSharesByExpense(expense.id)
        splitDao.deleteExpense(expense)
    }
    
    // Shares
    suspend fun getSharesByExpense(expenseId: String): List<ExpenseShare> =
        splitDao.getSharesByExpense(expenseId)
    
    suspend fun insertShares(shares: List<ExpenseShare>) = splitDao.insertShares(shares)
    
    // Settlements
    fun getSettlementsByGroup(groupId: String): Flow<List<Settlement>> = 
        splitDao.getSettlementsByGroup(groupId)
    
    suspend fun insertSettlement(settlement: Settlement) = splitDao.insertSettlement(settlement)
    
    suspend fun deleteSettlement(settlement: Settlement) = splitDao.deleteSettlement(settlement)
    
    /**
     * Add an expense and split it among members
     */
    suspend fun addSplitExpense(
        groupId: String,
        description: String,
        totalAmount: Double,
        paidById: String,
        splitType: SplitType = SplitType.EQUAL,
        memberShares: Map<String, Double>? = null // For non-equal splits
    ) {
        val expense = SplitExpense(
            groupId = groupId,
            description = description,
            totalAmount = totalAmount,
            paidById = paidById,
            splitType = splitType
        )
        splitDao.insertExpense(expense)
        
        val members = splitDao.getMembersByGroupSync(groupId)
        
        val shares = when (splitType) {
            SplitType.EQUAL -> {
                val shareAmount = totalAmount / members.size
                members.map { member ->
                    ExpenseShare(
                        expenseId = expense.id,
                        memberId = member.id,
                        shareAmount = shareAmount
                    )
                }
            }
            SplitType.EXACT -> {
                memberShares?.map { (memberId, amount) ->
                    ExpenseShare(
                        expenseId = expense.id,
                        memberId = memberId,
                        shareAmount = amount
                    )
                } ?: emptyList()
            }
            else -> {
                // Default to equal for now
                val shareAmount = totalAmount / members.size
                members.map { member ->
                    ExpenseShare(
                        expenseId = expense.id,
                        memberId = member.id,
                        shareAmount = shareAmount
                    )
                }
            }
        }
        
        splitDao.insertShares(shares)
    }
    
    /**
     * Calculate balance for each member in a group
     * Positive = they owe you, Negative = you owe them
     */
    suspend fun calculateBalances(groupId: String): List<MemberBalance> {
        val members = splitDao.getMembersByGroupSync(groupId)
        val currentUser = members.find { it.isCurrentUser } ?: return emptyList()
        val expenses = splitDao.getExpensesByGroup(groupId).first()
        val settlements = splitDao.getSettlementsByGroup(groupId).first()
        
        // Track net balance between current user and each other member
        // Positive = they owe current user, Negative = current user owes them
        val balanceWithMember = mutableMapOf<String, Double>()
        
        members.filter { !it.isCurrentUser }.forEach { member ->
            balanceWithMember[member.id] = 0.0
        }
        
        // Calculate from expenses
        expenses.forEach { expense ->
            val shares = splitDao.getSharesByExpense(expense.id)
            val myShare = shares.find { it.memberId == currentUser.id }?.shareAmount ?: 0.0
            
            if (expense.paidById == currentUser.id) {
                // I paid - others owe me their share
                shares.filter { it.memberId != currentUser.id }.forEach { share ->
                    balanceWithMember[share.memberId] = 
                        (balanceWithMember[share.memberId] ?: 0.0) + share.shareAmount
                }
            } else {
                // Someone else paid - I owe them my share
                expense.paidById?.let { payerId ->
                    if (payerId != currentUser.id) {
                        balanceWithMember[payerId] = 
                            (balanceWithMember[payerId] ?: 0.0) - myShare
                    }
                }
            }
        }
        
        // Apply settlements
        settlements.forEach { settlement ->
            if (settlement.fromMemberId == currentUser.id) {
                // I paid someone - reduce what I owe them (or increase what they owe me)
                balanceWithMember[settlement.toMemberId] = 
                    (balanceWithMember[settlement.toMemberId] ?: 0.0) + settlement.amount
            } else if (settlement.toMemberId == currentUser.id) {
                // Someone paid me - reduce what they owe me
                balanceWithMember[settlement.fromMemberId] = 
                    (balanceWithMember[settlement.fromMemberId] ?: 0.0) - settlement.amount
            }
        }
        
        // Convert to MemberBalance list, filtering out zero balances
        return members.filter { !it.isCurrentUser }
            .map { member ->
                MemberBalance(
                    member = member,
                    balance = balanceWithMember[member.id] ?: 0.0
                )
            }
            .filter { it.balance != 0.0 }
    }
    
    /**
     * Get groups with their summary info
     */
    suspend fun getGroupsWithSummary(): List<SplitGroupWithMembers> {
        val groups = splitDao.getAllGroups().first()
        return groups.map { group ->
            val members = splitDao.getMembersByGroupSync(group.id)
            val totalExpenses = splitDao.getTotalExpensesByGroup(group.id) ?: 0.0
            val balances = calculateBalances(group.id)
            val yourBalance = balances.sumOf { it.balance }
            
            SplitGroupWithMembers(
                group = group,
                members = members,
                totalExpenses = totalExpenses,
                yourBalance = yourBalance
            )
        }
    }
    
    /**
     * Get current user member from a list (sync helper)
     */
    fun getCurrentUserMemberSync(members: List<GroupMember>): GroupMember? {
        return members.find { it.isCurrentUser }
    }
}
