package com.theblankstate.epmanager.ui.transactions

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.theblankstate.epmanager.data.model.TransactionType
import com.theblankstate.epmanager.ui.components.formatAmount
import com.theblankstate.epmanager.ui.theme.*
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEdit: () -> Unit = {},
    viewModel: TransactionDetailViewModel = hiltViewModel(),
    currencyViewModel: com.theblankstate.epmanager.util.CurrencyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currencySymbol by currencyViewModel.currencySymbol.collectAsState(initial = "₹")
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showQrDialog by remember { mutableStateOf(false) }
    var showLinkDialog by remember { mutableStateOf(false) }
    var showRuleDialog by remember { mutableStateOf(false) }
    var rulePattern by remember { mutableStateOf("") }
    
    // Navigate back when deleted
    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) {
            onNavigateBack()
        }
    }
    
    // Show success/error toasts
    LaunchedEffect(uiState.successMessage, uiState.error) {
        uiState.successMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessages()
        }
        uiState.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessages()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transaction Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (uiState.transaction != null) {
                        // Edit button
                        IconButton(onClick = onNavigateToEdit) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Edit",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        // Delete button
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            uiState.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(Spacing.md))
                        Text(
                            text = uiState.error ?: "Error",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            
            uiState.transaction != null -> {
                val transaction = uiState.transaction!!
                val category = uiState.category
                val account = uiState.account
                
                // Smart account name: show bank name if account name is generic
                val displayAccountName = when {
                    account == null -> null
                    account.name.equals("Bank Account", ignoreCase = true) && !account.bankCode.isNullOrBlank() -> {
                        account.bankCode // Show bank code like "HDFC" instead of "Bank Account"
                    }
                    account.isLinked && !account.bankCode.isNullOrBlank() -> {
                        "${account.name} (${account.bankCode})"
                    }
                    else -> account.name
                }
                
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Amount Header
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.md),
                        colors = CardDefaults.cardColors(
                            containerColor = if (transaction.type == TransactionType.EXPENSE)
                                Error.copy(alpha = 0.1f)
                            else
                                Success.copy(alpha = 0.1f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Spacing.xl),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Type Badge
                            AssistChip(
                                onClick = {},
                                label = { 
                                    Text(
                                        if (transaction.type == TransactionType.EXPENSE) "Expense" else "Income"
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (transaction.type == TransactionType.EXPENSE)
                                            Icons.Filled.ArrowDownward
                                        else
                                            Icons.Filled.ArrowUpward,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = if (transaction.type == TransactionType.EXPENSE)
                                        Error.copy(alpha = 0.2f)
                                    else
                                        Success.copy(alpha = 0.2f),
                                    labelColor = if (transaction.type == TransactionType.EXPENSE)
                                        Error
                                    else
                                        Success
                                )
                            )
                            
                            Spacer(modifier = Modifier.height(Spacing.md))
                            
                            // Amount
                            Text(
                                text = "${if (transaction.type == TransactionType.EXPENSE) "-" else "+"}${formatAmount(transaction.amount, currencySymbol)}",
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (transaction.type == TransactionType.EXPENSE) Error else Success
                            )
                            
                            // Category
                            if (category != null) {
                                Spacer(modifier = Modifier.height(Spacing.sm))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(Color(category.color))
                                    )
                                    Text(
                                        text = category.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                    
                    // Details Section
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.md)
                    ) {
                        Column(
                            modifier = Modifier.padding(Spacing.md),
                            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            Text(
                                text = "Details",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            
                            HorizontalDivider()
                            
                            // Date & Time
                            DetailRow(
                                icon = Icons.Filled.CalendarToday,
                                label = "Date",
                                value = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
                                    .format(Date(transaction.date))
                            )
                            
                            DetailRow(
                                icon = Icons.Filled.AccessTime,
                                label = "Time",
                                value = SimpleDateFormat("hh:mm a", Locale.getDefault())
                                    .format(Date(transaction.date))
                            )
                            
                            // Account with smart naming
                            if (displayAccountName != null) {
                                DetailRow(
                                    icon = Icons.Filled.AccountBalance,
                                    label = "Account",
                                    value = displayAccountName
                                )
                            }
                            
                            // Note
                            if (!transaction.note.isNullOrBlank()) {
                                DetailRow(
                                    icon = Icons.Filled.Notes,
                                    label = "Note",
                                    value = transaction.note
                                )
                            }
                            
                            // Recurring indicator
                            if (transaction.isRecurring) {
                                DetailRow(
                                    icon = Icons.Filled.Repeat,
                                    label = "Recurring",
                                    value = "Yes"
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(Spacing.md))
                    
                    // SMS Details Section (only if SMS metadata present)
                    if (transaction.smsSender != null || transaction.merchantName != null || 
                        transaction.refNumber != null || transaction.senderName != null || 
                        transaction.receiverName != null) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Spacing.md)
                        ) {
                            Column(
                                modifier = Modifier.padding(Spacing.md),
                                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                            ) {
                                Text(
                                    text = "Transaction Details",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                
                                HorizontalDivider()
                                
                                // SMS Sender (Bank) with optional Link button
                                if (!transaction.smsSender.isNullOrBlank()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        DetailRow(
                                            icon = Icons.Filled.Sms,
                                            label = "From Bank",
                                            value = transaction.smsSender,
                                            modifier = Modifier.weight(1f)
                                        )
                                        
                                        // Show Link button if transaction has no linked account
                                        if (transaction.accountId == null && uiState.linkedAccounts.isNotEmpty()) {
                                            TextButton(
                                                onClick = { showLinkDialog = true }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Link,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Text("Link", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                }
                                
                                // Sender (for credits)
                                if (!transaction.senderName.isNullOrBlank()) {
                                    DetailRow(
                                        icon = Icons.Filled.PersonAdd,
                                        label = "Received From",
                                        value = transaction.senderName,
                                        onRuleClick = {
                                            rulePattern = transaction.senderName
                                            showRuleDialog = true
                                        }
                                    )
                                }
                                
                                // Receiver (for debits)
                                if (!transaction.receiverName.isNullOrBlank()) {
                                    DetailRow(
                                        icon = Icons.Filled.Person,
                                        label = "Paid To",
                                        value = transaction.receiverName,
                                        onRuleClick = {
                                            rulePattern = transaction.receiverName
                                            showRuleDialog = true
                                        }
                                    )
                                }
                                
                                // Merchant
                                if (!transaction.merchantName.isNullOrBlank() && 
                                    transaction.merchantName != transaction.receiverName) {
                                    DetailRow(
                                        icon = Icons.Filled.Store,
                                        label = "Merchant",
                                        value = transaction.merchantName,
                                        onRuleClick = {
                                            rulePattern = transaction.merchantName
                                            showRuleDialog = true
                                        }
                                    )
                                }
                                
                                // UPI ID with QR Code button
                                // Only show if it's a valid UPI ID (must contain @)
                                if (!transaction.upiId.isNullOrBlank() && transaction.upiId.contains("@")) {
                                    DetailRow(
                                        icon = Icons.Filled.QrCode,
                                        label = "UPI ID",
                                        value = transaction.upiId,
                                        onRuleClick = {
                                            rulePattern = transaction.upiId
                                            showRuleDialog = true
                                        }
                                    )
                                    
                                    // Show QR Code button
                                    OutlinedButton(
                                        onClick = { showQrDialog = true },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.QrCode2,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(Spacing.xs))
                                        Text("View QR Code")
                                    }
                                }
                                
                                // Reference Number
                                if (!transaction.refNumber.isNullOrBlank()) {
                                    DetailRow(
                                        icon = Icons.Filled.Tag,
                                        label = "Reference No.",
                                        value = transaction.refNumber
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(Spacing.md))
                    }
                    
                    // Original SMS Section
                    if (!transaction.originalSms.isNullOrBlank()) {
                        OriginalSmsCard(
                            sms = transaction.originalSms,
                            smsSender = transaction.smsSender
                        )
                        Spacer(modifier = Modifier.height(Spacing.md))
                    }
                    
                    // Location Section
                    if (transaction.latitude != null && transaction.longitude != null) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Spacing.md)
                        ) {
                            Column(
                                modifier = Modifier.padding(Spacing.md),
                                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                            ) {
                                Text(
                                    text = "Location",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                
                                HorizontalDivider()
                                
                                // Location Name
                                if (!transaction.locationName.isNullOrBlank()) {
                                    DetailRow(
                                        icon = Icons.Filled.Place,
                                        label = "Place",
                                        value = transaction.locationName
                                    )
                                }
                                
                                // Coordinates
                                DetailRow(
                                    icon = Icons.Filled.MyLocation,
                                    label = "Coordinates",
                                    value = "${String.format("%.6f", transaction.latitude)}, ${String.format("%.6f", transaction.longitude)}"
                                )
                                
                                // Static Map Preview using OpenStreetMap
                                // Using free OSM static map service (no API key needed)
                                val zoom = 15
                                val lat = transaction.latitude
                                val lon = transaction.longitude
                                // Using a free static map service that works without API key
                                val mapUrl = "https://www.openstreetmap.org/export/embed.html?bbox=${lon - 0.003},${lat - 0.002},${lon + 0.003},${lat + 0.002}&layer=mapnik&marker=${lat},${lon}"
                                
                                // Alternative: Use direct tile image from OSM
                                val tileX = ((lon + 180.0) / 360.0 * (1 shl zoom)).toInt()
                                val latRad = Math.toRadians(lat)
                                val tileY = ((1.0 - kotlin.math.ln(kotlin.math.tan(latRad) + 1.0 / kotlin.math.cos(latRad)) / Math.PI) / 2.0 * (1 shl zoom)).toInt()
                                val tileUrl = "https://tile.openstreetmap.org/$zoom/$tileX/$tileY.png"
                                
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(150.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    coil.compose.SubcomposeAsyncImage(
                                        model = coil.request.ImageRequest.Builder(context)
                                            .data(tileUrl)
                                            .crossfade(true)
                                            .addHeader("User-Agent", "EpManager/1.0")
                                            .build(),
                                        contentDescription = "Location Map",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                        loading = {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(24.dp),
                                                strokeWidth = 2.dp
                                            )
                                        },
                                        error = {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Map,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(32.dp)
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "Tap to view map",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    )
                                    
                                    // Location marker overlay
                                    Icon(
                                        imageVector = Icons.Filled.LocationOn,
                                        contentDescription = null,
                                        tint = Color.Red,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                
                                // Open in Maps button
                                OutlinedButton(
                                    onClick = {
                                        try {
                                            // Try Google Maps first
                                            val gmmIntentUri = Uri.parse("geo:0,0?q=${transaction.latitude},${transaction.longitude}(Transaction Location)")
                                            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                            context.startActivity(mapIntent)
                                        } catch (e: Exception) {
                                            // Fallback to browser with Google Maps URL
                                            val browserUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=${transaction.latitude},${transaction.longitude}")
                                            val browserIntent = Intent(Intent.ACTION_VIEW, browserUri)
                                            context.startActivity(browserIntent)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Map,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(Spacing.xs))
                                    Text("Open in Maps")
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(Spacing.md))
                    }
                    
                    // Metadata Section
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.md),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(Spacing.md),
                            verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                        ) {
                            Text(
                                text = "Record Info",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Text(
                                text = "Created: ${SimpleDateFormat("MMM d, yyyy 'at' hh:mm a", Locale.getDefault()).format(Date(transaction.createdAt))}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            if (transaction.updatedAt != transaction.createdAt) {
                                Text(
                                    text = "Updated: ${SimpleDateFormat("MMM d, yyyy 'at' hh:mm a", Locale.getDefault()).format(Date(transaction.updatedAt))}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            Text(
                                text = "ID: ${transaction.id.take(8)}...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                            
                            // Sync status
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                            ) {
                                Icon(
                                    imageVector = if (transaction.isSynced) Icons.Filled.CloudDone else Icons.Filled.CloudOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = if (transaction.isSynced) Success else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (transaction.isSynced) "Synced to cloud" else "Not synced",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(Spacing.xxl))
                }
            }
        }
    }
    
    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Delete Transaction") },
            text = { 
                Text("Are you sure you want to delete this transaction? This will also reverse the account balance change.") 
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTransaction()
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
    
    // UPI QR Code Dialog
    if (showQrDialog && uiState.transaction?.upiId != null) {
        UpiQrCodeDialog(
            upiId = uiState.transaction!!.upiId!!,
            amount = uiState.transaction!!.amount,
            onDismiss = { showQrDialog = false }
        )
    }
    
    // Link to Account Dialog
    if (showLinkDialog && uiState.transaction?.smsSender != null) {
        LinkToAccountDialog(
            senderId = uiState.transaction!!.smsSender!!,
            accounts = uiState.linkedAccounts,
            onAccountSelected = { accountId ->
                viewModel.linkSenderToAccount(uiState.transaction!!.smsSender!!, accountId)
            },
            onDismiss = { showLinkDialog = false }
        )
    }

    // Categorization Rule Dialog
    if (showRuleDialog && rulePattern.isNotBlank()) {
        var selectedCategory by remember { mutableStateOf(uiState.category) }
        var expanded by remember { mutableStateOf(false) }
        
        AlertDialog(
            onDismissRequest = { showRuleDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Filled.BookmarkAdd,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text("Create Categorization Rule") },
            text = { 
                Column(
                    verticalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    Text("All transactions containing '$rulePattern' will be categorized as:")
                    
                    // Category Dropdown
                    Box {
                        OutlinedButton(
                            onClick = { expanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (selectedCategory != null) {
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(CircleShape)
                                                .background(Color(selectedCategory!!.color))
                                        )
                                    }
                                    Text(selectedCategory?.name ?: "Select Category")
                                }
                                Icon(
                                    imageVector = Icons.Filled.ArrowDropDown,
                                    contentDescription = null
                                )
                            }
                        }
                        
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            uiState.allCategories.forEach { cat ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(cat.color))
                                            )
                                            Text(cat.name)
                                        }
                                    },
                                    onClick = {
                                        selectedCategory = cat
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedCategory?.let { cat ->
                            viewModel.createCategorizationRule(rulePattern, cat.id)
                        }
                        showRuleDialog = false
                    },
                    enabled = selectedCategory != null
                ) {
                    Text("Create Rule")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRuleDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}


@Composable
private fun DetailRow(
    icon: ImageVector,
    label: String,
    value: String,
    showCopy: Boolean = true,
    onRuleClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        
        if (showCopy) {
            IconButton(
                onClick = { copyToClipboard(context, label, value) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.ContentCopy,
                    contentDescription = "Copy $label",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (onRuleClick != null) {
            IconButton(
                onClick = onRuleClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.BookmarkAdd,
                    contentDescription = "Create Rule",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Copy text to clipboard and show toast
 */
private fun copyToClipboard(context: Context, label: String, value: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, value)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "$label copied", Toast.LENGTH_SHORT).show()
}

/**
 * Generate QR code bitmap for UPI payment
 */
private fun generateUpiQrCode(upiId: String, amount: Double? = null): Bitmap? {
    return try {
        val upiUri = buildString {
            append("upi://pay?pa=$upiId")
            append("&pn=Payment")
            if (amount != null && amount > 0) {
                append("&am=$amount")
            }
            append("&cu=INR")
        }
        
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(upiUri, BarcodeFormat.QR_CODE, 512, 512)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        
        for (x in 0 until width) {
            for (y in 0 until height) {                bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
        bitmap
    } catch (e: Exception) {
        null
    }
}

/**
 * Original SMS Card with expandable view and copy functionality
 */
@Composable
private fun OriginalSmsCard(
    sms: String,
    smsSender: String?
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Sms,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Original SMS",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    IconButton(
                        onClick = { copyToClipboard(context, "SMS", sms) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ContentCopy,
                            contentDescription = "Copy SMS",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                    Icon(
                        imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
            
            if (!smsSender.isNullOrBlank()) {
                Text(
                    text = "From: $smsSender",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                )
            }
            
            if (expanded) {
                HorizontalDivider(color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.2f))
                Text(
                    text = sms,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}

/**
 * UPI Details Dialog - Shows QR code, UPI ID with options to copy and pay
 */
@Composable
private fun UpiQrCodeDialog(
    upiId: String,
    amount: Double,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val qrBitmap = remember(upiId, amount) { generateUpiQrCode(upiId, amount) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                text = "UPI Payment QR",
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                // QR Code Image
                if (qrBitmap != null) {
                    Card(
                        modifier = Modifier.size(200.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "UPI QR Code",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                        )
                    }
                } else {
                    // Fallback if QR generation fails
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Filled.QrCode,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp)
                            )
                            Text("QR generation failed")
                        }
                    }
                }
                
                // UPI ID display
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.md),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "UPI ID",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        Text(
                            text = upiId,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                
                // Copy button
                OutlinedButton(
                    onClick = { copyToClipboard(context, "UPI ID", upiId) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Text("Copy UPI ID")
                }
                
                // Pay with UPI button
                Button(
                    onClick = {
                        try {
                            val upiUri = Uri.parse(buildString {
                                append("upi://pay?pa=$upiId")
                                append("&pn=Payment")
                                if (amount > 0) {
                                    append("&am=$amount")
                                }
                                append("&cu=INR")
                            })
                            val intent = Intent(Intent.ACTION_VIEW, upiUri)
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "No UPI app found", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Filled.Payment,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Text("Pay with UPI App")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

/**
 * Dialog to select a bank account to link the SMS sender to
 */
@Composable
private fun LinkToAccountDialog(
    senderId: String,
    accounts: List<com.theblankstate.epmanager.data.model.Account>,
    onAccountSelected: (accountId: String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                text = "Link Sender to Account",
                style = MaterialTheme.typography.titleLarge
            ) 
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                Text(
                    text = "Link \"$senderId\" to:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(Spacing.sm))
                
                if (accounts.isEmpty()) {
                    Text(
                        text = "No linked accounts found. Create a linked bank account first.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    accounts.forEach { account ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    onAccountSelected(account.id)
                                    onDismiss()
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
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
                                        .background(Color(account.color)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.AccountBalance,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = account.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    if (!account.linkedSenderIds.isNullOrBlank()) {
                                        Text(
                                            text = "Senders: ${account.linkedSenderIds}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                }
                                
                                Icon(
                                    imageVector = Icons.Filled.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
