package com.theblankstate.epmanager.ui.export

import androidx.compose.foundation.layout.*
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
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    onNavigateBack: () -> Unit,
    viewModel: ExportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
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
                .padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            // Export All Transactions
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(Spacing.lg)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.TableChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(Spacing.md))
                        Column {
                            Text(
                                text = "Export to CSV",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "All transactions as spreadsheet",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(Spacing.md))
                    
                    Text(
                        text = "Exports all your transactions in CSV format that can be opened in Excel, Google Sheets, or any spreadsheet app.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(Spacing.md))
                    
                    Button(
                        onClick = { viewModel.exportAllToCSV() },
                        enabled = !uiState.isExporting,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (uiState.isExporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.FileDownload,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(Spacing.sm))
                            Text("Export All Transactions")
                        }
                    }
                }
            }
            
            // Monthly Report
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(Spacing.lg)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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
                                text = "Summary with category breakdown",
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
                            Icon(
                                imageVector = Icons.Filled.Share,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(Spacing.sm))
                            Text("Generate & Share Report")
                        }
                    }
                }
            }
            
            // Info card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier.padding(Spacing.md),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Text(
                        text = "Exported files open in your device's share menu. You can save them to files, send via email, or share to any app.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun getMonthName(month: Int): String {
    val calendar = Calendar.getInstance().apply {
        set(Calendar.MONTH, month)
    }
    return SimpleDateFormat("MMMM", Locale.getDefault()).format(calendar.time)
}
