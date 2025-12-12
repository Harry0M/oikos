package com.theblankstate.epmanager.ui.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.theblankstate.epmanager.data.model.Category
import com.theblankstate.epmanager.data.model.CategoryType
import com.theblankstate.epmanager.ui.theme.*

// Available icons for category selection
private val availableIcons = listOf(
    "Restaurant", "DirectionsCar", "ShoppingBag", "Movie", "Receipt",
    "LocalHospital", "School", "ShoppingCart", "Subscriptions", "Flight",
    "Work", "Computer", "TrendingUp", "CardGiftcard", "Replay",
    "Home", "Pets", "SportsEsports", "Fitness", "LocalCafe",
    "LocalBar", "LocalGroceryStore", "LocalMall", "MoreHoriz"
)

// Available colors for category selection
private val availableColors = listOf(
    0xFFFF6B6B, 0xFF4ECDC4, 0xFFFFE66D, 0xFF95E1D3, 0xFFF38181,
    0xFFAA96DA, 0xFF74B9FF, 0xFF55A3FF, 0xFFA29BFE, 0xFFFD79A8,
    0xFF22C55E, 0xFF14B8A6, 0xFF3B82F6, 0xFF8B5CF6, 0xFFF59E0B,
    0xFF9CA3AF, 0xFFEF4444, 0xFFF97316, 0xFF84CC16, 0xFF06B6D4
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    onNavigateBack: () -> Unit,
    viewModel: CategoriesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Categories",
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
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showAddDialog() },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Add Category"
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Row
            TabRow(
                selectedTabIndex = uiState.selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = uiState.selectedTab == 0,
                    onClick = { viewModel.setSelectedTab(0) },
                    text = { Text("Expense") },
                    icon = { Icon(Icons.Filled.ArrowCircleUp, null) }
                )
                Tab(
                    selected = uiState.selectedTab == 1,
                    onClick = { viewModel.setSelectedTab(1) },
                    text = { Text("Income") },
                    icon = { Icon(Icons.Filled.ArrowCircleDown, null) }
                )
            }
            
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                val categories = if (uiState.selectedTab == 0) 
                    uiState.expenseCategories 
                else 
                    uiState.incomeCategories
                
                if (categories.isEmpty()) {
                    EmptyState(
                        isExpense = uiState.selectedTab == 0,
                        onAddClick = { viewModel.showAddDialog() }
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(Spacing.md),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        items(
                            items = categories,
                            key = { it.id }
                        ) { category ->
                            CategoryItem(
                                category = category,
                                onEdit = { viewModel.showEditDialog(category) },
                                onDelete = { viewModel.deleteCategory(category) }
                            )
                        }
                        
                        item {
                            Spacer(modifier = Modifier.height(Spacing.huge))
                        }
                    }
                }
            }
        }
    }
    
    // Add/Edit Dialog
    if (uiState.showAddDialog) {
        AddEditCategoryDialog(
            existingCategory = uiState.editingCategory,
            defaultType = if (uiState.selectedTab == 0) CategoryType.EXPENSE else CategoryType.INCOME,
            onDismiss = { viewModel.hideDialog() },
            onConfirm = { name, icon, color, type ->
                viewModel.saveCategory(name, icon, color, type)
            }
        )
    }
}

@Composable
private fun EmptyState(
    isExpense: Boolean,
    onAddClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (isExpense) "📊" else "💰",
            style = MaterialTheme.typography.displayLarge
        )
        Spacer(modifier = Modifier.height(Spacing.md))
        Text(
            text = "No ${if (isExpense) "Expense" else "Income"} Categories",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(Spacing.xs))
        Text(
            text = "Tap + to add a custom category",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CategoryItem(
    category: Category,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Color Circle with Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(category.color).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getCategoryIcon(category.icon),
                    contentDescription = null,
                    tint = Color(category.color),
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(Spacing.md))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (category.isDefault) {
                        AssistChip(
                            onClick = {},
                            label = { 
                                Text(
                                    "Default", 
                                    style = MaterialTheme.typography.labelSmall
                                ) 
                            },
                            modifier = Modifier.height(24.dp)
                        )
                    }
                }
            }
            
            if (!category.isDefault) {
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
    
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Category") },
            text = { Text("Are you sure you want to delete '${category.name}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditCategoryDialog(
    existingCategory: Category?,
    defaultType: CategoryType,
    onDismiss: () -> Unit,
    onConfirm: (name: String, icon: String, color: Long, type: CategoryType) -> Unit
) {
    var name by remember { mutableStateOf(existingCategory?.name ?: "") }
    var selectedIcon by remember { mutableStateOf(existingCategory?.icon ?: "MoreHoriz") }
    var selectedColor by remember { mutableStateOf(existingCategory?.color ?: availableColors[0]) }
    var selectedType by remember { mutableStateOf(existingCategory?.type ?: defaultType) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (existingCategory != null) "Edit Category" else "Add Category",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                // Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    placeholder = { Text("Category name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Type selector (only for new categories)
                if (existingCategory == null) {
                    Text(
                        text = "Type",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        FilterChip(
                            selected = selectedType == CategoryType.EXPENSE,
                            onClick = { selectedType = CategoryType.EXPENSE },
                            label = { Text("Expense") },
                            leadingIcon = if (selectedType == CategoryType.EXPENSE) {
                                { Icon(Icons.Filled.Check, null, Modifier.size(18.dp)) }
                            } else null
                        )
                        FilterChip(
                            selected = selectedType == CategoryType.INCOME,
                            onClick = { selectedType = CategoryType.INCOME },
                            label = { Text("Income") },
                            leadingIcon = if (selectedType == CategoryType.INCOME) {
                                { Icon(Icons.Filled.Check, null, Modifier.size(18.dp)) }
                            } else null
                        )
                    }
                }
                
                // Icon Picker
                Text(
                    text = "Icon",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    items(availableIcons) { iconName ->
                        val isSelected = iconName == selectedIcon
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) 
                                        MaterialTheme.colorScheme.primaryContainer
                                    else 
                                        MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { selectedIcon = iconName },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = getCategoryIcon(iconName),
                                contentDescription = iconName,
                                tint = if (isSelected) 
                                    MaterialTheme.colorScheme.primary 
                                else 
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                
                // Color Picker
                Text(
                    text = "Color",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    items(availableColors) { color ->
                        val isSelected = color == selectedColor
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(color))
                                .clickable { selectedColor = color },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name, selectedIcon, selectedColor, selectedType)
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text(if (existingCategory != null) "Save" else "Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Helper function to get icon from name
@Composable
private fun getCategoryIcon(iconName: String) = when (iconName) {
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
