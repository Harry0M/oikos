package com.theblankstate.epmanager.ui.transactions

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
    
    // Bottom Sheet & Dialog States
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showQrDialog by remember { mutableStateOf(false) }
    var showLinkDialog by remember { mutableStateOf(false) }
    var rulePattern by remember { mutableStateOf("") }
    
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    // Effects
    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) {
            onNavigateBack()
        }
    }
    
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
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Transaction Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (uiState.transaction != null) {
                        IconButton(onClick = onNavigateToEdit) {
                            Icon(Icons.Filled.Edit, "Edit", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Filled.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.error != null && uiState.transaction == null) {
                 Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Filled.Error, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(16.dp))
                    Text(uiState.error ?: "Error", color = MaterialTheme.colorScheme.error)
                }
            } else if (uiState.transaction != null) {
                val transaction = uiState.transaction!!
                val category = uiState.category
                val account = uiState.account
                
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    // Header (Amount & Category Button)
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val amountColor = if (transaction.type == TransactionType.EXPENSE) MaterialTheme.colorScheme.error else Color(0xFF16A34A)
                            Text(
                                text = "${if (transaction.type == TransactionType.EXPENSE) "-" else "+"}${formatAmount(transaction.amount, currencySymbol)}",
                                style = MaterialTheme.typography.displayLarge,
                                fontWeight = FontWeight.Black,
                                color = amountColor
                            )
                            
                            Spacer(Modifier.height(16.dp))
                            
                            // Category Badge (Better visibility as requested)
                            val categoryName = category?.name ?: "Uncategorized"
                            val categoryColor = category?.color?.let { Color(it) } ?: MaterialTheme.colorScheme.outline
                            
                            FilledTonalButton(
                                onClick = { 
                                    rulePattern = transaction.merchantName ?: transaction.receiverName ?: transaction.senderName ?: ""
                                    showBottomSheet = true 
                                },
                                shape = CircleShape,
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = categoryColor.copy(alpha = 0.1f),
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                ),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(categoryColor)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(categoryName, style = MaterialTheme.typography.labelLarge)
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.Filled.ChevronRight, null, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    
                    // General Details
                    item {
                        DetailSection(title = "General") {
                             DetailListItem(
                                 icon = Icons.Filled.CalendarToday,
                                 headline = "Date",
                                 supporting = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(Date(transaction.date))
                             )
                             DetailListItem(
                                 icon = Icons.Filled.AccessTime,
                                 headline = "Time",
                                 supporting = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(transaction.date))
                             )
                             val displayAccountName = when {
                                account == null -> null
                                account.name.equals("Bank Account", ignoreCase = true) && !account.bankCode.isNullOrBlank() -> account.bankCode
                                account.isLinked && !account.bankCode.isNullOrBlank() -> "${account.name} (${account.bankCode})"
                                else -> account.name
                             }
                             if (displayAccountName != null) {
                                 DetailListItem(
                                     icon = Icons.Filled.AccountBalance,
                                     headline = "Account",
                                     supporting = displayAccountName
                                 )
                             }
                             if (!transaction.note.isNullOrBlank()) {
                                 DetailListItem(
                                     icon = Icons.Filled.Notes,
                                     headline = "Note",
                                     supporting = transaction.note,
                                     enableCopy = true
                                 )
                             }
                        }
                    }
                    
                    // Transaction Info
                    if (transaction.smsSender != null || transaction.merchantName != null || 
                        transaction.refNumber != null || transaction.senderName != null || transaction.receiverName != null) {
                        item {
                            DetailSection(title = "Transaction Info") {
                                if (!transaction.smsSender.isNullOrBlank()) {
                                    DetailListItem(
                                        icon = Icons.Filled.Sms,
                                        headline = "Bank / Sender",
                                        supporting = transaction.smsSender,
                                        trailingContent = {
                                            if (transaction.accountId == null && uiState.linkedAccounts.isNotEmpty()) {
                                                FilledTonalButton(onClick = { showLinkDialog = true }) {
                                                    Icon(Icons.Filled.Link, null, modifier = Modifier.size(16.dp))
                                                    Spacer(Modifier.width(4.dp))
                                                    Text("Link")
                                                }
                                            }
                                        }
                                    )
                                }
                                
                                val entities = listOfNotNull(
                                    if (!transaction.senderName.isNullOrBlank()) "Received From" to transaction.senderName else null,
                                    if (!transaction.receiverName.isNullOrBlank()) "Paid To" to transaction.receiverName else null,
                                    if (!transaction.merchantName.isNullOrBlank() && transaction.merchantName != transaction.receiverName) "Merchant" to transaction.merchantName else null
                                )
                                
                                entities.forEach { (label, name) ->
                                    DetailListItem(
                                        icon = Icons.Filled.Store,
                                        headline = label,
                                        supporting = name,
                                        enableCopy = true,
                                        trailingContent = {
                                            FilledTonalButton(onClick = {
                                                rulePattern = name!!
                                                showBottomSheet = true
                                            }) {
                                                Text(category?.name ?: "Category", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    )
                                }
                                
                                if (!transaction.upiId.isNullOrBlank() && transaction.upiId.contains("@")) {
                                    DetailListItem(
                                        icon = Icons.Filled.QrCode,
                                        headline = "UPI ID",
                                        supporting = transaction.upiId,
                                        enableCopy = true,
                                        trailingContent = {
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                IconButton(onClick = { showQrDialog = true }) {
                                                    Icon(Icons.Filled.QrCode2, "QR", tint = MaterialTheme.colorScheme.primary)
                                                }
                                                FilledTonalButton(onClick = {
                                                    rulePattern = transaction.upiId!!
                                                    showBottomSheet = true
                                                }) {
                                                    Text(category?.name ?: "Category", style = MaterialTheme.typography.labelSmall)
                                                }
                                            }
                                        }
                                    )
                                }
                                
                                if (!transaction.refNumber.isNullOrBlank()) {
                                    DetailListItem(
                                        icon = Icons.Filled.Tag,
                                        headline = "Reference No.",
                                        supporting = transaction.refNumber,
                                        enableCopy = true
                                    )
                                }
                            }
                        }
                    }
                    
                    // Location
                    if (transaction.latitude != null && transaction.longitude != null) {
                         item {
                             DetailSection(title = "Location") {
                                val lat = transaction.latitude!!
                                val lon = transaction.longitude!!
                                val zoom = 15
                                val tileX = ((lon + 180.0) / 360.0 * (1 shl zoom)).toInt()
                                val latRad = Math.toRadians(lat)
                                val tileY = ((1.0 - kotlin.math.ln(kotlin.math.tan(latRad) + 1.0 / kotlin.math.cos(latRad)) / Math.PI) / 2.0 * (1 shl zoom)).toInt()
                                val tileUrl = "https://tile.openstreetmap.org/$zoom/$tileX/$tileY.png"
                                
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .clip(MaterialTheme.shapes.medium)
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    coil.compose.SubcomposeAsyncImage(
                                        model = coil.request.ImageRequest.Builder(context)
                                            .data(tileUrl)
                                            .crossfade(true)
                                            .addHeader("User-Agent", "EpManager/1.0")
                                            .build(),
                                        contentDescription = "Map",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                        loading = { CircularProgressIndicator(modifier = Modifier.size(24.dp)) },
                                        error = { Icon(Icons.Filled.Map, null) }
                                    )
                                    Icon(Icons.Filled.LocationOn, null, tint = Color.Red, modifier = Modifier.size(32.dp))
                                    
                                    FilledTonalButton(
                                        onClick = {
                                            val gmmIntentUri = Uri.parse("geo:0,0?q=${lat},${lon}(Transaction)")
                                            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                            try {
                                                context.startActivity(mapIntent)
                                            } catch (e: Exception) {
                                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=${lat},${lon}")))
                                            }
                                        },
                                        modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)
                                    ) {
                                        Icon(Icons.Filled.Map, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Maps")
                                    }
                                }
                                
                                if (!transaction.locationName.isNullOrBlank()) {
                                    DetailListItem(
                                        icon = Icons.Filled.Place,
                                        headline = "Place",
                                        supporting = transaction.locationName
                                    )
                                }
                                
                                // Coordinates (Visible and Copyable as requested)
                                DetailListItem(
                                    icon = Icons.Filled.Grid4x4,
                                    headline = "Coordinates",
                                    supporting = String.format("%.6f, %.6f", lat, lon),
                                    enableCopy = true
                                )
                             }
                         }
                     }
                     
                     // Metadata Footer
                     item {
                         Column(
                             modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 32.dp),
                             horizontalAlignment = Alignment.CenterHorizontally
                         ) {
                             Text(
                                 "ID: ${transaction.id}",
                                 style = MaterialTheme.typography.labelSmall,
                                 color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.4f)
                             )
                             Text(
                                 "Created: ${SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault()).format(Date(transaction.createdAt))}",
                                 style = MaterialTheme.typography.labelSmall,
                                 color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.4f)
                             )
                         }
                     }
                }
            }
        }
    }
    
    // Dialogs & Sheets (Inside Screen scope)
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState
        ) {
            CategorizationBottomSheetContent(
                pattern = rulePattern,
                categories = uiState.allCategories,
                onCategorySelected = { cat ->
                    viewModel.createCategorizationRule(rulePattern, cat.id)
                    showBottomSheet = false
                }
            )
        }
    }

    if (showDeleteDialog) {
        ConfirmDeleteDialog(
            onConfirm = { 
                viewModel.deleteTransaction()
                showDeleteDialog = false
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
    
    if (showLinkDialog && uiState.transaction?.smsSender != null) {
        LinkToAccountDialog(
            senderId = uiState.transaction!!.smsSender!!,
            accounts = uiState.linkedAccounts,
            onAccountSelected = { id -> viewModel.linkSenderToAccount(uiState.transaction!!.smsSender!!, id) },
            onDismiss = { showLinkDialog = false }
        )
    }
    
    if (showQrDialog && uiState.transaction?.upiId != null) {
        UpiQrCodeDialog(
            upiId = uiState.transaction!!.upiId!!,
            amount = uiState.transaction!!.amount,
            onDismiss = { showQrDialog = false }
        )
    }
}

@Composable
fun DetailSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            content()
        }
    }
}

