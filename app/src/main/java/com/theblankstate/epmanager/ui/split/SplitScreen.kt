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
import com.theblankstate.epmanager.ui.components.formatAmount
import com.theblankstate.epmanager.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.absoluteValue

private val groupEmojis = listOf("👥", "🏠", "✈️", "🍕", "🎉", "💼", "🎮", "🛒", "☕", "🚗")

// Extended icon categories with 50+ icons
private val planTypeIcons = mapOf(
    "Travel" to listOf("✈️", "🏖️", "🗺️", "🏔️", "⛺", "🚂", "🚗", "🛳️", "🎒", "🧳"),
    "Food" to listOf("🍕", "🍔", "☕", "🍽️", "🛒", "🧺", "🥘", "🍱", "🍰", "🥗"),
    "Events" to listOf("🎉", "🎊", "💒", "🎂", "🎁", "🪅", "🎄", "🎃", "🎭", "🎬"),
    "Home" to listOf("🏠", "🏡", "🛋️", "🛏️", "🧹", "🔧", "🏗️", "🪴", "🛁", "🪑"),
    "Education" to listOf("📚", "🎒", "✏️", "📝", "🎓", "🏫", "💻", "📖", "🔬", "🧮"),
    "Sports" to listOf("⚽", "🏀", "🎮", "🏋️", "🚴", "⛳", "🎯", "🏓", "🎾", "🏊"),
    "Work" to listOf("💼", "📋", "📊", "🖥️", "📱", "💡", "🔬", "📈", "💰", "🏢"),
    "Other" to listOf("👥", "💰", "🪙", "💳", "🧾", "📦", "🎲", "🔖", "❤️", "⭐")
)

// All icons flattened for quick access
private val allIcons: List<String> = planTypeIcons.values.flatten()


@Composable
fun SplitScreen(
    onNavigateBack: () -> Unit,
    viewModel: SplitViewModel = hiltViewModel(),
    currencyViewModel: com.theblankstate.epmanager.util.CurrencyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currencySymbol by currencyViewModel.currencySymbol.collectAsState(initial = "₹")
    
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
            onDelete = { viewModel.deleteGroup(uiState.selectedGroup!!) },
            currencySymbol = currencySymbol
        )
    } else {
        // Show groups list
        GroupsListScreen(
            groups = uiState.groups,
            isLoading = uiState.isLoading,
            onNavigateBack = onNavigateBack,
            onGroupClick = { viewModel.selectGroup(it.group) },
            onCreateGroup = { viewModel.showCreateGroupSheet() },
            currencySymbol = currencySymbol
        )
    }
    
    // Bottom Sheets
    if (uiState.showCreateGroupSheet) {
        CreateGroupSheet(
            friends = uiState.friends,
            onDismiss = { viewModel.hideCreateGroupSheet() },
            onConfirm = { name, emoji, planType, members, selectedFriends, budget ->
                viewModel.createGroup(name, emoji, planType, members, selectedFriends, budget)
            },
            currencySymbol = currencySymbol
        )
    }

    
    if (uiState.showAddExpenseSheet) {
        AddExpenseSheet(
            members = uiState.groupMembers,
            accounts = uiState.accounts,
            onDismiss = { viewModel.hideAddExpenseSheet() },
            onConfirm = { desc, amount, paidBy, accountId, enableSplit, includedMemberIds, customShares, instantSettle ->
                viewModel.addExpense(desc, amount, paidBy, accountId, enableSplit, includedMemberIds, customShares, instantSettle)
            },
            currencySymbol = currencySymbol
        )
    }

    
    if (uiState.showAddMemberSheet) {
        AddMemberSheet(
            friends = uiState.friends,
            existingMembers = uiState.groupMembers,
            onDismiss = { viewModel.hideAddMemberSheet() },
            onConfirmManual = { name, phone ->
                viewModel.addMember(name, phone)
            },
            onConfirmFriend = { friend ->
                viewModel.addLinkedMember(friend)
            }
        )
    }
    
    if (uiState.showSettleSheet && uiState.settleWithMember != null) {
        SettleSheet(
            member = uiState.settleWithMember!!,
            balance = uiState.memberBalances.find { it.member.id == uiState.settleWithMember!!.id }?.balance ?: 0.0,
            accounts = uiState.accounts,
            onDismiss = { viewModel.hideSettleSheet() },
            onConfirm = { amount, accountId -> viewModel.settleUp(amount, accountId) },
            currencySymbol = currencySymbol
        )
    }
}

