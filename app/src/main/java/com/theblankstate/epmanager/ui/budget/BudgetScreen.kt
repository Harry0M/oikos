package com.theblankstate.epmanager.ui.budget

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.theblankstate.epmanager.data.model.BudgetPeriod
import com.theblankstate.epmanager.data.model.BudgetWithSpending
import com.theblankstate.epmanager.data.model.Category
import com.theblankstate.epmanager.ui.components.SpendingProgressBar
import com.theblankstate.epmanager.ui.components.formatCurrency
import com.theblankstate.epmanager.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    onNavigateBack: () -> Unit,
    viewModel: BudgetViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Budgets",
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
                    contentDescription = "Add Budget"
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
                // Overview Card
                item {
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
                                text = "Monthly Overview",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            
                            Spacer(modifier = Modifier.height(Spacing.md))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Total Budget",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = formatCurrency(uiState.totalBudget),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Spent",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = formatCurrency(uiState.totalSpent),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (uiState.totalSpent > uiState.totalBudget) 
                                            Error else MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(Spacing.md))
                            
                            SpendingProgressBar(
                                spent = uiState.totalSpent,
                                budget = uiState.totalBudget,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                
                // Budget List
                if (uiState.budgets.isEmpty()) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Spacing.xxl),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "💰",
                                    style = MaterialTheme.typography.displayLarge
                                )
                                Spacer(modifier = Modifier.height(Spacing.md))
                                Text(
                                    text = "No Budgets Yet",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Tap + to create a budget",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    item {
                        Text(
                            text = "Your Budgets",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    items(
                        items = uiState.budgets,
                        key = { it.budget.id }
                    ) { budgetWithSpending ->
                        BudgetItem(
                            budgetWithSpending = budgetWithSpending,
                            onDelete = { viewModel.deleteBudget(budgetWithSpending.budget) }
                        )
                    }
                }
                
                // Bottom spacing
                item {
                    Spacer(modifier = Modifier.height(Spacing.huge))
                }
            }
        }
    }
    
    // Add Budget Dialog
    if (uiState.showAddDialog) {
        AddBudgetDialog(
            categories = uiState.categories,
            onDismiss = { viewModel.hideAddDialog() },
            onConfirm = { categoryId, amount, period ->
                viewModel.addBudget(categoryId, amount, period)
            }
        )
    }
}

@Composable
private fun BudgetItem(
    budgetWithSpending: BudgetWithSpending,
    onDelete: () -> Unit
) {
    val category = budgetWithSpending.category
    val isOverBudget = budgetWithSpending.spent > budgetWithSpending.budget.amount
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(Spacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = category?.name ?: "Unknown",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = budgetWithSpending.budget.period.name.lowercase()
                            .replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = formatCurrency(budgetWithSpending.spent),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isOverBudget) Error else category?.let { Color(it.color) } 
                                ?: MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "of ${formatCurrency(budgetWithSpending.budget.amount)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(Spacing.sm))
            
            SpendingProgressBar(
                spent = budgetWithSpending.spent,
                budget = budgetWithSpending.budget.amount,
                color = category?.let { Color(it.color) } ?: MaterialTheme.colorScheme.primary
            )
            
            if (isOverBudget) {
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    text = "⚠️ Over budget by ${formatCurrency(budgetWithSpending.spent - budgetWithSpending.budget.amount)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Error
                )
            } else {
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    text = "${formatCurrency(budgetWithSpending.remaining)} remaining",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddBudgetDialog(
    categories: List<Category>,
    onDismiss: () -> Unit,
    onConfirm: (categoryId: String, amount: Double, period: BudgetPeriod) -> Unit
) {
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var amount by remember { mutableStateOf("") }
    var selectedPeriod by remember { mutableStateOf(BudgetPeriod.MONTHLY) }
    var expandedCategory by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Budget",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                // Category Dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedCategory,
                    onExpandedChange = { expandedCategory = it }
                ) {
                    OutlinedTextField(
                        value = selectedCategory?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategory) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
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
                
                // Amount
                OutlinedTextField(
                    value = amount,
                    onValueChange = { 
                        amount = it.filter { c -> c.isDigit() || c == '.' }
                    },
                    label = { Text("Budget Amount") },
                    prefix = { Text("₹") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Period
                Text(
                    text = "Period",
                    style = MaterialTheme.typography.labelMedium
                )
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(BudgetPeriod.WEEKLY, BudgetPeriod.MONTHLY).forEachIndexed { index, period ->
                        SegmentedButton(
                            selected = selectedPeriod == period,
                            onClick = { selectedPeriod = period },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = 2)
                        ) {
                            Text(period.name.lowercase().replaceFirstChar { it.uppercase() })
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amountValue = amount.toDoubleOrNull()
                    if (selectedCategory != null && amountValue != null && amountValue > 0) {
                        onConfirm(selectedCategory!!.id, amountValue, selectedPeriod)
                    }
                },
                enabled = selectedCategory != null && amount.toDoubleOrNull()?.let { it > 0 } == true
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
