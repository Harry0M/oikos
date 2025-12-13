package com.theblankstate.epmanager.ui.add

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.theblankstate.epmanager.data.model.Account
import com.theblankstate.epmanager.data.model.Category
import com.theblankstate.epmanager.data.model.TransactionType
import com.theblankstate.epmanager.ui.theme.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    onNavigateBack: () -> Unit,
    onTransactionSaved: () -> Unit,
    viewModel: AddTransactionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Location permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        
        if (fineGranted || coarseGranted) {
            // Get location
            scope.launch {
                getCurrentLocation(context)?.let { location ->
                    val locationName = getLocationName(context, location)
                    viewModel.updateLocation(location, locationName)
                }
            }
        }
    }
    
    // Request location on screen load
    LaunchedEffect(Unit) {
        if (hasLocationPermission(context)) {
            getCurrentLocation(context)?.let { location ->
                val locationName = getLocationName(context, location)
                viewModel.updateLocation(location, locationName)
            }
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }
    
    // Navigate back when saved
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onTransactionSaved()
        }
    }
    
    // Show error snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }
    
    // Show settle success snackbar
    LaunchedEffect(uiState.settleSuccess) {
        uiState.settleSuccess?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSettleSuccess()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Add Transaction",
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
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.md)
        ) {
            // Transaction Type Toggle
            TransactionTypeToggle(
                selectedType = uiState.transactionType,
                onTypeSelected = viewModel::updateTransactionType
            )
            
            Spacer(modifier = Modifier.height(Spacing.xl))
            
            // Amount Input
            AmountInput(
                amount = uiState.amount,
                onAmountChange = viewModel::updateAmount,
                isExpense = uiState.transactionType == TransactionType.EXPENSE
            )
            
            Spacer(modifier = Modifier.height(Spacing.xl))
            
            // Category Selection
            Text(
                text = "Category",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            
            CategorySelector(
                categories = uiState.categories,
                selectedCategory = uiState.selectedCategory,
                onCategorySelected = viewModel::selectCategory
            )
            
            Spacer(modifier = Modifier.height(Spacing.xl))
            
            // Note Input
            OutlinedTextField(
                value = uiState.note,
                onValueChange = viewModel::updateNote,
                label = { Text("Note (optional)") },
                placeholder = { Text("Add a note...") },
                modifier = Modifier.fillMaxWidth(),
                shape = InputFieldShape,
                maxLines = 2
            )
            
            Spacer(modifier = Modifier.height(Spacing.xl))
            
            // Account Selection
            Text(
                text = "Account",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                items(
                    items = uiState.accounts,
                    key = { it.id }
                ) { account ->
                    val isSelected = account.id == uiState.selectedAccount?.id
                    
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.selectAccount(account) },
                        label = { Text(account.name) },
                        leadingIcon = {
                            Icon(
                                imageVector = when (account.name.lowercase()) {
                                    "cash" -> Icons.Filled.Money
                                    "bank account" -> Icons.Filled.AccountBalance
                                    "upi" -> Icons.Filled.PhoneAndroid
                                    "credit card" -> Icons.Filled.CreditCard
                                    else -> Icons.Filled.Wallet
                                },
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(Spacing.md))
            
            // Location indicator
            if (uiState.currentLocation != null || uiState.locationName != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CardShapeSmall)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = Success,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = uiState.locationName ?: "Location recorded",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { viewModel.toggleLocationRecording(!uiState.isLocationEnabled) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (uiState.isLocationEnabled) Icons.Filled.Close else Icons.Filled.Add,
                            contentDescription = if (uiState.isLocationEnabled) "Remove location" else "Add location",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            
            // Quick Split Section (only for expenses)
            if (uiState.transactionType == TransactionType.EXPENSE) {
                Spacer(modifier = Modifier.height(Spacing.md))
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (uiState.isSplitEnabled) 
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        else 
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Column(modifier = Modifier.padding(Spacing.md)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Groups,
                                    contentDescription = null,
                                    tint = if (uiState.isSplitEnabled) 
                                        MaterialTheme.colorScheme.primary 
                                    else 
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Column {
                                    Text(
                                        text = "Split this expense",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Quickly split with friends",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = uiState.isSplitEnabled,
                                onCheckedChange = { viewModel.toggleSplit(it) }
                            )
                        }
                        
                        // Show friend selection when split is enabled
                        if (uiState.isSplitEnabled) {
                            Spacer(modifier = Modifier.height(Spacing.md))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(Spacing.md))
                            
                            // Friends selection
                            if (uiState.friends.isNotEmpty()) {
                                Text(
                                    text = "Select Friends",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(Spacing.xs))
                                
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                                ) {
                                    items(
                                        items = uiState.friends,
                                        key = { it.odiserId }
                                    ) { friend ->
                                        val isSelected = uiState.selectedFriends.any { it.odiserId == friend.odiserId }
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { viewModel.toggleFriendSelection(friend) },
                                            label = { Text(friend.displayName ?: friend.email) },
                                            leadingIcon = if (isSelected) {
                                                { Icon(Icons.Filled.Check, null, Modifier.size(16.dp)) }
                                            } else null
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(Spacing.sm))
                            }
                            
                            // Manual member input
                            var newMemberName by remember { mutableStateOf("") }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = newMemberName,
                                    onValueChange = { newMemberName = it },
                                    label = { Text("Add person") },
                                    placeholder = { Text("Name") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    shape = InputFieldShape
                                )
                                FilledTonalButton(
                                    onClick = {
                                        viewModel.addManualMember(newMemberName)
                                        newMemberName = ""
                                    },
                                    enabled = newMemberName.isNotBlank()
                                ) {
                                    Icon(Icons.Filled.Add, null, Modifier.size(18.dp))
                                }
                            }
                            
                            // Show added manual members
                            if (uiState.manualSplitMembers.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(Spacing.sm))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                                ) {
                                    items(uiState.manualSplitMembers) { name ->
                                        InputChip(
                                            selected = true,
                                            onClick = { viewModel.removeManualMember(name) },
                                            label = { Text(name) },
                                            trailingIcon = { 
                                                Icon(Icons.Filled.Close, "Remove", Modifier.size(16.dp)) 
                                            }
                                        )
                                    }
                                }
                            }
                            
                            // Summary
                            val totalPeople = 1 + uiState.selectedFriends.size + uiState.manualSplitMembers.size
                            if (totalPeople > 1 && uiState.amount.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(Spacing.sm))
                                val amount = uiState.amount.toDoubleOrNull() ?: 0.0
                                val perPerson = amount / totalPeople
                                Text(
                                    text = "₹${String.format("%.0f", perPerson)} each ($totalPeople people)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(Spacing.lg))
            
            // Save Button
            Button(
                onClick = viewModel::saveTransaction,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !uiState.isSaving && uiState.amount.isNotEmpty(),
                shape = ButtonShapePill
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = if (uiState.transactionType == TransactionType.EXPENSE) 
                            "Add Expense" else "Add Income",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            
            // Split Balances Section at bottom (who owes whom)
            // Filter based on transaction type:
            // - Expense: show "you owe" (negative amounts) - balances where you need to pay
            // - Income: show "owes you" (positive amounts) - balances where you receive payment
            val filteredBalances = uiState.owedBalances.filter { balance ->
                if (uiState.transactionType == TransactionType.EXPENSE) {
                    balance.amount < 0 // You owe them
                } else {
                    balance.amount > 0 // They owe you
                }
            }
            
            if (filteredBalances.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Spacing.xl))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(Spacing.md))
                
                Text(
                    text = if (uiState.transactionType == TransactionType.EXPENSE) 
                        "Settle Debts" else "Collect Payments",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
                
                filteredBalances.forEach { owedBalance ->
                    OwedBalanceListItem(
                        owedBalance = owedBalance,
                        onSettle = { viewModel.showSettleSheet(owedBalance) }
                    )
                    Spacer(modifier = Modifier.height(Spacing.sm))
                }
            }
            
            Spacer(modifier = Modifier.height(Spacing.md))
        }
    }
    
    // Settle Bottom Sheet
    if (uiState.showSettleSheet && uiState.selectedSettleBalance != null) {
        SettleSheet(
            owedBalance = uiState.selectedSettleBalance!!,
            accounts = uiState.accounts,
            onDismiss = { viewModel.hideSettleSheet() },
            onConfirm = { amount, accountId -> viewModel.settleBalance(amount, accountId) }
        )
    }
}

private fun hasLocationPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED ||
    ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
}

