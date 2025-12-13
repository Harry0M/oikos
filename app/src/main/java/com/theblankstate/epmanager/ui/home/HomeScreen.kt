package com.theblankstate.epmanager.ui.home

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import com.theblankstate.epmanager.ui.components.*
import com.theblankstate.epmanager.ui.theme.Spacing
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToAddTransaction: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToTransactionDetail: (String) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lazyListState = rememberLazyListState()
    var hasNavigated by remember { mutableStateOf(false) }
    
    // Force refresh data every time this screen becomes visible
    LaunchedEffect(Unit) {
        viewModel.refresh()
        hasNavigated = false // Reset when screen is shown
    }
    
    // Detect when user scrolls past the last transaction item (scroll up to see more)
    // This triggers navigation to full Transactions screen
    LaunchedEffect(lazyListState) {
        snapshotFlow { 
            // Check if we've scrolled to the transactions section and are at/near the end
            val isInTransactions = lazyListState.firstVisibleItemIndex >= 4
            val canScrollForward = lazyListState.canScrollForward
            isInTransactions to canScrollForward
        }
            .distinctUntilChanged()
            .filter { (isInTransactions, canScrollForward) -> 
                // Trigger when in transactions section and can't scroll further (reached end)
                isInTransactions && !canScrollForward && !hasNavigated
            }
            .collect {
                hasNavigated = true
                triggerVibration(context)
                onNavigateToTransactions()
            }
    }
    
    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToAddTransaction,
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Add Transaction"
                    )
                },
                text = {
                    Text(
                        text = "Add",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
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
            AnimatedVisibility(visible = true, enter = fadeIn(), exit = fadeOut()) {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                // Header
                item {
                    Text(
                        text = "Dashboard",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = Spacing.sm)
                    )
                }
                
                // Balance Card
                item {
                    BalanceCard(
                        totalBalance = uiState.totalBalance,
                        monthlyIncome = uiState.monthlyIncome,
                        monthlyExpenses = uiState.monthlyExpenses
                    )
                }
                
                // Quick Stats Row
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                    ) {
                        StatCard(
                            title = "Today",
                            value = formatCurrency(uiState.todaySpending),
                            subtitle = "spent",
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "This Month",
                            value = "${uiState.recentTransactions.size}",
                            subtitle = "transactions",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                // Recent Transactions Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent Transactions",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        TextButton(onClick = onNavigateToTransactions) {
                            Text("View All")
                        }
                    }
                }
                
                // Transactions List - Limited to 5 items
                if (uiState.recentTransactions.isEmpty()) {
                    item {
                        EmptyTransactionState()
                    }
                } else {
                    val displayTransactions = uiState.recentTransactions.take(5)
                    items(
                        items = displayTransactions,
                        key = { it.transaction.id }
                    ) { transactionWithCategory ->
                        TransactionItem(
                            transaction = transactionWithCategory.transaction,
                            categoryName = transactionWithCategory.categoryName,
                            categoryColor = transactionWithCategory.categoryColor,
                            onClick = { onNavigateToTransactionDetail(transactionWithCategory.transaction.id) }
                        )
                    }
                }
                
                // Bottom spacing for FAB
                item {
                    Spacer(modifier = Modifier.height(Spacing.huge))
                }
            }
            }
        }
    }
}

/**
 * Trigger vibration feedback for pull gesture
 */
private fun triggerVibration(context: android.content.Context) {
    try {
        val vibrator = context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? Vibrator
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(100)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
