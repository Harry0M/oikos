package com.theblankstate.epmanager.ui.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.theblankstate.epmanager.data.model.Account
import com.theblankstate.epmanager.data.model.AccountType
import com.theblankstate.epmanager.data.model.BankRegistry
import com.theblankstate.epmanager.ui.components.formatCurrency
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
    viewModel: AccountsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
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
                                text = formatCurrency(uiState.totalBalance),
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
                } else {
                    items(
                        items = uiState.accounts,
                        key = { it.id }
                    ) { account ->
                        AccountItem(
                            account = account,
                            onEdit = { viewModel.showEditDialog(account) },
                            onDelete = { viewModel.deleteAccount(account) },
                            onLink = { viewModel.showLinkDialog(account) },
                            onUnlink = { viewModel.unlinkAccount(account) }
                        )
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(Spacing.huge))
                }
            }
        }
    }
    
    // Add/Edit Dialog
    if (uiState.showAddDialog) {
        AddEditAccountDialog(
            existingAccount = uiState.editingAccount,
            bankSuggestions = uiState.bankSuggestions,
            onDismiss = { viewModel.hideDialog() },
            onConfirm = { name, type, icon, color, balance ->
                viewModel.saveAccount(name, type, icon, color, balance)
            },
            onConfirmLinked = { name, bankCode, accountNumber, type, balance ->
                viewModel.createLinkedAccount(name, bankCode, accountNumber, type, balance)
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
private fun AccountItem(
    account: Account,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onLink: () -> Unit,
    onUnlink: () -> Unit
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
                        if (account.isLinked) {
                            AssistChip(
                                onClick = {},
                                label = { 
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Link,
                                            contentDescription = null,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            "Linked",
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
                    }
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatCurrency(account.balance),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (account.balance >= 0) Success else Error
                    )
                }
            }
            
            // Bank linking info and actions
            if (account.type != AccountType.CASH) {
                Divider(modifier = Modifier.padding(horizontal = Spacing.md))
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (account.isLinked) {
                        // Show linked bank info
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AccountBalance,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    text = account.bankCode ?: "Bank",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (account.accountNumber != null) {
                                    Text(
                                        text = "****${account.accountNumber}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        
                        // Unlink button
                        TextButton(
                            onClick = onUnlink,
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
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
                    } else {
                        // Show link prompt
                        Text(
                            text = "Not linked for SMS auto-detection",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        // Link button
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
                    }
                }
            }
            
            // Delete button row for non-default accounts
            if (!account.isDefault) {
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
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Account") },
            text = { Text("Are you sure you want to delete '${account.name}'? This will not delete transactions associated with this account.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditAccountDialog(
    existingAccount: Account?,
    bankSuggestions: List<BankRegistry.BankInfo>,
    onDismiss: () -> Unit,
    onConfirm: (name: String, type: AccountType, icon: String, color: Long, balance: Double) -> Unit,
    onConfirmLinked: (name: String, bankCode: String, accountNumber: String?, type: AccountType, balance: Double) -> Unit
) {
    var name by remember { mutableStateOf(existingAccount?.name ?: "") }
    var selectedType by remember { mutableStateOf(existingAccount?.type ?: AccountType.BANK) }
    var selectedIcon by remember { mutableStateOf(existingAccount?.icon ?: "AccountBalance") }
    var selectedColor by remember { mutableStateOf(existingAccount?.color ?: availableColors[0]) }
    var balance by remember { mutableStateOf(existingAccount?.balance?.toString() ?: "0") }
    var expandedType by remember { mutableStateOf(false) }
    
    // New fields for linking
    var createAsLinked by remember { mutableStateOf(false) }
    var selectedBank by remember { mutableStateOf<BankRegistry.BankInfo?>(null) }
    var accountNumber by remember { mutableStateOf("") }
    var expandedBank by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
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
            Text(
                text = if (existingAccount != null) "Edit Account" else "Add Account",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                // Name
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name") },
                        placeholder = { Text("Account name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                // Type Dropdown
                item {
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
                }
                
                // Balance
                item {
                    OutlinedTextField(
                        value = balance,
                        onValueChange = { 
                            balance = it.filter { c -> c.isDigit() || c == '.' || c == '-' }
                        },
                        label = { Text("Balance") },
                        prefix = { Text("₹") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                // Link to Bank option (only for new accounts and non-cash)
                if (existingAccount == null && selectedType != AccountType.CASH) {
                    item {
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
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Link for SMS Auto-Detection",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "Automatically track transactions from bank SMS",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Switch(
                                        checked = createAsLinked,
                                        onCheckedChange = { createAsLinked = it }
                                    )
                                }
                                
                                if (createAsLinked) {
                                    Spacer(modifier = Modifier.height(Spacing.md))
                                    
                                    // Bank Selector
                                    ExposedDropdownMenuBox(
                                        expanded = expandedBank,
                                        onExpandedChange = { expandedBank = it }
                                    ) {
                                        OutlinedTextField(
                                            value = selectedBank?.name ?: "",
                                            onValueChange = { searchQuery = it },
                                            label = { Text("Select Bank") },
                                            placeholder = { Text("Search banks...") },
                                            trailingIcon = { 
                                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedBank) 
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Filled.AccountBalance,
                                                    contentDescription = null
                                                )
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .menuAnchor()
                                        )
                                        
                                        ExposedDropdownMenu(
                                            expanded = expandedBank,
                                            onDismissRequest = { expandedBank = false }
                                        ) {
                                            filteredBanks.take(10).forEach { bank ->
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
                                                                Text(bank.name)
                                                                Text(
                                                                    text = bank.code,
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                )
                                                            }
                                                        }
                                                    },
                                                    onClick = {
                                                        selectedBank = bank
                                                        selectedColor = bank.color
                                                        if (name.isBlank()) {
                                                            name = bank.name
                                                        }
                                                        expandedBank = false
                                                        searchQuery = ""
                                                    }
                                                )
                                            }
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(Spacing.sm))
                                    
                                    // Account Number (Last 4 digits)
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
                                }
                            }
                        }
                    }
                }
                
                // Icon Picker
                item {
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
                }
                
                // Color Picker
                item {
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
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val balanceValue = balance.toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank()) {
                        if (createAsLinked && selectedBank != null) {
                            onConfirmLinked(
                                name, 
                                selectedBank!!.code, 
                                accountNumber.takeIf { it.isNotBlank() },
                                selectedType,
                                balanceValue
                            )
                        } else {
                            onConfirm(name, selectedType, selectedIcon, selectedColor, balanceValue)
                        }
                    }
                },
                enabled = name.isNotBlank() && (!createAsLinked || selectedBank != null)
            ) {
                Text(if (existingAccount != null) "Save" else "Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Dialog to link an existing account to a bank for SMS auto-detection
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LinkAccountDialog(
    account: Account,
    bankSuggestions: List<BankRegistry.BankInfo>,
    onDismiss: () -> Unit,
    onConfirm: (bankCode: String, accountNumber: String?, senderIds: List<String>) -> Unit
) {
    var selectedBank by remember { mutableStateOf<BankRegistry.BankInfo?>(null) }
    var accountNumber by remember { mutableStateOf(account.accountNumber ?: "") }
    var expandedBank by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
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
                    text = "Link Account",
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = account.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                            text = "Link this account to automatically detect and assign transactions from bank SMS messages.",
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
                        filteredBanks.take(10).forEach { bank ->
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
                                            Text(bank.name)
                                            Text(
                                                text = "Sender IDs: ${bank.senderPatterns.joinToString(", ")}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    selectedBank = bank
                                    expandedBank = false
                                    searchQuery = ""
                                }
                            )
                        }
                    }
                }
                
                // Account Number (Last 4 digits)
                OutlinedTextField(
                    value = accountNumber,
                    onValueChange = { 
                        if (it.length <= 4 && it.all { c -> c.isDigit() }) {
                            accountNumber = it
                        }
                    },
                    label = { Text("Last 4 Digits") },
                    placeholder = { Text("e.g., 1234") },
                    supportingText = { 
                        Text("Enter last 4 digits of your account/card number to differentiate multiple accounts") 
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Preview of what will be matched
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
                                text = "SMS Matching Preview",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(Spacing.xs))
                            Text(
                                text = "Will match SMS from: ${selectedBank!!.senderPatterns.joinToString(", ")}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (accountNumber.isNotBlank()) {
                                Text(
                                    text = "With account ending: ****$accountNumber",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
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
private fun getAccountIcon(iconName: String) = when (iconName) {
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
