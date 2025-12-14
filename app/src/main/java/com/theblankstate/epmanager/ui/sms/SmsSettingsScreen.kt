package com.theblankstate.epmanager.ui.sms

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.theblankstate.epmanager.data.model.BankRegistry
import com.theblankstate.epmanager.data.model.SmsTemplate
import com.theblankstate.epmanager.sms.SmsPermissionManager
import com.theblankstate.epmanager.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsSettingsScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: SmsSettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    
    var showAddBankDialog by remember { mutableStateOf(false) }
    var showLearnFromSmsDialog by remember { mutableStateOf(false) }
    var selectedTemplate by remember { mutableStateOf<SmsTemplate?>(null) }
    
    val hasPermission = remember { SmsPermissionManager.hasAllPermissions(context) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SMS Auto-Detection") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddBankDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Bank")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            // Permission Status
            item {
                PermissionStatusCard(
                    hasPermission = hasPermission,
                    onRequestPermission = {
                        // This would need to be handled by the activity
                    }
                )
            }
            
            // Learn from SMS Card
            item {
                LearnFromSmsCard(
                    onClick = { showLearnFromSmsDialog = true }
                )
            }
            
            // Custom Banks Section
            item {
                Text(
                    text = "Custom Bank Templates",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = Spacing.md)
                )
            }
            
            if (uiState.customTemplates.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Spacing.lg),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(Spacing.sm))
                            Text(
                                text = "No custom banks added",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Add your bank if it's not auto-detected",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(uiState.customTemplates) { template ->
                    CustomBankCard(
                        template = template,
                        onClick = { selectedTemplate = template },
                        onDelete = { viewModel.deleteTemplate(template) }
                    )
                }
            }
            
            // Built-in Banks Section
            item {
                Text(
                    text = "Supported Banks (Built-in)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = Spacing.lg)
                )
                Text(
                    text = "These banks are automatically detected",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            items(BankRegistry.banks) { bank ->
                BuiltInBankCard(bank = bank)
            }
            
            // UPI Providers
            item {
                Text(
                    text = "UPI & Wallets (Built-in)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = Spacing.lg)
                )
            }
            
            items(BankRegistry.upiProviders) { provider ->
                BuiltInBankCard(bank = provider)
            }
            
            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(Spacing.huge))
            }
        }
    }
    
    // Add Bank Bottom Sheet
    if (showAddBankDialog) {
        AddCustomBankSheet(
            onDismiss = { showAddBankDialog = false },
            onConfirm = { bankName, senderIds ->
                viewModel.addCustomBank(bankName, senderIds)
                showAddBankDialog = false
            }
        )
    }
    
    // Learn from SMS Dialog
    if (showLearnFromSmsDialog) {
        LearnFromSmsDialog(
            onDismiss = { showLearnFromSmsDialog = false },
            onSubmit = { sms, senderId ->
                viewModel.learnFromSample(sms, senderId)
                showLearnFromSmsDialog = false
            },
            isLoading = uiState.isLearning
        )
    }
}

@Composable
fun PermissionStatusCard(
    hasPermission: Boolean,
    onRequestPermission: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (hasPermission) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (hasPermission) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (hasPermission) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.error
            )
            
            Spacer(modifier = Modifier.width(Spacing.md))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (hasPermission) "SMS Permission Granted" else "SMS Permission Required",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = if (hasPermission) 
                        "Bank transactions will be auto-detected" 
                    else 
                        "Enable to auto-track expenses from SMS",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            if (!hasPermission) {
                TextButton(onClick = onRequestPermission) {
                    Text("Enable")
                }
            }
        }
    }
}

@Composable
fun LearnFromSmsCard(
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(32.dp)
            )
            
            Spacer(modifier = Modifier.width(Spacing.md))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Teach with Sample SMS ✨",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Paste a bank SMS and AI will learn the format",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null
            )
        }
    }
}

@Composable
fun CustomBankCard(
    template: SmsTemplate,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.AccountBalance,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.width(Spacing.md))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = template.bankName,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "Sender IDs: ${template.senderIds}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (template.usageCount > 0) {
                    Text(
                        text = "Used ${template.usageCount} times",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun BuiltInBankCard(
    bank: BankRegistry.BankInfo
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = androidx.compose.ui.graphics.Color(bank.color),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = bank.code.take(2),
                        style = MaterialTheme.typography.labelSmall,
                        color = androidx.compose.ui.graphics.Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(Spacing.sm))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = bank.name,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = bank.senderPatterns.joinToString(", "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Supported",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCustomBankSheet(
    onDismiss: () -> Unit,
    onConfirm: (bankName: String, senderIds: String) -> Unit
) {
    var bankName by remember { mutableStateOf("") }
    var senderIds by remember { mutableStateOf("") }
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
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Add Custom Bank",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
            
            Spacer(modifier = Modifier.height(Spacing.sm))
            
            Text(
                text = "If your bank SMS is not auto-detected, add it here.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(Spacing.lg))
            
            Text(
                text = "Bank Name",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            OutlinedTextField(
                value = bankName,
                onValueChange = { bankName = it },
                placeholder = { Text("e.g., Kotak Mahindra Bank") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(Spacing.md))
            
            Text(
                text = "SMS Sender ID",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            
            // Help card explaining how to find sender ID
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Spacing.sm),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(Spacing.sm)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(modifier = Modifier.width(Spacing.xs))
                        Text(
                            text = "How to find Sender ID?",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text(
                        text = "Check your SMS sender name. It usually looks like:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "JD-KOTAKB-S, JX-KOTAKB-T, JK-KOTAKB-S",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "The common part \"KOTAKB\" is the sender ID you need!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(modifier = Modifier.height(Spacing.sm))
            OutlinedTextField(
                value = senderIds,
                onValueChange = { senderIds = it },
                placeholder = { Text("e.g., MYBANK, MY-BANK") },
                supportingText = {
                    Text("Comma-separated sender IDs from your bank SMS")
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(Spacing.xxl))
            
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
                    onClick = { onConfirm(bankName, senderIds) },
                    enabled = bankName.isNotBlank() && senderIds.isNotBlank(),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Add Bank")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearnFromSmsDialog(
    onDismiss: () -> Unit,
    onSubmit: (sms: String, senderId: String) -> Unit,
    isLoading: Boolean
) {
    var smsText by remember { mutableStateOf("") }
    var senderId by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text("Teach AI with SMS") 
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                Text(
                    text = "Paste a sample bank SMS and our AI will learn to detect similar messages automatically.",
                    style = MaterialTheme.typography.bodySmall
                )
                
                OutlinedTextField(
                    value = senderId,
                    onValueChange = { senderId = it },
                    label = { Text("Sender ID (from SMS)") },
                    placeholder = { Text("e.g., HDFCBK or VK-MYBANK") },
                    supportingText = {
                        Text("Check your SMS app for the sender name")
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = smsText,
                    onValueChange = { smsText = it },
                    label = { Text("SMS Content") },
                    placeholder = { 
                        Text("Paste your bank SMS here...\n\nExample: Rs.1,234.00 debited from A/c **5678 on 11-12-24") 
                    },
                    minLines = 4,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (isLoading) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(Spacing.sm))
                        Text("AI is learning...", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(smsText, senderId) },
                enabled = smsText.isNotBlank() && senderId.isNotBlank() && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Learn Pattern")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("Cancel")
            }
        }
    )
}
