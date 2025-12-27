package com.theblankstate.epmanager.ui.transactions

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.theblankstate.epmanager.ui.components.EmptyTransactionState
import com.theblankstate.epmanager.ui.components.TransactionItem
import com.theblankstate.epmanager.ui.theme.InputFieldShape
import com.theblankstate.epmanager.ui.theme.Spacing
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    onNavigateToAddTransaction: () -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToTransactionDetail: (String) -> Unit = {},
    viewModel: TransactionsViewModel = hiltViewModel(),
    currencyViewModel: com.theblankstate.epmanager.util.CurrencyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currencySymbol by currencyViewModel.currencySymbol.collectAsState(initial = "₹")
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    
    // Date Picker Dialog
    if (uiState.showDatePicker) {
        val datePickerState = rememberDatePickerState()
        
        DatePickerDialog(
            onDismissRequest = { viewModel.hideDatePicker() },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { date ->
                            if (uiState.isSelectingStartDate) {
                                viewModel.setCustomStartDate(date)
                                // Show end date picker next
                                viewModel.showDatePicker(false)
                            } else {
                                viewModel.setCustomEndDate(date)
                            }
                        }
                    }
                ) {
                    Text(if (uiState.isSelectingStartDate) "Next" else "Done")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideDatePicker() }) {
                    Text("Cancel")
                }
            }
        ) {
            Column {
                Text(
                    text = if (uiState.isSelectingStartDate) "Select Start Date" else "Select End Date",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                )
                DatePicker(state = datePickerState)
            }
        }
    }
    
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddTransaction,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Add Transaction"
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Header with search
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.md)
                    .padding(top = Spacing.md)
            ) {
                Text(
                    text = "Transactions",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(Spacing.md))
                
                // Search Bar
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = viewModel::updateSearchQuery,
                    placeholder = { Text("Search transactions...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = InputFieldShape,
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                )
                
                Spacer(modifier = Modifier.height(Spacing.md))
                
                // Type Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    TypeFilter.entries.forEach { filter ->
                        FilterChip(
                            selected = uiState.typeFilter == filter,
                            onClick = { viewModel.updateTypeFilter(filter) },
                            label = { Text(filter.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(Spacing.sm))
                
                // Time Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    TimeFilter.entries.forEach { filter ->
                        FilterChip(
                            selected = uiState.timeFilter == filter,
                            onClick = { viewModel.updateTimeFilter(filter) },
                            label = { 
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (filter == TimeFilter.CUSTOM) {
                                        Icon(
                                            imageVector = Icons.Filled.DateRange,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(filter.label)
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        )
                    }
                }
                
                // Custom Date Range Display
                if (uiState.timeFilter == TimeFilter.CUSTOM && uiState.customStartDate != null && uiState.customEndDate != null) {
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Spacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Custom Range",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = "${dateFormat.format(Date(uiState.customStartDate!!))} - ${dateFormat.format(Date(uiState.customEndDate!!))}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            
                            IconButton(onClick = { viewModel.clearCustomDates() }) {
                                Icon(
                                    imageVector = Icons.Filled.Clear,
                                    contentDescription = "Clear dates",
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(Spacing.sm))
                
                // Category Filter Chips
                if (uiState.availableCategories.isNotEmpty()) {
                    Text(
                        text = "Categories",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        // "All" category chip
                        FilterChip(
                            selected = uiState.selectedCategoryId == null,
                            onClick = { viewModel.updateCategoryFilter(null) },
                            label = { Text("All") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        )
                        
                        // Category chips
                        uiState.availableCategories.forEach { category ->
                            FilterChip(
                                selected = uiState.selectedCategoryId == category.id,
                                onClick = { viewModel.updateCategoryFilter(category.id) },
                                label = { Text(category.name) },
                                leadingIcon = {
                                    Surface(
                                        shape = MaterialTheme.shapes.small,
                                        color = Color(category.color),
                                        modifier = Modifier.size(12.dp)
                                    ) {}
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(category.color).copy(alpha = 0.2f),
                                    selectedLabelColor = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(Spacing.sm))
                }
                
                // Results count
                Text(
                    text = "${uiState.transactions.size} transaction${if (uiState.transactions.size != 1) "s" else ""}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(Spacing.sm))
            
            // Transaction List
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.transactions.isEmpty()) {
                EmptyTransactionState(
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = Spacing.md,
                        end = Spacing.md,
                        bottom = Spacing.huge
                    ),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    items(
                        items = uiState.transactions,
                        key = { it.transaction.id }
                    ) { transactionWithCategory ->
                        TransactionItem(
                            transaction = transactionWithCategory.transaction,
                            categoryName = transactionWithCategory.categoryName,
                            categoryColor = transactionWithCategory.categoryColor,
                            currencySymbol = currencySymbol,
                            onClick = { onNavigateToTransactionDetail(transactionWithCategory.transaction.id) }
                        )
                    }
                }
            }
        }
    }
}
