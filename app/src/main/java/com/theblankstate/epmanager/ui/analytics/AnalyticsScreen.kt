package com.theblankstate.epmanager.ui.analytics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.theblankstate.epmanager.ui.components.*
import com.theblankstate.epmanager.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = hiltViewModel(),
    currencyViewModel: com.theblankstate.epmanager.util.CurrencyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currencySymbol by currencyViewModel.currencySymbol.collectAsState(initial = "₹")
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        // Header
        item {
            Text(
                text = "Analytics",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        // Period Selector
        item {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                AnalyticsPeriod.entries.forEachIndexed { index, period ->
                    SegmentedButton(
                        selected = uiState.selectedPeriod == period,
                        onClick = { viewModel.selectPeriod(period) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = AnalyticsPeriod.entries.size
                        )
                    ) {
                        Text(
                            text = period.label,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
        
        if (uiState.isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        } else {
            // Summary Cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    // Expenses Card
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(Spacing.md)) {
                            Text(
                                text = "Expenses",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = formatAmount(uiState.totalExpenses, currencySymbol),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    
                    // Income Card
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer // or secondaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(Spacing.md)) {
                            Text(
                                text = "Income",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = formatAmount(uiState.totalIncome, currencySymbol),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
            
            // Pie Chart Section
            if (uiState.categorySpending.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Spacing.lg)
                        ) {
                            Text(
                                text = "Spending by Category",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            
                            Spacer(modifier = Modifier.height(Spacing.lg))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Pie Chart
                                SpendingPieChart(
                                    categorySpending = uiState.categorySpending.take(6),
                                    size = 160.dp,
                                    strokeWidth = 28.dp
                                )
                                
                                // Legend
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                                ) {
                                    uiState.categorySpending.take(4).forEach { spending ->
                                        ChartLegendItem(
                                            color = Color(spending.category.color),
                                            label = spending.category.name.split(" ").first(),
                                            value = "${spending.percentage.toInt()}%"
                                        )
                                    }
                                    if (uiState.categorySpending.size > 4) {
                                        Text(
                                            text = "+${uiState.categorySpending.size - 4} more",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // Monthly Bar Chart
            if (uiState.monthlyData.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Spacing.lg)
                        ) {
                            Text(
                                text = "Monthly Spending",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            
                            Spacer(modifier = Modifier.height(Spacing.lg))
                            
                            MonthlyBarChart(
                                data = uiState.monthlyData.map { it.month to it.expense },
                                barColor = MaterialTheme.colorScheme.primary,
                                height = 120.dp
                            )
                        }
                    }
                }
            }
            
            // Quick Stats
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    Card(modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.padding(Spacing.md)) {
                            Text(
                                text = "Avg. Daily",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatAmount(uiState.averageDailySpend, currencySymbol),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Card(modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.padding(Spacing.md)) {
                            Text(
                                text = "Top Category",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = uiState.topCategory?.category?.name?.split(" ")?.first() ?: "None",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            
            // Category Breakdown List
            if (uiState.categorySpending.isNotEmpty()) {
                item {
                    Text(
                        text = "Category Breakdown",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = Spacing.md)
                    )
                }
                
                items(
                    items = uiState.categorySpending,
                    key = { it.category.id }
                ) { spending ->
                    CategorySpendingItem(spending = spending, currencySymbol = currencySymbol)
                }
            }
            
            // Empty State
            if (uiState.categorySpending.isEmpty() && !uiState.isLoading) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Spacing.xxl),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "📊",
                                style = MaterialTheme.typography.displayLarge
                            )
                            Spacer(modifier = Modifier.height(Spacing.md))
                            Text(
                                text = "No Data Yet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(Spacing.xs))
                            Text(
                                text = "Add some transactions to see your spending analytics",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(Spacing.huge))
            }
        }
    }
}

@Composable
private fun CategorySpendingItem(
    spending: CategorySpending,
    currencySymbol: String
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = spending.category.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Text(
                        text = "${spending.percentage.toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(Spacing.xs))
                
                SpendingProgressBar(
                    spent = spending.amount,
                    budget = spending.amount, // Show full bar
                    color = Color(spending.category.color)
                )
            }
            
            Spacer(modifier = Modifier.width(Spacing.md))
            
            Text(
                text = formatAmount(spending.amount, currencySymbol),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(spending.category.color)
            )
        }
    }
}