@Composable
private fun GroupsListScreen(
    groups: List<SplitGroupWithMembers>,
    isLoading: Boolean,
    onNavigateBack: () -> Unit,
    onGroupClick: (SplitGroupWithMembers) -> Unit,
    onCreateGroup: () -> Unit,
    currencySymbol: String
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
                        onClick = { onGroupClick(groupWithMembers) },
                        currencySymbol = currencySymbol
                    )
                }
            }
        }
    }
}

@Composable
private fun GroupCard(
    groupWithMembers: SplitGroupWithMembers,
    onClick: () -> Unit,
    currencySymbol: String
) {
    // Minimal design without heavy backgrounds
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.md, horizontal = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Clean emoji without heavy background
            Text(
                groupWithMembers.group.emoji, 
                style = MaterialTheme.typography.headlineMedium
            )
            
            Spacer(modifier = Modifier.width(Spacing.md))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    groupWithMembers.group.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    Text(
                        "${groupWithMembers.members.size} members",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (groupWithMembers.group.planType != PlanType.OTHER) {
                        Text("•", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                        Text(
                            groupWithMembers.group.planType.displayName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    formatAmount(groupWithMembers.totalExpenses, currencySymbol),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                groupWithMembers.group.budget?.let { budget ->
                    val progress = ((groupWithMembers.totalExpenses / budget) * 100).toInt()
                    Text(
                        "${progress}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (progress > 100) Error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(start = Spacing.xs)
            )
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
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
    onDelete: () -> Unit,
    currencySymbol: String
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
                                    "${formatAmount(totalSpent, currencySymbol)} / ${formatAmount(budget, currencySymbol)}",
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
                    BalanceCard(balance = balance, onSettle = { onSettle(balance.member) }, currencySymbol = currencySymbol)
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
                    ExpenseCard(expense = expense, paidBy = paidBy, currencySymbol = currencySymbol)
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
private fun BalanceCard(balance: MemberBalance, onSettle: () -> Unit, currencySymbol: String) {
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
                formatAmount(balance.balance.absoluteValue, currencySymbol),
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
private fun ExpenseCard(expense: SplitExpense, paidBy: GroupMember?, currencySymbol: String) {
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
                formatAmount(expense.totalAmount, currencySymbol),
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
    friends: List<Friend> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (name: String, emoji: String, planType: PlanType, members: List<String>, selectedFriends: List<Friend>, budget: Double?) -> Unit,
    currencySymbol: String
) {
    var name by remember { mutableStateOf("") }
    var selectedEmoji by remember { mutableStateOf("👥") }
    var selectedPlanType by remember { mutableStateOf(PlanType.OTHER) }
    var memberNames by remember { mutableStateOf(listOf("")) }
    var selectedFriends by remember { mutableStateOf<Set<Friend>>(emptySet()) }
    var budgetText by remember { mutableStateOf("") }
    var showAllIcons by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    // Auto-select emoji based on plan type
    LaunchedEffect(selectedPlanType) {
        selectedEmoji = selectedPlanType.emoji
    }
    
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
                "Plan Your...",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(Spacing.md))
            
            // Plan Type Selector
            Text("What are you planning?", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(Spacing.sm))
            
            LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                items(PlanType.entries.toList()) { planType ->
                    FilterChip(
                        selected = selectedPlanType == planType,
                        onClick = { selectedPlanType = planType },
                        label = { Text(planType.displayName) },
                        leadingIcon = {
                            Text(planType.emoji, style = MaterialTheme.typography.bodyMedium)
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(Spacing.lg))
            
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("${selectedPlanType.displayName} Name") },
                placeholder = { Text("e.g., ${selectedPlanType.displayName} with friends") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(Spacing.md))
            
            OutlinedTextField(
                value = budgetText,
                onValueChange = { budgetText = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Budget (optional)") },
                placeholder = { Text("Set a spending limit") },
                prefix = { Text(currencySymbol) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(Spacing.md))
            
            // Icon Selector with Categories
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Choose Icon", style = MaterialTheme.typography.labelLarge)
                TextButton(onClick = { showAllIcons = !showAllIcons }) {
                    Text(if (showAllIcons) "Show Less" else "Show All")
                }
            }
            Spacer(modifier = Modifier.height(Spacing.sm))
            
            if (showAllIcons) {
                // Show icons by category
                planTypeIcons.forEach { (category, icons) ->
                    Text(
                        category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        items(icons) { emoji ->
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
                                Text(emoji, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(Spacing.sm))
                }
            } else {
                // Show quick icons
                LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    items(groupEmojis + listOf(selectedPlanType.emoji).filter { it !in groupEmojis }) { emoji ->
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
            }
            
            Spacer(modifier = Modifier.height(Spacing.lg))
            
            // Linked Friends Section
            if (friends.isNotEmpty()) {
                Text(
                    "Add Linked Friends",
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    "Tap to add to group",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
                
                LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    items(friends, key = { it.odiserId }) { friend ->
                        val isSelected = selectedFriends.contains(friend)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedFriends = if (isSelected) {
                                    selectedFriends - friend
                                } else {
                                    selectedFriends + friend
                                }
                            },
                            label = { 
                                Text(friend.displayName ?: friend.email.substringBefore("@"))
                            },
                            leadingIcon = {
                                if (isSelected) {
                                    Icon(
                                        Icons.Filled.Check,
                                        null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                } else {
                                    Icon(
                                        Icons.Filled.Link,
                                        null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(Spacing.lg))
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Add Manual Members", style = MaterialTheme.typography.labelLarge)
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
                        onConfirm(
                            name, 
                            selectedEmoji,
                            selectedPlanType,
                            memberNames.filter { it.isNotBlank() },
                            selectedFriends.toList(),
                            budget
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank(),
                shape = ButtonShape
            ) {
                Text("Create ${selectedPlanType.displayName}")
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
    onConfirm: (description: String, amount: Double, paidById: String, accountId: String?, enableSplit: Boolean, includedMemberIds: List<String>?, customShares: Map<String, Double>?, instantSettle: Boolean) -> Unit,
    currencySymbol: String
) {
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var selectedPayer by remember { mutableStateOf(members.find { it.isCurrentUser }) }
    // Default to Cash account
    val cashAccount = accounts.find { it.name.equals("Cash", ignoreCase = true) }
    var selectedAccount by remember { mutableStateOf(cashAccount ?: accounts.firstOrNull()) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    // New state for bottom sheet selectors
    var showPayerSheet by remember { mutableStateOf(false) }
    var showAccountSheet by remember { mutableStateOf(false) }
    var showRepayAccountSheet by remember { mutableStateOf(false) }
    
    // Repayment account when someone else pays
    var repaymentAccount by remember { mutableStateOf(cashAccount ?: accounts.firstOrNull()) }
    
    // Split options
    var enableSplit by remember { mutableStateOf(true) }
    var selectedMemberIds by remember { mutableStateOf(members.map { it.id }.toSet()) }
    
    // Custom share per member
    var customSharesEnabled by remember { mutableStateOf(false) }
    var customShares by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    
    // Instant settle toggle (when someone else pays)
    var instantSettle by remember { mutableStateOf(false) }
    
    val isPaidByMe = selectedPayer?.isCurrentUser == true
    val amountValue = amount.toDoubleOrNull() ?: 0.0
    val membersInSplit = members.filter { it.id in selectedMemberIds }
    val autoCalculatedShare = if (membersInSplit.isNotEmpty()) amountValue / membersInSplit.size else 0.0

    
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
                prefix = { Text(currencySymbol) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(Spacing.md))
            
            // Paid By - Clickable field that opens bottom sheet
            Text("Paid by", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(Spacing.xs))
            Card(
                onClick = { showPayerSheet = true },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.outlinedCardColors()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.md),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                (if (selectedPayer?.isCurrentUser == true) "Y" else selectedPayer?.name?.firstOrNull()?.toString() ?: "?"),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.width(Spacing.sm))
                        Text(
                            if (selectedPayer?.isCurrentUser == true) "You" else selectedPayer?.name ?: "Select...",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null)
                }
            }
            
            // Account Selection - Always show (mandatory)
            if (accounts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Spacing.md))
                Text(
                    if (isPaidByMe) "From Account" else "Your Repayment Account",
                    style = MaterialTheme.typography.labelLarge
                )
                if (!isPaidByMe) {
                    Text(
                        "You'll repay ${selectedPayer?.name ?: "them"} from this account",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(Spacing.xs))
                Card(
                    onClick = { showAccountSheet = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.outlinedCardColors()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.md),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.AccountBalance,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(Spacing.sm))
                            Text(
                                selectedAccount?.name ?: "Select Account",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null)
                    }
                }
            }
            
            // Instant Settle option (only when someone else pays)
            if (!isPaidByMe && enableSplit) {
                Spacer(modifier = Modifier.height(Spacing.md))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Mark as Settled", style = MaterialTheme.typography.labelLarge)
                        Text(
                            "Record transaction and clear your debt immediately",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = instantSettle,
                        onCheckedChange = { instantSettle = it }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(Spacing.lg))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(Spacing.md))
            
            // Split Options Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Split this expense", style = MaterialTheme.typography.labelLarge)
                    Text(
                        if (enableSplit) "Split among ${membersInSplit.size} members" else "No split - personal expense",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = enableSplit,
                    onCheckedChange = { enableSplit = it }
                )
            }
            
            // Member Selection for Split
            if (enableSplit) {
                Spacer(modifier = Modifier.height(Spacing.md))
                Text("Include in split:", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(Spacing.sm))
                
                LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    items(members) { member ->
                        val isSelected = member.id in selectedMemberIds
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedMemberIds = if (isSelected) {
                                    selectedMemberIds - member.id
                                } else {
                                    selectedMemberIds + member.id
                                }
                            },
                            label = { Text(if (member.isCurrentUser) "You" else member.name) },
                            leadingIcon = {
                                if (isSelected) {
                                    Icon(Icons.Filled.Check, null, modifier = Modifier.size(18.dp))
                                }
                            }
                        )
                    }
                }
                
                // Custom split amounts per member
                Spacer(modifier = Modifier.height(Spacing.md))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Custom amounts", style = MaterialTheme.typography.labelMedium)
                    Switch(
                        checked = customSharesEnabled,
                        onCheckedChange = { customSharesEnabled = it }
                    )
                }
                
                if (customSharesEnabled && amountValue > 0) {
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    membersInSplit.forEach { member ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = Spacing.xs),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                if (member.isCurrentUser) "You" else member.name,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            OutlinedTextField(
                                value = customShares[member.id] ?: String.format("%.2f", autoCalculatedShare),
                                onValueChange = { newValue ->
                                    customShares = customShares + (member.id to newValue.filter { c -> c.isDigit() || c == '.' })
                                },
                                prefix = { Text(currencySymbol) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.width(110.dp)
                            )
                        }
                    }
                } else if (amountValue > 0) {
                    Text(
                        "Each pays: ${currencySymbol}${String.format("%.2f", autoCalculatedShare)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = Spacing.xs)
                    )
                }
            }

            
            Spacer(modifier = Modifier.height(Spacing.xl))
            
            Button(
                onClick = {
                    if (description.isNotBlank() && amountValue > 0 && selectedPayer != null) {
                        // Convert customShares map from String to Double for all members
                        val customSharesMap = if (customSharesEnabled) {
                            customShares.mapNotNull { (memberId, value) ->
                                value.toDoubleOrNull()?.let { memberId to it }
                            }.toMap().takeIf { it.isNotEmpty() }
                        } else null
                        val includedIds = if (enableSplit && selectedMemberIds.size < members.size) selectedMemberIds.toList() else null
                        onConfirm(description, amountValue, selectedPayer!!.id, selectedAccount?.id, enableSplit, includedIds, customSharesMap, instantSettle)
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
    
    // Payer Selection Bottom Sheet
    if (showPayerSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPayerSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg)
                    .padding(bottom = Spacing.xxl)
            ) {
                Text(
                    "Who paid?",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(Spacing.lg))
                
                members.forEach { member ->
                    val isSelected = selectedPayer?.id == member.id
                    Card(
                        onClick = {
                            selectedPayer = member
                            showPayerSheet = false
                        },
                        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xs),
                        colors = if (isSelected) CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ) else CardDefaults.outlinedCardColors()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Spacing.md),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    (if (member.isCurrentUser) "Y" else member.name.firstOrNull()?.toString() ?: "?"),
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(Spacing.md))
                            Text(
                                if (member.isCurrentUser) "You" else member.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            if (isSelected) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Account Selection Bottom Sheet
    if (showAccountSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAccountSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg)
                    .padding(bottom = Spacing.xxl)
            ) {
                Text(
                    "Select Account",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(Spacing.lg))
                
                accounts.forEach { account ->
                    val isSelected = selectedAccount?.id == account.id
                    Card(
                        onClick = {
                            selectedAccount = account
                            showAccountSheet = false
                        },
                        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xs),
                        colors = if (isSelected) CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ) else CardDefaults.outlinedCardColors()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Spacing.md),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.AccountBalance,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(Spacing.md))
                            Column {
                                Text(
                                    account.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                Text(
                                    "${currencySymbol}${String.format("%.2f", account.balance)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            if (isSelected) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddMemberSheet(
    friends: List<Friend> = emptyList(),
    existingMembers: List<GroupMember> = emptyList(),
    onDismiss: () -> Unit,
    onConfirmManual: (name: String, phone: String?) -> Unit,
    onConfirmFriend: (Friend) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState()
    
    // Filter out friends that are already members
    val existingLinkedUserIds = existingMembers.mapNotNull { it.linkedUserId }.toSet()
    val availableFriends = friends.filter { it.odiserId !in existingLinkedUserIds }
    
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
            
            // Show linked friends quick-add
            if (availableFriends.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Spacing.lg))
                
                Text(
                    "Add Linked Friend",
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    "Tap to add instantly",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(Spacing.sm))
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    items(availableFriends, key = { it.odiserId }) { friend ->
                        SuggestionChip(
                            onClick = {
                                onConfirmFriend(friend)
                                onDismiss()
                            },
                            label = { 
                                Text(friend.displayName ?: friend.email.substringBefore("@"))
                            },
                            icon = {
                                Icon(
                                    Icons.Filled.Link,
                                    null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(Spacing.md))
                HorizontalDivider()
            }
            
            Spacer(modifier = Modifier.height(Spacing.lg))
            
            Text(
                "Or Add Manually",
                style = MaterialTheme.typography.labelLarge
            )
            
            Spacer(modifier = Modifier.height(Spacing.sm))
            
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
                onClick = { if (name.isNotBlank()) onConfirmManual(name, phone.ifBlank { null }) },
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
    onConfirm: (amount: Double, accountId: String?) -> Unit,
    currencySymbol: String
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
                        formatAmount(balance.absoluteValue, currencySymbol),
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
                prefix = { Text(currencySymbol) },
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
