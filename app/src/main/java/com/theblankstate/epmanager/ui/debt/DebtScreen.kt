package com.theblankstate.epmanager.ui.debt

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.theblankstate.epmanager.data.model.Debt
import com.theblankstate.epmanager.data.model.DebtType
import com.theblankstate.epmanager.data.model.Friend
import com.theblankstate.epmanager.ui.components.formatCurrency
import com.theblankstate.epmanager.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtScreen(
    onNavigateBack: () -> Unit,
    onNavigateToHistory: (String) -> Unit,
    viewModel: DebtViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Debts & Credits",
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showAddSheet() },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Add"
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Summary Cards
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.md),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                SummaryCard(
                    title = "I Owe",
                    amount = uiState.totalDebtOwed,
                    color = Error,
                    icon = Icons.Filled.ArrowUpward,
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    title = "Owed to Me",
                    amount = uiState.totalCreditOwed,
                    color = Green,
                    icon = Icons.Filled.ArrowDownward,
                    modifier = Modifier.weight(1f)
                )
            }
            
            // Tab Row
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Debts (${uiState.debts.size})") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Credits (${uiState.credits.size})") }
                )
            }
            
            // Content
            val items = if (selectedTab == 0) uiState.debts else uiState.credits
            
            if (items.isEmpty()) {
                EmptyState(
                    isDebt = selectedTab == 0,
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(Spacing.md),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    items(items, key = { it.id }) { debt ->
                        DebtItem(
                            debt = debt,
                            onClick = { viewModel.showDebtDetail(debt) }
                        )
                    }
                }
            }
        }
    }
    
    // Add Debt/Credit Sheet
    if (uiState.showAddSheet) {
        AddDebtSheet(
            friends = uiState.friends,
            onDismiss = { viewModel.hideAddSheet() },
            onConfirm = { type, name, amount, dueDate, friendId, notes ->
                viewModel.addDebt(type, name, amount, dueDate, friendId, notes)
            }
        )
    }
    
    // Debt Detail Sheet
    if (uiState.showDetailSheet && uiState.selectedDebt != null) {
        DebtDetailSheet(
            debt = uiState.selectedDebt!!,
            payments = uiState.selectedDebtPayments,
            onDismiss = { viewModel.hideDetailSheet() },
            onAddPayment = { viewModel.showPaymentSheet() },
            onSettle = { viewModel.settleDebt() },
            onDelete = { viewModel.deleteDebt() },
            onViewHistory = { onNavigateToHistory(uiState.selectedDebt!!.id) }
        )
    }
    
    // Payment Sheet
    if (uiState.showPaymentSheet && uiState.selectedDebt != null) {
        AddPaymentSheet(
            debt = uiState.selectedDebt!!,
            onDismiss = { viewModel.hidePaymentSheet() },
            onConfirm = { amount, note ->
                viewModel.addPayment(amount, null, note)
            }
        )
    }
}

