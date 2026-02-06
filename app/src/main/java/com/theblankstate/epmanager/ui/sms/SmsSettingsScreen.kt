package com.theblankstate.epmanager.ui.sms

import android.app.Activity
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
import com.theblankstate.epmanager.data.model.AvailableBank
import com.theblankstate.epmanager.sms.BankDiscoveryResult
import com.theblankstate.epmanager.sms.DiscoveredBank
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
    var showScanDialog by remember { mutableStateOf(false) }
    var showScanResult by remember { mutableStateOf(false) }
    var selectedTemplate by remember { mutableStateOf<SmsTemplate?>(null) }
    var selectedAvailableBank by remember { mutableStateOf<AvailableBank?>(null) }
    
    val hasPermission = remember { SmsPermissionManager.hasAllPermissions(context) }
    
    // Auto-scan banks only on first install or once per week
    LaunchedEffect(hasPermission) {
        if (hasPermission && uiState.discoveryResult == null && !uiState.isDiscovering && viewModel.shouldAutoScan()) {
            viewModel.discoverBanks()
        }
    }
    
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
            if (!hasPermission) {
                item {
                    PermissionStatusCard(
                        hasPermission = hasPermission,
                        onRequestPermission = {
                            (context as? Activity)?.let { SmsPermissionManager.requestPermissions(it) }
                        }
                    )
                }
            }
            
            // Discover Banks Card - Primary action for new users
            if (hasPermission) {
                item {
                    DiscoverBanksCard(
                        onClick = { viewModel.discoverBanks() },
                        isDiscovering = uiState.isDiscovering,
                        hasDiscoveryResult = uiState.discoveryResult != null
                    )
                }
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
                            containerColor = androidx.compose.ui.graphics.Color.Transparent
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
            
            
            // Your Banks Section - shows discovered banks after scan
            val discoveryResult = uiState.discoveryResult
            if (discoveryResult != null && discoveryResult.isComplete) {
                // Header for Your Banks
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = Spacing.lg),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "🏦 Your Banks",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${discoveryResult.detectedBanks.size} banks found in your SMS",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        // Re-scan button
                        TextButton(onClick = { viewModel.discoverBanks() }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Rescan")
                        }
                    }
                }
                
                // Show detected banks (from registry)
                items(discoveryResult.detectedBanks) { bank ->
                    // Find matching available bank by code for edit/delete
                    val matchingAvailableBank = uiState.availableBanks.find { 
                        it.bankCode.equals(bank.bankInfo?.code ?: bank.bankName.uppercase(), ignoreCase = true) 
                    }
                    
                    YourBankCard(
                        bank = bank,
                        availableBank = matchingAvailableBank,
                        onAddNewIds = {
                            if (bank.hasNewSenderIds) {
                                viewModel.addCustomBank(bank.bankName, bank.newSenderIds.joinToString(","))
                            }
                        },
                        onEdit = { availableBank ->
                            // Set available bank for editing
                            selectedAvailableBank = availableBank
                        },
                        onDelete = { availableBank ->
                            viewModel.deleteAvailableBank(availableBank.id)
                        }
                    )
                }
                
                // Show unknown senders section if any
                if (discoveryResult.unknownSenders.isNotEmpty()) {
                    item {
                        Text(
                            text = "📋 Other Financial SMS",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = Spacing.lg)
                        )
                        Text(
                            text = "Add to new or existing bank:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    items(discoveryResult.unknownSenders) { sender ->
                        UnknownSenderMainScreenItem(
                            bank = sender,
                            customTemplates = uiState.customTemplates,
                            detectedBanks = discoveryResult.detectedBanks,
                            onAddAsNew = {
                                viewModel.addCustomBank(sender.bankName, sender.senderIds.joinToString(","))
                            },
                            onAddToExistingTemplate = { templateId ->
                                val senderId = sender.senderIds.firstOrNull() ?: return@UnknownSenderMainScreenItem
                                viewModel.addSenderToExistingBank(senderId, templateId)
                            },
                            onAddToDetectedBank = { detectedBank ->
                                // Add sender to the detected bank by adding it as a custom bank entry
                                val senderToAdd = sender.senderIds.firstOrNull() ?: return@UnknownSenderMainScreenItem
                                // Find if there's already an available bank for this detected bank
                                val existingAvailableBank = uiState.availableBanks.find {
                                    it.bankCode.equals(detectedBank.bankInfo?.code ?: detectedBank.bankName.uppercase(), ignoreCase = true)
                                }
                                if (existingAvailableBank != null) {
                                    viewModel.addSenderToAvailableBank(senderToAdd, existingAvailableBank.id)
                                } else {
                                    // Add as new entry to the detected bank
                                    val existingSenderIds = detectedBank.senderIds.joinToString(",")
                                    viewModel.addCustomBank(detectedBank.bankName, "$existingSenderIds,$senderToAdd")
                                }
                            }
                        )
                    }
                }
            }
            
            
            // Note: Built-in banks are now hidden
            // Banks are auto-discovered from user's SMS and shown in the results sheet
            
            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(Spacing.huge))
            }
        }
    }
    
    // Add Bank Bottom Sheet
    if (showAddBankDialog) {
        // Get uncategorized sender IDs from discovery results (Other Financial SMS)
        val uncategorizedSenderIds = uiState.discoveryResult?.unknownSenders
            ?.flatMap { it.senderIds }
            ?: emptyList()
        
        AddCustomBankSheet(
            uncategorizedSenders = uncategorizedSenderIds,
            onDismiss = { showAddBankDialog = false },
            onConfirm = { bankName, senderIds ->
                viewModel.addCustomBank(bankName, senderIds)
                showAddBankDialog = false
            }
        )
    }
    
    // Edit SmsTemplate Bank Bottom Sheet
    if (selectedTemplate != null) {
        // Get uncategorized sender IDs from discovery results
        val uncategorizedSenderIds = uiState.discoveryResult?.unknownSenders
            ?.flatMap { it.senderIds }
            ?: emptyList()
        
        EditCustomBankSheet(
            template = selectedTemplate!!,
            uncategorizedSenders = uncategorizedSenderIds,
            onDismiss = { selectedTemplate = null },
            onSave = { templateId, bankName, senderIds ->
                viewModel.updateCustomBank(templateId, bankName, senderIds)
                selectedTemplate = null
            }
        )
    }
    
    // Edit Available Bank Bottom Sheet (for scanned banks)
    if (selectedAvailableBank != null) {
        val uncategorizedSenderIds = uiState.discoveryResult?.unknownSenders
            ?.flatMap { it.senderIds }
            ?: emptyList()
        
        // Reuse the same sheet component with the available bank data
        EditCustomBankSheet(
            template = SmsTemplate(
                id = selectedAvailableBank!!.id,
                bankName = selectedAvailableBank!!.bankName,
                senderIds = selectedAvailableBank!!.senderIds
            ),
            uncategorizedSenders = uncategorizedSenderIds,
            onDismiss = { selectedAvailableBank = null },
            onSave = { bankId, bankName, senderIds ->
                // Call updateAvailableBank instead of updateCustomBank
                viewModel.updateAvailableBank(bankId, bankName, senderIds)
                selectedAvailableBank = null
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

    if (showScanDialog) {
        ScanOptionsSheet(
            onDismiss = { showScanDialog = false },
            onScan = { option, date ->
                viewModel.scanSms(option, date)
                showScanDialog = false
            }
        )
    }
    
    // Scan Progress Dialog
    if (uiState.isScanning) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Scanning SMS...") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Scanned:", style = MaterialTheme.typography.bodyMedium)
                        Text("${uiState.scanProgress?.scanned ?: 0}", fontWeight = FontWeight.Bold)
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Transactions Found:", style = MaterialTheme.typography.bodyMedium)
                        Text("${uiState.scanProgress?.found ?: 0}", fontWeight = FontWeight.Bold)
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("New Added:", style = MaterialTheme.typography.bodyMedium)
                        Text("${uiState.scanProgress?.new ?: 0}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {}
        )
    }
    
    // Scan Result Dialog (when complete)
    LaunchedEffect(uiState.scanProgress?.isComplete) {
        if (uiState.scanProgress?.isComplete == true) {
            showScanResult = true
        }
    }
    
    if (showScanResult && !uiState.isScanning) {
        AlertDialog(
            onDismissRequest = { 
                showScanResult = false
                viewModel.clearScanProgress()
            },
            icon = { Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Scan Complete") },
            text = {
                Text(
                    "Scanned ${uiState.scanProgress?.scanned} messages.\n" +
                    "Found ${uiState.scanProgress?.new} new transactions."
                )
            },
            confirmButton = {
                Button(onClick = { 
                    showScanResult = false
                    viewModel.clearScanProgress()
                }) {
                    Text("Done")
                }
            }
        )
    }
    
    // Bank Discovery Progress Dialog
    if (uiState.isDiscovering) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Discovering Banks...") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    
                    Text(
                        text = "Scanning SMS for transaction patterns",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    uiState.discoveryResult?.let { result ->
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        Text(
                            text = "Scanned: ${result.scannedCount} messages",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Banks found: ${result.detectedBanks.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            confirmButton = {}
        )
    }
    
    // Banks are now displayed directly on the main screen (in "Your Banks" section)
    // No bottom sheet popup needed
}

@Composable
fun PermissionStatusCard(
    hasPermission: Boolean,
    onRequestPermission: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onRequestPermission),
        colors = CardDefaults.cardColors(
            containerColor = androidx.compose.ui.graphics.Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = androidx.compose.ui.graphics.Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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

/**
 * Card to initiate bank discovery scan
 */
@Composable
fun DiscoverBanksCard(
    onClick: () -> Unit,
    isDiscovering: Boolean,
    hasDiscoveryResult: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = androidx.compose.ui.graphics.Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !isDiscovering, onClick = onClick)
                .padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (hasDiscoveryResult) Icons.Default.Refresh else Icons.Default.Search,
                contentDescription = null,
                tint = if (!hasDiscoveryResult)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(40.dp)
            )
            
            Spacer(modifier = Modifier.width(Spacing.md))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (hasDiscoveryResult) "Scan Again" else "🔍 Discover Your Banks",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (!hasDiscoveryResult)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (hasDiscoveryResult)
                        "Rescan SMS to find additional banks"
                    else
                        "Scan your SMS to automatically find your banks",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (!hasDiscoveryResult)
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (!isDiscovering) {
                FilledTonalButton(
                    onClick = onClick,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (!hasDiscoveryResult)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    )
                ) {
                    Text(
                        text = if (hasDiscoveryResult) "Rescan" else "Scan Now",
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            } else {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            }
        }
    }
}

/**
 * Card showing a discovered bank on the main screen
 * Compact view with status indicator and edit/delete options
 */
@Composable
fun YourBankCard(
    bank: DiscoveredBank,
    availableBank: AvailableBank? = null, // Optional: for edit/delete operations
    onAddNewIds: () -> Unit = {},
    onEdit: ((AvailableBank) -> Unit)? = null,
    onDelete: ((AvailableBank) -> Unit)? = null
) {
    var showMenu by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = androidx.compose.ui.graphics.Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Bank icon
            Surface(
                shape = MaterialTheme.shapes.small,
                color = if (bank.bankInfo != null)
                    androidx.compose.ui.graphics.Color(bank.bankInfo.color)
                else
                    MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = bank.bankName.take(2).uppercase(),
                        style = MaterialTheme.typography.titleSmall,
                        color = androidx.compose.ui.graphics.Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(Spacing.md))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = bank.bankName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (bank.isKnownBank && !bank.hasNewSenderIds) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Configured",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Text(
                    text = "${bank.transactionCount} transactions",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = bank.senderIds.joinToString(", "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Action button based on state
            when {
                bank.hasNewSenderIds -> {
                    // Has new sender IDs to add
                    FilledTonalButton(
                        onClick = onAddNewIds,
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = Spacing.md)
                    ) {
                        Text("+${bank.newSenderIds.size} IDs", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            
            // Options menu for edit/delete
            if (availableBank != null && (onEdit != null || onDelete != null)) {
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        if (onEdit != null) {
                            DropdownMenuItem(
                                text = { Text("Edit") },
                                onClick = {
                                    onEdit(availableBank)
                                    showMenu = false
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            )
                        }
                        if (onDelete != null) {
                            DropdownMenuItem(
                                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    onDelete(availableBank)
                                    showMenu = false
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Card showing a discovered bank from SMS scan
 */
@Composable
fun DiscoveredBankCard(
    bank: DiscoveredBank,
    onAddAsCustom: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = androidx.compose.ui.graphics.Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(Spacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Bank Icon/Avatar
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = if (bank.bankInfo != null) 
                        androidx.compose.ui.graphics.Color(bank.bankInfo.color) 
                    else 
                        MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = bank.bankName.take(2).uppercase(),
                            style = MaterialTheme.typography.titleSmall,
                            color = androidx.compose.ui.graphics.Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(Spacing.md))
                
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = bank.bankName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (bank.isKnownBank) {
                            Spacer(modifier = Modifier.width(Spacing.xs))
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Known Bank",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Text(
                        text = "${bank.transactionCount} transactions • ${bank.senderIds.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Add button
                TextButton(onClick = onAddAsCustom) {
                    Text("Add")
                }
            }
            
            // Expanded sample SMS
            if (expanded && bank.sampleSms != null) {
                Spacer(modifier = Modifier.height(Spacing.sm))
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = bank.sampleSms,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(Spacing.sm),
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Bottom sheet showing bank discovery results
 * Shows detected banks first, then other financial SMS senders
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankDiscoveryResultsSheet(
    result: BankDiscoveryResult,
    onAddBank: (DiscoveredBank) -> Unit,
    onAddToExistingBank: (senderId: String, templateId: String) -> Unit,
    customTemplates: List<SmsTemplate>,
    onDismiss: () -> Unit
) {
    var selectedSenderForExisting by remember { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    // Dialog to select which existing bank to add to
    if (selectedSenderForExisting != null && customTemplates.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { selectedSenderForExisting = null },
            title = { 
                Text(
                    "Add to Bank",
                    style = MaterialTheme.typography.titleLarge
                ) 
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Text(
                        "Add \"$selectedSenderForExisting\" to:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    customTemplates.forEach { template ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onAddToExistingBank(selectedSenderForExisting!!, template.id)
                                    selectedSenderForExisting = null
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = androidx.compose.ui.graphics.Color.Transparent
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Spacing.md),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.AccountBalance,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(Spacing.md))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        template.bankName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        template.senderIds,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { selectedSenderForExisting = null }) {
                    Text("Cancel")
                }
            }
        )
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
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📊 Scan Results",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${result.scannedCount} SMS scanned",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(Spacing.md))
            
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                // Banks Found Section
                if (result.detectedBanks.isNotEmpty()) {
                    item {
                        Text(
                            text = "🏦 Banks Found (${result.detectedBanks.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "These banks are automatically recognized",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(Spacing.xs))
                    }
                    
                    items(result.detectedBanks) { bank ->
                        DiscoveredBankResultItem(
                            bank = bank,
                            onAdd = { onAddBank(bank) }
                        )
                    }
                    
                    item { Spacer(modifier = Modifier.height(Spacing.md)) }
                }
                
                // Other SMS Section (for unknown senders)
                if (result.unknownSenders.isNotEmpty()) {
                    item {
                        Text(
                            text = "📋 Other Financial SMS (${result.unknownSenders.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Add to new or existing bank:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(Spacing.xs))
                    }
                    
                    items(result.unknownSenders) { sender ->
                        UnknownSenderResultItem(
                            bank = sender,
                            hasExistingBanks = customTemplates.isNotEmpty(),
                            onAddAsNew = { onAddBank(sender) },
                            onAddToExisting = { 
                                selectedSenderForExisting = sender.senderIds.firstOrNull() 
                            }
                        )
                    }
                }
                
                // Empty state
                if (result.detectedBanks.isEmpty() && result.unknownSenders.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = androidx.compose.ui.graphics.Color.Transparent
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Spacing.lg),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Message,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(Spacing.sm))
                                Text(
                                    text = "No bank SMS found",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = "Try scanning a longer time period or add banks manually",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(Spacing.md))
            
            // Close button
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Done")
            }
        }
    }
}

/**
 * Single item in discovery results
 */
@Composable
fun DiscoveredBankResultItem(
    bank: DiscoveredBank,
    onAdd: () -> Unit
) {
    var showSample by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = androidx.compose.ui.graphics.Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(Spacing.sm)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showSample = !showSample },
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Bank icon
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = if (bank.bankInfo != null)
                        androidx.compose.ui.graphics.Color(bank.bankInfo.color)
                    else
                        MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = bank.bankName.take(2).uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            color = androidx.compose.ui.graphics.Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(Spacing.sm))
                
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = bank.bankName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (bank.isKnownBank) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "Verified",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Text(
                        text = "${bank.transactionCount} transactions • ${bank.senderIds.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // Show new sender IDs if any
                    if (bank.hasNewSenderIds) {
                        Text(
                            text = "New IDs: ${bank.newSenderIds.joinToString(", ")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                // Show different button based on whether there are new sender IDs
                if (bank.isKnownBank && !bank.hasNewSenderIds) {
                    // Already fully configured in registry
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Configured",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                } else {
                    FilledTonalButton(
                        onClick = onAdd,
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = Spacing.md)
                    ) {
                        Text(
                            if (bank.hasNewSenderIds && bank.isKnownBank) "+${bank.newSenderIds.size} IDs" else "Add",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
            
            // Sample SMS preview
            if (showSample && bank.sampleSms != null) {
                Spacer(modifier = Modifier.height(Spacing.xs))
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = bank.sampleSms,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(Spacing.xs),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Item for unknown senders on main screen with Add and Add to Existing options
 */
@Composable
fun UnknownSenderMainScreenItem(
    bank: DiscoveredBank,
    customTemplates: List<SmsTemplate>,
    detectedBanks: List<DiscoveredBank> = emptyList(),
    onAddAsNew: () -> Unit,
    onAddToExistingTemplate: (templateId: String) -> Unit,
    onAddToDetectedBank: (detectedBank: DiscoveredBank) -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = androidx.compose.ui.graphics.Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Bank icon
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = bank.bankName.take(2).uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = androidx.compose.ui.graphics.Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(Spacing.sm))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = bank.bankName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${bank.transactionCount} txns • ${bank.senderIds.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // Single + button with dropdown menu
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add sender",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    // Create new custom bank option
                    DropdownMenuItem(
                        text = { Text("Create new custom bank") },
                        onClick = {
                            onAddAsNew()
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    )
                    
                    // Add to detected banks (from current scan) if any exist
                    if (detectedBanks.isNotEmpty()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.xs))
                        
                        Text(
                            text = "Add to scanned bank:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs)
                        )
                        
                        detectedBanks.take(10).forEach { detectedBank ->
                            DropdownMenuItem(
                                text = { 
                                    Column {
                                        Text(
                                            detectedBank.bankName,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            detectedBank.senderIds.joinToString(", "),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                },
                                onClick = {
                                    onAddToDetectedBank(detectedBank)
                                    showMenu = false
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.AccountBalance,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            )
                        }
                    }
                    
                    // Add to custom templates (only if templates exist)
                    if (customTemplates.isNotEmpty()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.xs))
                        
                        Text(
                            text = "Add to custom bank:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs)
                        )
                        
                        customTemplates.forEach { template ->
                            DropdownMenuItem(
                                text = { 
                                    Column {
                                        Text(
                                            template.bankName,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            template.senderIds,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                },
                                onClick = {
                                    onAddToExistingTemplate(template.id)
                                    showMenu = false
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.AccountBalance,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Item for unknown/unrecognized senders with option to add to existing bank
 */
@Composable
fun UnknownSenderResultItem(
    bank: DiscoveredBank,
    hasExistingBanks: Boolean,
    onAddAsNew: () -> Unit,
    onAddToExisting: () -> Unit
) {
    var showSample by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = androidx.compose.ui.graphics.Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(Spacing.sm)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showSample = !showSample },
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Bank icon
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = bank.bankName.take(2).uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            color = androidx.compose.ui.graphics.Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(Spacing.sm))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = bank.bankName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${bank.transactionCount} transactions • ${bank.senderIds.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(Spacing.sm))
            
            // Action buttons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                // Add as New Bank button
                FilledTonalButton(
                    onClick = onAddAsNew,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = Spacing.sm, vertical = Spacing.xs)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Bank", style = MaterialTheme.typography.labelSmall)
                }
                
                // Add to Existing button (only if there are existing banks)
                if (hasExistingBanks) {
                    OutlinedButton(
                        onClick = onAddToExisting,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = Spacing.sm, vertical = Spacing.xs)
                    ) {
                        Icon(
                            Icons.Default.AccountBalance,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add to Existing", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            
            // Sample SMS preview
            if (showSample && bank.sampleSms != null) {
                Spacer(modifier = Modifier.height(Spacing.xs))
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = bank.sampleSms,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(Spacing.xs),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddCustomBankSheet(
    uncategorizedSenders: List<String> = emptyList(),
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
            
            // Show uncategorized senders as clickable chips
            if (uncategorizedSenders.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Spacing.md))
                
                Text(
                    text = "Select from uncategorized SMS senders:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(Spacing.sm))
                
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    uncategorizedSenders.forEach { sender ->
                        val isSelected = senderIds.uppercase().contains(sender.uppercase())
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (isSelected) {
                                    // Remove from senderIds
                                    val currentIds = senderIds.split(",").map { it.trim() }.filter { 
                                        it.isNotEmpty() && !it.equals(sender, ignoreCase = true) 
                                    }
                                    senderIds = currentIds.joinToString(", ")
                                } else {
                                    // Add to senderIds
                                    val currentIds = senderIds.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                                    senderIds = (currentIds + sender).joinToString(", ")
                                }
                            },
                            label = { Text(sender) },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                }
            }
            
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditCustomBankSheet(
    template: SmsTemplate,
    uncategorizedSenders: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (templateId: String, bankName: String, senderIds: String) -> Unit
) {
    var bankName by remember { mutableStateOf(template.bankName) }
    var senderIdsList by remember { 
        mutableStateOf(
            template.senderIds.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
        ) 
    }
    var newSenderId by remember { mutableStateOf("") }
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
                    text = "Edit Bank",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
            
            Spacer(modifier = Modifier.height(Spacing.lg))
            
            // Bank Name
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
            
            Spacer(modifier = Modifier.height(Spacing.lg))
            
            // Current Sender IDs
            Text(
                text = "SMS Sender IDs",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            
            if (senderIdsList.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    senderIdsList.forEach { senderId ->
                        InputChip(
                            selected = true,
                            onClick = { },
                            label = { Text(senderId) },
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        senderIdsList = senderIdsList.filter { it != senderId }
                                    },
                                    modifier = Modifier.size(18.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Remove",
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        )
                    }
                }
            } else {
                Text(
                    text = "No sender IDs. Add at least one below.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            
            Spacer(modifier = Modifier.height(Spacing.md))
            
            // Add new sender ID
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                OutlinedTextField(
                    value = newSenderId,
                    onValueChange = { newSenderId = it.uppercase() },
                    placeholder = { Text("Add sender ID") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                
                IconButton(
                    onClick = {
                        if (newSenderId.isNotBlank() && !senderIdsList.contains(newSenderId.trim().uppercase())) {
                            senderIdsList = senderIdsList + newSenderId.trim().uppercase()
                            newSenderId = ""
                        }
                    },
                    enabled = newSenderId.isNotBlank()
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add",
                        tint = if (newSenderId.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Uncategorized senders as suggestions
            if (uncategorizedSenders.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Spacing.md))
                
                Text(
                    text = "Select from uncategorized senders:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(Spacing.sm))
                
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    uncategorizedSenders.forEach { sender ->
                        val isAlreadyAdded = senderIdsList.any { it.equals(sender, ignoreCase = true) }
                        FilterChip(
                            selected = isAlreadyAdded,
                            onClick = {
                                if (isAlreadyAdded) {
                                    senderIdsList = senderIdsList.filter { !it.equals(sender, ignoreCase = true) }
                                } else {
                                    senderIdsList = senderIdsList + sender.uppercase()
                                }
                            },
                            label = { Text(sender) },
                            leadingIcon = if (isAlreadyAdded) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                }
            }
            
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
                    onClick = { 
                        onSave(template.id, bankName, senderIdsList.joinToString(",")) 
                    },
                    enabled = bankName.isNotBlank() && senderIdsList.isNotEmpty(),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save")
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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanOptionsSheet(
    onDismiss: () -> Unit,
    onScan: (ScanOption, Long?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    var selectedOption by remember { mutableStateOf(ScanOption.LAST_WEEK) }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf<Long?>(null) }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = Spacing.lg)
                .padding(bottom = Spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Text(
                text = "Scan Options",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "Choose a time range to scan for missed messages.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(Spacing.sm))
            
            // Options
            ScanOptionRow(
                title = "Last 7 Days",
                selected = selectedOption == ScanOption.LAST_WEEK,
                onClick = { selectedOption = ScanOption.LAST_WEEK }
            )
            
            ScanOptionRow(
                title = "Since App Install",
                selected = selectedOption == ScanOption.SINCE_INSTALL,
                onClick = { selectedOption = ScanOption.SINCE_INSTALL }
            )
            
            ScanOptionRow(
                title = "Custom Start Date",
                selected = selectedOption == ScanOption.CUSTOM,
                onClick = { 
                    selectedOption = ScanOption.CUSTOM
                    showDatePicker = true
                },
                subtitle = selectedDate?.let { 
                    java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(it))
                }
            )
            
            Spacer(modifier = Modifier.height(Spacing.lg))
            
            Button(
                onClick = { onScan(selectedOption, selectedDate) },
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedOption != ScanOption.CUSTOM || selectedDate != null
            ) {
                Text("Start Scan")
            }
        }
    }
    
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedDate = datePickerState.selectedDateMillis
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
fun ScanOptionRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    subtitle: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Spacer(modifier = Modifier.width(Spacing.md))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