private suspend fun getCurrentLocation(context: Context): Location? {
    return try {
        if (!hasLocationPermission(context)) return null
        
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        val cancellationTokenSource = CancellationTokenSource()
        
        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            cancellationTokenSource.token
        ).await()
    } catch (e: Exception) {
        null
    }
}

private fun getLocationName(context: Context, location: Location): String? {
    return try {
        val geocoder = Geocoder(context, Locale.getDefault())
        val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
        if (!addresses.isNullOrEmpty()) {
            val address = addresses[0]
            
            // Build a more complete location name
            val parts = mutableListOf<String>()
            
            // Add specific place/building name if available
            address.featureName?.let { 
                if (it != address.subLocality && it != address.locality && !it.matches(Regex("\\d+"))) {
                    parts.add(it)
                }
            }
            
            // Add sub-locality (neighborhood/area)
            address.subLocality?.let { parts.add(it) }
            
            // Add locality (city)
            address.locality?.let { parts.add(it) }
            
            // If we have parts, join them; otherwise use the full address line
            if (parts.isNotEmpty()) {
                parts.distinct().joinToString(", ")
            } else {
                // Fallback to full address line
                address.getAddressLine(0)?.split(",")?.take(2)?.joinToString(", ")
            }
        } else null
    } catch (e: Exception) {
        null
    }
}