@Composable
private fun SummaryCard(
    title: String,
    amount: Double,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(Spacing.xs))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatCurrency(amount),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun DebtItem(
    debt: Debt,
    onClick: () -> Unit
) {
    val color = if (debt.type == DebtType.DEBT) Error else Green
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(Spacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (debt.type == DebtType.DEBT) 
                            Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(Spacing.md))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = debt.personName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (debt.linkedFriendId != null) {
                        Text(
                            text = "🔗 Linked friend",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (debt.dueDate != null) {
                        val daysLeft = debt.daysUntilDue ?: 0
                        Text(
                            text = if (daysLeft < 0) "Overdue by ${-daysLeft} days"
                                   else if (daysLeft == 0) "Due today"
                                   else "Due in $daysLeft days",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (debt.isOverdue) Error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatCurrency(debt.remainingAmount),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                    Text(
                        text = "of ${formatCurrency(debt.totalAmount)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Progress bar
            Spacer(modifier = Modifier.height(Spacing.sm))
            LinearProgressIndicator(
                progress = { debt.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape),
                color = color,
                trackColor = color.copy(alpha = 0.2f)
            )
        }
    }
}

@Composable
private fun EmptyState(
    isDebt: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (isDebt) "💳" else "💰",
            style = MaterialTheme.typography.displayLarge
        )
        Spacer(modifier = Modifier.height(Spacing.md))
        Text(
            text = if (isDebt) "No Active Debts" else "No Active Credits",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = if (isDebt) "Add money you owe to others" else "Add money owed to you",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddDebtSheet(
    friends: List<Friend>,
    onDismiss: () -> Unit,
    onConfirm: (type: DebtType, name: String, amount: Double, dueDate: Long?, friendId: String?, notes: String?) -> Unit
) {
    var selectedType by remember { mutableStateOf(DebtType.DEBT) }
    var personName by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var selectedFriend by remember { mutableStateOf<Friend?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var dueDate by remember { mutableStateOf<Long?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg)
                .padding(bottom = Spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Text(
                text = "Add Debt/Credit",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            // Type selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                FilterChip(
                    selected = selectedType == DebtType.DEBT,
                    onClick = { selectedType = DebtType.DEBT },
                    label = { Text("I Owe (Debt)") },
                    leadingIcon = if (selectedType == DebtType.DEBT) {
                        { Icon(Icons.Filled.Check, null, Modifier.size(16.dp)) }
                    } else null,
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = selectedType == DebtType.CREDIT,
                    onClick = { selectedType = DebtType.CREDIT },
                    label = { Text("Owed to Me") },
                    leadingIcon = if (selectedType == DebtType.CREDIT) {
                        { Icon(Icons.Filled.Check, null, Modifier.size(16.dp)) }
                    } else null,
                    modifier = Modifier.weight(1f)
                )
            }
            
            // Friend selector
            if (friends.isNotEmpty()) {
                Text(
                    text = "Select from friends (optional)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    items(friends) { friend ->
                        FilterChip(
                            selected = selectedFriend?.odiserId == friend.odiserId,
                            onClick = { 
                                selectedFriend = if (selectedFriend?.odiserId == friend.odiserId) null else friend
                                if (selectedFriend != null) {
                                    personName = friend.displayName ?: friend.email
                                }
                            },
                            label = { Text(friend.displayName ?: friend.email) }
                        )
                    }
                }
            }
            
            // Person name
            OutlinedTextField(
                value = personName,
                onValueChange = { personName = it },
                label = { Text(if (selectedType == DebtType.DEBT) "Creditor Name" else "Debtor Name") },
                placeholder = { Text("Who?") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = InputFieldShape
            )
            
            // Amount
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Amount") },
                prefix = { Text("₹") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = InputFieldShape
            )
            
            // Due date
            OutlinedTextField(
                value = dueDate?.let { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(it)) } ?: "",
                onValueChange = {},
                label = { Text("Due Date (Optional)") },
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Filled.CalendarMonth, "Select date")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true },
                shape = InputFieldShape
            )
            
            // Notes
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                shape = InputFieldShape
            )
            
            Spacer(modifier = Modifier.height(Spacing.md))
            
            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = ButtonShapePill
                ) {
                    Text("Cancel")
                }
                
                Button(
                    onClick = {
                        val amountValue = amount.toDoubleOrNull()
                        if (personName.isNotBlank() && amountValue != null && amountValue > 0) {
                            onConfirm(
                                selectedType,
                                personName,
                                amountValue,
                                dueDate,
                                selectedFriend?.odiserId,
                                notes.takeIf { it.isNotBlank() }
                            )
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = personName.isNotBlank() && amount.toDoubleOrNull()?.let { it > 0 } == true,
                    shape = ButtonShapePill
                ) {
                    Text("Add")
                }
            }
        }
    }
    
    // Date picker
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = dueDate ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dueDate = datePickerState.selectedDateMillis
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DebtDetailSheet(
    debt: Debt,
    payments: List<com.theblankstate.epmanager.data.model.DebtPayment>,
    onDismiss: () -> Unit,
    onAddPayment: () -> Unit,
    onSettle: () -> Unit,
    onDelete: () -> Unit,
    onViewHistory: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val color = if (debt.type == DebtType.DEBT) Error else Green
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg)
                .padding(bottom = Spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (debt.type == DebtType.DEBT) 
                            Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                        contentDescription = null,
                        tint = color
                    )
                }
                Spacer(modifier = Modifier.width(Spacing.md))
                Column {
                    Text(
                        text = debt.personName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (debt.type == DebtType.DEBT) "You owe" else "Owes you",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Amount summary
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = color.copy(alpha = 0.1f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.md)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Remaining", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            formatCurrency(debt.remainingAmount),
                            fontWeight = FontWeight.Bold,
                            color = color
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Paid", style = MaterialTheme.typography.bodyMedium)
                        Text(formatCurrency(debt.paidAmount))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total", style = MaterialTheme.typography.bodyMedium)
                        Text(formatCurrency(debt.totalAmount))
                    }
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    LinearProgressIndicator(
                        progress = { debt.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape),
                        color = color
                    )
                    Text(
                        text = "${(debt.progress * 100).toInt()}% paid",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
            
            // Payment history
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Payment History",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(onClick = onViewHistory) {
                    Text("View All")
                }
            }
            
            if (payments.isNotEmpty()) {
                payments.forEach { payment ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = Spacing.xs),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = formatCurrency(payment.amount),
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                                    .format(Date(payment.paidAt)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (payment.note != null) {
                            Text(
                                text = payment.note,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    HorizontalDivider()
                }
            }
            
            Spacer(modifier = Modifier.height(Spacing.md))
            
            // Action buttons
            if (debt.remainingAmount > 0) {
                Button(
                    onClick = onAddPayment,
                    modifier = Modifier.fillMaxWidth(),
                    shape = ButtonShapePill
                ) {
                    Icon(Icons.Filled.Payment, null)
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Text(if (debt.type == DebtType.DEBT) "Record Payment" else "Record Receipt")
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                OutlinedButton(
                    onClick = onSettle,
                    modifier = Modifier.weight(1f),
                    shape = ButtonShapePill
                ) {
                    Text("Mark Settled")
                }
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    shape = ButtonShapePill,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Error
                    )
                ) {
                    Text("Delete")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddPaymentSheet(
    debt: Debt,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, note: String?) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg)
                .padding(bottom = Spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Text(
                text = if (debt.type == DebtType.DEBT) "Record Payment" else "Record Receipt",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "Remaining: ${formatCurrency(debt.remainingAmount)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Quick amounts
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                val quickAmounts = listOf(
                    debt.remainingAmount,
                    (debt.remainingAmount / 2).toLong().toDouble(),
                    (debt.remainingAmount / 4).toLong().toDouble()
                ).filter { it > 0 }
                
                items(quickAmounts.size) { index ->
                    val quickAmount = quickAmounts[index]
                    SuggestionChip(
                        onClick = { amount = quickAmount.toLong().toString() },
                        label = { Text(formatCurrency(quickAmount)) }
                    )
                }
            }
            
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Amount") },
                prefix = { Text("₹") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = InputFieldShape
            )
            
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                shape = InputFieldShape
            )
            
            Spacer(modifier = Modifier.height(Spacing.md))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = ButtonShapePill
                ) {
                    Text("Cancel")
                }
                
                Button(
                    onClick = {
                        val amountValue = amount.toDoubleOrNull()
                        if (amountValue != null && amountValue > 0) {
                            onConfirm(amountValue, note.takeIf { it.isNotBlank() })
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = amount.toDoubleOrNull()?.let { it > 0 && it <= debt.remainingAmount } == true,
                    shape = ButtonShapePill
                ) {
                    Text("Confirm")
                }
            }
        }
    }
}
