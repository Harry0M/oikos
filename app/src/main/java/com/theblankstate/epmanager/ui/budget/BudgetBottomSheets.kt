package com.theblankstate.epmanager.ui.budget

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.theblankstate.epmanager.data.model.BudgetPeriod
import com.theblankstate.epmanager.data.model.Category
import com.theblankstate.epmanager.data.model.CategoryType
import com.theblankstate.epmanager.ui.theme.*

/**
 * Helper function to convert Material icon names to emoji for display
 */
private fun getCategoryEmoji(iconName: String): String {
    return when (iconName) {
        "Restaurant" -> "🍔"
        "ShoppingCart" -> "🛒"
        "DirectionsCar" -> "🚗"
        "LocalGasStation" -> "⛽"
        "Home" -> "🏠"
        "Lightbulb" -> "💡"
        "Smartphone" -> "📱"
        "Computer" -> "💻"
        "SportsEsports" -> "🎮"
        "Movie" -> "🎬"
        "MusicNote" -> "🎵"
        "Book" -> "📚"
        "Checkroom" -> "👕"
        "LocalHospital" -> "🏥"
        "Flight" -> "✈️"
        "CardGiftcard" -> "🎁"
        "AttachMoney" -> "💰"
        "CreditCard" -> "💳"
        "TrendingUp" -> "📈"
        "School" -> "🎓"
        "FitnessCenter" -> "💪"
        "Pets" -> "🐕"
        "Coffee" -> "☕"
        "MoreHoriz" -> "⋯"
        "ShoppingBag" -> "🛍️"
        "Receipt" -> "🧾"
        "Work" -> "💼"
        "Replay" -> "🔄"
        "SwapVert" -> "↕️"
        "Subscriptions" -> "📺"
        else -> "💰"
    }
}

