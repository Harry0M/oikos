package com.theblankstate.epmanager.ui.export

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.theblankstate.epmanager.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExportScreen(
    onNavigateBack: () -> Unit,
    viewModel: ExportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDatePicker by remember { mutableStateOf(false) }
    var isSelectingStartDate by remember { mutableStateOf(true) }
    
    // Handle messages
    LaunchedEffect(uiState.exportSuccess, uiState.exportError) {
        uiState.exportSuccess?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
        uiState.exportError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }
    
    // Date picker dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = if (isSelectingStartDate) 
                uiState.customStartDate ?: System.currentTimeMillis() 
            else 
                uiState.customEndDate ?: System.currentTimeMillis()
        )
        
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { date ->
                        if (isSelectingStartDate) {
                            viewModel.setCustomDateRange(
                                date,
                                uiState.customEndDate ?: System.currentTimeMillis()
                            )
                            isSelectingStartDate = false
                        } else {
                            viewModel.setCustomDateRange(
                                uiState.customStartDate ?: date,
                                date
                            )
                            showDatePicker = false
                        }
                    }
                }) {
                    Text(if (isSelectingStartDate) "Next" else "Done")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                title = {
                    Text(
                        text = if (isSelectingStartDate) "Select Start Date" else "Select End Date",
                        modifier = Modifier.padding(Spacing.md)
                    )
                }
            )
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Export Data",
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
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            // ==================== DATE RANGE ====================
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(Spacing.lg)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.DateRange,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(Spacing.sm))
                        Text(
                            text = "Time Period",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(Spacing.md))
                    
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        DateRangeType.entries.forEach { type ->
                            FilterChip(
                                selected = uiState.dateRangeType == type,
                                onClick = { 
                                    if (type == DateRangeType.CUSTOM) {
                                        isSelectingStartDate = true
                                        showDatePicker = true
                                    } else {
                                        viewModel.setDateRangeType(type)
                                    }
                                },
                                label = { Text(getDateRangeLabel(type)) }
                            )
                        }
                    }
                    
                    // Show selected custom date range
                    if (uiState.dateRangeType == DateRangeType.CUSTOM && 
                        uiState.customStartDate != null && uiState.customEndDate != null) {
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                        Text(
                            text = "${dateFormat.format(Date(uiState.customStartDate!!))} - ${dateFormat.format(Date(uiState.customEndDate!!))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            // ==================== TRANSACTION TYPE ====================
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(Spacing.lg)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.SwapVert,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(Spacing.sm))
                        Text(
                            text = "Transaction Type",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(Spacing.md))
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        FilterChip(
                            selected = uiState.includeExpenses,
                            onClick = { viewModel.toggleExpenses() },
                            label = { Text("Expenses") },
                            leadingIcon = {
                                if (uiState.includeExpenses) {
                                    Icon(Icons.Filled.Check, contentDescription = null, Modifier.size(18.dp))
                                }
                            }
                        )
                        FilterChip(
                            selected = uiState.includeIncome,
                            onClick = { viewModel.toggleIncome() },
                            label = { Text("Income") },
                            leadingIcon = {
                                if (uiState.includeIncome) {
                                    Icon(Icons.Filled.Check, contentDescription = null, Modifier.size(18.dp))
                                }
                            }
                        )
                    }
                }
            }
            
            // ==================== CATEGORIES ====================
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(Spacing.lg)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Category,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                            Spacer(modifier = Modifier.width(Spacing.sm))
                            Text(
                                text = "Categories",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Row {
                            TextButton(onClick = { viewModel.selectAllCategories() }) {
                                Text("All", style = MaterialTheme.typography.labelSmall)
                            }
                            TextButton(onClick = { viewModel.clearAllCategories() }) {
                                Text("None", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                    
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        // Uncategorized option
                        FilterChip(
                            selected = "uncategorized" in uiState.selectedCategoryIds,
                            onClick = { viewModel.toggleCategory("uncategorized") },
                            label = { Text("Uncategorized") }
                        )
                        
                        uiState.categories.forEach { category ->
                            FilterChip(
                                selected = category.id in uiState.selectedCategoryIds,
                                onClick = { viewModel.toggleCategory(category.id) },
                                label = { Text("${category.icon} ${category.name}") }
                            )
                        }
                    }
                }
            }
            
            // ==================== ACCOUNTS ====================
            if (uiState.accounts.isNotEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(Spacing.lg)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.AccountBalance,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(Spacing.sm))
                                Text(
                                    text = "Accounts",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Row {
                                TextButton(onClick = { viewModel.selectAllAccounts() }) {
                                    Text("All", style = MaterialTheme.typography.labelSmall)
                                }
                                TextButton(onClick = { viewModel.clearAllAccounts() }) {
                                    Text("None", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                        
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            FilterChip(
                                selected = "default" in uiState.selectedAccountIds,
                                onClick = { viewModel.toggleAccount("default") },
                                label = { Text("Default") }
                            )
                            
                            uiState.accounts.forEach { account ->
                                FilterChip(
                                    selected = account.id in uiState.selectedAccountIds,
                                    onClick = { viewModel.toggleAccount(account.id) },
                                    label = { Text("${account.icon} ${account.name}") }
                                )
                            }
                        }
                    }
                }
            }
            
            // ==================== EXPORT OPTIONS ====================
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(Spacing.lg)) {
                    Text(
                        text = "Export Options",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Include Notes")
                        Switch(
                            checked = uiState.includeNotes,
                            onCheckedChange = { viewModel.toggleIncludeNotes() }
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Include Account Info")
                        Switch(
                            checked = uiState.includeAccountInfo,
                            onCheckedChange = { viewModel.toggleIncludeAccountInfo() }
                        )
                    }
                }
            }
            
            // ==================== PREVIEW & EXPORT ====================
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(Spacing.lg)) {
                    Text(
                        text = "Export Preview",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    
                    uiState.preview?.let { preview ->
                        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${preview.transactionCount}",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Transactions",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = currencyFormat.format(preview.totalExpenses),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = "Expenses",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = currencyFormat.format(preview.totalIncome),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Income",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(Spacing.md))
                    
                    Button(
                        onClick = { viewModel.exportFiltered() },
                        enabled = !uiState.isExporting && (uiState.preview?.transactionCount ?: 0) > 0,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (uiState.isExporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Filled.FileDownload, contentDescription = null)
                            Spacer(modifier = Modifier.width(Spacing.sm))
                            Text("Export to CSV")
                        }
                    }
                }
            }
            
            // ==================== MONTHLY REPORT ====================
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(Spacing.lg)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Assessment,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(Spacing.md))
                        Column {
                            Text(
                                text = "Monthly Report",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Summary with category & account breakdown",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(Spacing.md))
                    
                    // Month/Year Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                    ) {
                        // Month selector
                        var monthExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = monthExpanded,
                            onExpandedChange = { monthExpanded = it },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = getMonthName(uiState.selectedMonth),
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Month") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = monthExpanded) },
                                modifier = Modifier.menuAnchor()
                            )
                            
                            ExposedDropdownMenu(
                                expanded = monthExpanded,
                                onDismissRequest = { monthExpanded = false }
                            ) {
                                (0..11).forEach { month ->
                                    DropdownMenuItem(
                                        text = { Text(getMonthName(month)) },
                                        onClick = {
                                            viewModel.setMonth(month)
                                            monthExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                        
                        // Year selector
                        var yearExpanded by remember { mutableStateOf(false) }
                        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                        ExposedDropdownMenuBox(
                            expanded = yearExpanded,
                            onExpandedChange = { yearExpanded = it },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = uiState.selectedYear.toString(),
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Year") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = yearExpanded) },
                                modifier = Modifier.menuAnchor()
                            )
                            
                            ExposedDropdownMenu(
                                expanded = yearExpanded,
                                onDismissRequest = { yearExpanded = false }
                            ) {
                                ((currentYear - 5)..currentYear).reversed().forEach { year ->
                                    DropdownMenuItem(
                                        text = { Text(year.toString()) },
                                        onClick = {
                                            viewModel.setYear(year)
                                            yearExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(Spacing.md))
                    
                    OutlinedButton(
                        onClick = { viewModel.exportMonthlyReport() },
                        enabled = !uiState.isExporting,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (uiState.isExporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Filled.Share, contentDescription = null)
                            Spacer(modifier = Modifier.width(Spacing.sm))
                            Text("Generate & Share Report")
                        }
                    }
                }
            }
        }
    }
}

private fun getDateRangeLabel(type: DateRangeType): String {
    return when (type) {
        DateRangeType.TODAY -> "Today"
        DateRangeType.THIS_WEEK -> "This Week"
        DateRangeType.THIS_MONTH -> "This Month"
        DateRangeType.LAST_MONTH -> "Last Month"
        DateRangeType.THIS_YEAR -> "This Year"
        DateRangeType.ALL_TIME -> "All Time"
        DateRangeType.CUSTOM -> "Custom"
    }
}

private fun getMonthName(month: Int): String {
    val calendar = Calendar.getInstance().apply {
        set(Calendar.MONTH, month)
    }
    return SimpleDateFormat("MMMM", Locale.getDefault()).format(calendar.time)
}
