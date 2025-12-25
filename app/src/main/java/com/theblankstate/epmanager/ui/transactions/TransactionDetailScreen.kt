package com.theblankstate.epmanager.ui.transactions

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.theblankstate.epmanager.data.model.TransactionType
import com.theblankstate.epmanager.ui.components.formatAmount
import com.theblankstate.epmanager.ui.theme.*
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
    
    // Navigate back when deleted
    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) {
            onNavigateBack()
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
                                
                                // SMS Sender (Bank)
                                if (!transaction.smsSender.isNullOrBlank()) {
                                    DetailRow(
                                        icon = Icons.Filled.Sms,
                                        label = "From Bank",
                                        value = transaction.smsSender
                                    )
                                }
                                
                                // Sender (for credits)
                                if (!transaction.senderName.isNullOrBlank()) {
                                    DetailRow(
                                        icon = Icons.Filled.PersonAdd,
                                        label = "Received From",
                                        value = transaction.senderName
                                    )
                                }
                                
                                // Receiver (for debits)
                                if (!transaction.receiverName.isNullOrBlank()) {
                                    DetailRow(
                                        icon = Icons.Filled.Person,
                                        label = "Paid To",
                                        value = transaction.receiverName
                                    )
                                }
                                
                                // Merchant
                                if (!transaction.merchantName.isNullOrBlank() && 
                                    transaction.merchantName != transaction.receiverName) {
                                    DetailRow(
                                        icon = Icons.Filled.Store,
                                        label = "Merchant",
                                        value = transaction.merchantName
                                    )
                                }
                                
                                // UPI ID
                                if (!transaction.upiId.isNullOrBlank()) {
                                    DetailRow(
                                        icon = Icons.Filled.QrCode,
                                        label = "UPI ID",
                                        value = transaction.upiId
                                    )
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
}

@Composable
private fun DetailRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Column {
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
}
