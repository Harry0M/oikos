package com.theblankstate.epmanager.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Accessibility-enhanced composables and utilities for TalkBack support.
 */

/**
 * A text composable that is marked as a heading for screen readers.
 * Use this for section headers to improve navigation.
 */
@Composable
fun AccessibleHeading(
    text: String,
    modifier: Modifier = Modifier,
    style: @Composable () -> Unit = { Text(text, style = MaterialTheme.typography.headlineMedium) }
) {
    Box(
        modifier = modifier.semantics { heading() }
    ) {
        style()
    }
}

/**
 * Composable wrapper that adds a content description for accessibility.
 */
@Composable
fun AccessibleBox(
    description: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier.semantics {
            contentDescription = description
        }
    ) {
        content()
    }
}

/**
 * Tooltip overlay for onboarding hints.
 * Shows a floating hint box that can be dismissed.
 */
@Composable
fun OnboardingTooltip(
    text: String,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    autoDismissMs: Long = 5000L
) {
    var visible by remember { mutableStateOf(isVisible) }
    
    LaunchedEffect(isVisible) {
        visible = isVisible
        if (isVisible && autoDismissMs > 0) {
            delay(autoDismissMs)
            visible = false
            onDismiss()
        }
    }
    
    if (visible) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            shadowElevation = 8.dp,
            onClick = {
                visible = false
                onDismiss()
            }
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tap to dismiss",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * Object containing standard content descriptions for common UI elements.
 * Use these for consistency across the app.
 */
object ContentDescriptions {
    // Navigation
    const val BACK_BUTTON = "Navigate back"
    const val CLOSE_BUTTON = "Close"
    const val MENU_BUTTON = "Open menu"
    const val MORE_OPTIONS = "More options"
    
    // Actions
    const val ADD_TRANSACTION = "Add new transaction"
    const val ADD_EXPENSE = "Add expense"
    const val ADD_INCOME = "Add income"
    const val DELETE = "Delete"
    const val EDIT = "Edit"
    const val SAVE = "Save"
    const val CANCEL = "Cancel"
    const val SEARCH = "Search"
    const val FILTER = "Filter"
    
    // Transaction types
    const val EXPENSE_ICON = "Expense"
    const val INCOME_ICON = "Income"
    
    // Categories
    fun categoryIcon(categoryName: String) = "$categoryName category"
    
    // Amounts
    fun amount(value: String, type: String) = "$type of $value"
    
    // Charts
    const val PIE_CHART = "Spending breakdown chart"
    const val BAR_CHART = "Monthly comparison chart"
    const val LINE_CHART = "Trend chart"
    
    // Status
    const val LOADING = "Loading content"
    const val EMPTY_STATE = "No items to display"
    const val ERROR_STATE = "An error occurred"
}
