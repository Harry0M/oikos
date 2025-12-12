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
import com.theblankstate.epmanager.ui.components.formatCurrency
import com.theblankstate.epmanager.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(
    onNavigateBack: () -> Unit,
    viewModel: GoalsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
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
                        totalTarget = uiState.totalTarget
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
                        onContribute = { viewModel.showContributeDialog(goal) },
                        onDelete = { viewModel.deleteGoal(goal) }
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
                    CompletedGoalCard(goal = goal)
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(Spacing.huge))
            }
        }
    }
    
    // Add Goal Dialog
    if (uiState.showAddDialog) {
        AddGoalDialog(
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
            onDismiss = { viewModel.hideContributeDialog() },
            onContribute = { amount -> viewModel.addContribution(goal.id, amount) }
        )
    }
}

@Composable
private fun OverviewCard(totalSaved: Double, totalTarget: Double) {
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
                        text = formatCurrency(totalSaved),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "of ${formatCurrency(totalTarget)}",
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
    onContribute: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth()
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
                            text = "${formatCurrency(goal.savedAmount)} of ${formatCurrency(goal.targetAmount)}",
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
                Text(
                    text = "${(goal.progress * 100).toInt()}% saved",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(goal.color)
                )
                
                Text(
                    text = "${formatCurrency(goal.remaining)} to go",
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
private fun CompletedGoalCard(goal: SavingsGoal) {
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
                    text = formatCurrency(goal.targetAmount),
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
private fun AddGoalDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, amount: Double, presetIndex: Int, targetDate: Long?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var selectedPreset by remember { mutableIntStateOf(8) } // Default: General
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Savings Goal", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
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
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Target amount
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Target Amount") },
                    prefix = { Text("₹") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amountValue = amount.toDoubleOrNull()
                    if (name.isNotBlank() && amountValue != null && amountValue > 0) {
                        onConfirm(name, amountValue, selectedPreset, null)
                    }
                },
                enabled = name.isNotBlank() && amount.toDoubleOrNull()?.let { it > 0 } == true
            ) {
                Text("Create Goal")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ContributeDialog(
    goal: SavingsGoal,
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
                    text = "Current: ${formatCurrency(goal.savedAmount)} / ${formatCurrency(goal.targetAmount)}",
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
