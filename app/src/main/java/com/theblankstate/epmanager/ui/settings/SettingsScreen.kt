package com.theblankstate.epmanager.ui.settings

import android.content.Intent
import android.net.Uri
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
import com.theblankstate.epmanager.util.CurrencyViewModel
import com.theblankstate.epmanager.data.model.CurrencyProvider
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onNavigateToBudget: () -> Unit = {},
    onNavigateToRecurring: () -> Unit = {},
    onNavigateToExport: () -> Unit = {},
    onNavigateToGoals: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToCategories: () -> Unit = {},
    onNavigateToAccounts: () -> Unit = {},
    onNavigateToSubscriptions: () -> Unit = {},
    onNavigateToSplit: () -> Unit = {},
    onNavigateToSmsSettings: () -> Unit = {},
    onNavigateToFriends: () -> Unit = {},
    onNavigateToDebtCredit: () -> Unit = {},
    onNavigateToThemeCustomization: () -> Unit = {},
    onNavigateToTerms: () -> Unit = {},
    onNavigateToOpenSource: () -> Unit = {},
    authViewModel: AuthViewModel = hiltViewModel(),
    themeViewModel: com.theblankstate.epmanager.ui.theme.ThemeViewModel = hiltViewModel(),
    currencyViewModel: CurrencyViewModel = hiltViewModel()
) {
    val authState by authViewModel.uiState.collectAsState()
    val themeState by themeViewModel.themeState.collectAsState()
    val currentCurrencyCode by currencyViewModel.currencyCode.collectAsState(initial = "INR")
    var showClearDataDialog by remember { mutableStateOf(false) }
    
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
                        if (authState.isSyncing) {
                            Text(
                                text = "Syncing data...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            val lastSync = authState.lastSyncTime
                            val syncText = if (lastSync != null) {
                                "Last synced: ${java.text.SimpleDateFormat("MMM dd, HH:mm").format(java.util.Date(lastSync))}"
                            } else {
                                "Tap to manage account & sync"
                            }
                            Text(
                                text = syncText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                    
                    Icon(
                        imageVector = Icons.Filled.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        } else {
            // Not logged in - show sign-in card
            val context = androidx.compose.ui.platform.LocalContext.current
            val coroutineScope = rememberCoroutineScope()
            var isSigningIn by remember { mutableStateOf(false) }
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Spacing.xxs)
                    .clickable(enabled = !isSigningIn) {
                        coroutineScope.launch {
                            isSigningIn = true
                            try {
                                val activity = context as? android.app.Activity
                                if (activity != null) {
                                    val googleSignInHelper = com.theblankstate.epmanager.ui.auth.GoogleSignInHelper(activity)
                                    googleSignInHelper.signIn()
                                        .onSuccess { idToken ->
                                            authViewModel.signInWithGoogle(idToken)
                                        }
                                }
                            } finally {
                                isSigningIn = false
                            }
                        }
                    },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Login,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(Spacing.md))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Sign In",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Sync your data across devices",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    if (isSigningIn) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(Spacing.lg))
        
        // Social Section
        SettingsSectionHeader("Social")
        
        SettingsItem(
            icon = Icons.Filled.People,
            title = "Friends",
            subtitle = "Add friends for shared expenses",
            onClick = onNavigateToFriends
        )
        
        // Animated Plan Your text
        val planTypes = listOf("Trip", "Vacation", "Groceries", "Party", "Event", "Home", "Project", "Adventure")
        var currentTypeIndex by remember { mutableIntStateOf(0) }
        
        LaunchedEffect(Unit) {
            while (true) {
                kotlinx.coroutines.delay(1500)
                currentTypeIndex = (currentTypeIndex + 1) % planTypes.size
            }
        }
        
        SettingsItem(
            icon = Icons.Filled.Groups,
            title = "",
            titleContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Plan Your ", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = planTypes[currentTypeIndex],
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            },
            subtitle = "Track shared expenses with groups",
            onClick = onNavigateToSplit
        )


        
        SettingsItem(
            icon = Icons.Filled.CreditCard,
            title = "Debts & Credits",
            subtitle = "Track money you owe or are owed",
            onClick = onNavigateToDebtCredit
        )
        
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
            title = "Expenses",
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
        
        Spacer(modifier = Modifier.height(Spacing.lg))
        
        // Preferences Section
        // Appearance Section
        SettingsSectionHeader("Appearance")
        
        // Dark Mode Selector
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.xxs),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(modifier = Modifier.padding(Spacing.md)) {
                Text(
                    text = "Dark Mode",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
                
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    com.theblankstate.epmanager.data.repository.DarkModePreference.entries.forEachIndexed { index, pref ->
                        SegmentedButton(
                            selected = themeState.darkModePreference == pref,
                            onClick = { themeViewModel.setDarkModePreference(pref) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = 3)
                        ) {
                            Text(pref.name.lowercase().replaceFirstChar { it.uppercase() })
                        }
                    }
                }
            }
        }
        
        // Theme Selector
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.xxs),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(modifier = Modifier.padding(Spacing.md)) {
                Text(
                    text = "App Theme",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
                
                // Theme Chips
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    FilterChip(
                        selected = themeState.appTheme == com.theblankstate.epmanager.data.repository.AppTheme.MONOCHROME,
                        onClick = { themeViewModel.setAppTheme(com.theblankstate.epmanager.data.repository.AppTheme.MONOCHROME) },
                        label = { Text("Monochrome") }
                    )
                    FilterChip(
                        selected = themeState.appTheme == com.theblankstate.epmanager.data.repository.AppTheme.ROSE,
                        onClick = { themeViewModel.setAppTheme(com.theblankstate.epmanager.data.repository.AppTheme.ROSE) },
                        label = { Text("Rose") }
                    )
                    FilterChip(
                        selected = themeState.appTheme == com.theblankstate.epmanager.data.repository.AppTheme.CUSTOM,
                        onClick = { themeViewModel.setAppTheme(com.theblankstate.epmanager.data.repository.AppTheme.CUSTOM) },
                        label = { Text("Custom") }
                    )
                }
                
                // Edit Colors Button (only visible if Custom is selected)
                if (themeState.appTheme == com.theblankstate.epmanager.data.repository.AppTheme.CUSTOM) {
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    Button(
                        onClick = onNavigateToThemeCustomization,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Filled.Palette,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(Spacing.sm))
                        Text("Customize Colors")
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(Spacing.lg))

        SettingsSectionHeader("Preferences")
        
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
            icon = Icons.Filled.Sms,
            title = "SMS Auto-Detection",
            subtitle = "Bank SMS parsing & custom templates",
            onClick = onNavigateToSmsSettings
        )
        
        SettingsItem(
            icon = Icons.Filled.AttachMoney,
            title = "Currency",
            subtitle = "${CurrencyProvider.getSymbol(currentCurrencyCode)} - ${CurrencyProvider.getCurrency(currentCurrencyCode)?.name ?: currentCurrencyCode}"
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
            icon = Icons.Filled.Sync,
            title = "Sync All Data",
            subtitle = "Manually sync everything to cloud",
            onClick = { authViewModel.backupEverything() }
        )
        
        SettingsItem(
            icon = Icons.Filled.FileDownload,
            title = "Export Data",
            subtitle = "Export as CSV or PDF",
            onClick = onNavigateToExport
        )
        
        SettingsItem(
            icon = Icons.Filled.Delete,
            title = "Clear All Data",
            subtitle = "Delete all transactions (Local only)",
            isDestructive = true,
            onClick = { showClearDataDialog = true } 
        )
        
        if (showClearDataDialog) {
            AlertDialog(
                onDismissRequest = { showClearDataDialog = false },
                title = { Text("Clear All Data") },
                text = { Text("Are you sure? This will delete ALL data from this device. If you haven't synced, data will be lost forever.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            authViewModel.wipeLocalData()
                            showClearDataDialog = false
                        }
                    ) {
                        Text("Clear All", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearDataDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
        
        Spacer(modifier = Modifier.height(Spacing.lg))
        
        // Help Section
        SettingsSectionHeader("Help")
        
        // Legal Section
        SettingsSectionHeader("Legal & Information")

        SettingsItem(
            icon = Icons.Filled.Description,
            title = "Terms & Conditions",
            subtitle = "Read our terms of service",
            onClick = onNavigateToTerms
        )

        SettingsItem(
            icon = Icons.Filled.Code,
            title = "Open Source Licenses",
            subtitle = "Third-party software notices",
            onClick = onNavigateToOpenSource
        )

        val context = androidx.compose.ui.platform.LocalContext.current
        SettingsItem(
            icon = Icons.Filled.Lock,
            title = "Privacy Policy",
            subtitle = "Data usage and protection",
            onClick = {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://oikos.theblankstate.com/privacy"))
                    context.startActivity(intent)
                } catch (e: Exception) {
                    // Fallback or ignore
                }
            }
        )

        SettingsItem(
            icon = Icons.AutoMirrored.Filled.Help,
            title = "Help & Support",
            subtitle = "Contact: theblankstateteam@gmail.com",
            onClick = {
                try {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:theblankstateteam@gmail.com")
                        putExtra(Intent.EXTRA_SUBJECT, "Support Request - EP Manager")
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    // Fallback or ignore
                }
            }
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
    onClick: () -> Unit = {},
    titleContent: (@Composable () -> Unit)? = null
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
                if (titleContent != null) {
                    titleContent()
                } else {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = if (isDestructive) 
                            MaterialTheme.colorScheme.error 
                        else 
                            MaterialTheme.colorScheme.onSurface
                    )
                }
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