@Composable
private fun TransactionTypeToggle(
    selectedType: TransactionType,
    onTypeSelected: (TransactionType) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(PillShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(Spacing.xxs),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xxs)
    ) {
        TransactionType.entries.forEach { type ->
            val isSelected = type == selectedType
            val backgroundColor by animateColorAsState(
                targetValue = if (isSelected) {
                    if (type == TransactionType.EXPENSE) Error else Green
                } else {
                    Color.Transparent
                },
                label = "tab_bg"
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) White else MaterialTheme.colorScheme.onSurfaceVariant,
                label = "tab_text"
            )
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(PillShape)
                    .background(backgroundColor)
                    .clickable { onTypeSelected(type) }
                    .padding(vertical = Spacing.sm),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = type.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor
                )
            }
        }
    }
}

@Composable
private fun AmountInput(
    amount: String,
    onAmountChange: (String) -> Unit,
    isExpense: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "₹",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        OutlinedTextField(
            value = amount,
            onValueChange = onAmountChange,
            textStyle = MaterialTheme.typography.displayMedium.copy(
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = if (isExpense) Error else Green
            ),
            placeholder = {
                Text(
                    text = "0",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent
            )
        )
    }
}

@Composable
private fun CategorySelector(
    categories: List<Category>,
    selectedCategory: Category?,
    onCategorySelected: (Category) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        items(
            items = categories,
            key = { it.id }
        ) { category ->
            val isSelected = category.id == selectedCategory?.id
            val categoryColor = Color(category.color)
            
            Column(
                modifier = Modifier
                    .clip(CardShapeSmall)
                    .background(
                        if (isSelected) categoryColor.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .border(
                        width = if (isSelected) 2.dp else 0.dp,
                        color = if (isSelected) categoryColor else Color.Transparent,
                        shape = CardShapeSmall
                    )
                    .clickable { onCategorySelected(category) }
                    .padding(Spacing.md),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(categoryColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Category,
                        contentDescription = null,
                        tint = categoryColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    text = category.name.split(" ").first(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) categoryColor else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
@Composable
private fun OwedBalanceListItem(
    owedBalance: OwedBalance,
    onSettle: () -> Unit
) {
    val isOwedToYou = owedBalance.amount > 0
    val displayAmount = kotlin.math.abs(owedBalance.amount)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isOwedToYou) 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            else 
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Group emoji
            Text(
                text = owedBalance.groupEmoji,
                style = MaterialTheme.typography.titleLarge
            )
            
            Spacer(modifier = Modifier.width(Spacing.sm))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = owedBalance.member.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (owedBalance.isLinkedFriend) {
                        Text(" 🔗", style = MaterialTheme.typography.bodySmall)
                    }
                }
                Text(
                    text = if (isOwedToYou) "owes you" else "you owe",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = owedBalance.groupName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Text(
                text = "₹${String.format("%.0f", displayAmount)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isOwedToYou) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.error
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettleSheet(
    owedBalance: OwedBalance,
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, accountId: String?) -> Unit
) {
    var amount by remember { mutableStateOf(kotlin.math.abs(owedBalance.amount).toString()) }
    var selectedAccount by remember { mutableStateOf<Account?>(null) }
    var accountExpanded by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    
    val isOwed = owedBalance.amount > 0 // They owe me
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg)
        ) {
            Text(
                text = if (isOwed) 
                    "Record payment from ${owedBalance.member.name}"
                else 
                    "Record payment to ${owedBalance.member.name}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(Spacing.sm))
            
            Text(
                text = "${owedBalance.groupEmoji} ${owedBalance.groupName}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(Spacing.xl))
            
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
            
            // Account selection dropdown
            ExposedDropdownMenuBox(
                expanded = accountExpanded,
                onExpandedChange = { accountExpanded = it }
            ) {
                OutlinedTextField(
                    value = selectedAccount?.name ?: "Select account",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Account") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                
                ExposedDropdownMenu(
                    expanded = accountExpanded,
                    onDismissRequest = { accountExpanded = false }
                ) {
                    accounts.forEach { account ->
                        DropdownMenuItem(
                            text = { Text(account.name) },
                            onClick = { 
                                selectedAccount = account
                                accountExpanded = false 
                            }
                        )
                    }
                }
            }
            
            if (isOwed) {
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(
                    text = "This will record income in your account",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = Spacing.xs)
                )
            } else {
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(
                    text = "This will record an expense from your account",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
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
            
            Spacer(modifier = Modifier.height(Spacing.lg))
        }
    }
}


