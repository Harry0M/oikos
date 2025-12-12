package com.theblankstate.epmanager.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.theblankstate.epmanager.ui.auth.AuthViewModel
import com.theblankstate.epmanager.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToBudget: () -> Unit = {},
    onNavigateToRecurring: () -> Unit = {},
    onNavigateToExport: () -> Unit = {},
    onNavigateToGoals: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToAIInsights: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToCategories: () -> Unit = {},
    onNavigateToAccounts: () -> Unit = {},
    onNavigateToSubscriptions: () -> Unit = {},
    onNavigateToSplit: () -> Unit = {},
    onNavigateToSmsSettings: () -> Unit = {},
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val authState by authViewModel.uiState.collectAsState()
    var isDarkMode by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.md)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(Spacing.xl))
        
        // Account Section
        SettingsSectionHeader("Account")
        
        // Show different UI based on auth state
        if (authState.isLoggedIn) {
            // Logged in - show profile
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Spacing.xxs)
                    .clickable(onClick = onNavigateToProfile),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.AccountCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(Spacing.md))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = authState.user?.email ?: "Signed In",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Tap to manage account & sync",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                    
                    Icon(
                        imageVector = Icons.Filled.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        } else {
            // Not logged in - show sign in option
            SettingsItem(
                icon = Icons.Filled.Person,
                title = "Sign In",
                subtitle = "Sign in to backup your data",
                onClick = onNavigateToLogin
            )
        }
        
        Spacer(modifier = Modifier.height(Spacing.lg))
        
        // Budgeting Section
        SettingsSectionHeader("Budgeting")
        
        SettingsItem(
            icon = Icons.Filled.Savings,
            title = "Budgets",
            subtitle = "Set spending limits per category",
            onClick = onNavigateToBudget
        )
        
        SettingsItem(
            icon = Icons.Filled.Repeat,
            title = "Recurring Expenses",
            subtitle = "Manage subscriptions and bills",
            onClick = onNavigateToRecurring
        )
        
        SettingsItem(
            icon = Icons.Filled.Subscriptions,
            title = "Subscriptions",
            subtitle = "Track your monthly subscriptions",
            onClick = onNavigateToSubscriptions
        )
        
        SettingsItem(
            icon = Icons.Filled.Flag,
            title = "Savings Goals",
            subtitle = "Track your savings targets",
            onClick = onNavigateToGoals
        )
        
        SettingsItem(
            icon = Icons.Filled.Groups,
            title = "Split Expenses",
            subtitle = "Split bills with friends and groups",
            onClick = onNavigateToSplit
        )
        
        Spacer(modifier = Modifier.height(Spacing.lg))
        
        // AI Features Section
        SettingsSectionHeader("AI Features")
        
        SettingsItem(
            icon = Icons.Filled.AutoAwesome,
            title = "AI Insights ✨",
            subtitle = "Smart spending analysis & predictions",
            onClick = onNavigateToAIInsights
        )
        
        SettingsItem(
            icon = Icons.Filled.Sms,
            title = "SMS Auto-Detection ✨",
            subtitle = "AI-powered bank SMS parsing",
            onClick = onNavigateToSmsSettings
        )
        
        Spacer(modifier = Modifier.height(Spacing.lg))
        
        // Preferences Section
        SettingsSectionHeader("Preferences")
        
        SettingsToggleItem(
            icon = Icons.Filled.DarkMode,
            title = "Dark Mode",
            subtitle = "Use dark theme",
            checked = isDarkMode,
            onCheckedChange = { isDarkMode = it }
        )
        
        SettingsItem(
            icon = Icons.Filled.Category,
            title = "Categories",
            subtitle = "Manage expense categories",
            onClick = onNavigateToCategories
        )
        
        SettingsItem(
            icon = Icons.Filled.AccountBalance,
            title = "Accounts",
            subtitle = "Manage payment accounts",
            onClick = onNavigateToAccounts
        )
        
        SettingsItem(
            icon = Icons.Filled.AttachMoney,
            title = "Currency",
            subtitle = "INR - Indian Rupee"
        )
        
        Spacer(modifier = Modifier.height(Spacing.lg))
        
        // Notifications Section
        SettingsSectionHeader("Notifications")
        
        SettingsItem(
            icon = Icons.Filled.Notifications,
            title = "Notification Settings",
            subtitle = "Budget alerts, reminders, insights",
            onClick = onNavigateToNotifications
        )
        
        Spacer(modifier = Modifier.height(Spacing.lg))
        
        // Data Section
        SettingsSectionHeader("Data")
        
        SettingsItem(
            icon = Icons.Filled.FileDownload,
            title = "Export Data",
            subtitle = "Export as CSV or PDF",
            onClick = onNavigateToExport
        )
        
        SettingsItem(
            icon = Icons.Filled.Delete,
            title = "Clear All Data",
            subtitle = "Delete all transactions",
            isDestructive = true
        )
        
        Spacer(modifier = Modifier.height(Spacing.lg))
        
        // About Section
        SettingsSectionHeader("About")
        
        SettingsItem(
            icon = Icons.AutoMirrored.Filled.Help,
            title = "Help & Support",
            subtitle = "Get help with the app"
        )
        
        SettingsItem(
            icon = Icons.Filled.Info,
            title = "About",
            subtitle = "EP Manager v1.0.0"
        )
        
        Spacer(modifier = Modifier.height(Spacing.huge))
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = Spacing.sm)
    )
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isDestructive: Boolean = false,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xxs)
            .clickable(onClick = onClick),
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
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isDestructive) 
                    MaterialTheme.colorScheme.error 
                else 
                    MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(Spacing.md))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (isDestructive) 
                        MaterialTheme.colorScheme.error 
                    else 
                        MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xxs),
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
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(Spacing.md))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}
