package com.theblankstate.epmanager.data.local.dao

import androidx.room.*
import com.theblankstate.epmanager.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SplitDao {
    // Groups
    @Query("SELECT * FROM split_groups ORDER BY createdAt DESC")
    fun getAllGroups(): Flow<List<SplitGroup>>
    
    @Query("SELECT * FROM split_groups WHERE id = :id")
    suspend fun getGroupById(id: String): SplitGroup?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: SplitGroup)
    
    @Update
    suspend fun updateGroup(group: SplitGroup)
    
    @Delete
    suspend fun deleteGroup(group: SplitGroup)
    
    // Group Members
    @Query("SELECT * FROM group_members WHERE groupId = :groupId ORDER BY isCurrentUser DESC, name ASC")
    fun getMembersByGroup(groupId: String): Flow<List<GroupMember>>
    
    @Query("SELECT * FROM group_members WHERE groupId = :groupId")
    suspend fun getMembersByGroupSync(groupId: String): List<GroupMember>
    
    @Query("SELECT * FROM group_members WHERE id = :id")
    suspend fun getMemberById(id: String): GroupMember?
    
    @Query("SELECT * FROM group_members WHERE groupId = :groupId AND isCurrentUser = 1 LIMIT 1")
    suspend fun getCurrentUserMember(groupId: String): GroupMember?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: GroupMember)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembers(members: List<GroupMember>)
    
    @Update
    suspend fun updateMember(member: GroupMember)
    
    @Delete
    suspend fun deleteMember(member: GroupMember)
    
    // Split Expenses
    @Query("SELECT * FROM split_expenses WHERE groupId = :groupId ORDER BY date DESC")
    fun getExpensesByGroup(groupId: String): Flow<List<SplitExpense>>
    
    @Query("SELECT * FROM split_expenses WHERE id = :id")
    suspend fun getExpenseById(id: String): SplitExpense?
    
    @Query("SELECT SUM(totalAmount) FROM split_expenses WHERE groupId = :groupId")
    suspend fun getTotalExpensesByGroup(groupId: String): Double?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: SplitExpense)
    
    @Update
    suspend fun updateExpense(expense: SplitExpense)
    
    @Delete
    suspend fun deleteExpense(expense: SplitExpense)
    
    // Expense Shares
    @Query("SELECT * FROM expense_shares WHERE expenseId = :expenseId")
    suspend fun getSharesByExpense(expenseId: String): List<ExpenseShare>
    
    @Query("SELECT * FROM expense_shares WHERE memberId = :memberId")
    suspend fun getSharesByMember(memberId: String): List<ExpenseShare>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShare(share: ExpenseShare)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShares(shares: List<ExpenseShare>)
    
    @Delete
    suspend fun deleteShare(share: ExpenseShare)
    
    @Query("DELETE FROM expense_shares WHERE expenseId = :expenseId")
    suspend fun deleteSharesByExpense(expenseId: String)
    
    // Settlements
    @Query("SELECT * FROM settlements WHERE groupId = :groupId ORDER BY date DESC")
    fun getSettlementsByGroup(groupId: String): Flow<List<Settlement>>
    
    @Query("SELECT SUM(amount) FROM settlements WHERE groupId = :groupId AND fromMemberId = :memberId")
    suspend fun getTotalPaidByMember(groupId: String, memberId: String): Double?
    
    @Query("SELECT SUM(amount) FROM settlements WHERE groupId = :groupId AND toMemberId = :memberId")
    suspend fun getTotalReceivedByMember(groupId: String, memberId: String): Double?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettlement(settlement: Settlement)
    
    @Delete
    suspend fun deleteSettlement(settlement: Settlement)
    
    @Query("DELETE FROM split_groups")
    suspend fun deleteAllGroups()
}
