package com.theblankstate.epmanager.ui.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.theblankstate.epmanager.data.model.GoalPresets
import com.theblankstate.epmanager.data.model.SavingsGoal
import com.theblankstate.epmanager.ui.components.formatAmount
import com.theblankstate.epmanager.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToHistory: (String) -> Unit,
    viewModel: GoalsViewModel = hiltViewModel(),
    currencyViewModel: com.theblankstate.epmanager.util.CurrencyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currencySymbol by currencyViewModel.currencySymbol.collectAsState(initial = "₹")
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Savings Goals",
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
                Icon(Icons.Filled.Add, contentDescription = "Add Goal")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            // Overview Card
            if (uiState.totalTarget > 0) {
                item {
                    OverviewCard(
                        totalSaved = uiState.totalSaved,
                        totalTarget = uiState.totalTarget,
                        currencySymbol = currencySymbol
                    )
                }
            }
            
            // Active Goals
            if (uiState.activeGoals.isEmpty() && !uiState.isLoading) {
                item {
                    EmptyGoalsCard(onAddClick = { viewModel.showAddDialog() })
                }
            } else {
                items(
                    items = uiState.activeGoals,
                    key = { it.id }
                ) { goal ->
                    GoalCard(
                        goal = goal,
                        currencySymbol = currencySymbol,
                        onContribute = { viewModel.showContributeDialog(goal) },
                        onDelete = { viewModel.deleteGoal(goal) },
                        onClick = { onNavigateToHistory(goal.id) }
                    )
                }
            }
            
            // Completed Goals
            if (uiState.completedGoals.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(Spacing.md))
                    Text(
                        text = "Completed 🎉",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                items(
                    items = uiState.completedGoals,
                    key = { it.id }
                ) { goal ->
                    CompletedGoalCard(goal = goal, currencySymbol = currencySymbol)
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(Spacing.huge))
            }
        }
    }
    
    // Add Goal Sheet
    if (uiState.showAddDialog) {
        AddGoalSheet(
            onDismiss = { viewModel.hideAddDialog() },
            onConfirm = { name, amount, presetIndex, targetDate ->
                viewModel.createGoal(name, amount, presetIndex, targetDate)
            }
        )
    }
    
    // Contribute Dialog
    uiState.showContributeDialog?.let { goal ->
        ContributeDialog(
            goal = goal,
            currencySymbol = currencySymbol,
            onDismiss = { viewModel.hideContributeDialog() },
            onContribute = { amount -> viewModel.addContribution(goal.id, amount) }
        )
    }
}

@Composable
private fun OverviewCard(totalSaved: Double, totalTarget: Double, currencySymbol: String) {
    val progress = if (totalTarget > 0) (totalSaved / totalTarget).toFloat() else 0f
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(Spacing.lg)
        ) {
            Text(
                text = "Total Savings Progress",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Spacer(modifier = Modifier.height(Spacing.md))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = formatAmount(totalSaved, currencySymbol),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "of ${formatAmount(totalTarget, currencySymbol)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
                
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Success
                )
            }
            
            Spacer(modifier = Modifier.height(Spacing.md))
            
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(ButtonShapePill),
                color = Success,
                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
            )
        }
    }
}

@Composable
private fun GoalCard(
    goal: SavingsGoal,
    currencySymbol: String,
    onContribute: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(Spacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(goal.color).copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = getGoalEmoji(goal.icon),
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(Spacing.md))
                    
                    Column {
                        Text(
                            text = goal.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${formatAmount(goal.savedAmount, currencySymbol)} of ${formatAmount(goal.targetAmount, currencySymbol)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "Options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(Spacing.md))
            
            // Progress bar
            LinearProgressIndicator(
                progress = { goal.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(ButtonShapePill),
                color = Color(goal.color),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            
            Spacer(modifier = Modifier.height(Spacing.sm))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${(goal.progress * 100).toInt()}% saved",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(goal.color)
                    )
                    // Show target date if set
                    goal.targetDate?.let { targetDate ->
                        val daysRemaining = ((targetDate - System.currentTimeMillis()) / (24 * 60 * 60 * 1000)).toInt()
                        val dateText = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                            .format(java.util.Date(targetDate))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.CalendarMonth,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = if (daysRemaining >= 0) MaterialTheme.colorScheme.primary else Error
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (daysRemaining >= 0) "$daysRemaining days left" else "Overdue",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (daysRemaining >= 0) MaterialTheme.colorScheme.primary else Error
                            )
                        }
                    }
                }
                
                Text(
                    text = "${formatAmount(goal.remaining, currencySymbol)} to go",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(Spacing.md))
            
            Button(
                onClick = onContribute,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(goal.color)
                )
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text("Add Money")
            }
        }
    }
    
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Goal") },
            text = { Text("Delete '${goal.name}'? This cannot be undone.") },
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

