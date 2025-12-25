package com.theblankstate.epmanager.ui.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.theblankstate.epmanager.data.model.Account
import com.theblankstate.epmanager.data.model.AccountType
import com.theblankstate.epmanager.ui.components.formatAmount
import com.theblankstate.epmanager.ui.theme.*

// Available icons for account selection
private val availableIcons = listOf(
    "Money", "AccountBalance", "PhoneAndroid", "CreditCard", "Wallet",
    "Savings", "PiggyBank", "Business", "Store", "AttachMoney"
)

// Available colors for account selection
private val availableColors = listOf(
    0xFF22C55E, 0xFF3B82F6, 0xFF8B5CF6, 0xFFF59E0B, 0xFFEF4444,
    0xFF14B8A6, 0xFFF97316, 0xFF06B6D4, 0xFFEC4899, 0xFF84CC16
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    onNavigateBack: () -> Unit,
    viewModel: AccountsViewModel = hiltViewModel(),
    currencyViewModel: com.theblankstate.epmanager.util.CurrencyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currencySymbol by currencyViewModel.currencySymbol.collectAsState(initial = "₹")
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Accounts",
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
                onClick = { viewModel.showAddDialog() },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Add Account"
                )
            }
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                // Total Balance Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Spacing.lg),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Total Balance",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(Spacing.xs))
                            Text(
                                text = formatAmount(uiState.totalBalance, currencySymbol),
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "${uiState.accounts.size} account${if (uiState.accounts.size != 1) "s" else ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
                
                // Bank-Linked Accounts Section (SMS Auto-Detection)
                if (uiState.linkedAccounts.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Bank-Linked Accounts",
                            subtitle = "SMS auto-detection enabled",
                            icon = Icons.Filled.Link
                        )
                    }
                    
                    items(
                        items = uiState.linkedAccounts,
                        key = { it.id }
                    ) { account ->
                        LinkedAccountItem(
                            account = account,
                            onEdit = { viewModel.showEditDialog(account) },
                            onDelete = { viewModel.deleteAccount(account) },
                            onUnlink = { viewModel.unlinkAccount(account) },
                            currencySymbol = currencySymbol
                        )
                    }
                }
                
                // Payment Accounts Section (No SMS)
                if (uiState.unlinkedAccounts.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Payment Accounts",
                            subtitle = "Manual tracking only",
                            icon = Icons.Filled.AccountBalanceWallet
                        )
                    }
                    
                    items(
                        items = uiState.unlinkedAccounts,
                        key = { it.id }
                    ) { account ->
                        UnlinkedAccountItem(
                            account = account,
                            onEdit = { viewModel.showEditDialog(account) },
                            onDelete = { viewModel.deleteAccount(account) },
                            onLink = { viewModel.showLinkDialog(account) },
                            currencySymbol = currencySymbol
                        )
                    }
                }
                
                // Empty state
                if (uiState.accounts.isEmpty()) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Spacing.xxl),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "💳",
                                    style = MaterialTheme.typography.displayLarge
                                )
                                Spacer(modifier = Modifier.height(Spacing.md))
                                Text(
                                    text = "No Accounts",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Tap + to add a payment account",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                
                // Help Card
                item {
                    HelpCard()
                }
                
                item {
                    Spacer(modifier = Modifier.height(Spacing.huge))
                }
            }
        }
    }
    
    // Add/Edit Sheet (Bottom Sheet)
    if (uiState.showAddDialog) {
        AddEditAccountSheet(
            existingAccount = uiState.editingAccount,
            bankSuggestions = uiState.bankSuggestions,
            onDismiss = { viewModel.hideDialog() },
            onConfirm = { name, type, icon, color, balance ->
                viewModel.saveAccount(name, type, icon, color, balance)
            },
            onConfirmLinked = { name, bankSuggestion, accountNumber, type, balance ->
                viewModel.createLinkedAccount(name, bankSuggestion, accountNumber, type, balance)
            }
        )
    }
    
    // Link Account Dialog
    if (uiState.showLinkDialog && uiState.linkingAccount != null) {
        LinkAccountDialog(
            account = uiState.linkingAccount!!,
            bankSuggestions = uiState.bankSuggestions,
            onDismiss = { viewModel.hideLinkDialog() },
            onConfirm = { bankCode, accountNumber, senderIds ->
                viewModel.linkAccount(
                    uiState.linkingAccount!!.id,
                    bankCode,
                    accountNumber,
                    senderIds
                )
            }
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HelpCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(Spacing.md)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "How SMS Auto-Detection Works",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.height(Spacing.sm))
            Text(
                text = "• Bank-Linked accounts automatically track transactions from bank SMS\n" +
                       "• Only SMS from linked banks are parsed - no unwanted entries\n" +
                       "• Link your account to a bank using the 'Link Bank' button\n" +
                       "• Custom SMS templates from Settings appear in the bank selector",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LinkedAccountItem(
    account: Account,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onUnlink: () -> Unit,
    currencySymbol: String
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp, 
            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        )
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon Circle with bank branding
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(account.color).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getAccountIcon(account.icon),
                        contentDescription = null,
                        tint = Color(account.color),
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(Spacing.md))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = account.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                    ) {
                        // Bank Badge
                        AssistChip(
                            onClick = {},
                            label = { 
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.AccountBalance,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        account.bankCode ?: "Bank",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                labelColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.height(24.dp)
                        )
                        
                        // Linked Badge
                        AssistChip(
                            onClick = {},
                            label = { 
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Sms,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        "Auto",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = Success.copy(alpha = 0.1f),
                                labelColor = Success
                            ),
                            modifier = Modifier.height(24.dp)
                        )
                    }
                    
                    // Account number hint
                    if (account.accountNumber != null) {
                        Text(
                            text = "****${account.accountNumber}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatAmount(account.balance, currencySymbol),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (account.balance >= 0) Success else Error
                    )
                }
            }
            
            // Actions Row
            HorizontalDivider(modifier = Modifier.padding(horizontal = Spacing.md))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.md, vertical = Spacing.xs),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onUnlink,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Warning
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.LinkOff,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Unlink", style = MaterialTheme.typography.labelMedium)
                }
                
                if (!account.isDefault) {
                    TextButton(
                        onClick = { showDeleteDialog = true },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
    
    if (showDeleteDialog) {
        DeleteConfirmDialog(
            accountName = account.name,
            onConfirm = {
                onDelete()
                showDeleteDialog = false
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}

@Composable
private fun UnlinkedAccountItem(
    account: Account,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onLink: () -> Unit,
    currencySymbol: String
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon Circle
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(account.color).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getAccountIcon(account.icon),
                        contentDescription = null,
                        tint = Color(account.color),
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(Spacing.md))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = account.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                    ) {
                        AssistChip(
                            onClick = {},
                            label = { 
                                Text(
                                    account.type.name.replace("_", " "),
                                    style = MaterialTheme.typography.labelSmall
                                ) 
                            },
                            modifier = Modifier.height(24.dp)
                        )
                        if (account.isDefault) {
                            AssistChip(
                                onClick = {},
                                label = { 
                                    Text(
                                        "Default",
                                        style = MaterialTheme.typography.labelSmall
                                    ) 
                                },
                                modifier = Modifier.height(24.dp)
                            )
                        }
                    }
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatAmount(account.balance, currencySymbol),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (account.balance >= 0) Success else Error
                    )
                }
            }
            
            // Link prompt for non-cash accounts
            if (account.type != AccountType.CASH) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = Spacing.md))
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.md, vertical = Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Enable SMS auto-detection →",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Row {
                        TextButton(
                            onClick = onLink,
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Link,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Link Bank", style = MaterialTheme.typography.labelMedium)
                        }
                        
                        if (!account.isDefault) {
                            TextButton(
                                onClick = { showDeleteDialog = true },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = "Delete",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            } else if (!account.isDefault) {
                // Cash account actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.md, vertical = Spacing.xs),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = { showDeleteDialog = true },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
    
    if (showDeleteDialog) {
        DeleteConfirmDialog(
            accountName = account.name,
            onConfirm = {
                onDelete()
                showDeleteDialog = false
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}

@Composable
private fun DeleteConfirmDialog(
    accountName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Account") },
        text = { Text("Are you sure you want to delete '$accountName'? This will not delete transactions associated with this account.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditAccountSheet(
    existingAccount: Account?,
    bankSuggestions: List<BankSuggestion>,
    onDismiss: () -> Unit,
    onConfirm: (name: String, type: AccountType, icon: String, color: Long, balance: Double) -> Unit,
    onConfirmLinked: (name: String, bankSuggestion: BankSuggestion, accountNumber: String?, type: AccountType, balance: Double) -> Unit
) {
    var name by remember { mutableStateOf(existingAccount?.name ?: "") }
    var selectedType by remember { mutableStateOf(existingAccount?.type ?: AccountType.BANK) }
    var selectedIcon by remember { mutableStateOf(existingAccount?.icon ?: "AccountBalance") }
    var selectedColor by remember { mutableStateOf(existingAccount?.color ?: availableColors[0]) }
    var balance by remember { mutableStateOf(existingAccount?.balance?.toString() ?: "0") }
    var expandedType by remember { mutableStateOf(false) }
    
    // Linking fields
    var enableSmsDetection by remember { mutableStateOf(false) }
    var selectedBank by remember { mutableStateOf<BankSuggestion?>(null) }
    var accountNumber by remember { mutableStateOf("") }
    var expandedBank by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    // Filter and group banks
    val registryBanks = bankSuggestions.filter { !it.isCustom }
    val customBanks = bankSuggestions.filter { it.isCustom }
    
    val filteredBanks = remember(searchQuery, bankSuggestions) {
        if (searchQuery.isBlank()) bankSuggestions
        else bankSuggestions.filter { 
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.code.contains(searchQuery, ignoreCase = true)
        }
    }
    
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
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (existingAccount != null) "Edit Account" else "Add Account",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close")
                }
            }
            
            Spacer(modifier = Modifier.height(Spacing.lg))
            
            // Name
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Account Name *") },
                placeholder = { Text("e.g., HDFC Savings") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(Spacing.md))
            
            // Type Dropdown
            ExposedDropdownMenuBox(
                expanded = expandedType,
                onExpandedChange = { expandedType = it }
            ) {
                OutlinedTextField(
                    value = selectedType.name.replace("_", " "),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedType) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                
                ExposedDropdownMenu(
                    expanded = expandedType,
                    onDismissRequest = { expandedType = false }
                ) {
                    AccountType.entries.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.name.replace("_", " ")) },
                            onClick = {
                                selectedType = type
                                expandedType = false
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(Spacing.md))
            
            // Balance
            OutlinedTextField(
                value = balance,
                onValueChange = { 
                    balance = it.filter { c -> c.isDigit() || c == '.' || c == '-' }
                },
                label = { Text("Current Balance") },
                prefix = { Text("₹") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(Spacing.md))
            
            // Icon Picker
            Text(
                text = "Icon",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(Spacing.xs))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                items(availableIcons) { iconName ->
                    val isSelected = iconName == selectedIcon
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) 
                                    MaterialTheme.colorScheme.primaryContainer
                                else 
                                    MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable { selectedIcon = iconName },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getAccountIcon(iconName),
                            contentDescription = iconName,
                            tint = if (isSelected) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(Spacing.md))
            
            // Color Picker
            Text(
                text = "Color",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(Spacing.xs))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                items(availableColors) { color ->
                    val isSelected = color == selectedColor
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(color))
                            .clickable { selectedColor = color },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "Selected",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(Spacing.md))
            
            // SMS Detection toggle (for new non-cash accounts)
            if (existingAccount == null && selectedType != AccountType.CASH) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (enableSmsDetection) 
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        else 
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(Spacing.md)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Sms,
                                        contentDescription = null,
                                        tint = if (enableSmsDetection) 
                                            MaterialTheme.colorScheme.primary 
                                        else 
                                            MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Enable SMS Auto-Detection",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Text(
                                    text = "Automatically track transactions from bank SMS",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = enableSmsDetection,
                                onCheckedChange = { enableSmsDetection = it }
                            )
                        }
                        
                        if (enableSmsDetection) {
                            Spacer(modifier = Modifier.height(Spacing.md))
                            
                            // Bank Selector - Opens Bottom Sheet
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { expandedBank = true }
                            ) {
                                OutlinedTextField(
                                    value = selectedBank?.name ?: "",
                                    onValueChange = {},
                                    readOnly = true,
                                    enabled = false,
                                    label = { Text("Select Bank *") },
                                    placeholder = { Text("Tap to select bank...") },
                                    trailingIcon = { 
                                        Icon(Icons.Filled.ArrowDropDown, contentDescription = null) 
                                    },
                                    leadingIcon = {
                                        if (selectedBank != null) {
                                            Box(
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(selectedBank!!.color))
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Filled.AccountBalance,
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(Spacing.sm))
                            
                            // Account Number (optional)
                            OutlinedTextField(
                                value = accountNumber,
                                onValueChange = { 
                                    if (it.length <= 4 && it.all { c -> c.isDigit() }) {
                                        accountNumber = it
                                    }
                                },
                                label = { Text("Last 4 Digits (Optional)") },
                                placeholder = { Text("e.g., 1234") },
                                supportingText = { 
                                    Text("Helps match SMS to this specific account") 
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(Spacing.xxl))
            
            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                
                Button(
                    onClick = {
                        val balanceValue = balance.toDoubleOrNull() ?: 0.0
                        if (name.isNotBlank()) {
                            if (enableSmsDetection && selectedBank != null) {
                                onConfirmLinked(
                                    name, 
                                    selectedBank!!, 
                                    accountNumber.takeIf { it.isNotBlank() },
                                    selectedType,
                                    balanceValue
                                )
                            } else {
                                onConfirm(name, selectedType, selectedIcon, selectedColor, balanceValue)
                            }
                        }
                    },
                    enabled = name.isNotBlank() && (!enableSmsDetection || selectedBank != null),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (existingAccount != null) "Save" else "Add")
                }
            }
        }
    }
    
    // Bank Selection Bottom Sheet
    if (expandedBank) {
        ModalBottomSheet(
            onDismissRequest = { expandedBank = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg)
                    .padding(bottom = Spacing.xxl)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Select Bank",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { expandedBank = false }) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }
                
                Spacer(modifier = Modifier.height(Spacing.md))
                
                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search banks...") },
                    leadingIcon = {
                        Icon(Icons.Filled.Search, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(Spacing.md))
                
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    // Custom templates first (if any)
                    if (customBanks.isNotEmpty()) {
                        item {
                            Text(
                                text = "Your Custom Templates",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = Spacing.sm)
                            )
                        }
                        items(customBanks, key = { it.code }) { bank ->
                            BankListItem(
                                bank = bank,
                                isCustom = true,
                                isSelected = selectedBank?.code == bank.code,
                                onClick = {
                                    selectedBank = bank
                                    selectedColor = bank.color
                                    if (name.isBlank()) name = bank.name
                                    expandedBank = false
                                }
                            )
                        }
                        item {
                            HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.sm))
                        }
                    }
                    
                    // Popular banks
                    item {
                        Text(
                            text = "Popular Banks",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = Spacing.sm)
                        )
                    }
                    
                    items(filteredBanks.filter { !it.isCustom }, key = { it.code }) { bank ->
                        BankListItem(
                            bank = bank,
                            isCustom = false,
                            isSelected = selectedBank?.code == bank.code,
                            onClick = {
                                selectedBank = bank
                                selectedColor = bank.color
                                if (name.isBlank()) name = bank.name
                                expandedBank = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BankDropdownItem(
    bank: BankSuggestion,
    isCustom: Boolean,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = { 
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(bank.color))
                )
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(bank.name)
                        if (isCustom) {
                            Text(
                                text = "Custom",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Text(
                        text = bank.senderPatterns.take(3).joinToString(", "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        onClick = onClick
    )
}

@Composable
fun BankListItem(
    bank: BankSuggestion,
    isCustom: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(bank.color)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = bank.code.take(2),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    Text(
                        text = bank.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    if (isCustom) {
                        AssistChip(
                            onClick = {},
                            label = { Text("Custom", style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.height(20.dp),
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
                Text(
                    text = bank.senderPatterns.take(3).joinToString(", "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LinkAccountDialog(
    account: Account,
    bankSuggestions: List<BankSuggestion>,
    onDismiss: () -> Unit,
    onConfirm: (bankCode: String, accountNumber: String?, senderIds: List<String>) -> Unit
) {
    var selectedBank by remember { mutableStateOf<BankSuggestion?>(null) }
    var accountNumber by remember { mutableStateOf(account.accountNumber ?: "") }
    var expandedBank by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    val customBanks = bankSuggestions.filter { it.isCustom }
    
    val filteredBanks = remember(searchQuery, bankSuggestions) {
        if (searchQuery.isBlank()) bankSuggestions
        else bankSuggestions.filter { 
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.code.contains(searchQuery, ignoreCase = true)
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Link Bank for SMS Auto-Detection",
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = account.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                // Info card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(Spacing.md),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "After linking, SMS from this bank will automatically create transactions in this account.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                
                // Bank Selector
                ExposedDropdownMenuBox(
                    expanded = expandedBank,
                    onExpandedChange = { expandedBank = it }
                ) {
                    OutlinedTextField(
                        value = selectedBank?.name ?: searchQuery,
                        onValueChange = { 
                            searchQuery = it
                            selectedBank = null
                        },
                        label = { Text("Select Bank *") },
                        placeholder = { Text("Search banks...") },
                        trailingIcon = { 
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedBank) 
                        },
                        leadingIcon = {
                            if (selectedBank != null) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Color(selectedBank!!.color))
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.AccountBalance,
                                    contentDescription = null
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = expandedBank,
                        onDismissRequest = { expandedBank = false }
                    ) {
                        // Custom templates first
                        if (customBanks.isNotEmpty()) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Your Custom Templates",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                },
                                onClick = {},
                                enabled = false
                            )
                            customBanks.forEach { bank ->
                                BankDropdownItem(
                                    bank = bank,
                                    isCustom = true,
                                    onClick = {
                                        selectedBank = bank
                                        expandedBank = false
                                        searchQuery = ""
                                    }
                                )
                            }
                            HorizontalDivider()
                        }
                        
                        filteredBanks.filter { !it.isCustom }.take(10).forEach { bank ->
                            BankDropdownItem(
                                bank = bank,
                                isCustom = false,
                                onClick = {
                                    selectedBank = bank
                                    expandedBank = false
                                    searchQuery = ""
                                }
                            )
                        }
                    }
                }
                
                // Account Number
                OutlinedTextField(
                    value = accountNumber,
                    onValueChange = { 
                        if (it.length <= 4 && it.all { c -> c.isDigit() }) {
                            accountNumber = it
                        }
                    },
                    label = { Text("Last 4 Digits (Optional)") },
                    placeholder = { Text("e.g., 1234") },
                    supportingText = { 
                        Text("Helps differentiate multiple accounts from same bank") 
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Preview
                if (selectedBank != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(Spacing.md)
                        ) {
                            Text(
                                text = "✓ SMS from these senders will be tracked:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                                color = Success
                            )
                            Text(
                                text = selectedBank!!.senderPatterns.joinToString(", "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (selectedBank != null) {
                        onConfirm(
                            selectedBank!!.code,
                            accountNumber.takeIf { it.isNotBlank() },
                            selectedBank!!.senderPatterns
                        )
                    }
                },
                enabled = selectedBank != null
            ) {
                Text("Link")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Helper function to get icon from name
@Composable
fun getAccountIcon(iconName: String) = when (iconName) {
    "Money" -> Icons.Filled.Money
    "AccountBalance" -> Icons.Filled.AccountBalance
    "PhoneAndroid" -> Icons.Filled.PhoneAndroid
    "CreditCard" -> Icons.Filled.CreditCard
    "Wallet" -> Icons.Filled.Wallet
    "Savings" -> Icons.Filled.Savings
    "PiggyBank" -> Icons.Filled.Savings
    "Business" -> Icons.Filled.Business
    "Store" -> Icons.Filled.Store
    "AttachMoney" -> Icons.Filled.AttachMoney
    else -> Icons.Filled.AccountBalance
}
