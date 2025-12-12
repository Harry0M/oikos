package com.theblankstate.epmanager.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// ==========================================
// MATERIAL 3 SHAPES
// ==========================================
val AppShapes = Shapes(
    // Small - Chips, small buttons
    small = RoundedCornerShape(8.dp),
    
    // Medium - Cards, text fields
    medium = RoundedCornerShape(16.dp),
    
    // Large - Bottom sheets, large cards
    large = RoundedCornerShape(24.dp),
    
    // Extra Large - Feature cards, dialogs
    extraLarge = RoundedCornerShape(32.dp)
)

// ==========================================
// CUSTOM SHAPES
// ==========================================

// Pill shape for buttons and tags
val PillShape = RoundedCornerShape(percent = 50)

// Card shapes
val CardShape = RoundedCornerShape(24.dp)
val CardShapeLarge = RoundedCornerShape(32.dp)
val CardShapeSmall = RoundedCornerShape(16.dp)

// Button shapes
val ButtonShape = RoundedCornerShape(12.dp)
val ButtonShapePill = PillShape

// Dialog shape
val DialogShape = RoundedCornerShape(28.dp)

// Bottom sheet shape
val BottomSheetShape = RoundedCornerShape(
    topStart = 28.dp,
    topEnd = 28.dp,
    bottomStart = 0.dp,
    bottomEnd = 0.dp
)

// Feature icon container
val IconContainerShape = RoundedCornerShape(12.dp)

// Tag shape
val TagShape = PillShape

// FAB shape
val FabShape = RoundedCornerShape(16.dp)

// Input field shape
val InputFieldShape = RoundedCornerShape(12.dp)
