package com.theblankstate.epmanager.ui.edit

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.theblankstate.epmanager.data.model.Category
import com.theblankstate.epmanager.data.model.TransactionType
import com.theblankstate.epmanager.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionScreen(
    onNavigateBack: () -> Unit,
    onTransactionSaved: () -> Unit,
    viewModel: EditTransactionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Navigate back when saved
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onTransactionSaved()
        }
    }
    
    // Show error snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Edit Transaction",
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
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(Spacing.md)
            ) {
                // Transaction Type Toggle
                TransactionTypeToggle(
                    selectedType = uiState.transactionType,
                    onTypeSelected = viewModel::updateTransactionType
                )
                
                Spacer(modifier = Modifier.height(Spacing.xl))
                
                // Amount Input
                AmountInput(
                    amount = uiState.amount,
                    onAmountChange = viewModel::updateAmount,
                    isExpense = uiState.transactionType == TransactionType.EXPENSE
                )
                
                Spacer(modifier = Modifier.height(Spacing.xl))
                
                // Category Selection
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
                
                CategorySelector(
                    categories = uiState.categories,
                    selectedCategory = uiState.selectedCategory,
                    onCategorySelected = viewModel::selectCategory
                )
                
                Spacer(modifier = Modifier.height(Spacing.xl))
                
                // Note Input
                OutlinedTextField(
                    value = uiState.note,
                    onValueChange = viewModel::updateNote,
                    label = { Text("Note (optional)") },
                    placeholder = { Text("Add a note...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = InputFieldShape,
                    maxLines = 2
                )
                
                Spacer(modifier = Modifier.height(Spacing.xl))
                
                // Account Selection
                Text(
                    text = "Account",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    items(
                        items = uiState.accounts,
                        key = { it.id }
                    ) { account ->
                        val isSelected = account.id == uiState.selectedAccount?.id
                        
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectAccount(account) },
                            label = { Text(account.name) },
                            leadingIcon = {
                                Icon(
                                    imageVector = when (account.name.lowercase()) {
                                        "cash" -> Icons.Filled.Money
                                        "bank account" -> Icons.Filled.AccountBalance
                                        "upi" -> Icons.Filled.PhoneAndroid
                                        "credit card" -> Icons.Filled.CreditCard
                                        else -> Icons.Filled.Wallet
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Save Button
                Button(
                    onClick = viewModel::saveTransaction,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = !uiState.isSaving && uiState.amount.isNotEmpty(),
                    shape = ButtonShapePill
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Save Changes",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(Spacing.md))
            }
        }
    }
}

@Composable
private fun TransactionTypeToggle(
    selectedType: TransactionType,
    onTypeSelected: (TransactionType) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(PillShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(Spacing.xxs),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xxs)
    ) {
        TransactionType.entries.forEach { type ->
            val isSelected = type == selectedType
            val backgroundColor by animateColorAsState(
                targetValue = if (isSelected) {
                    if (type == TransactionType.EXPENSE) Error else Green
                } else {
                    Color.Transparent
                },
                label = "tab_bg"
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) White else MaterialTheme.colorScheme.onSurfaceVariant,
                label = "tab_text"
            )
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(PillShape)
                    .background(backgroundColor)
                    .clickable { onTypeSelected(type) }
                    .padding(vertical = Spacing.sm),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = type.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor
                )
            }
        }
    }
}

@Composable
private fun AmountInput(
    amount: String,
    onAmountChange: (String) -> Unit,
    isExpense: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "₹",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        OutlinedTextField(
            value = amount,
            onValueChange = onAmountChange,
            textStyle = MaterialTheme.typography.displayMedium.copy(
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = if (isExpense) Error else Green
            ),
            placeholder = {
                Text(
                    text = "0",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent
            )
        )
    }
}

@Composable
private fun CategorySelector(
    categories: List<Category>,
    selectedCategory: Category?,
    onCategorySelected: (Category) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        items(
            items = categories,
            key = { it.id }
        ) { category ->
            val isSelected = category.id == selectedCategory?.id
            val categoryColor = Color(category.color)
            
            Column(
                modifier = Modifier
                    .clip(CardShapeSmall)
                    .background(
                        if (isSelected) categoryColor.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .border(
                        width = if (isSelected) 2.dp else 0.dp,
                        color = if (isSelected) categoryColor else Color.Transparent,
                        shape = CardShapeSmall
                    )
                    .clickable { onCategorySelected(category) }
                    .padding(Spacing.md),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(categoryColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Category,
                        contentDescription = null,
                        tint = categoryColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    text = category.name.split(" ").first(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) categoryColor else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
