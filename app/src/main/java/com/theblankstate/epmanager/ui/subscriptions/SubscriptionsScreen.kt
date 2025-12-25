package com.theblankstate.epmanager.ui.subscriptions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.theblankstate.epmanager.data.model.Category
import com.theblankstate.epmanager.data.model.RecurringExpense
import com.theblankstate.epmanager.data.model.RecurringFrequency
import com.theblankstate.epmanager.ui.components.formatAmount
import com.theblankstate.epmanager.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SubscriptionsViewModel = hiltViewModel(),
    currencyViewModel: com.theblankstate.epmanager.util.CurrencyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currencySymbol by currencyViewModel.currencySymbol.collectAsState(initial = "₹")
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Subscriptions",
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
                    contentDescription = "Add Subscription"
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
                // Cost Summary Cards
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                    ) {
                        CostSummaryCard(
                            title = "Monthly",
                            amount = uiState.totalMonthlyAmount,
                            icon = Icons.Filled.CalendarMonth,
                            color = MaterialTheme.colorScheme.primary,
                            currencySymbol = currencySymbol,
                            modifier = Modifier.weight(1f)
                        )
                        CostSummaryCard(
                            title = "Yearly",
                            amount = uiState.totalYearlyAmount,
                            icon = Icons.Filled.DateRange,
                            color = MaterialTheme.colorScheme.tertiary,
                            currencySymbol = currencySymbol,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                // Upcoming Renewals Section
                if (uiState.upcomingRenewals.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(Spacing.md)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.NotificationsActive,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                    Spacer(modifier = Modifier.width(Spacing.sm))
                                    Text(
                                        text = "Upcoming Renewals",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Spacer(modifier = Modifier.height(Spacing.sm))
                                uiState.upcomingRenewals.forEach { subscription ->
                                    val category = uiState.categories.find { it.id == subscription.categoryId }
                                    UpcomingRenewalItem(
                                        subscription = subscription,
                                        category = category,
                                        currencySymbol = currencySymbol
                                    )
                                }
                            }
                        }
                    }
                }
                
                // All Subscriptions Header
                item {
                    Text(
                        text = "All Subscriptions (${uiState.subscriptions.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = Spacing.sm)
                    )
                }
                
                if (uiState.subscriptions.isEmpty()) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Spacing.xxl),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "📱",
                                    style = MaterialTheme.typography.displayLarge
                                )
                                Spacer(modifier = Modifier.height(Spacing.md))
                                Text(
                                    text = "No Subscriptions",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Add Netflix, Spotify, and more",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(
                        items = uiState.subscriptions,
                        key = { it.id }
                    ) { subscription ->
                        val category = uiState.categories.find { it.id == subscription.categoryId }
                        SubscriptionItem(
                            subscription = subscription,
                            category = category,
                            onEdit = { viewModel.showEditDialog(subscription) },
                            onToggleActive = { viewModel.toggleActive(subscription) },
                            onCancel = { viewModel.cancelSubscription(subscription) },
                            currencySymbol = currencySymbol
                        )
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(Spacing.huge))
                }
            }
        }
    }
    
    // Add/Edit Sheet
    if (uiState.showAddDialog) {
        AddEditSubscriptionSheet(
            existingSubscription = uiState.editingSubscription,
            categories = uiState.categories,
            currencySymbol = currencySymbol,
            onDismiss = { viewModel.hideDialog() },
            onConfirm = { name, amount, categoryId, frequency, scheduleDay, autoAdd ->
                viewModel.saveSubscription(name, amount, categoryId, frequency, scheduleDay, autoAdd)
            }
        )
    }
}

