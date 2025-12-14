package com.theblankstate.epmanager.ui.home

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut

import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.theblankstate.epmanager.ui.components.*
import com.theblankstate.epmanager.ui.theme.*
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import java.text.SimpleDateFormat
import java.util.*

// Helper function to get icon from name
private fun getCategoryIcon(iconName: String): ImageVector = when (iconName) {
    "Restaurant" -> Icons.Filled.Restaurant
    "DirectionsCar" -> Icons.Filled.DirectionsCar
    "ShoppingBag" -> Icons.Filled.ShoppingBag
    "Movie" -> Icons.Filled.Movie
    "Receipt" -> Icons.Filled.Receipt
    "LocalHospital" -> Icons.Filled.LocalHospital
    "School" -> Icons.Filled.School
    "ShoppingCart" -> Icons.Filled.ShoppingCart
    "Subscriptions" -> Icons.Filled.Subscriptions
    "Flight" -> Icons.Filled.Flight
    "Work" -> Icons.Filled.Work
    "Computer" -> Icons.Filled.Computer
    "TrendingUp" -> Icons.Filled.TrendingUp
    "CardGiftcard" -> Icons.Filled.CardGiftcard
    "Replay" -> Icons.Filled.Replay
    "Home" -> Icons.Filled.Home
    "Pets" -> Icons.Filled.Pets
    "SportsEsports" -> Icons.Filled.SportsEsports
    "FitnessCenter" -> Icons.Filled.FitnessCenter
    "Fitness" -> Icons.Filled.FitnessCenter
    "LocalCafe" -> Icons.Filled.LocalCafe
    "LocalBar" -> Icons.Filled.LocalBar
    "LocalGroceryStore" -> Icons.Filled.LocalGroceryStore
    "LocalMall" -> Icons.Filled.LocalMall
    "Money" -> Icons.Filled.Money
    "AccountBalance" -> Icons.Filled.AccountBalance
    "PhoneAndroid" -> Icons.Filled.PhoneAndroid
    "CreditCard" -> Icons.Filled.CreditCard
    else -> Icons.Filled.MoreHoriz
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToAddTransaction: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToTransactionDetail: (String) -> Unit = {},
    onNavigateToAccounts: () -> Unit = {},
    onNavigateToRecurring: () -> Unit = {},
    onNavigateToSubscriptions: () -> Unit = {},
    onNavigateToGoals: () -> Unit = {},
    onNavigateToDebtCredit: () -> Unit = {},
    onNavigateToSplit: () -> Unit = {},
    onNavigateToFriends: () -> Unit = {},
    onNavigateToCategories: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lazyListState = rememberLazyListState()
    
    // Quick actions sheet state
    var showQuickActionsSheet by remember { mutableStateOf(false) }
    
    // Force refresh data every time this screen becomes visible
    LaunchedEffect(Unit) {
        viewModel.refresh()
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
                // Header with action buttons
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = Spacing.sm),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Dashboard",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                        ) {
                            // Accounts button
                            IconButton(
                                onClick = onNavigateToAccounts
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AccountBalance,
                                    contentDescription = "Accounts",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            // Quick actions button
                            FilledIconButton(
                                onClick = { showQuickActionsSheet = true }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = "Quick Actions"
                                )
                            }
                        }
                    }
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
                    
                    // See More button at the end
                    item {
                        TextButton(
                            onClick = onNavigateToTransactions,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = Spacing.sm)
                        ) {
                            Text(
                                text = "See More Transactions",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(start = Spacing.xs)
                                    .size(16.dp)
                            )
                        }
                    }
                }
                
                // Upcoming Dues Section
                if (uiState.upcomingDues.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(Spacing.lg))
                        Text(
                            text = "Upcoming Dues",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(Spacing.sm))
                    }
                    
                    items(
                        items = uiState.upcomingDues.take(5),
                        key = { "${it.type}_${it.id}" }
                    ) { due ->
                        UpcomingDueItem(
                            due = due,
                            onClick = { viewModel.selectDue(due) }
                        )
                    }
                    
                    if (uiState.upcomingDues.size > 5) {
                        item {
                            Text(
                                text = "+${uiState.upcomingDues.size - 5} more dues",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = Spacing.sm)
                            )
                        }
                    }
                }
                
                // Budget Alerts Section
                if (uiState.budgetAlerts.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(Spacing.lg))
                        Text(
                            text = "Budget Exceeding",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Error
                        )
                        Spacer(modifier = Modifier.height(Spacing.sm))
                    }
                    
                    items(uiState.budgetAlerts) { budget ->
                        BudgetAlertItem(
                            budget = budget,
                            loading = false
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
    
    // Quick Actions Sheet
    if (showQuickActionsSheet) {
        QuickActionsSheet(
            onDismiss = { showQuickActionsSheet = false },
            onRecurringClick = { showQuickActionsSheet = false; onNavigateToRecurring() },
            onSubscriptionsClick = { showQuickActionsSheet = false; onNavigateToSubscriptions() },
            onGoalsClick = { showQuickActionsSheet = false; onNavigateToGoals() },
            onDebtCreditClick = { showQuickActionsSheet = false; onNavigateToDebtCredit() },
            onSplitClick = { showQuickActionsSheet = false; onNavigateToSplit() },
            onFriendsClick = { showQuickActionsSheet = false; onNavigateToFriends() },
            onCategoriesClick = { showQuickActionsSheet = false; onNavigateToCategories() }
        )
    }
    
    // Upcoming Due Action Sheet
    uiState.selectedDue?.let { due ->
        UpcomingDueActionSheet(
            due = due,
            onDismiss = { viewModel.dismissDueSheet() },
            onPayNow = { amount, note ->
                when (due) {
                    is UpcomingDue.DebtDue -> viewModel.payDebt(due.id, amount, note)
                    is UpcomingDue.SubscriptionDue, is UpcomingDue.ExpenseDue -> viewModel.paySubscriptionEarly(due)
                    else -> { }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickActionsSheet(
    onDismiss: () -> Unit,
    onRecurringClick: () -> Unit,
    onSubscriptionsClick: () -> Unit,
    onGoalsClick: () -> Unit,
    onDebtCreditClick: () -> Unit,
    onSplitClick: () -> Unit,
    onFriendsClick: () -> Unit,
    onCategoriesClick: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg)
                .padding(bottom = Spacing.xxl)
        ) {
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(Spacing.md))
            
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                QuickActionItem(
                    icon = Icons.Filled.Repeat,
                    title = "Expenses",
                    subtitle = "Manage recurring transactions",
                    onClick = onRecurringClick
                )
                QuickActionItem(
                    icon = Icons.Filled.Subscriptions,
                    title = "Subscriptions",
                    subtitle = "Track your subscriptions",
                    onClick = onSubscriptionsClick
                )
                QuickActionItem(
                    icon = Icons.Filled.Savings,
                    title = "Savings Goals",
                    subtitle = "Set and track goals",
                    onClick = onGoalsClick
                )
                QuickActionItem(
                    icon = Icons.Filled.CreditCard,
                    title = "Debts & Credits",
                    subtitle = "Track money owed",
                    onClick = onDebtCreditClick
                )
                QuickActionItem(
                    icon = Icons.Filled.Groups,
                    title = "Split Expenses",
                    subtitle = "Split bills with friends",
                    onClick = onSplitClick
                )
                QuickActionItem(
                    icon = Icons.Filled.People,
                    title = "Friends",
                    subtitle = "Manage friends",
                    onClick = onFriendsClick
                )
                QuickActionItem(
                    icon = Icons.Filled.Category,
                    title = "Categories",
                    subtitle = "Manage expense categories",
                    onClick = onCategoriesClick
                )
            }
        }
    }
}

@Composable
private fun QuickActionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
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
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(Spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
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

@Composable
private fun UpcomingDueItem(
    due: UpcomingDue,
    onClick: () -> Unit
) {
    val now = System.currentTimeMillis()
    val daysUntil = ((due.dueDate - now) / (24 * 60 * 60 * 1000)).toInt()
    val dateFormatter = SimpleDateFormat("MMM d", Locale.getDefault())
    
    val isSettled = when(due) {
        is UpcomingDue.ExpenseDue -> due.isSettled
        is UpcomingDue.SubscriptionDue -> due.isSettled
        else -> false
    }
    
    val (icon, iconColor, bgColor) = when {
        isSettled -> Triple(Icons.Filled.CheckCircle, Success, Success.copy(alpha = 0.05f))
        due.type == UpcomingDueType.DEBT -> Triple(Icons.Filled.ArrowUpward, Error, Error.copy(alpha = 0.1f))
        due.type == UpcomingDueType.CREDIT -> Triple(Icons.Filled.ArrowDownward, Success, Success.copy(alpha = 0.1f))
        due.type == UpcomingDueType.EXPENSE -> Triple(Icons.Filled.Repeat, Warning, Warning.copy(alpha = 0.1f))
        due.type == UpcomingDueType.SUBSCRIPTION -> Triple(Icons.Filled.Subscriptions, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
        due.type == UpcomingDueType.GOAL -> Triple(Icons.Filled.Flag, Success, Success.copy(alpha = 0.1f))
        else -> Triple(Icons.Filled.Info, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.surfaceVariant)
    }
    
    // Dim content if settled
    val contentAlpha = if (isSettled) 0.6f else 1f
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs)
            .then(if (!isSettled) Modifier.clickable(onClick = onClick) else Modifier),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(Spacing.md))
            
            // Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = due.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = when {
                            isSettled -> "Settled for today"
                            due.type == UpcomingDueType.DEBT -> "To pay"
                            due.type == UpcomingDueType.CREDIT -> "To receive"
                            due.type == UpcomingDueType.EXPENSE -> "Expense"
                            due.type == UpcomingDueType.SUBSCRIPTION -> "Subscription"
                            due.type == UpcomingDueType.GOAL -> "Goal deadline"
                            else -> ""
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha)
                    )
                    
                    if (!isSettled) {
                        Text(
                            text = " • ${dateFormatter.format(Date(due.dueDate))}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (daysUntil <= 3) Error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Amount and days
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatCurrency(due.amount),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = iconColor.copy(alpha = contentAlpha)
                )
                
                if (!isSettled) {
                    Text(
                        text = when {
                            daysUntil == 0 -> "Today"
                            daysUntil == 1 -> "Tomorrow"
                            daysUntil < 0 -> "Overdue"
                            else -> "$daysUntil days"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (daysUntil <= 1) Error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UpcomingDueActionSheet(
    due: UpcomingDue,
    onDismiss: () -> Unit,
    onPayNow: (Double, String?) -> Unit
) {
    var paymentAmount by remember { mutableStateOf(due.amount.toString()) }
    var note by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    val isDebtOrCredit = due.type == UpcomingDueType.DEBT || due.type == UpcomingDueType.CREDIT
    val isSubscriptionOrExpense = due.type == UpcomingDueType.SUBSCRIPTION || due.type == UpcomingDueType.EXPENSE
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg)
                .padding(bottom = Spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            // Header
            Text(
                text = when (due.type) {
                    UpcomingDueType.DEBT -> "Pay Debt"
                    UpcomingDueType.CREDIT -> "Record Collection"
                    UpcomingDueType.EXPENSE, UpcomingDueType.SUBSCRIPTION -> "Pay Early"
                    UpcomingDueType.GOAL -> "Goal Due"
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            // Due Info
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(Spacing.md)
                ) {
                    Text(
                        text = due.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Due: ${SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(due.dueDate))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatCurrency(due.amount),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            // For debt/credit: allow entering amount
            if (isDebtOrCredit) {
                OutlinedTextField(
                    value = paymentAmount,
                    onValueChange = { paymentAmount = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Payment Amount") },
                    prefix = { Text("₹") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = InputFieldShape
                )
                
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = InputFieldShape
                )
            }
            
            // For subscription/expense: show info
            if (isSubscriptionOrExpense) {
                Text(
                    text = "Paying early will record this transaction and update the next due date.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // For goals: just show info
            if (due.type == UpcomingDueType.GOAL) {
                Text(
                    text = "Visit Goals screen to add contribution.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(Spacing.md))
            
            // Action Buttons
            if (due.type != UpcomingDueType.GOAL) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = ButtonShapePill
                    ) {
                        Text("Cancel")
                    }
                    
                    Button(
                        onClick = {
                            val amount = paymentAmount.toDoubleOrNull() ?: due.amount
                            onPayNow(amount, note.ifEmpty { null })
                        },
                        modifier = Modifier.weight(1f),
                        shape = ButtonShapePill
                    ) {
                        Text(if (isDebtOrCredit) "Record Payment" else "Pay Now")
                    }
                }
            } else {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = ButtonShapePill
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
private fun BudgetAlertItem(
    budget: com.theblankstate.epmanager.data.model.BudgetWithSpending,
    loading: Boolean
) {
    val progress = budget.percentage / 100f
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Category info
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Error.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        getCategoryIcon(budget.category?.icon ?: "help_outline").let { icon ->
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = Error,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    
                    Column {
                        Text(
                            text = budget.category?.name ?: "Unknown",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${budget.budget.period.label} Budget",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${budget.percentage.toInt()}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Error
                    )
                    
                    if (budget.spent > budget.budget.amount) {
                         val extra = budget.spent - budget.budget.amount
                         Text(
                             text = "+${formatCurrency(extra)}",
                             style = MaterialTheme.typography.labelSmall,
                             color = Error,
                             fontWeight = FontWeight.Bold
                         )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(Spacing.sm))
            
            // Progress Bar
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = Error,
                trackColor = Error.copy(alpha = 0.2f),
            )
            
            Spacer(modifier = Modifier.height(Spacing.xs))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Spent: ${formatCurrency(budget.spent)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Error
                )
                Text(
                    text = "Limit: ${formatCurrency(budget.budget.amount)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
