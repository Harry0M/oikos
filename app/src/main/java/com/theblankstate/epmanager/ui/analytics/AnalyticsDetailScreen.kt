package com.theblankstate.epmanager.ui.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.theblankstate.epmanager.ui.components.*
import com.theblankstate.epmanager.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsDetailScreen(
    detailType: AnalyticsDetailType,
    onNavigateBack: () -> Unit,
    viewModel: AnalyticsViewModel = hiltViewModel(),
    currencyViewModel: com.theblankstate.epmanager.util.CurrencyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currencySymbol by currencyViewModel.currencySymbol.collectAsState(initial = "₹")
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = getDetailTitle(detailType),
                        fontWeight = FontWeight.SemiBold
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
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            when (detailType) {
                AnalyticsDetailType.FINANCIAL_HEALTH -> {
                    // Financial Health Header
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Spacing.xl),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                FinancialHealthGauge(
                                    score = uiState.healthScore,
                                    size = 180.dp,
                                    strokeWidth = 18.dp
                                )
                                
                                Spacer(modifier = Modifier.height(Spacing.lg))
                                
                                Text(
                                    text = getHealthLabel(uiState.healthScore),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = getHealthColor(uiState.healthScore)
                                )
                                
                                Spacer(modifier = Modifier.height(Spacing.xs))
                                
                                Text(
                                    text = getHealthDescription(uiState.healthScore),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    
                    // Net Worth Card
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Spacing.lg)
                            ) {
                                Text(
                                    text = "Net Worth",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                
                                Spacer(modifier = Modifier.height(Spacing.md))
                                
                                Text(
                                    text = formatAmount(uiState.netWorth, currencySymbol),
                                    style = MaterialTheme.typography.displaySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (uiState.netWorth >= 0) 
                                        Color(0xFF22C55E) 
                                    else 
                                        MaterialTheme.colorScheme.error
                                )
                                
                                Spacer(modifier = Modifier.height(Spacing.lg))
                                
                                // Breakdown
                                DetailRow("Total Account Balance", formatAmount(uiState.totalAccountBalance, currencySymbol))
                                DetailRow("Total Goals Saved", "+${formatAmount(uiState.totalGoalsSaved, currencySymbol)}", Color(0xFF22C55E))
                                DetailRow("Credit Owed to You", "+${formatAmount(uiState.totalCreditOwed, currencySymbol)}", Color(0xFF22C55E))
                                DetailRow("Debt You Owe", "-${formatAmount(uiState.totalDebtOwed, currencySymbol)}", MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                    
                    // Savings Rate
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Spacing.lg)
                            ) {
                                Text(
                                    text = "Savings Rate",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                
                                Spacer(modifier = Modifier.height(Spacing.md))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${uiState.savingsRate.toInt()}%",
                                        style = MaterialTheme.typography.displaySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = when {
                                            uiState.savingsRate >= 20 -> Color(0xFF22C55E)
                                            uiState.savingsRate >= 0 -> Color(0xFFF59E0B)
                                            else -> MaterialTheme.colorScheme.error
                                        }
                                    )
                                    
                                    Text(
                                        text = when {
                                            uiState.savingsRate >= 30 -> "Excellent!"
                                            uiState.savingsRate >= 20 -> "Great"
                                            uiState.savingsRate >= 10 -> "Good"
                                            uiState.savingsRate >= 0 -> "Needs Work"
                                            else -> "Overspending"
                                        },
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(Spacing.lg))
                                
                                SavingsRateBar(rate = uiState.savingsRate)
                                
                                Spacer(modifier = Modifier.height(Spacing.lg))
                                
                                // Calculation breakdown
                                DetailRow("Income", formatAmount(uiState.totalIncome, currencySymbol))
                                DetailRow("Expenses", formatAmount(uiState.totalExpenses, currencySymbol))
                                HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.sm))
                                DetailRow("Saved", formatAmount(uiState.totalIncome - uiState.totalExpenses, currencySymbol),
                                    if (uiState.totalIncome >= uiState.totalExpenses) Color(0xFF22C55E) else MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                    
                    // Health Score Factors
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Spacing.lg)
                            ) {
                                Text(
                                    text = "Score Factors",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                
                                Spacer(modifier = Modifier.height(Spacing.md))
                                
                                ScoreFactor(
                                    label = "Net Worth",
                                    description = if (uiState.netWorth > 0) "Positive net worth" else "Negative net worth",
                                    isPositive = uiState.netWorth > 0
                                )
                                ScoreFactor(
                                    label = "Savings Rate",
                                    description = "${uiState.savingsRate.toInt()}% of income saved",
                                    isPositive = uiState.savingsRate >= 10
                                )
                                ScoreFactor(
                                    label = "Debt Level",
                                    description = if (uiState.totalDebtOwed == 0.0) "No debt" else "Active debt",
                                    isPositive = uiState.totalDebtOwed == 0.0
                                )
                            }
                        }
                    }
                }
                
                AnalyticsDetailType.SPENDING_TREND -> {
                    // Weekly Chart
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Spacing.lg)
                            ) {
                                Text(
                                    text = "7-Day Spending",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                
                                Spacer(modifier = Modifier.height(Spacing.lg))
                                
                                DailySpendingLineChart(
                                    data = uiState.dailySpending,
                                    height = 150.dp
                                )
                            }
                        }
                    }
                    
                    // Daily Breakdown
                    item {
                        Text(
                            text = "Daily Breakdown",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    items(uiState.dailySpending.reversed()) { day ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Spacing.md),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = day.dayLabel,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = formatAmount(day.amount, currencySymbol),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (day.amount > uiState.averageDailySpend) 
                                        MaterialTheme.colorScheme.error 
                                    else 
                                        Color(0xFF22C55E)
                                )
                            }
                        }
                    }
                    
                    // Stats Summary
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Spacing.lg)
                            ) {
                                Text(
                                    text = "Summary",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                
                                Spacer(modifier = Modifier.height(Spacing.md))
                                
                                val totalWeek = uiState.dailySpending.sumOf { it.amount }
                                val maxDay = uiState.dailySpending.maxByOrNull { it.amount }
                                val minDay = uiState.dailySpending.minByOrNull { it.amount }
                                
                                DetailRow("Total (7 days)", formatAmount(totalWeek, currencySymbol))
                                DetailRow("Daily Average", formatAmount(uiState.averageDailySpend, currencySymbol))
                                if (maxDay != null) {
                                    DetailRow("Highest (${maxDay.dayLabel})", formatAmount(maxDay.amount, currencySymbol), MaterialTheme.colorScheme.error)
                                }
                                if (minDay != null) {
                                    DetailRow("Lowest (${minDay.dayLabel})", formatAmount(minDay.amount, currencySymbol), Color(0xFF22C55E))
                                }
                            }
                        }
                    }
                }
                
                AnalyticsDetailType.CATEGORY_SPENDING -> {
                    // Pie Chart
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Spacing.lg),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                SpendingPieChart(
                                    categorySpending = uiState.categorySpending.take(8),
                                    size = 200.dp,
                                    strokeWidth = 32.dp
                                )
                                
                                Spacer(modifier = Modifier.height(Spacing.lg))
                                
                                // Full Legend
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                                ) {
                                    uiState.categorySpending.forEach { spending ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(12.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(spending.category.color))
                                                )
                                                Text(
                                                    text = spending.category.name,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            }
                                            Text(
                                                text = "${spending.percentage.toInt()}%",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    // Category List
                    item {
                        Text(
                            text = "All Categories",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    items(uiState.categorySpending) { spending ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Spacing.md)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(CircleShape)
                                                .background(Color(spending.category.color))
                                        )
                                        Text(
                                            text = spending.category.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    Text(
                                        text = formatAmount(spending.amount, currencySymbol),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(spending.category.color)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(Spacing.sm))
                                
                                SpendingProgressBar(
                                    spent = spending.percentage.toDouble(),
                                    budget = 100.0,
                                    color = Color(spending.category.color)
                                )
                                
                                Spacer(modifier = Modifier.height(Spacing.xs))
                                
                                Text(
                                    text = "${spending.percentage.toInt()}% of total spending",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                
                AnalyticsDetailType.MONTHLY_OVERVIEW -> {
                    // Monthly Chart
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Spacing.lg)
                            ) {
                                Text(
                                    text = "6-Month Overview",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                
                                Spacer(modifier = Modifier.height(Spacing.lg))
                                
                                MonthlyBarChart(
                                    data = uiState.monthlyData.map { it.month to it.expense },
                                    barColor = MaterialTheme.colorScheme.primary,
                                    height = 150.dp
                                )
                            }
                        }
                    }
                    
                    // Monthly Breakdown
                    item {
                        Text(
                            text = "Monthly Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    items(uiState.monthlyData.reversed()) { month ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Spacing.md)
                            ) {
                                Text(
                                    text = month.month,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                
                                Spacer(modifier = Modifier.height(Spacing.sm))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.lg)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Expenses",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = formatAmount(month.expense, currencySymbol),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                    
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Income",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = formatAmount(month.income, currencySymbol),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF22C55E)
                                        )
                                    }
                                    
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Net",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        val net = month.income - month.expense
                                        Text(
                                            text = "${if (net >= 0) "+" else ""}${formatAmount(net, currencySymbol)}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (net >= 0) Color(0xFF22C55E) else MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                AnalyticsDetailType.GOALS -> {
                    // Overall Progress
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Spacing.lg),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "${uiState.goalsProgress.toInt()}%",
                                    style = MaterialTheme.typography.displayMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                                
                                Text(
                                    text = "Overall Progress",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                
                                Spacer(modifier = Modifier.height(Spacing.lg))
                                
                                DetailRow("Total Saved", formatAmount(uiState.totalGoalsSaved, currencySymbol), Color(0xFF22C55E))
                                DetailRow("Total Target", formatAmount(uiState.totalGoalsTarget, currencySymbol))
                                DetailRow("Remaining", formatAmount(uiState.totalGoalsTarget - uiState.totalGoalsSaved, currencySymbol))
                            }
                        }
                    }
                    
                    // Individual Goals
                    item {
                        Text(
                            text = "All Goals",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    items(uiState.activeGoals) { goalProgress ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Spacing.md)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = goalProgress.goal.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "${goalProgress.progress.toInt()}%",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(goalProgress.goal.color)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(Spacing.sm))
                                
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(12.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth((goalProgress.progress / 100f).coerceIn(0f, 1f))
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(goalProgress.goal.color))
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(Spacing.sm))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = formatAmount(goalProgress.goal.savedAmount, currencySymbol),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = formatAmount(goalProgress.goal.targetAmount, currencySymbol),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
                
                AnalyticsDetailType.DEBTS -> {
                    // Summary
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Spacing.lg)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.lg)
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "You Owe",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = formatAmount(uiState.totalDebtOwed, currencySymbol),
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                    
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "Owed to You",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = formatAmount(uiState.totalCreditOwed, currencySymbol),
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF22C55E)
                                        )
                                    }
                                }
                                
                                HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.lg))
                                
                                val netDebt = uiState.totalCreditOwed - uiState.totalDebtOwed
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "Net Position",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "${if (netDebt >= 0) "+" else ""}${formatAmount(netDebt, currencySymbol)}",
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (netDebt >= 0) Color(0xFF22C55E) else MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    item {
                        Text(
                            text = "Go to Debts screen to manage individual debts",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                AnalyticsDetailType.BUDGET -> {
                    // Overall Utilization
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Spacing.lg),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "${uiState.budgetUtilization.toInt()}%",
                                    style = MaterialTheme.typography.displayMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        uiState.budgetUtilization > 100 -> MaterialTheme.colorScheme.error
                                        uiState.budgetUtilization > 80 -> Color(0xFFF59E0B)
                                        else -> Color(0xFF22C55E)
                                    }
                                )
                                
                                Text(
                                    text = "Budget Used",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                
                                Spacer(modifier = Modifier.height(Spacing.lg))
                                
                                val budgetColor = when {
                                    uiState.budgetUtilization > 100 -> MaterialTheme.colorScheme.error
                                    uiState.budgetUtilization > 80 -> Color(0xFFF59E0B)
                                    else -> MaterialTheme.colorScheme.primary
                                }
                                
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(20.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth((uiState.budgetUtilization / 100f).coerceIn(0f, 1f))
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(budgetColor)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(Spacing.lg))
                                
                                DetailRow("Total Budget", formatAmount(uiState.totalBudget, currencySymbol))
                                DetailRow("Spent", formatAmount(uiState.totalBudgetSpent, currencySymbol), MaterialTheme.colorScheme.error)
                                DetailRow("Remaining", formatAmount(uiState.totalBudget - uiState.totalBudgetSpent, currencySymbol), Color(0xFF22C55E))
                                
                                if (uiState.overBudgetCategories > 0) {
                                    Spacer(modifier = Modifier.height(Spacing.md))
                                    Surface(
                                        shape = RoundedCornerShape(Spacing.sm),
                                        color = MaterialTheme.colorScheme.errorContainer
                                    ) {
                                        Text(
                                            text = "${uiState.overBudgetCategories} categories over budget",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.padding(Spacing.sm)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    item {
                        Text(
                            text = "Go to Budget screen to manage budgets",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                AnalyticsDetailType.ACCOUNTS -> {
                    // Total Balance
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Spacing.lg),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Total Balance",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formatAmount(uiState.totalAccountBalance, currencySymbol),
                                    style = MaterialTheme.typography.displaySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (uiState.totalAccountBalance >= 0) 
                                        MaterialTheme.colorScheme.primary 
                                    else 
                                        MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                    
                    // Account List
                    item {
                        Text(
                            text = "All Accounts",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    items(uiState.accountBalances) { accountBalance ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Spacing.md),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color(accountBalance.account.color)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = accountBalance.account.name.take(2).uppercase(),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(Spacing.md))
                                
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = accountBalance.account.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "${accountBalance.percentage.toInt()}% of total",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                
                                Text(
                                    text = formatAmount(accountBalance.balance, currencySymbol),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (accountBalance.balance >= 0) 
                                        Color(accountBalance.account.color) 
                                    else 
                                        MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
                
                AnalyticsDetailType.TRANSACTIONS -> {
                    // Summary
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Spacing.lg)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "${uiState.transactionCount}",
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "Transactions",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        val avg = if (uiState.transactionCount > 0) 
                                            uiState.totalExpenses / uiState.transactionCount else 0.0
                                        Text(
                                            text = formatAmount(avg, currencySymbol),
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                        Text(
                                            text = "Average",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    // Top Expenses
                    item {
                        Text(
                            text = "Largest Expenses",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    items(uiState.topTransactions) { topTxn ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Spacing.md),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(Color(topTxn.categoryColor ?: 0xFF6B7280))
                                )
                                
                                Spacer(modifier = Modifier.width(Spacing.md))
                                
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = topTxn.transaction.note ?: topTxn.categoryName ?: "Expense",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = topTxn.categoryName ?: "Uncategorized",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                
                                Text(
                                    text = formatAmount(topTxn.transaction.amount, currencySymbol),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
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
// HELPER FUNCTIONS & COMPOSABLES
// ==========================================

private fun getDetailTitle(type: AnalyticsDetailType): String = when (type) {
    AnalyticsDetailType.FINANCIAL_HEALTH -> "Financial Health"
    AnalyticsDetailType.SPENDING_TREND -> "Spending Trend"
    AnalyticsDetailType.CATEGORY_SPENDING -> "Category Analysis"
    AnalyticsDetailType.MONTHLY_OVERVIEW -> "Monthly Overview"
    AnalyticsDetailType.GOALS -> "Savings Goals"
    AnalyticsDetailType.DEBTS -> "Debts & Credits"
    AnalyticsDetailType.BUDGET -> "Budget Analysis"
    AnalyticsDetailType.ACCOUNTS -> "Account Balances"
    AnalyticsDetailType.TRANSACTIONS -> "Transaction Analysis"
}

private fun getHealthLabel(score: Int): String = when {
    score >= 80 -> "Excellent"
    score >= 60 -> "Good"
    score >= 40 -> "Fair"
    score >= 20 -> "Needs Work"
    else -> "Critical"
}

@Composable
private fun getHealthColor(score: Int): Color = when {
    score >= 70 -> Color(0xFF22C55E)
    score >= 40 -> Color(0xFFF59E0B)
    else -> MaterialTheme.colorScheme.error
}

private fun getHealthDescription(score: Int): String = when {
    score >= 80 -> "Your finances are in great shape!"
    score >= 60 -> "You're doing well, keep it up!"
    score >= 40 -> "Some areas need improvement"
    score >= 20 -> "Consider making changes to improve"
    else -> "Take action to improve your finances"
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = valueColor
        )
    }
}

@Composable
private fun ScoreFactor(
    label: String,
    description: String,
    isPositive: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Icon(
            imageVector = if (isPositive) Icons.Filled.CheckCircle else Icons.Filled.Warning,
            contentDescription = null,
            tint = if (isPositive) Color(0xFF22C55E) else Color(0xFFF59E0B),
            modifier = Modifier.size(24.dp)
        )
        
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
