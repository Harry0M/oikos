package com.theblankstate.epmanager.ui.budget

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.theblankstate.epmanager.data.model.BudgetWithSpending
import com.theblankstate.epmanager.ui.components.formatAmount
import com.theblankstate.epmanager.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    onNavigateBack: () -> Unit,
    onNavigateToHistory: (String) -> Unit,
    viewModel: BudgetViewModel = hiltViewModel(),
    currencyViewModel: com.theblankstate.epmanager.util.CurrencyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currencySymbol by currencyViewModel.currencySymbol.collectAsState(initial = "₹")
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Budgets",
                        fontWeight = FontWeight.Bold
                    ) 
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showAddBudgetSheet() },
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
                contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                // Compact Summary Header
                item {
                    SummaryHeader(
                        totalBudget = uiState.totalBudget,
                        totalSpent = uiState.totalSpent,
                        currencySymbol = currencySymbol
                    )
                }
                
                // Budget List
                if (uiState.budgets.isEmpty()) {
                    item {
                        EmptyBudgetState()
                    }
                } else {
                    items(
                        items = uiState.budgets,
                        key = { it.budget.id }
                    ) { budgetWithSpending ->
                        CompactBudgetItem(
                            budgetWithSpending = budgetWithSpending,
                            currencySymbol = currencySymbol,
                            onDelete = { viewModel.deleteBudget(budgetWithSpending.budget) },
                            onClick = { 
                                budgetWithSpending.category?.id?.let { onNavigateToHistory(it) }
                            }
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
    
    // Bottom Sheets
    if (uiState.showAddBudgetSheet) {
        AddBudgetBottomSheet(
            categories = uiState.categories,
            selectedCategory = uiState.selectedCategory,
            onDismiss = { viewModel.hideAddBudgetSheet() },
            onCategorySelect = { viewModel.showSelectCategorySheet() },
            onAddNewCategory = { viewModel.showAddCategorySheet() },
            onConfirm = { categoryId, amount, period ->
                viewModel.addBudget(categoryId, amount, period)
            }
        )
    }
    
    if (uiState.showSelectCategorySheet) {
        SelectCategoryBottomSheet(
            categories = uiState.categories,
            onDismiss = { viewModel.hideSelectCategorySheet() },
            onCategorySelected = { category ->
                viewModel.selectCategory(category)
            },
            onAddNewCategory = { viewModel.showAddCategorySheet() }
        )
    }
    
    if (uiState.showAddCategorySheet) {
        AddCategoryBottomSheet(
            onDismiss = { viewModel.hideAddCategorySheet() },
            onConfirm = { name, emoji, color, type ->
                viewModel.addCategory(name, emoji, color, type)
            }
        )
    }
}

/**
 * Compact summary header showing total budget overview
 */
@Composable
private fun SummaryHeader(
    totalBudget: Double,
    totalSpent: Double,
    currencySymbol: String
) {
    val percentage = if (totalBudget > 0) (totalSpent / totalBudget).coerceIn(0.0, 1.0) else 0.0
    val isOverBudget = totalSpent > totalBudget
    val animatedProgress by animateFloatAsState(
        targetValue = percentage.toFloat(),
        label = "summary_progress"
    )
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
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
                        text = formatAmount(totalSpent, currencySymbol),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isOverBudget) Error else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "of ${formatAmount(totalBudget, currencySymbol)} budgeted",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Text(
                    text = "${(percentage * 100).toInt()}%",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isOverBudget) Error else MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(Spacing.sm))
            
            // Full width progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedProgress)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isOverBudget) Error else MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}

/**
 * Empty state when no budgets exist
 */
@Composable
private fun EmptyBudgetState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "💰",
            style = MaterialTheme.typography.displayMedium
        )
        Spacer(modifier = Modifier.height(Spacing.sm))
        Text(
            text = "No budgets yet",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = "Tap + to set spending limits",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Compact budget item - minimal and clean
 */
@Composable
private fun CompactBudgetItem(
    budgetWithSpending: BudgetWithSpending,
    currencySymbol: String,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val category = budgetWithSpending.category
    val isOverBudget = budgetWithSpending.spent > budgetWithSpending.budget.amount
    val percentage = if (budgetWithSpending.budget.amount > 0) 
        (budgetWithSpending.spent / budgetWithSpending.budget.amount).coerceIn(0.0, 1.0) 
    else 0.0
    val animatedProgress by animateFloatAsState(
        targetValue = percentage.toFloat(),
        label = "progress"
    )
    val categoryColor = category?.let { Color(it.color) } ?: MaterialTheme.colorScheme.primary
    
    var showDeleteConfirm by remember { mutableStateOf(false) }
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category color indicator
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(categoryColor)
            )
            
            Spacer(modifier = Modifier.width(Spacing.sm))
            
            // Category name and period (fixed width)
            Column(modifier = Modifier.width(80.dp)) {
                Text(
                    text = category?.name ?: "Unknown",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
                Text(
                    text = budgetWithSpending.budget.period.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.width(Spacing.sm))
            
            // Progress bar in the middle (takes remaining space)
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(animatedProgress)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isOverBudget) Error else categoryColor)
                    )
                }
                Text(
                    text = "${(percentage * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isOverBudget) Error else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(Spacing.sm))
            
            // Amount (fixed width)
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.width(80.dp)
            ) {
                Text(
                    text = formatAmount(budgetWithSpending.spent, currencySymbol),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isOverBudget) Error else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "/ ${formatAmount(budgetWithSpending.budget.amount, currencySymbol)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Delete button
            IconButton(
                onClick = { showDeleteConfirm = true },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
    
    // Delete confirmation dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Budget?") },
            text = { Text("This will remove the budget for ${category?.name ?: "this category"}.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    }
                ) {
                    Text("Delete", color = Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}


