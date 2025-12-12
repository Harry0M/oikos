@file:OptIn(ExperimentalMaterial3Api::class)

package com.theblankstate.epmanager.ui.split

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.theblankstate.epmanager.data.model.*
import com.theblankstate.epmanager.ui.components.formatCurrency
import com.theblankstate.epmanager.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.absoluteValue

private val groupEmojis = listOf("👥", "🏠", "✈️", "🍕", "🎉", "💼", "🎮", "🛒", "☕", "🚗")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitScreen(
    onNavigateBack: () -> Unit,
    viewModel: SplitViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Show group details if one is selected
    if (uiState.selectedGroup != null) {
        GroupDetailScreen(
            group = uiState.selectedGroup!!,
            members = uiState.groupMembers,
            expenses = uiState.groupExpenses,
            balances = uiState.memberBalances,
            onNavigateBack = { viewModel.clearSelection() },
            onAddExpense = { viewModel.showAddExpenseDialog() },
            onAddMember = { viewModel.showAddMemberDialog() },
            onSettle = { member -> viewModel.showSettleDialog(member) },
            onDelete = { viewModel.deleteGroup(uiState.selectedGroup!!) }
        )
    } else {
        // Show groups list
        GroupsListScreen(
            groups = uiState.groups,
            isLoading = uiState.isLoading,
            onNavigateBack = onNavigateBack,
            onGroupClick = { viewModel.selectGroup(it.group) },
            onCreateGroup = { viewModel.showCreateGroupDialog() }
        )
    }
    
    // Dialogs
    if (uiState.showCreateGroupDialog) {
        CreateGroupDialog(
            onDismiss = { viewModel.hideCreateGroupDialog() },
            onConfirm = { name, emoji, members ->
                viewModel.createGroup(name, emoji, members)
            }
        )
    }
    
    if (uiState.showAddExpenseDialog) {
        AddExpenseDialog(
            members = uiState.groupMembers,
            onDismiss = { viewModel.hideAddExpenseDialog() },
            onConfirm = { desc, amount, paidBy ->
                viewModel.addExpense(desc, amount, paidBy)
            }
        )
    }
    
    if (uiState.showAddMemberDialog) {
        AddMemberDialog(
            onDismiss = { viewModel.hideAddMemberDialog() },
            onConfirm = { name, phone ->
                viewModel.addMember(name, phone)
            }
        )
    }
    
    if (uiState.showSettleDialog && uiState.settleWithMember != null) {
        SettleDialog(
            member = uiState.settleWithMember!!,
            onDismiss = { viewModel.hideSettleDialog() },
            onConfirm = { amount -> viewModel.settleUp(amount) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroupsListScreen(
    groups: List<SplitGroupWithMembers>,
    isLoading: Boolean,
    onNavigateBack: () -> Unit,
    onGroupClick: (SplitGroupWithMembers) -> Unit,
    onCreateGroup: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text("Split Expenses", fontWeight = FontWeight.Bold) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateGroup,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Filled.Add, "Create Group")
            }
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (groups.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(paddingValues).padding(Spacing.xxl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("🤝", style = MaterialTheme.typography.displayLarge)
                Spacer(modifier = Modifier.height(Spacing.md))
                Text("No Groups Yet", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "Create a group to split expenses with friends",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                items(groups, key = { it.group.id }) { groupWithMembers ->
                    GroupCard(
                        groupWithMembers = groupWithMembers,
                        onClick = { onGroupClick(groupWithMembers) }
                    )
                }
            }
        }
    }
}

@Composable
private fun GroupCard(
    groupWithMembers: SplitGroupWithMembers,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(groupWithMembers.group.emoji, style = MaterialTheme.typography.titleLarge)
            }
            
            Spacer(modifier = Modifier.width(Spacing.md))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    groupWithMembers.group.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "${groupWithMembers.members.size} members",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    formatCurrency(groupWithMembers.totalExpenses),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "total",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroupDetailScreen(
    group: SplitGroup,
    members: List<GroupMember>,
    expenses: List<SplitExpense>,
    balances: List<MemberBalance>,
    onNavigateBack: () -> Unit,
    onAddExpense: () -> Unit,
    onAddMember: () -> Unit,
    onSettle: (GroupMember) -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(group.emoji)
                        Spacer(modifier = Modifier.width(Spacing.sm))
                        Text(group.name, fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onAddMember) {
                        Icon(Icons.Filled.PersonAdd, "Add Member")
                    }
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Filled.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddExpense,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Filled.Add, "Add Expense")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            // Balances Section
            item {
                Text("Balances", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            
            if (balances.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "All settled up! 🎉",
                            modifier = Modifier.padding(Spacing.lg),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            } else {
                items(balances, key = { it.member.id }) { balance ->
                    BalanceCard(balance = balance, onSettle = { onSettle(balance.member) })
                }
            }
            
            // Members Section
            item {
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text("Members (${members.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    items(members, key = { it.id }) { member ->
                        MemberChip(member = member)
                    }
                }
            }
            
            // Expenses Section
            item {
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text("Expenses (${expenses.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            
            if (expenses.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(Spacing.lg),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("💸", style = MaterialTheme.typography.displayMedium)
                            Text("No expenses yet", style = MaterialTheme.typography.bodyLarge)
                            Text("Tap + to add one", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                items(expenses, key = { it.id }) { expense ->
                    val paidBy = members.find { it.id == expense.paidById }
                    ExpenseCard(expense = expense, paidBy = paidBy)
                }
            }
            
            item { Spacer(modifier = Modifier.height(Spacing.huge)) }
        }
    }
    
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Group") },
            text = { Text("Delete '${group.name}' and all its expenses?") },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteConfirm = false }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun BalanceCard(balance: MemberBalance, onSettle: () -> Unit) {
    val isOwed = balance.balance > 0
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isOwed) Success.copy(alpha = 0.1f) else Error.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(balance.member.name, fontWeight = FontWeight.Medium)
                Text(
                    if (isOwed) "owes you" else "you owe",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                formatCurrency(balance.balance.absoluteValue),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isOwed) Success else Error
            )
            Spacer(modifier = Modifier.width(Spacing.sm))
            TextButton(onClick = onSettle) {
                Text("Settle")
            }
        }
    }
}

