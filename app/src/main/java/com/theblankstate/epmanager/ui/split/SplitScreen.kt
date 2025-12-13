@file:OptIn(ExperimentalMaterial3Api::class)

package com.theblankstate.epmanager.ui.split

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
            onAddExpense = { viewModel.showAddExpenseSheet() },
            onAddMember = { viewModel.showAddMemberSheet() },
            onSettle = { member -> viewModel.showSettleSheet(member) },
            onDelete = { viewModel.deleteGroup(uiState.selectedGroup!!) }
        )
    } else {
        // Show groups list
        GroupsListScreen(
            groups = uiState.groups,
            isLoading = uiState.isLoading,
            onNavigateBack = onNavigateBack,
            onGroupClick = { viewModel.selectGroup(it.group) },
            onCreateGroup = { viewModel.showCreateGroupSheet() }
        )
    }
    
    // Bottom Sheets
    if (uiState.showCreateGroupSheet) {
        CreateGroupSheet(
            onDismiss = { viewModel.hideCreateGroupSheet() },
            onConfirm = { name, emoji, members, budget ->
                viewModel.createGroup(name, emoji, members, budget)
            }
        )
    }
    
    if (uiState.showAddExpenseSheet) {
        AddExpenseSheet(
            members = uiState.groupMembers,
            accounts = uiState.accounts,
            onDismiss = { viewModel.hideAddExpenseSheet() },
            onConfirm = { desc, amount, paidBy, accountId ->
                viewModel.addExpense(desc, amount, paidBy, accountId)
            }
        )
    }
    
    if (uiState.showAddMemberSheet) {
        AddMemberSheet(
            onDismiss = { viewModel.hideAddMemberSheet() },
            onConfirm = { name, phone ->
                viewModel.addMember(name, phone)
            }
        )
    }
    
    if (uiState.showSettleSheet && uiState.settleWithMember != null) {
        SettleSheet(
            member = uiState.settleWithMember!!,
            balance = uiState.memberBalances.find { it.member.id == uiState.settleWithMember!!.id }?.balance ?: 0.0,
            accounts = uiState.accounts,
            onDismiss = { viewModel.hideSettleSheet() },
            onConfirm = { amount, accountId -> viewModel.settleUp(amount, accountId) }
        )
    }
}

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
                title = { Text("Split Expenses", fontWeight = FontWeight.Bold) },
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
                // Show budget progress if budget is set
                groupWithMembers.group.budget?.let { budget ->
                    val progress = (groupWithMembers.totalExpenses / budget).coerceIn(0.0, 1.0)
                    val isOverBudget = groupWithMembers.totalExpenses > budget
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { progress.toFloat() },
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                        color = if (isOverBudget) Error else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    formatCurrency(groupWithMembers.totalExpenses),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                // Show budget or "total" label
                groupWithMembers.group.budget?.let { budget ->
                    Text(
                        "of ${formatCurrency(budget)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (groupWithMembers.totalExpenses > budget) Error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } ?: Text(
                    "total",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

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
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            // Members Row
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    modifier = Modifier.padding(bottom = Spacing.sm)
                ) {
                    items(members, key = { it.id }) { member ->
                        MemberChip(member = member)
                    }
                }
            }
            
            // Budget Status (if budget is set)
            group.budget?.let { budget ->
                val totalSpent = expenses.sumOf { it.totalAmount }
                val progress = (totalSpent / budget).coerceIn(0.0, 1.0)
                val isOverBudget = totalSpent > budget
                val remaining = budget - totalSpent
                
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isOverBudget) 
                                Error.copy(alpha = 0.1f) 
                            else 
                                MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(Spacing.md)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    if (isOverBudget) "Over Budget!" else "Budget",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isOverBudget) Error else MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    "${formatCurrency(totalSpent)} / ${formatCurrency(budget)}",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isOverBudget) Error else MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.height(Spacing.sm))
                            LinearProgressIndicator(
                                progress = { progress.toFloat() },
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                                color = if (isOverBudget) Error else MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            Spacer(modifier = Modifier.height(Spacing.xs))
                            Text(
                                if (isOverBudget) 
                                    "₹${String.format("%.0f", -remaining)} over budget"
                                else 
                                    "₹${String.format("%.0f", remaining)} remaining",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isOverBudget) Error else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
            
            // Balances Section
            if (balances.isNotEmpty()) {
                item {
                    Text(
                        "Balances",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                items(balances, key = { it.member.id }) { balance ->
                    BalanceCard(balance = balance, onSettle = { onSettle(balance.member) })
                }
            } else if (expenses.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(Spacing.md),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🎉", style = MaterialTheme.typography.headlineMedium)
                            Spacer(modifier = Modifier.width(Spacing.md))
                            Text(
                                "All settled up!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
            
            // Expenses Section
            item {
                Spacer(modifier = Modifier.height(Spacing.md))
                Text(
                    "Expenses (${expenses.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            if (expenses.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(Spacing.xl),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("💸", style = MaterialTheme.typography.displayMedium)
                            Spacer(modifier = Modifier.height(Spacing.sm))
                            Text("No expenses yet", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "Tap + to add one",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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
            FilledTonalButton(
                onClick = onSettle,
                contentPadding = PaddingValues(horizontal = Spacing.md)
            ) {
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
                    "Paid by ${if (paidBy?.isCurrentUser == true) "You" else paidBy?.name ?: "Unknown"} • ${formatDate(expense.date)}",
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

// ==================== BOTTOM SHEETS ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateGroupSheet(
    onDismiss: () -> Unit,
    onConfirm: (name: String, emoji: String, members: List<String>, budget: Double?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedEmoji by remember { mutableStateOf("👥") }
    var memberNames by remember { mutableStateOf(listOf("")) }
    var budgetText by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg)
                .padding(bottom = Spacing.xxl)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "Create Group",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(Spacing.lg))
            
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Group Name") },
                placeholder = { Text("Trip to Goa, Roommates...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(Spacing.md))
            
            OutlinedTextField(
                value = budgetText,
                onValueChange = { budgetText = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Budget (optional)") },
                placeholder = { Text("Set a spending limit") },
                prefix = { Text("₹") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(Spacing.md))
            
            Text("Choose Icon", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(Spacing.sm))
            
            LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                items(groupEmojis) { emoji ->
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                if (emoji == selectedEmoji) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable { selectedEmoji = emoji },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(emoji, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(Spacing.lg))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Add Members", style = MaterialTheme.typography.labelLarge)
                TextButton(onClick = { memberNames = memberNames + "" }) {
                    Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add More")
                }
            }
            
            Spacer(modifier = Modifier.height(Spacing.sm))
            
            memberNames.forEachIndexed { index, memberName ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = memberName,
                        onValueChange = { newName ->
                            memberNames = memberNames.toMutableList().also { it[index] = newName }
                        },
                        label = { Text("Member ${index + 1}") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    if (memberNames.size > 1) {
                        IconButton(
                            onClick = {
                                memberNames = memberNames.toMutableList().also { it.removeAt(index) }
                            }
                        ) {
                            Icon(
                                Icons.Filled.Close,
                                "Remove",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(Spacing.sm))
            }
            
            Spacer(modifier = Modifier.height(Spacing.lg))
            
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val budget = budgetText.toDoubleOrNull()
                        onConfirm(name, selectedEmoji, memberNames.filter { it.isNotBlank() }, budget)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank(),
                shape = ButtonShape
            ) {
                Text("Create Group")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddExpenseSheet(
    members: List<GroupMember>,
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onConfirm: (description: String, amount: Double, paidById: String, accountId: String?) -> Unit
) {
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var selectedPayer by remember { mutableStateOf(members.find { it.isCurrentUser }) }
    var selectedAccount by remember { mutableStateOf<Account?>(null) }
    var payerExpanded by remember { mutableStateOf(false) }
    var accountExpanded by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    val isPaidByMe = selectedPayer?.isCurrentUser == true
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg)
                .padding(bottom = Spacing.xxl)
        ) {
            Text(
                "Add Expense",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(Spacing.lg))
            
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                placeholder = { Text("Dinner, Taxi, Groceries...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(Spacing.md))
            
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Amount") },
                prefix = { Text("₹") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(Spacing.md))
            
            ExposedDropdownMenuBox(
                expanded = payerExpanded,
                onExpandedChange = { payerExpanded = it }
            ) {
                OutlinedTextField(
                    value = if (selectedPayer?.isCurrentUser == true) "You" else selectedPayer?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Paid by") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(payerExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(expanded = payerExpanded, onDismissRequest = { payerExpanded = false }) {
                    members.forEach { member ->
                        DropdownMenuItem(
                            text = { Text(if (member.isCurrentUser) "You" else member.name) },
                            onClick = { selectedPayer = member; payerExpanded = false }
                        )
                    }
                }
            }
            
            // Show account selection only if paid by "You"
            if (isPaidByMe && accounts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Spacing.md))
                
                ExposedDropdownMenuBox(
                    expanded = accountExpanded,
                    onExpandedChange = { accountExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedAccount?.name ?: "Select Account",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("From Account") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(accountExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = accountExpanded, onDismissRequest = { accountExpanded = false }) {
                        accounts.forEach { account ->
                            DropdownMenuItem(
                                text = { Text(account.name) },
                                onClick = { selectedAccount = account; accountExpanded = false }
                            )
                        }
                    }
                }
                
                Text(
                    "This expense will be recorded as a transaction",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Spacing.xs)
                )
            }
            
            Spacer(modifier = Modifier.height(Spacing.xl))
            
            Button(
                onClick = {
                    val amountValue = amount.toDoubleOrNull()
                    if (description.isNotBlank() && amountValue != null && selectedPayer != null) {
                        onConfirm(description, amountValue, selectedPayer!!.id, selectedAccount?.id)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = description.isNotBlank() && amount.toDoubleOrNull() != null && selectedPayer != null,
                shape = ButtonShape
            ) {
                Text("Add Expense")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddMemberSheet(
    onDismiss: () -> Unit,
    onConfirm: (name: String, phone: String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState()
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg)
                .padding(bottom = Spacing.xxl)
        ) {
            Text(
                "Add Member",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(Spacing.lg))
            
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                placeholder = { Text("Enter member name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(Spacing.md))
            
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone (optional)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(Spacing.xl))
            
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name, phone.ifBlank { null }) },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank(),
                shape = ButtonShape
            ) {
                Text("Add Member")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettleSheet(
    member: GroupMember,
    balance: Double,
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, accountId: String?) -> Unit
) {
    var amount by remember { mutableStateOf(balance.absoluteValue.toString()) }
    var selectedAccount by remember { mutableStateOf<Account?>(null) }
    var accountExpanded by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    
    val isOwed = balance > 0 // They owe me
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg)
                .padding(bottom = Spacing.xxl)
        ) {
            Text(
                "Settle with ${member.name}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(Spacing.sm))
            
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isOwed) Success.copy(alpha = 0.1f) else Error.copy(alpha = 0.1f)
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(Spacing.md),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        if (isOwed) "${member.name} owes you" else "You owe ${member.name}",
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        formatCurrency(balance.absoluteValue),
                        fontWeight = FontWeight.Bold,
                        color = if (isOwed) Success else Error
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(Spacing.lg))
            
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Amount to settle") },
                prefix = { Text("₹") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            
            // Account selection for recording transaction
            if (accounts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Spacing.md))
                
                ExposedDropdownMenuBox(
                    expanded = accountExpanded,
                    onExpandedChange = { accountExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedAccount?.name ?: "Select Account (optional)",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(if (isOwed) "Receive to" else "Pay from") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(accountExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = accountExpanded, onDismissRequest = { accountExpanded = false }) {
                        accounts.forEach { account ->
                            DropdownMenuItem(
                                text = { Text(account.name) },
                                onClick = { selectedAccount = account; accountExpanded = false }
                            )
                        }
                    }
                }
                
                Text(
                    if (isOwed) "Record as income when ${member.name} pays you"
                    else "Record as expense when you pay ${member.name}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Spacing.xs)
                )
            }
            
            Spacer(modifier = Modifier.height(Spacing.xl))
            
            Button(
                onClick = { amount.toDoubleOrNull()?.let { onConfirm(it, selectedAccount?.id) } },
                modifier = Modifier.fillMaxWidth(),
                enabled = amount.toDoubleOrNull() != null,
                shape = ButtonShape
            ) {
                Text("Record Settlement")
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    return SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
}