@Composable
private fun CostSummaryCard(
    title: String,
    amount: Double,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    currencySymbol: String,
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
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(Spacing.xs))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatAmount(amount, currencySymbol),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun UpcomingRenewalItem(
    subscription: RecurringExpense,
    category: Category?,
    currencySymbol: String
) {
    val daysUntil = ((subscription.nextDueDate - System.currentTimeMillis()) / (24 * 60 * 60 * 1000)).toInt()
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(
                    if (daysUntil <= 1) Error 
                    else if (daysUntil <= 3) Warning 
                    else Success
                )
        )
        Spacer(modifier = Modifier.width(Spacing.sm))
        Text(
            text = subscription.name,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = if (daysUntil == 0) "Today" else "in $daysUntil days",
            style = MaterialTheme.typography.labelMedium,
            color = if (daysUntil <= 1) Error else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(Spacing.sm))
        Text(
            text = formatAmount(subscription.amount, currencySymbol),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SubscriptionItem(
    subscription: RecurringExpense,
    category: Category?,
    onEdit: () -> Unit,
    onToggleActive: () -> Unit,
    onCancel: () -> Unit,
    currencySymbol: String
) {
    var showCancelDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
        colors = CardDefaults.cardColors(
            containerColor = if (subscription.isActive) 
                MaterialTheme.colorScheme.surface 
            else 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(Spacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Color Circle
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            category?.let { Color(it.color).copy(alpha = 0.2f) }
                                ?: MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Subscriptions,
                        contentDescription = null,
                        tint = category?.let { Color(it.color) }
                            ?: MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(Spacing.md))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = subscription.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = subscription.frequency.label,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (category != null) {
                            Text(
                                text = " • ${category.name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(category.color)
                            )
                        }
                    }
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatAmount(subscription.amount, currencySymbol),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Error
                    )
                    Text(
                        text = "Next: ${formatDate(subscription.nextDueDate)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // Show billing day based on frequency
                    Text(
                        text = getBillingDayText(subscription.frequency, subscription.scheduleDay),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(Spacing.sm))
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (subscription.autoAdd) {
                    AssistChip(
                        onClick = {},
                        label = { Text("Auto-pay", style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.AutoMode,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        modifier = Modifier.height(28.dp)
                    )
                    Spacer(modifier = Modifier.width(Spacing.sm))
                }
                
                IconButton(
                    onClick = onToggleActive,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (subscription.isActive) 
                            Icons.Filled.PauseCircle else Icons.Filled.PlayCircle,
                        contentDescription = if (subscription.isActive) "Pause" else "Resume",
                        tint = if (subscription.isActive) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                IconButton(
                    onClick = { showCancelDialog = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Cancel,
                        contentDescription = "Cancel",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
    
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Cancel Subscription") },
            text = { Text("Are you sure you want to cancel '${subscription.name}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onCancel()
                        showCancelDialog = false
                    }
                ) {
                    Text("Cancel Subscription", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("Keep")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditSubscriptionSheet(
    existingSubscription: RecurringExpense?,
    categories: List<Category>,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String, amount: Double, categoryId: String?, frequency: RecurringFrequency, scheduleDay: Int, autoAdd: Boolean) -> Unit
) {
    var name by remember { mutableStateOf(existingSubscription?.name ?: "") }
    var amount by remember { mutableStateOf(existingSubscription?.amount?.toString() ?: "") }
    var selectedCategory by remember { mutableStateOf<Category?>(categories.find { it.id == existingSubscription?.categoryId }) }
    var selectedFrequency by remember { mutableStateOf(existingSubscription?.frequency ?: RecurringFrequency.MONTHLY) }
    var scheduleDay by remember { mutableIntStateOf(existingSubscription?.scheduleDay ?: 1) }
    var autoAdd by remember { mutableStateOf(existingSubscription?.autoAdd ?: false) }
    var expandedCategory by remember { mutableStateOf(false) }
    var expandedFrequency by remember { mutableStateOf(false) }
    var expandedScheduleDay by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    // Common subscription presets
    val presets = listOf(
        "Netflix", "Spotify", "YouTube Premium", "Amazon Prime",
        "Disney+", "Apple Music", "iCloud", "Google One"
    )
    
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
            Text(
                text = if (existingSubscription != null) "Edit Subscription" else "Add Subscription",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = Spacing.sm)
            )
            
            // Quick preset buttons (only for new subscriptions)
            if (existingSubscription == null) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    items(presets) { preset ->
                        SuggestionChip(
                            onClick = { name = preset },
                            label = { Text(preset, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
            
            // Name
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Service Name") },
                placeholder = { Text("Netflix, Spotify, etc.") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = InputFieldShape
            )
            
            // Amount
            OutlinedTextField(
                value = amount,
                onValueChange = { 
                    amount = it.filter { c -> c.isDigit() || c == '.' }
                },
                label = { Text("Amount") },
                prefix = { Text(currencySymbol) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = InputFieldShape
            )
            
            // Frequency Dropdown
            ExposedDropdownMenuBox(
                expanded = expandedFrequency,
                onExpandedChange = { expandedFrequency = it }
            ) {
                OutlinedTextField(
                    value = selectedFrequency.label,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Billing Cycle") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedFrequency) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = InputFieldShape
                )
                
                ExposedDropdownMenu(
                    expanded = expandedFrequency,
                    onDismissRequest = { expandedFrequency = false }
                ) {
                    listOf(
                        RecurringFrequency.WEEKLY,
                        RecurringFrequency.MONTHLY,
                        RecurringFrequency.QUARTERLY,
                        RecurringFrequency.YEARLY
                    ).forEach { freq ->
                        DropdownMenuItem(
                            text = { Text(freq.label) },
                            onClick = {
                                selectedFrequency = freq
                                expandedFrequency = false
                            }
                        )
                    }
                }
            }
            
            // Billing Day Picker (based on frequency)
            when (selectedFrequency) {
                RecurringFrequency.WEEKLY -> {
                    val days = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
                    ExposedDropdownMenuBox(
                        expanded = expandedScheduleDay,
                        onExpandedChange = { expandedScheduleDay = it }
                    ) {
                        OutlinedTextField(
                            value = days.getOrElse(scheduleDay - 1) { "Monday" },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Billing Day") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedScheduleDay) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = InputFieldShape
                        )
                        
                        ExposedDropdownMenu(
                            expanded = expandedScheduleDay,
                            onDismissRequest = { expandedScheduleDay = false }
                        ) {
                            days.forEachIndexed { index, day ->
                                DropdownMenuItem(
                                    text = { Text(day) },
                                    onClick = {
                                        scheduleDay = index + 1
                                        expandedScheduleDay = false
                                    }
                                )
                            }
                        }
                    }
                }
                RecurringFrequency.MONTHLY, RecurringFrequency.QUARTERLY, RecurringFrequency.YEARLY -> {
                    ExposedDropdownMenuBox(
                        expanded = expandedScheduleDay,
                        onExpandedChange = { expandedScheduleDay = it }
                    ) {
                        OutlinedTextField(
                            value = getOrdinalDay(scheduleDay),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Billing Date") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedScheduleDay) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = InputFieldShape
                        )
                        
                        ExposedDropdownMenu(
                            expanded = expandedScheduleDay,
                            onDismissRequest = { expandedScheduleDay = false }
                        ) {
                            (1..28).forEach { day ->
                                DropdownMenuItem(
                                    text = { Text(getOrdinalDay(day)) },
                                    onClick = {
                                        scheduleDay = day
                                        expandedScheduleDay = false
                                    }
                                )
                            }
                        }
                    }
                }
                else -> { }
            }
            
            // Category Dropdown
            ExposedDropdownMenuBox(
                expanded = expandedCategory,
                onExpandedChange = { expandedCategory = it }
            ) {
                OutlinedTextField(
                    value = selectedCategory?.name ?: "None",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategory) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = InputFieldShape
                )
                
                ExposedDropdownMenu(
                    expanded = expandedCategory,
                    onDismissRequest = { expandedCategory = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("None") },
                        onClick = {
                            selectedCategory = null
                            expandedCategory = false
                        }
                    )
                    categories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category.name) },
                            onClick = {
                                selectedCategory = category
                                expandedCategory = false
                            }
                        )
                    }
                }
            }
            
            // Auto-pay toggle
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.md),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-add transaction",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Add expense on renewal date",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = autoAdd,
                        onCheckedChange = { autoAdd = it }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(Spacing.md))
            
            // Action Buttons
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
                        if (name.isNotBlank() && amountValue != null && amountValue > 0) {
                            onConfirm(name, amountValue, selectedCategory?.id, selectedFrequency, scheduleDay, autoAdd)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = name.isNotBlank() && amount.toDoubleOrNull()?.let { it > 0 } == true,
                    shape = ButtonShapePill
                ) {
                    Text(if (existingSubscription != null) "Save" else "Add")
                }
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    return SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
}

private fun getOrdinalDay(day: Int): String {
    val suffix = when {
        day in 11..13 -> "th"
        day % 10 == 1 -> "st"
        day % 10 == 2 -> "nd"
        day % 10 == 3 -> "rd"
        else -> "th"
    }
    return "$day$suffix of each month"
}

private fun getBillingDayText(frequency: RecurringFrequency, scheduleDay: Int): String {
    val days = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    return when (frequency) {
        RecurringFrequency.DAILY -> "Every day"
        RecurringFrequency.WEEKLY -> "Every ${days.getOrElse(scheduleDay - 1) { "Mon" }}"
        RecurringFrequency.BIWEEKLY -> "Every 2nd ${days.getOrElse(scheduleDay - 1) { "Mon" }}"
        RecurringFrequency.MONTHLY -> "Every ${scheduleDay}${getOrdinalSuffix(scheduleDay)}"
        RecurringFrequency.QUARTERLY -> "Every ${scheduleDay}${getOrdinalSuffix(scheduleDay)} (quarterly)"
        RecurringFrequency.YEARLY -> "Yearly"
    }
}

private fun getOrdinalSuffix(day: Int): String {
    return when {
        day in 11..13 -> "th"
        day % 10 == 1 -> "st"
        day % 10 == 2 -> "nd"
        day % 10 == 3 -> "rd"
        else -> "th"
    }
}