@Composable
private fun CompletedGoalCard(goal: SavingsGoal, currencySymbol: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Success.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = Success,
                modifier = Modifier.size(32.dp)
            )
            
            Spacer(modifier = Modifier.width(Spacing.md))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = goal.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = formatAmount(goal.targetAmount, currencySymbol),
                    style = MaterialTheme.typography.bodySmall,
                    color = Success
                )
            }
        }
    }
}

@Composable
private fun EmptyGoalsCard(onAddClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.xxl),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🎯",
                style = MaterialTheme.typography.displayLarge
            )
            Spacer(modifier = Modifier.height(Spacing.md))
            Text(
                text = "No Savings Goals Yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Start saving for something special!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(Spacing.lg))
            Button(onClick = onAddClick) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text("Create Your First Goal")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddGoalSheet(
    onDismiss: () -> Unit,
    onConfirm: (name: String, amount: Double, presetIndex: Int, targetDate: Long?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var selectedPreset by remember { mutableIntStateOf(8) } // Default: General
    var targetDate by remember { mutableStateOf<Long?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000)
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
                text = "New Savings Goal",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = Spacing.sm)
            )
            
            // Preset selector
            Text(
                text = "Choose a theme",
                style = MaterialTheme.typography.labelMedium
            )
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                itemsIndexed(GoalPresets.presets) { index, preset ->
                    FilterChip(
                        selected = selectedPreset == index,
                        onClick = { selectedPreset = index },
                        label = { Text(preset.label) }
                    )
                }
            }
            
            // Goal name
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Goal Name") },
                placeholder = { Text("e.g., Trip to Goa") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = InputFieldShape
            )
            
            // Target amount
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Target Amount") },
                prefix = { Text("₹") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = InputFieldShape
            )
            
            // Target date picker
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true }
            ) {
                OutlinedTextField(
                    value = targetDate?.let { 
                        java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                            .format(java.util.Date(it))
                    } ?: "",
                    onValueChange = {},
                    readOnly = true,
                    enabled = false,
                    label = { Text("Target Completion Date (Optional)") },
                    placeholder = { Text("Select date") },
                    trailingIcon = {
                        Icon(Icons.Filled.CalendarMonth, contentDescription = null)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = InputFieldShape
                )
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
                            onConfirm(name, amountValue, selectedPreset, targetDate)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = name.isNotBlank() && amount.toDoubleOrNull()?.let { it > 0 } == true,
                    shape = ButtonShapePill
                ) {
                    Text("Create Goal")
                }
            }
        }
    }
    
    // Date Picker Dialog
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        targetDate = datePickerState.selectedDateMillis
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun ContributeDialog(
    goal: SavingsGoal,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onContribute: (Double) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to ${goal.name}") },
        text = {
            Column {
                Text(
                    text = "Current: ${formatAmount(goal.savedAmount, currencySymbol)} / ${formatAmount(goal.targetAmount, currencySymbol)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(Spacing.md))
                
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Amount to Add") },
                    prefix = { Text("₹") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(Spacing.sm))
                
                // Quick amounts
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    listOf(500, 1000, 5000).forEach { quickAmount ->
                        AssistChip(
                            onClick = { amount = quickAmount.toString() },
                            label = { Text("₹$quickAmount") }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    amount.toDoubleOrNull()?.let { onContribute(it) }
                },
                enabled = amount.toDoubleOrNull()?.let { it > 0 } == true
            ) {
                Text("Add Money")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun getGoalEmoji(icon: String): String {
    return when (icon) {
        "Flight" -> "✈️"
        "DirectionsCar" -> "🚗"
        "Home" -> "🏠"
        "PhoneAndroid" -> "📱"
        "School" -> "🎓"
        "Favorite" -> "💍"
        "LocalHospital" -> "🏥"
        "CardGiftcard" -> "🎁"
        else -> "💰"
    }
}
