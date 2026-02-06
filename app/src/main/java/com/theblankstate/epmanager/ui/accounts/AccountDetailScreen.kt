package com.theblankstate.epmanager.ui.accounts

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.theblankstate.epmanager.data.model.Account
import com.theblankstate.epmanager.data.model.Transaction
import com.theblankstate.epmanager.ui.components.DoodlePattern
import com.theblankstate.epmanager.ui.components.WaveBackground
import com.theblankstate.epmanager.ui.components.formatAmount
import com.theblankstate.epmanager.ui.theme.Spacing
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailScreen(
    accountId: String,
    onNavigateBack: () -> Unit,
    onNavigateToTransaction: (String) -> Unit,
    viewModel: AccountDetailViewModel = hiltViewModel(),
    currencyViewModel: com.theblankstate.epmanager.util.CurrencyViewModel = hiltViewModel()
) {
    LaunchedEffect(accountId) {
        viewModel.loadAccount(accountId)
    }

    val uiState by viewModel.uiState.collectAsState()
    val currencySymbol by currencyViewModel.currencySymbol.collectAsState(initial = "₹")
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    
    // Date Picker State
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val start = datePickerState.selectedDateMillis
                    if (start != null) {
                        viewModel.setCustomDateRange(start, System.currentTimeMillis())
                    }
                    showDatePicker = false
                }) { Text("OK") }
            }
        ) {
             DatePicker(state = datePickerState)
        }
    }
    
    // Custom Date Range Picker Dialog (Material 3 DateRangePicker)
    var showDateRangePicker by remember { mutableStateOf(false) }
    val dateRangePickerState = rememberDateRangePickerState()

    if (showDateRangePicker) {
        DatePickerDialog(
            onDismissRequest = { showDateRangePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val start = dateRangePickerState.selectedStartDateMillis
                    val end = dateRangePickerState.selectedEndDateMillis
                    if (start != null && end != null) {
                        viewModel.setCustomDateRange(start, end)
                    }
                    showDateRangePicker = false
                }) { Text("Apply") }
            }
        ) {
             DateRangePicker(state = dateRangePickerState)
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.account?.name ?: "Account Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = Spacing.xxl)
        ) {
            // 1. Header Card (Visuals + Balance)
            item {
                uiState.account?.let { account ->
                    AccountDetailHeader(
                        account = account,
                        totalIncome = uiState.totalIncome,
                        totalExpense = uiState.totalExpense,
                        netFlow = uiState.netFlow,
                        currencySymbol = currencySymbol
                    )
                }
            }

            // 2. Chart Section
            item {
                Column(modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.md)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Spending Trend",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        // Informatics Toggle
                        IconToggleButton(
                            checked = uiState.showInformatics,
                            onCheckedChange = { viewModel.toggleInformatics() }
                        ) {
                            Icon(
                                if(uiState.showInformatics) Icons.Filled.Info else Icons.Outlined.Info,
                                contentDescription = "Toggle Informatics",
                                tint = if(uiState.showInformatics) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(Spacing.sm))
                    
                    // Date Range Selector
                    DateRangeSelector(
                        selected = uiState.selectedRange,
                        onSelect = { range ->
                            if (range == DateRange.CUSTOM) {
                                showDateRangePicker = true
                            }
                            viewModel.onDateRangeSelected(range)
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(Spacing.md))
                    
                    // Chart Mode Selector
                    ChartModeSelector(
                        selected = uiState.chartMode,
                        onSelect = viewModel::setChartMode
                    )
                     Spacer(modifier = Modifier.height(Spacing.sm))

                    // Chart
                    AccountLineChart(
                        data = uiState.chartData,
                        mode = uiState.chartMode,
                        showInformatics = uiState.showInformatics,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    )
                }
            }
            
            // 3. Smart Stats
            item {
                SmartStatsRow(
                    dailyAverage = uiState.dailyAverage,
                    netFlow = uiState.netFlow,
                    currencySymbol = currencySymbol
                )
            }

            // 4. Transactions List Header & Filters
            item {
                Column(
                    modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Transactions",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        // Sort Button
                        var showSortMenu by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(Icons.Default.Sort, "Sort")
                            }
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                SortOption.values().forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option.label) },
                                        onClick = {
                                            viewModel.setSortOption(option)
                                            showSortMenu = false
                                        },
                                        leadingIcon = if (uiState.sortOption == option) {
                                            { Icon(Icons.Default.Check, null) }
                                        } else null
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(Spacing.sm))
                    
                    // Filters Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Type Filter
                        TransactionFilter.values().forEach { filter ->
                             FilterChip(
                                selected = uiState.transactionFilter == filter,
                                onClick = { viewModel.setTransactionFilter(filter) },
                                label = { Text(filter.label) },
                                leadingIcon = if(uiState.transactionFilter == filter) { 
                                    { Icon(Icons.Default.FilterList, null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                shape = CircleShape
                            )
                        }
                    }
                }
            }
            
            if (uiState.filteredTransactions.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No transactions found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(uiState.filteredTransactions) { transaction ->
                    TransactionItemCompact(
                        transaction = transaction,
                        currencySymbol = currencySymbol,
                        onClick = { onNavigateToTransaction(transaction.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun AccountDetailHeader(
    account: Account,
    totalIncome: Double,
    totalExpense: Double,
    netFlow: Double,
    currencySymbol: String
) {
    val containerColor = MaterialTheme.colorScheme.surfaceVariant
    val contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.md)
            .height(180.dp),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            WaveBackground(
                modifier = Modifier.matchParentSize(),
                color = contentColor.copy(alpha = 0.05f)
            )
            DoodlePattern(
                modifier = Modifier.matchParentSize(),
                color = contentColor.copy(alpha = 0.03f)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top: Icon + Name
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = Color(account.color).copy(alpha = 0.2f),
                        contentColor = Color(account.color),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.List, null, modifier = Modifier.size(20.dp)) 
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = account.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = contentColor
                    )
                }
                
                // Bottom: Balances
                Row(
                   modifier = Modifier.fillMaxWidth(),
                   horizontalArrangement = Arrangement.SpaceBetween,
                   verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text("Current Balance", style = MaterialTheme.typography.labelMedium)
                        Text(
                            formatAmount(account.balance, currencySymbol), 
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "+${formatAmount(totalIncome, currencySymbol)}", 
                            color = Color(0xFF16A34A),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                         Text(
                            "-${formatAmount(totalExpense, currencySymbol)}", 
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangeSelector(
    selected: DateRange,
    onSelect: (DateRange) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DateRange.values().forEach { range ->
            FilterChip(
                selected = range == selected,
                onClick = { onSelect(range) },
                label = { Text(range.label) },
                modifier = Modifier.weight(1f),
                shape = CircleShape,
                leadingIcon = if(range == DateRange.CUSTOM) {
                     { Icon(Icons.Default.CalendarMonth, null, modifier = Modifier.size(16.dp)) }
                } else null,
                colors = FilterChipDefaults.filterChipColors()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartModeSelector(
    selected: ChartMode,
    onSelect: (ChartMode) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ChartMode.values().forEach { mode ->
            FilterChip(
                selected = mode == selected,
                onClick = { onSelect(mode) },
                label = { Text(mode.label) },
                modifier = Modifier.weight(1f),
                shape = CircleShape,
                colors = FilterChipDefaults.filterChipColors()
            )
        }
    }
}

@Composable
fun SmartStatsRow(
    dailyAverage: Double,
    netFlow: Double,
    currencySymbol: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        SmartStatCard(
            label = "Avg Daily Spend",
            value = formatAmount(dailyAverage, currencySymbol),
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.primaryContainer
        )
        SmartStatCard(
            label = "Net Flow",
            value = formatAmount(netFlow, currencySymbol),
            modifier = Modifier.weight(1f),
            color = if(netFlow >= 0) Color(0xFFDCFCE7) else Color(0xFFFEE2E2), 
            contentColor = if(netFlow >= 0) Color(0xFF166534) else Color(0xFF991B1B)
        )
    }
}

@Composable
fun SmartStatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    color: Color,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = color,
            contentColor = contentColor
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun AccountLineChart(
    data: List<Float>,
    mode: ChartMode,
    showInformatics: Boolean,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary
) {
    val chartColor = when(mode) {
        ChartMode.INCOME -> Color(0xFF16A34A)
        ChartMode.EXPENSE -> MaterialTheme.colorScheme.error
        ChartMode.NET_FLOW -> MaterialTheme.colorScheme.primary
    }
    
    val textMeasurer = androidx.compose.ui.text.rememberTextMeasurer()
    val textStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)

    if (data.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("No data to chart", style = MaterialTheme.typography.bodySmall)
        }
        return
    }

    Canvas(modifier = modifier.padding(16.dp)) {
        val width = size.width
        val height = size.height
        val maxVal = data.maxOrNull() ?: 0f
        val minVal = data.minOrNull() ?: 0f
        val range = (maxVal - minVal).coerceAtLeast(1f)
        
        val points = data.mapIndexed { index, value ->
            val x = (index.toFloat() / (data.lastIndex.coerceAtLeast(1))) * width
            val y = height - ((value - minVal) / range) * height
            Offset(x, y)
        }
        
        // Draw Grid Lines if Informatics enabled
        if (showInformatics) {
            val steps = 4
            for (i in 0..steps) {
                val y = height * (i.toFloat() / steps)
                drawLine(
                    color = Color.Gray.copy(alpha = 0.2f),
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1.dp.toPx()
                )
                // Draw Y Axis Labels
                val value = maxVal - (range * (i.toFloat() / steps))
                val measuredText = textMeasurer.measure(
                    text = String.format("%.0f", value),
                    style = textStyle
                )
                drawText(
                    textLayoutResult = measuredText,
                    topLeft = Offset(-measuredText.size.width.toFloat() - 8f, y - measuredText.size.height / 2)
                )
            }
        }

        val path = Path().apply {
            if (points.isNotEmpty()) {
                moveTo(points.first().x, points.first().y)
                 for (i in 0 until points.size - 1) {
                    val p0 = points[i]
                    val p1 = points[i + 1]
                    val ctrl1 = Offset((p0.x + p1.x) / 2, p0.y)
                    val ctrl2 = Offset((p0.x + p1.x) / 2, p1.y)
                    cubicTo(ctrl1.x, ctrl1.y, ctrl2.x, ctrl2.y, p1.x, p1.y)
                }
            }
        }
        
        drawPath(
            path = path,
            color = chartColor,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        
        // Fill
        val fillPath = Path().apply {
            addPath(path)
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }
        
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    chartColor.copy(alpha = 0.3f),
                    Color.Transparent
                )
            )
        )
        
        // Data Points if Informatics enabled
        if (showInformatics) {
            points.forEach { point ->
                drawCircle(
                    color = chartColor,
                    radius = 3.dp.toPx(),
                    center = point
                )
            }
        }
    }
}

@Composable
fun TransactionItemCompact(
    transaction: Transaction,
    currencySymbol: String,
    onClick: () -> Unit
) {
    val dateFormatter = SimpleDateFormat("MMM dd", Locale.getDefault())
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.md, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            val title = transaction.merchantName ?: transaction.note?.takeIf { it.isNotEmpty() } ?: "Transaction"
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                dateFormatter.format(Date(transaction.date)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Text(
            formatAmount(transaction.amount, currencySymbol),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = if(transaction.type == com.theblankstate.epmanager.data.model.TransactionType.INCOME) 
                Color(0xFF16A34A) else MaterialTheme.colorScheme.onSurface
        )
    }
    HorizontalDivider(modifier = Modifier.padding(horizontal = Spacing.md), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f))
}
