package com.theblankstate.epmanager.ui.recurring

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.theblankstate.epmanager.data.model.Category
import com.theblankstate.epmanager.data.model.RecurringExpense
import com.theblankstate.epmanager.data.model.RecurringFrequency
import com.theblankstate.epmanager.ui.components.formatCurrency
import com.theblankstate.epmanager.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringScreen(
    onNavigateBack: () -> Unit,
    onNavigateToHistory: (String) -> Unit,
    viewModel: RecurringViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Expenses",
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
                    contentDescription = "Add Recurring"
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
                // Info Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(Spacing.md),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(Spacing.sm))
                            Text(
                                text = "Scheduled expenses are automatically added on their due date when auto-add is enabled.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                
                if (uiState.recurringExpenses.isEmpty()) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Spacing.xxl),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "🔄",
                                    style = MaterialTheme.typography.displayLarge
                                )
                                Spacer(modifier = Modifier.height(Spacing.md))
                                Text(
                                    text = "No Scheduled Expenses",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Tap + to add subscriptions, bills, etc.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(
                        items = uiState.recurringExpenses,
                        key = { it.id }
                    ) { recurring ->
                        val category = uiState.categories.find { it.id == recurring.categoryId }
                        RecurringItem(
                            recurring = recurring,
                            category = category,
                            onToggleActive = { viewModel.toggleActive(recurring) },
                            onDelete = { viewModel.deleteRecurring(recurring) },
                            onClick = { onNavigateToHistory(recurring.id) }
                        )
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(Spacing.huge))
                }
            }
        }
    }
    
    // Add Sheet
    if (uiState.showAddDialog) {
        AddRecurringSheet(
            categories = uiState.categories,
            onDismiss = { viewModel.hideAddDialog() },
            onConfirm = { name, amount, categoryId, frequency, scheduleDay, autoAdd ->
                viewModel.addRecurringExpense(name, amount, categoryId, frequency, scheduleDay, autoAdd)
            }
        )
    }
}

@Composable
private fun RecurringItem(
    recurring: RecurringExpense,
    category: Category?,
    onToggleActive: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {

    var showDeleteDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (recurring.isActive) 
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = recurring.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (category != null) {
                            Text(
                                text = category.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(category.color)
                            )
                            Text(
                                text = " • ",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = recurring.frequency.label,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Text(
                    text = formatCurrency(recurring.amount),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Error
                )
            }
            
            Spacer(modifier = Modifier.height(Spacing.sm))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(Spacing.xxs))
                    Text(
                        text = "Next: ${formatDate(recurring.nextDueDate)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (recurring.autoAdd) {
                        Spacer(modifier = Modifier.width(Spacing.sm))
                        AssistChip(
                            onClick = {},
                            label = { Text("Auto", style = MaterialTheme.typography.labelSmall) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.AutoMode,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            modifier = Modifier.height(24.dp)
                        )
                    }
                }
                
                Row {
                    IconButton(onClick = onToggleActive) {
                        Icon(
                            imageVector = if (recurring.isActive) 
                                Icons.Filled.PauseCircle else Icons.Filled.PlayCircle,
                            contentDescription = if (recurring.isActive) "Pause" else "Resume",
                            tint = if (recurring.isActive) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
    
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Expense") },
            text = { Text("Are you sure you want to delete '${recurring.name}'?") },
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
private fun AddRecurringSheet(
    categories: List<Category>,
    onDismiss: () -> Unit,
    onConfirm: (name: String, amount: Double, categoryId: String?, frequency: RecurringFrequency, scheduleDay: Int, autoAdd: Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var selectedFrequency by remember { mutableStateOf(RecurringFrequency.MONTHLY) }
    var scheduleDay by remember { mutableIntStateOf(1) }
    var autoAdd by remember { mutableStateOf(true) }
    var expandedCategory by remember { mutableStateOf(false) }
    var expandedFrequency by remember { mutableStateOf(false) }
    var expandedScheduleDay by remember { mutableStateOf(false) }
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
            // Header
            Text(
                text = "Add Expense",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = Spacing.sm)
            )
            
            // Name
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                placeholder = { Text("Netflix, Rent, etc.") },
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
                prefix = { Text("₹") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = InputFieldShape
            )
            
            // Category Dropdown
            ExposedDropdownMenuBox(
                expanded = expandedCategory,
                onExpandedChange = { expandedCategory = it }
            ) {
                OutlinedTextField(
                    value = selectedCategory?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category (Optional)") },
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
            
            // Frequency Dropdown
            ExposedDropdownMenuBox(
                expanded = expandedFrequency,
                onExpandedChange = { expandedFrequency = it }
            ) {
                OutlinedTextField(
                    value = selectedFrequency.label,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Frequency") },
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
                    RecurringFrequency.entries.forEach { freq ->
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
            
            // Schedule Day Picker (based on frequency)
            when (selectedFrequency) {
                RecurringFrequency.WEEKLY, RecurringFrequency.BIWEEKLY -> {
                    val days = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
                    ExposedDropdownMenuBox(
                        expanded = expandedScheduleDay,
                        onExpandedChange = { expandedScheduleDay = it }
                    ) {
                        OutlinedTextField(
                            value = days.getOrElse(scheduleDay - 1) { "Monday" },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Repeat on") },
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
                RecurringFrequency.MONTHLY, RecurringFrequency.QUARTERLY -> {
                    ExposedDropdownMenuBox(
                        expanded = expandedScheduleDay,
                        onExpandedChange = { expandedScheduleDay = it }
                    ) {
                        OutlinedTextField(
                            value = getOrdinalDay(scheduleDay),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Day of month") },
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
                else -> { /* Daily and Yearly don't need day selection */ }
            }
            
            // Auto-add toggle
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
                            text = "Automatically add on due date",
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
                    Text("Add")
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
    return "$day$suffix of every month"
}
