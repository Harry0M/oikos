package com.theblankstate.epmanager.ui.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.theblankstate.epmanager.ui.components.*
import com.theblankstate.epmanager.ui.theme.*

enum class AnalyticsDetailType {
    FINANCIAL_HEALTH,
    SPENDING_TREND,
    CATEGORY_SPENDING,
    MONTHLY_OVERVIEW,
    GOALS,
    DEBTS,
    BUDGET,
    ACCOUNTS,
    TRANSACTIONS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = hiltViewModel(),
    currencyViewModel: com.theblankstate.epmanager.util.CurrencyViewModel = hiltViewModel(),
    onNavigateToDetail: (AnalyticsDetailType) -> Unit = {}
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
            // ==========================================
            // 1. FINANCIAL HEALTH (TOP)
            // ==========================================
            item {
                ClickableCard(
                    onClick = { onNavigateToDetail(AnalyticsDetailType.FINANCIAL_HEALTH) },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.lg),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CardHeader(
                            title = "Financial Health",
                            icon = Icons.Filled.Favorite
                        )
                        
                        Spacer(modifier = Modifier.height(Spacing.md))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FinancialHealthGauge(
                                score = uiState.healthScore,
                                size = 140.dp,
                                strokeWidth = 14.dp
                            )
                            
                            Column(
                                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                            ) {
                                StatItem(
                                    label = "Net Worth",
                                    value = formatAmount(uiState.netWorth, currencySymbol),
                                    valueColor = if (uiState.netWorth >= 0) 
                                        MaterialTheme.colorScheme.primary 
                                    else 
                                        MaterialTheme.colorScheme.error,
                                    isLarge = true
                                )
                                
                                StatItem(
                                    label = "Savings Rate",
                                    value = "${uiState.savingsRate.toInt()}%",
                                    valueColor = when {
                                        uiState.savingsRate >= 20 -> Color(0xFF22C55E)
                                        uiState.savingsRate >= 0 -> Color(0xFFF59E0B)
                                        else -> MaterialTheme.colorScheme.error
                                    }
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(Spacing.lg))
                        
                        SavingsRateBar(rate = uiState.savingsRate)
                    }
                }
            }
            
            // ==========================================
            // 2. INCOME VS EXPENSES
            // ==========================================
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
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
                    
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
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
            
            // ==========================================
            // 3. DAILY SPENDING TREND (Before Monthly)
            // ==========================================
            if (uiState.dailySpending.isNotEmpty()) {
                item {
                    ClickableCard(
                        onClick = { onNavigateToDetail(AnalyticsDetailType.SPENDING_TREND) }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Spacing.lg)
                        ) {
                            CardHeader(
                                title = "Spending Trend (7 Days)",
                                icon = Icons.Filled.TrendingDown,
                                trailing = "Avg: ${formatAmount(uiState.averageDailySpend, currencySymbol)}"
                            )
                            
                            Spacer(modifier = Modifier.height(Spacing.lg))
                            
                            DailySpendingLineChart(
                                data = uiState.dailySpending,
                                height = 100.dp
                            )
                        }
                    }
                }
            }
            
            // ==========================================
            // 4. MONTHLY OVERVIEW (After Daily)
            // ==========================================
            if (uiState.monthlyData.isNotEmpty()) {
                item {
                    ClickableCard(
                        onClick = { onNavigateToDetail(AnalyticsDetailType.MONTHLY_OVERVIEW) }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Spacing.lg)
                        ) {
                            CardHeader(
                                title = "Monthly Overview",
                                icon = Icons.Filled.CalendarMonth
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
            
            // ==========================================
            // 5. CATEGORY SPENDING PIE CHART
            // ==========================================
            if (uiState.categorySpending.isNotEmpty()) {
                item {
                    ClickableCard(
                        onClick = { onNavigateToDetail(AnalyticsDetailType.CATEGORY_SPENDING) }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Spacing.lg)
                        ) {
                            CardHeader(
                                title = "Spending by Category",
                                icon = Icons.Filled.PieChart
                            )
                            
                            Spacer(modifier = Modifier.height(Spacing.lg))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SpendingPieChart(
                                    categorySpending = uiState.categorySpending.take(6),
                                    size = 160.dp,
                                    strokeWidth = 28.dp
                                )
                                
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
            
            // ==========================================
            // 6. QUICK STATS ROW
            // ==========================================
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
            
            // ==========================================
            // 7. GOALS PROGRESS
            // ==========================================
            if (uiState.activeGoals.isNotEmpty()) {
                item {
                    ClickableCard(
                        onClick = { onNavigateToDetail(AnalyticsDetailType.GOALS) }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Spacing.lg)
                        ) {
                            CardHeader(
                                title = "Savings Goals",
                                icon = Icons.Filled.Savings,
                                iconTint = MaterialTheme.colorScheme.tertiary,
                                trailing = "${uiState.goalsProgress.toInt()}%"
                            )
                            
                            Spacer(modifier = Modifier.height(Spacing.sm))
                            
                            Text(
                                text = "${formatAmount(uiState.totalGoalsSaved, currencySymbol)} / ${formatAmount(uiState.totalGoalsTarget, currencySymbol)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Spacer(modifier = Modifier.height(Spacing.md))
                            
                            uiState.activeGoals.take(3).forEach { goalProgress ->
                                GoalProgressMiniBar(
                                    name = goalProgress.goal.name,
                                    progress = goalProgress.progress / 100f,
                                    color = goalProgress.goal.color
                                )
                                Spacer(modifier = Modifier.height(Spacing.sm))
                            }
                        }
                    }
                }
            }
            
            // ==========================================
            // 8. DEBT OVERVIEW
            // ==========================================
            if (uiState.totalDebtOwed > 0 || uiState.totalCreditOwed > 0) {
                item {
                    ClickableCard(
                        onClick = { onNavigateToDetail(AnalyticsDetailType.DEBTS) }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Spacing.lg)
                        ) {
                            CardHeader(
                                title = "Debts & Credits",
                                icon = Icons.Filled.SwapHoriz
                            )
                            
                            Spacer(modifier = Modifier.height(Spacing.md))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "You Owe",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = formatAmount(uiState.totalDebtOwed, currencySymbol),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                                
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Owed to You",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = formatAmount(uiState.totalCreditOwed, currencySymbol),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF22C55E)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // ==========================================
            // 9. BUDGET UTILIZATION
            // ==========================================
            if (uiState.totalBudget > 0) {
                item {
                    ClickableCard(
                        onClick = { onNavigateToDetail(AnalyticsDetailType.BUDGET) }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Spacing.lg)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CardHeader(
                                    title = "Budget",
                                    icon = Icons.Filled.AccountBalanceWallet
                                )
                                
                                if (uiState.overBudgetCategories > 0) {
                                    Surface(
                                        shape = RoundedCornerShape(Spacing.xs),
                                        color = MaterialTheme.colorScheme.errorContainer
                                    ) {
                                        Text(
                                            text = "${uiState.overBudgetCategories} Over",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.padding(
                                                horizontal = Spacing.sm,
                                                vertical = Spacing.xs
                                            )
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(Spacing.md))
                            
                            val budgetColor = when {
                                uiState.budgetUtilization > 100 -> MaterialTheme.colorScheme.error
                                uiState.budgetUtilization > 80 -> Color(0xFFF59E0B)
                                else -> MaterialTheme.colorScheme.primary
                            }
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(16.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth((uiState.budgetUtilization / 100f).coerceIn(0f, 1f))
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(budgetColor)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(Spacing.sm))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${formatAmount(uiState.totalBudgetSpent, currencySymbol)} spent",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${uiState.budgetUtilization.toInt()}% of ${formatAmount(uiState.totalBudget, currencySymbol)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
            
            // ==========================================
            // 10. ACCOUNT BALANCES
            // ==========================================
            if (uiState.accountBalances.isNotEmpty()) {
                item {
                    ClickableCard(
                        onClick = { onNavigateToDetail(AnalyticsDetailType.ACCOUNTS) }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Spacing.lg)
                        ) {
                            CardHeader(
                                title = "Account Balances",
                                icon = Icons.Filled.AccountBalance,
                                trailing = formatAmount(uiState.totalAccountBalance, currencySymbol)
                            )
                            
                            Spacer(modifier = Modifier.height(Spacing.lg))
                            
                            AccountBalanceChart(accounts = uiState.accountBalances)
                        }
                    }
                }
            }
            
            // ==========================================
            // 11. TOP TRANSACTIONS
            // ==========================================
            if (uiState.topTransactions.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToDetail(AnalyticsDetailType.TRANSACTIONS) }
                            .padding(top = Spacing.md),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Top Expenses",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            imageVector = Icons.Filled.ArrowForward,
                            contentDescription = "View all",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                items(
                    items = uiState.topTransactions.take(3),
                    key = { it.transaction.id }
                ) { topTxn ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Spacing.md),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(Color(topTxn.categoryColor ?: 0xFF6B7280))
                            )
                            
                            Spacer(modifier = Modifier.width(Spacing.md))
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = topTxn.transaction.note ?: topTxn.categoryName ?: "Expense",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1
                                )
                                Text(
                                    text = topTxn.categoryName ?: "Uncategorized",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            Text(
                                text = formatAmount(topTxn.transaction.amount, currencySymbol),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
            
            // ==========================================
            // 12. CATEGORY BREAKDOWN LIST
            // ==========================================
            if (uiState.categorySpending.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToDetail(AnalyticsDetailType.CATEGORY_SPENDING) }
                            .padding(top = Spacing.md),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Category Breakdown",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            imageVector = Icons.Filled.ArrowForward,
                            contentDescription = "View all",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                items(
                    items = uiState.categorySpending.take(5),
                    key = { it.category.id }
                ) { spending ->
                    CategorySpendingItem(spending = spending, currencySymbol = currencySymbol)
                }
            }
            
            // Empty State
            if (uiState.categorySpending.isEmpty() && !uiState.isLoading) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
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

// ==========================================
// HELPER COMPOSABLES
// ==========================================

@Composable
private fun ClickableCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    colors: CardColors = CardDefaults.cardColors(),
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = colors
    ) {
        content()
    }
}

@Composable
private fun CardHeader(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    trailing: String? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            if (trailing != null) {
                Text(
                    text = trailing,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = iconTint
                )
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = "View details",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    isLarge: Boolean = false
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = if (isLarge) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}

@Composable
private fun CategorySpendingItem(
    spending: CategorySpending,
    currencySymbol: String
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
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
                    budget = spending.amount,
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
