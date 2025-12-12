package com.theblankstate.epmanager.ui.split

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.theblankstate.epmanager.data.model.*
import com.theblankstate.epmanager.data.repository.SplitRepository
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
    val isLoading: Boolean = true,
    val showCreateGroupDialog: Boolean = false,
    val showAddExpenseDialog: Boolean = false,
    val showAddMemberDialog: Boolean = false,
    val showSettleDialog: Boolean = false,
    val settleWithMember: GroupMember? = null
)

@HiltViewModel
class SplitViewModel @Inject constructor(
    private val splitRepository: SplitRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SplitUiState())
    val uiState: StateFlow<SplitUiState> = _uiState.asStateFlow()
    
    init {
        loadGroups()
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
    
    // Dialog visibility
    fun showCreateGroupDialog() {
        _uiState.update { it.copy(showCreateGroupDialog = true) }
    }
    
    fun hideCreateGroupDialog() {
        _uiState.update { it.copy(showCreateGroupDialog = false) }
    }
    
    fun showAddExpenseDialog() {
        _uiState.update { it.copy(showAddExpenseDialog = true) }
    }
    
    fun hideAddExpenseDialog() {
        _uiState.update { it.copy(showAddExpenseDialog = false) }
    }
    
    fun showAddMemberDialog() {
        _uiState.update { it.copy(showAddMemberDialog = true) }
    }
    
    fun hideAddMemberDialog() {
        _uiState.update { it.copy(showAddMemberDialog = false) }
    }
    
    fun showSettleDialog(member: GroupMember) {
        _uiState.update { it.copy(showSettleDialog = true, settleWithMember = member) }
    }
    
    fun hideSettleDialog() {
        _uiState.update { it.copy(showSettleDialog = false, settleWithMember = null) }
    }
    
    // Actions
    fun createGroup(name: String, emoji: String, memberNames: List<String>) {
        viewModelScope.launch {
            val group = SplitGroup(name = name, emoji = emoji)
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
            
            hideCreateGroupDialog()
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
            hideAddMemberDialog()
        }
    }
    
    fun addExpense(description: String, amount: Double, paidById: String) {
        val groupId = _uiState.value.selectedGroup?.id ?: return
        
        viewModelScope.launch {
            splitRepository.addSplitExpense(
                groupId = groupId,
                description = description,
                totalAmount = amount,
                paidById = paidById
            )
            hideAddExpenseDialog()
            
            // Refresh balances
            val balances = splitRepository.calculateBalances(groupId)
            _uiState.update { it.copy(memberBalances = balances) }
        }
    }
    
    fun settleUp(amount: Double) {
        val groupId = _uiState.value.selectedGroup?.id ?: return
        val settleWith = _uiState.value.settleWithMember ?: return
        
        viewModelScope.launch {
            val currentUser = splitRepository.getCurrentUserMember(groupId) ?: return@launch
            
            val settlement = Settlement(
                groupId = groupId,
                fromMemberId = currentUser.id,
                toMemberId = settleWith.id,
                amount = amount
            )
            splitRepository.insertSettlement(settlement)
            hideSettleDialog()
            
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