@Composable
fun DetailListItem(
    icon: ImageVector,
    headline: String,
    supporting: String?,
    enableCopy: Boolean = false,
    trailingContent: (@Composable () -> Unit)? = null
) {
    if (supporting == null) return
    val context = LocalContext.current
    
    ListItem(
        headlineContent = { Text(headline, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        supportingContent = { 
            Text(
                supporting, 
                style = MaterialTheme.typography.bodyLarge, 
                color = MaterialTheme.colorScheme.onSurface
            ) 
        },
        leadingContent = {
            Icon(
                icon, 
                null, 
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
            )
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (trailingContent != null) {
                    trailingContent()
                }
                if (enableCopy) {
                    IconButton(onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText(headline, supporting)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "$headline copied", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Filled.ContentCopy, "Copy", modifier = Modifier.size(18.dp))
                    }
                }
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
fun ConfirmDeleteDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Transaction?") },
        text = { Text("This action cannot be undone. Balance will be reverted.") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Delete", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        icon = { Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error) }
    )
}

@Composable
fun LinkToAccountDialog(
    senderId: String,
    accounts: List<com.theblankstate.epmanager.data.model.Account>,
    onAccountSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Link Sender: $senderId") },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                items(accounts.size) { i ->
                    val acc = accounts[i]
                    ListItem(
                        headlineContent = { Text(acc.name) },
                        leadingContent = { Icon(Icons.Filled.AccountBalance, null) },
                        modifier = Modifier.clickable { 
                            onAccountSelected(acc.id)
                            onDismiss() 
                        }
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun UpiQrCodeDialog(upiId: String, amount: Double, onDismiss: () -> Unit) {
    val upiString = "upi://pay?pa=$upiId&am=$amount"
    val context = LocalContext.current
    val bitmap = remember(upiString) {
        try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(upiString, BarcodeFormat.QR_CODE, 512, 512)
            val w = bitMatrix.width
            val h = bitMatrix.height
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565)
            for (x in 0 until w) {
                for (y in 0 until h) {
                    bmp.setPixel(x, y, if (bitMatrix[x, y]) AndroidColor.BLACK else AndroidColor.WHITE)
                }
            }
            bmp
        } catch (e: Exception) { null }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("UPI QR Code") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "QR Code",
                        modifier = Modifier.size(240.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(upiId, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text("Requested: ₹$amount", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }
        },
        confirmButton = { 
            // Open in UPI App option as requested
            Button(onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(upiString))
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "No UPI app found", Toast.LENGTH_SHORT).show()
                }
            }) {
                Icon(Icons.Filled.Payment, null)
                Spacer(Modifier.width(8.dp))
                Text("Pay with UPI App")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorizationBottomSheetContent(
    pattern: String,
    categories: List<com.theblankstate.epmanager.data.model.Category>,
    onCategorySelected: (com.theblankstate.epmanager.data.model.Category) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp)
            .padding(horizontal = 24.dp)
    ) {
        Text(
            "Categorize Activity",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Link transactions related to '$pattern' to a category.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.heightIn(max = 400.dp)
        ) {
            items(categories.size) { i ->
                val cat = categories[i]
                Surface(
                    onClick = { onCategorySelected(cat) },
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(Color(cat.color))
                        )
                        Spacer(Modifier.width(16.dp))
                        Text(cat.name, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}
