package com.theblankstate.epmanager

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.theblankstate.epmanager.data.sync.SettlementSyncManager
import com.theblankstate.epmanager.navigation.AppNavHost
import com.theblankstate.epmanager.navigation.BottomNavItem
import com.theblankstate.epmanager.navigation.Screen
import com.theblankstate.epmanager.sms.SmsPermissionManager
import com.theblankstate.epmanager.ui.theme.EpmanagerTheme
import com.theblankstate.epmanager.ui.theme.Spacing
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var settlementSyncManager: SettlementSyncManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Start listening for settlement notifications from linked friends
        settlementSyncManager.startListening()
        
        enableEdgeToEdge()
        setContent {
            val themeViewModel: com.theblankstate.epmanager.ui.theme.ThemeViewModel = androidx.hilt.navigation.compose.hiltViewModel()
            val themeState by themeViewModel.themeState.collectAsState()
            
            EpmanagerTheme(
                darkModePreference = themeState.darkModePreference,
                appTheme = themeState.appTheme,
                customPrimary = themeState.customPrimary,
                customSecondary = themeState.customSecondary,
                customTertiary = themeState.customTertiary
            ) {
                ExpenseManagerApp()
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        settlementSyncManager.stopListening()
    }
}

@Composable
fun ExpenseManagerApp() {
    val context = LocalContext.current
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    // Get onboarding status from preferences
    val userPreferencesViewModel: com.theblankstate.epmanager.ui.onboarding.OnboardingViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val hasCompletedOnboarding by userPreferencesViewModel.hasCompletedOnboarding.collectAsState(initial = null)
    
    // Permission states
    var hasSmsPermission by remember { 
        mutableStateOf(SmsPermissionManager.hasAllPermissions(context)) 
    }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var permissionRequestCount by remember { mutableStateOf(0) }
    
    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasSmsPermission = permissions.values.all { it }
        permissionRequestCount++
        
        if (!hasSmsPermission && permissionRequestCount < 2) {
            showPermissionDialog = true
        }
    }
    
    // Check and request permissions on first launch (only if onboarding completed)
    LaunchedEffect(hasCompletedOnboarding) {
        if (hasCompletedOnboarding == true && !hasSmsPermission) {
            // Show dialog first, then request
            showPermissionDialog = true
        }
    }
    
    // Show bottom bar on main sections, but not during onboarding
    val showBottomBar = remember(currentRoute, hasCompletedOnboarding) {
        hasCompletedOnboarding == true && currentRoute in listOf(
            Screen.Home.route,
            Screen.Budget.route,
            Screen.Analytics.route,
            Screen.Settings.route,
            Screen.Transactions.route
        )
    }
    
    // SMS Permission Dialog (only show after onboarding)
    if (showPermissionDialog && !hasSmsPermission && hasCompletedOnboarding == true) {
        SmsPermissionDialog(
            onConfirm = {
                showPermissionDialog = false
                val permissions = mutableListOf(
                    Manifest.permission.RECEIVE_SMS,
                    Manifest.permission.READ_SMS
                )
                // Add notification permission for Android 13+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissions.add(Manifest.permission.POST_NOTIFICATIONS)
                }
                permissionLauncher.launch(permissions.toTypedArray())
            },
            onDismiss = {
                showPermissionDialog = false
            }
        )
    }
    
    // Wait for onboarding status to load before showing UI
    val onboardingCompleted = hasCompletedOnboarding
    if (onboardingCompleted == null) {
        Box(modifier = Modifier.fillMaxSize()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
        return
    }
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                NavigationBar {
                    BottomNavItem.items.forEach { item ->
                        val isSelected = currentRoute == item.route
                        
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                // Special handling for Home to ensure clean navigation
                                if (item.route == Screen.Home.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            inclusive = true
                                        }
                                        launchSingleTop = true
                                    }
                                } else {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.title
                                )
                            },
                            label = {
                                Text(text = item.title)
                            },
                            alwaysShowLabel = true
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        AppNavHost(
            navController = navController,
            hasCompletedOnboarding = onboardingCompleted,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
fun SmsPermissionDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Sms,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                text = "Auto-Track Expenses",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                Text(
                    text = "EP Manager can automatically detect transactions from your bank SMS and add them to your expense list.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(Spacing.xs))
                
                Text(
                    text = "Benefits:",
                    style = MaterialTheme.typography.titleSmall
                )
                
                Text(
                    text = "• No manual entry for most transactions\n• Instant expense tracking\n• Works with all major Indian banks\n• Your SMS data stays on device",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Enable Auto-Tracking")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Maybe Later")
            }
        }
    )
}