@Composable
private fun MemberChip(member: GroupMember) {
    AssistChip(
        onClick = {},
        label = { 
            Text(
                if (member.isCurrentUser) "You" else member.name,
                style = MaterialTheme.typography.labelMedium
            ) 
        },
        leadingIcon = {
            Icon(Icons.Filled.Person, null, modifier = Modifier.size(16.dp))
        }
    )
}

@Composable
private fun ExpenseCard(expense: SplitExpense, paidBy: GroupMember?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(expense.description, fontWeight = FontWeight.Medium)
                Text(
                    "Paid by ${paidBy?.name ?: "Unknown"} • ${formatDate(expense.date)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                formatCurrency(expense.totalAmount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Error
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateGroupDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, emoji: String, members: List<String>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedEmoji by remember { mutableStateOf("👥") }
    var member1 by remember { mutableStateOf("") }
    var member2 by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Group", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Group Name") },
                    placeholder = { Text("Trip to Goa, Roommates...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Text("Icon", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    items(groupEmojis) { emoji ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (emoji == selectedEmoji) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { selectedEmoji = emoji },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(emoji)
                        }
                    }
                }
                
                Text("Add Members", style = MaterialTheme.typography.labelMedium)
                OutlinedTextField(
                    value = member1,
                    onValueChange = { member1 = it },
                    label = { Text("Member 1") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = member2,
                    onValueChange = { member2 = it },
                    label = { Text("Member 2") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { 
                    if (name.isNotBlank()) {
                        onConfirm(name, selectedEmoji, listOf(member1, member2))
                    }
                },
                enabled = name.isNotBlank()
            ) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddExpenseDialog(
    members: List<GroupMember>,
    onDismiss: () -> Unit,
    onConfirm: (description: String, amount: Double, paidById: String) -> Unit
) {
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var selectedPayer by remember { mutableStateOf(members.find { it.isCurrentUser }) }
    var expanded by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Expense", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    placeholder = { Text("Dinner, Taxi...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Amount") },
                    prefix = { Text("₹") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedPayer?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Paid by") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        members.forEach { member ->
                            DropdownMenuItem(
                                text = { Text(if (member.isCurrentUser) "You" else member.name) },
                                onClick = { selectedPayer = member; expanded = false }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amountValue = amount.toDoubleOrNull()
                    if (description.isNotBlank() && amountValue != null && selectedPayer != null) {
                        onConfirm(description, amountValue, selectedPayer!!.id)
                    }
                },
                enabled = description.isNotBlank() && amount.toDoubleOrNull() != null && selectedPayer != null
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun AddMemberDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, phone: String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Member", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone (optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name, phone.ifBlank { null }) },
                enabled = name.isNotBlank()
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun SettleDialog(
    member: GroupMember,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settle with ${member.name}", fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Amount paid") },
                prefix = { Text("₹") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { amount.toDoubleOrNull()?.let { onConfirm(it) } },
                enabled = amount.toDoubleOrNull() != null
            ) { Text("Record Payment") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun formatDate(timestamp: Long): String {
    return SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
}
