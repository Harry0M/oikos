package com.theblankstate.epmanager.ui.notifications

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.theblankstate.epmanager.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: NotificationSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.updatePermissionStatus(isGranted)
        if (isGranted) {
            scope.launch {
                snackbarHostState.showSnackbar("Notifications enabled!")
            }
        }
    }
    
    // Check permission on launch
    LaunchedEffect(Unit) {
        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        viewModel.updatePermissionStatus(hasPermission)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Notifications",
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
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(Spacing.md)
        ) {
            // Permission Card
            if (!uiState.hasPermission) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(Spacing.lg)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.NotificationsOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(Spacing.sm))
                            Text(
                                text = "Notifications Disabled",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        
                        Text(
                            text = "Enable notifications to receive budget alerts, bill reminders, and spending insights.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        
                        Spacer(modifier = Modifier.height(Spacing.md))
                        
                        Button(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }
                        ) {
                            Text("Enable Notifications")
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(Spacing.lg))
            }
            
            // Budget Alerts
            NotificationToggleItem(
                icon = Icons.Filled.Warning,
                title = "Budget Alerts",
                subtitle = "Get notified when approaching or exceeding budget limits",
                checked = uiState.budgetAlerts,
                onCheckedChange = { viewModel.setBudgetAlerts(it) },
                enabled = uiState.hasPermission
            )
            
            Spacer(modifier = Modifier.height(Spacing.md))
            
            // Recurring Reminders
            NotificationToggleItem(
                icon = Icons.Filled.Event,
                title = "Bill Reminders",
                subtitle = "Reminders for upcoming recurring expenses",
                checked = uiState.recurringReminders,
                onCheckedChange = { viewModel.setRecurringReminders(it) },
                enabled = uiState.hasPermission
            )
            
            Spacer(modifier = Modifier.height(Spacing.md))
            
            // Daily Insights
            NotificationToggleItem(
                icon = Icons.Filled.Insights,
                title = "Daily Insights",
                subtitle = "Daily summary of your spending",
                checked = uiState.dailyInsights,
                onCheckedChange = { viewModel.setDailyInsights(it) },
                enabled = uiState.hasPermission
            )
            
            Spacer(modifier = Modifier.height(Spacing.lg))
            
            // Test Notifications
            if (uiState.hasPermission) {
                OutlinedButton(
                    onClick = { viewModel.sendTestNotification() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.NotificationsActive, contentDescription = null)
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Text("Send Test Notification")
                }
            }
        }
    }
}

@Composable
private fun NotificationToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) 
                MaterialTheme.colorScheme.surface 
            else 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
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
                tint = if (enabled && checked) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(Spacing.md))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (enabled) 
                        MaterialTheme.colorScheme.onSurface 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled
            )
        }
    }
}