/**
 * Bottom sheet for adding a new budget
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBudgetBottomSheet(
    categories: List<Category>,
    onDismiss: () -> Unit,
    onCategorySelect: () -> Unit,
    onAddNewCategory: () -> Unit,
    onConfirm: (categoryId: String, amount: Double, period: BudgetPeriod) -> Unit,
    selectedCategory: Category? = null
) {
    var amount by remember { mutableStateOf("") }
    var selectedPeriod by remember { mutableStateOf(BudgetPeriod.MONTHLY) }
    
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg)
                .padding(bottom = Spacing.xxl)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Add Budget",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close")
                }
            }
            
            Spacer(modifier = Modifier.height(Spacing.lg))
            
            // Category Selection
            Text(
                text = "Category",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(Spacing.sm))
            
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCategorySelect() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.md),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (selectedCategory != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(selectedCategory.color).copy(alpha = 0.2f),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = getCategoryEmoji(selectedCategory.icon),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                            Text(
                                text = selectedCategory.name,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    } else {
                        Text(
                            text = "Select Category",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowRight,
                        contentDescription = null
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(Spacing.lg))
            
            // Amount Input
            Text(
                text = "Budget Amount",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(Spacing.sm))
            
            OutlinedTextField(
                value = amount,
                onValueChange = { 
                    amount = it.filter { c -> c.isDigit() || c == '.' }
                },
                placeholder = { Text("0.00") },
                prefix = { 
                    Text(
                        "₹",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                textStyle = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = InputFieldShape,
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(Spacing.lg))
            
            // Period Selection
            Text(
                text = "Budget Period",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(Spacing.sm))
            
            // Period options in a grid
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                BudgetPeriod.entries.chunked(2).forEach { rowPeriods ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                    ) {
                        rowPeriods.forEach { period ->
                            FilterChip(
                                selected = selectedPeriod == period,
                                onClick = { selectedPeriod = period },
                                label = { Text(period.label) },
                                modifier = Modifier.weight(1f),
                                leadingIcon = if (selectedPeriod == period) {
                                    { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                } else null
                            )
                        }
                        // Add empty space if odd number in row
                        if (rowPeriods.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(Spacing.xxl))
            
            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                
                Button(
                    onClick = {
                        val amountValue = amount.toDoubleOrNull()
                        if (selectedCategory != null && amountValue != null && amountValue > 0) {
                            onConfirm(selectedCategory.id, amountValue, selectedPeriod)
                        }
                    },
                    enabled = selectedCategory != null && amount.toDoubleOrNull()?.let { it > 0 } == true,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Add Budget")
                }
            }
        }
    }
}

/**
 * Bottom sheet for selecting a category
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectCategoryBottomSheet(
    categories: List<Category>,
    onDismiss: () -> Unit,
    onCategorySelected: (Category) -> Unit,
    onAddNewCategory: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Select Category",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close")
                }
            }
            
            Spacer(modifier = Modifier.height(Spacing.md))
            
            // Add New Category Button
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAddNewCategory() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.md),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Text(
                        text = "Add New Category",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(Spacing.md))
            
            // Categories List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                items(categories) { category ->
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onCategorySelected(category)
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Spacing.md),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(category.color).copy(alpha = 0.2f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = getCategoryEmoji(category.icon),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                            Text(
                                text = category.name,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
                
                // Bottom spacing
                item {
                    Spacer(modifier = Modifier.height(Spacing.xxl))
                }
            }
        }
    }
}

/**
 * Bottom sheet for adding a new category
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCategoryBottomSheet(
    onDismiss: () -> Unit,
    onConfirm: (name: String, icon: String, color: Long, type: CategoryType) -> Unit
) {
    var categoryName by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf("ShoppingCart") }
    var selectedColor by remember { mutableStateOf(0xFF6750A4) }
    
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    // Available Material Icons (these map to the icon names used in Category model)
    val iconOptions = listOf(
        "Restaurant" to "🍔",
        "ShoppingCart" to "🛒",
        "DirectionsCar" to "🚗",
        "LocalGasStation" to "⛽",
        "Home" to "🏠",
        "Lightbulb" to "💡",
        "Smartphone" to "📱",
        "Computer" to "💻",
        "SportsEsports" to "🎮",
        "Movie" to "🎬",
        "MusicNote" to "🎵",
        "Book" to "📚",
        "Checkroom" to "👕",
        "LocalHospital" to "🏥",
        "Flight" to "✈️",
        "CardGiftcard" to "🎁",
        "AttachMoney" to "💰",
        "CreditCard" to "💳",
        "TrendingUp" to "📈",
        "School" to "🎓",
        "FitnessCenter" to "💪",
        "Pets" to "🐕",
        "Coffee" to "☕",
        "MoreHoriz" to "⋯"
    )
    
    // Available colors
    val colors = listOf(
        0xFFE57373, 0xFFF06292, 0xFFBA68C8, 0xFF9575CD,
        0xFF7986CB, 0xFF64B5F6, 0xFF4FC3F7, 0xFF4DD0E1,
        0xFF4DB6AC, 0xFF81C784, 0xFFAED581, 0xFFDCE775,
        0xFFFFD54F, 0xFFFFB74D, 0xFFFF8A65, 0xFFA1887F
    )
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg)
                .padding(bottom = Spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add Category",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }
            }
            
            // Category Name
            item {
                Column {
                    Text(
                        text = "Category Name",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    
                    OutlinedTextField(
                        value = categoryName,
                        onValueChange = { categoryName = it },
                        placeholder = { Text("e.g., Groceries") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = InputFieldShape,
                        singleLine = true
                    )
                }
            }
            
            // Icon Selection
            item {
                Column {
                    Text(
                        text = "Icon",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    
                    // Icon Grid (using emoji representations)
                    Column(
                        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                    ) {
                        iconOptions.chunked(6).forEach { rowIcons ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                            ) {
                                rowIcons.forEach { (iconName, emoji) ->
                                    Surface(
                                        shape = CircleShape,
                                        color = if (selectedIcon == iconName) 
                                            MaterialTheme.colorScheme.primaryContainer
                                        else 
                                            MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clickable { selectedIcon = iconName }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = emoji,
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // Color Selection
            item {
                Column {
                    Text(
                        text = "Color",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    
                    // Color Grid
                    Column(
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        colors.chunked(8).forEach { rowColors ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                            ) {
                                rowColors.forEach { color ->
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(color),
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clickable { selectedColor = color }
                                    ) {
                                        if (selectedColor == color) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Filled.Check,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // Action Buttons
            item {
                Spacer(modifier = Modifier.height(Spacing.md))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    
                    Button(
                        onClick = {
                            if (categoryName.isNotBlank()) {
                                onConfirm(categoryName, selectedIcon, selectedColor, CategoryType.EXPENSE)
                            }
                        },
                        enabled = categoryName.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Add")
                    }
                }
            }
        }
    }
}
