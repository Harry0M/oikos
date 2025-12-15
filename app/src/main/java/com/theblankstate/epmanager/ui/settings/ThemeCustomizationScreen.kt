package com.theblankstate.epmanager.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.theblankstate.epmanager.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeCustomizationScreen(
    onNavigateBack: () -> Unit,
    viewModel: com.theblankstate.epmanager.ui.theme.ThemeViewModel = hiltViewModel()
) {
    val themeState by viewModel.themeState.collectAsState()
    
    // Use values directly from themeState to ensure we always show the persistent state
    // This avoids "sticking" to the default value if the DataStore loads asynchronously
    val primaryColor = themeState.customPrimary
    val secondaryColor = themeState.customSecondary
    val tertiaryColor = themeState.customTertiary

    // Pre-defined palettes for quick selection
    val predefinedColors = listOf(
        0xFF2196F3.toInt() to "Blue",
        0xFF4CAF50.toInt() to "Green",
        0xFFFFC107.toInt() to "Amber",
        0xFFE91E63.toInt() to "Pink",
        0xFF9C27B0.toInt() to "Purple",
        0xFF00BCD4.toInt() to "Cyan",
        0xFFFF5722.toInt() to "Deep Orange",
        0xFF607D8B.toInt() to "Blue Grey",
        0xFF000000.toInt() to "Black",
        0xFFFFFFFF.toInt() to "White",
        0xFF795548.toInt() to "Brown",
        0xFF673AB7.toInt() to "Deep Purple"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Custom Theme") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = Spacing.md)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            // Preview Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(primaryColor)
                )
            ) {
                Column(
                    modifier = Modifier.padding(Spacing.lg),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Preview Header",
                        style = MaterialTheme.typography.headlineSmall,
                        color = if (primaryColor == 0xFFFFFFFF.toInt()) Color.Black else Color.White
                    )
                    Spacer(modifier = Modifier.height(Spacing.md))
                    Button(
                        onClick = { },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(secondaryColor),
                            contentColor = if (secondaryColor == 0xFFFFFFFF.toInt()) Color.Black else Color.White
                        )
                    ) {
                        Text("Secondary Action")
                    }
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    Text(
                        text = "Tertiary Accent",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(tertiaryColor)
                    )
                }
            }

            // Primary Color Selector
            ColorSelectorSection(
                title = "Primary Color",
                selectedColor = primaryColor,
                colors = predefinedColors,
                onColorSelected = { 
                    viewModel.setCustomColors(it, secondaryColor, tertiaryColor)
                }
            )

            // Secondary Color Selector
            ColorSelectorSection(
                title = "Secondary Color",
                selectedColor = secondaryColor,
                colors = predefinedColors,
                onColorSelected = { 
                    viewModel.setCustomColors(primaryColor, it, tertiaryColor)
                }
            )

             // Tertiary Color Selector
            ColorSelectorSection(
                title = "Tertiary Color",
                selectedColor = tertiaryColor,
                colors = predefinedColors,
                onColorSelected = { 
                    viewModel.setCustomColors(primaryColor, secondaryColor, it)
                }
            )
            
            Spacer(modifier = Modifier.height(Spacing.xl))
        }
    }
}

@Composable
fun ColorSelectorSection(
    title: String,
    selectedColor: Int,
    colors: List<Pair<Int, String>>,
    onColorSelected: (Int) -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(Spacing.sm))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            colors.take(4).forEach { (colorInt, _) ->
                ColorCircle(
                    color = colorInt,
                    isSelected = colorInt == selectedColor,
                    onClick = { onColorSelected(colorInt) }
                )
            }
        }
        Spacer(modifier = Modifier.height(Spacing.sm))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            colors.drop(4).take(4).forEach { (colorInt, _) ->
                ColorCircle(
                    color = colorInt,
                    isSelected = colorInt == selectedColor,
                    onClick = { onColorSelected(colorInt) }
                )
            }
        }
    }
}

@Composable
fun ColorCircle(
    color: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Color(color))
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                shape = CircleShape
            )
            .clickable(onClick = onClick)
    )
}
