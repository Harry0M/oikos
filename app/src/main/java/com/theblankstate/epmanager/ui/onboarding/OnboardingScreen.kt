package com.theblankstate.epmanager.ui.onboarding

import android.Manifest
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.theblankstate.epmanager.data.model.Currency
import com.theblankstate.epmanager.data.model.CurrencyProvider
import com.theblankstate.epmanager.data.repository.AppTheme
import com.theblankstate.epmanager.ui.auth.AuthViewModel
import com.theblankstate.epmanager.ui.auth.GoogleSignInHelper
import com.theblankstate.epmanager.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class OnboardingStep {
    WELCOME,
    CURRENCY,
    THEME,
    PERMISSIONS,
    COMPLETE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onOnboardingComplete: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel(),
    onboardingViewModel: OnboardingViewModel = hiltViewModel(),
    themeViewModel: com.theblankstate.epmanager.ui.theme.ThemeViewModel = hiltViewModel()
) {
    val authState by authViewModel.uiState.collectAsState()
    val onboardingState by onboardingViewModel.uiState.collectAsState()
    
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var currentStep by remember { mutableStateOf(OnboardingStep.WELCOME) }
    var isGoogleSignInLoading by remember { mutableStateOf(false) }
    var selectedCurrency by remember { mutableStateOf<Currency?>(null) }
    var selectedTheme by remember { mutableStateOf(AppTheme.ROSE) }
    var currencySearchQuery by remember { mutableStateOf("") }
    
    // Track if user explicitly initiated sign-in during this onboarding session
    // This is set to true ONLY when user clicks the sign-in button
    var userInitiatedSignIn by remember { mutableStateOf(false) }
    
    // Location permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        currentStep = OnboardingStep.COMPLETE
    }
    
    // Handle successful login - check for existing currency automatically
    // Only trigger if user explicitly initiated sign-in (not persisted session)
    LaunchedEffect(authState.isLoggedIn, userInitiatedSignIn, currentStep) {
        if (authState.isLoggedIn && userInitiatedSignIn && currentStep == OnboardingStep.WELCOME) {
            // Await the result before deciding where to go
            val result = onboardingViewModel.checkRemoteCurrency()
            if (result.isSuccess && result.getOrDefault(false)) {
                // Currency was found and restored - skip to theme
                currentStep = OnboardingStep.THEME
            } else {
                // No currency found or error - show currency selection
                currentStep = OnboardingStep.CURRENCY
            }
        }
    }
    
    // Handle errors
    LaunchedEffect(authState.error) {
        authState.error?.let {
            snackbarHostState.showSnackbar(it)
            authViewModel.clearError()
        }
    }
    
    val handleGoogleSignIn: () -> Unit = {
        coroutineScope.launch {
            isGoogleSignInLoading = true
            userInitiatedSignIn = true  // Mark that user explicitly initiated sign-in
            try {
                val activity = context as? Activity
                if (activity != null) {
                    val googleSignInHelper = GoogleSignInHelper(activity)
                    googleSignInHelper.signIn()
                        .onSuccess { idToken ->
                            authViewModel.signInWithGoogle(idToken)
                        }
                        .onFailure { e ->
                            snackbarHostState.showSnackbar(e.message ?: "Google Sign-In failed")
                        }
                }
            } catch (e: Exception) {
                snackbarHostState.showSnackbar(e.message ?: "Google Sign-In error")
            } finally {
                isGoogleSignInLoading = false
            }
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
        ) {
            // Animated content with smooth transitions
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    slideInHorizontally(
                        animationSpec = tween(400, easing = FastOutSlowInEasing),
                        initialOffsetX = { it }
                    ) + fadeIn(tween(300)) togetherWith 
                    slideOutHorizontally(
                        animationSpec = tween(400, easing = FastOutSlowInEasing),
                        targetOffsetX = { -it }
                    ) + fadeOut(tween(200))
                },
                label = "onboarding_step"
            ) { step ->
                when (step) {
                    OnboardingStep.WELCOME -> WelcomeStep(
                        isLoading = authState.isLoading || isGoogleSignInLoading,
                        onGoogleSignIn = handleGoogleSignIn,
                        onSkip = { currentStep = OnboardingStep.CURRENCY },
                        onAcceptTerms = {
                            coroutineScope.launch {
                                onboardingViewModel.acceptTerms()
                            }
                        }
                    )
                    
                    OnboardingStep.CURRENCY -> {
                        var isRestoring by remember { mutableStateOf(false) }
                        
                        CurrencyStep(
                            searchQuery = currencySearchQuery,
                            onSearchQueryChange = { currencySearchQuery = it },
                            selectedCurrency = selectedCurrency,
                            onCurrencySelected = { selectedCurrency = it },
                            onNext = {
                                if (selectedCurrency != null) {
                                    currentStep = OnboardingStep.THEME
                                }
                            },
                            onRestore = {
                                coroutineScope.launch {
                                    isRestoring = true
                                    val result = onboardingViewModel.checkRemoteCurrency()
                                    isRestoring = false
                                    if (result.isSuccess) {
                                        if (result.getOrDefault(false)) {
                                            snackbarHostState.showSnackbar("Currency restored!")
                                            currentStep = OnboardingStep.THEME
                                        } else {
                                            snackbarHostState.showSnackbar("No backup found", duration = SnackbarDuration.Short)
                                        }
                                    } else {
                                        snackbarHostState.showSnackbar("Error: ${result.exceptionOrNull()?.message}")
                                    }
                                }
                            },
                            isRestoring = isRestoring
                        )
                    }
                    
                    OnboardingStep.THEME -> ThemeStep(
                        selectedTheme = selectedTheme,
                        onThemeSelected = { selectedTheme = it },
                        themeViewModel = themeViewModel,
                        onNext = {
                            currentStep = OnboardingStep.PERMISSIONS
                        }
                    )
                    
                    OnboardingStep.PERMISSIONS -> PermissionsStep(
                        onRequestPermissions = {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        },
                        onSkip = {
                            currentStep = OnboardingStep.COMPLETE
                        }
                    )
                    
                    OnboardingStep.COMPLETE -> CompleteStep(
                        selectedCurrency = selectedCurrency,
                        hasCurrencyBeenSet = onboardingState.hasCurrencyBeenSet,
                        onComplete = {
                            coroutineScope.launch {
                                selectedCurrency?.let { 
                                    onboardingViewModel.setCurrency(it.code)
                                }
                                onboardingViewModel.setTheme(selectedTheme)
                                onboardingViewModel.completeOnboarding()
                                onOnboardingComplete()
                            }
                        }
                    )
                }
            }
            
            // Animated progress indicator
            AnimatedVisibility(
                visible = currentStep != OnboardingStep.WELCOME,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = Spacing.xl)
            ) {
                val steps = OnboardingStep.entries.filter { it != OnboardingStep.WELCOME }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    steps.forEach { step ->
                        val isActive = currentStep.ordinal >= step.ordinal
                        val isCurrent = currentStep == step
                        
                        val animatedSize by animateDpAsState(
                            targetValue = if (isCurrent) 12.dp else 8.dp,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = "dot_size"
                        )
                        
                        val animatedColor by animateColorAsState(
                            targetValue = if (isActive) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.outlineVariant,
                            animationSpec = tween(300),
                            label = "dot_color"
                        )
                        
                        Box(
                            modifier = Modifier
                                .size(animatedSize)
                                .clip(CircleShape)
                                .background(animatedColor)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WelcomeStep(
    isLoading: Boolean,
    onGoogleSignIn: () -> Unit,
    onSkip: () -> Unit,
    onAcceptTerms: () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }
    var termsAccepted by remember { mutableStateOf(false) }
    var showTermsSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    LaunchedEffect(Unit) { isVisible = true }
    
    val iconScale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "icon_scale"
    )
    
    val contentAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(600, delayMillis = 200),
        label = "content_alpha"
    )
    
    // Terms Bottom Sheet
    if (showTermsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showTermsSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            TermsBottomSheetContent(
                onAccept = {
                    termsAccepted = true
                    onAcceptTerms()
                    showTermsSheet = false
                },
                onDismiss = { showTermsSheet = false }
            )
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.xl)
            .padding(bottom = Spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Animated logo with gradient
        Box(
            modifier = Modifier
                .size(120.dp)
                .scale(iconScale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.AccountBalanceWallet,
                contentDescription = null,
                modifier = Modifier.size(60.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        
        Spacer(modifier = Modifier.height(Spacing.xxl))
        
        Text(
            text = "Welcome to",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.graphicsLayer { alpha = contentAlpha }
        )
        
        Text(
            text = "Oikos",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.graphicsLayer { alpha = contentAlpha }
        )
        
        Spacer(modifier = Modifier.height(Spacing.xs))
        
        Text(
            text = "Auto SMS tracking • Recurring expenses\nSplit with friends • All Free",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.graphicsLayer { alpha = contentAlpha }
        )
        
        Spacer(modifier = Modifier.height(Spacing.sm))
        
        Text(
            text = "Track expenses effortlessly.\nSync across devices.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.graphicsLayer { alpha = contentAlpha }
        )
        
        Spacer(modifier = Modifier.height(Spacing.xl))
        
        // Terms checkbox row
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { alpha = contentAlpha },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = termsAccepted,
                    onCheckedChange = { 
                        if (it) {
                            showTermsSheet = true
                        } else {
                            termsAccepted = false
                        }
                    }
                )
                Spacer(modifier = Modifier.width(Spacing.xs))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "I accept the Terms & Conditions",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Tap to read and accept",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = { showTermsSheet = true }) {
                    Text("Read")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(Spacing.lg))
        
        // Google Sign In Button
        Button(
            onClick = onGoogleSignIn,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .graphicsLayer { alpha = contentAlpha },
            enabled = !isLoading && termsAccepted,
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    Icons.Filled.Person, 
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text(
                    text = "Continue with Google",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        
        Spacer(modifier = Modifier.height(Spacing.md))
        
        // Skip / Try without account
        TextButton(
            onClick = onSkip,
            modifier = Modifier.graphicsLayer { alpha = contentAlpha },
            enabled = termsAccepted
        ) {
            Text(
                text = "Try without account",
                color = if (termsAccepted) MaterialTheme.colorScheme.onSurfaceVariant 
                       else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
        
        if (!termsAccepted) {
            Text(
                text = "Please accept terms to continue",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                modifier = Modifier.graphicsLayer { alpha = contentAlpha }
            )
        }
    }
}

@Composable
private fun TermsBottomSheetContent(
    onAccept: () -> Unit,
    onDismiss: () -> Unit
) {
    val scrollState = rememberScrollState()
    var hasScrolledToBottom by remember { mutableStateOf(false) }
    var contentMeasured by remember { mutableStateOf(false) }
    
    // Check if scrolled to bottom or if content doesn't need scrolling
    LaunchedEffect(scrollState.value, scrollState.maxValue, contentMeasured) {
        // Give a tiny delay to let layout settle
        kotlinx.coroutines.delay(100)
        contentMeasured = true
        
        if (scrollState.maxValue == 0) {
            // Content fits without scrolling - enable immediately
            hasScrolledToBottom = true
        } else if (scrollState.value >= scrollState.maxValue - 50) {
            hasScrolledToBottom = true
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.85f)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Terms & Conditions",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Filled.Close, contentDescription = "Close")
            }
        }
        
        HorizontalDivider()
        
        // Scrollable content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            // App Header
            Text(
                text = "Oikos v1.0",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = "Last updated: December 2024",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            TermsSection("1. Acceptance of Terms", """
By using Oikos ("the App"), you agree to be bound by these Terms and Conditions. If you do not agree to these terms, please do not use the App.

Your acceptance of these terms is recorded with a timestamp for compliance purposes. You must accept these terms before using the app.
            """.trimIndent())
            
            TermsSection("2. Privacy & Data Storage", """
LOCAL-FIRST STORAGE
All your financial data including transactions, budgets, recurring expenses, savings goals, and account balances are stored locally on your device. We do not have access to your personal financial information.

NO DATA SELLING
We do not sell, share, rent, or monetize your personal data in any way. Your financial information is yours alone.

CLOUD SYNC (Optional)
If you choose to sign in with Google, your data may be synced to Google Firebase for backup purposes. This enables:
• Cross-device synchronization
• Data recovery after reinstallation
• Sharing split expenses with friends

This is entirely optional and under your control. You can use the app without signing in.

ANONYMOUS ANALYTICS
We may collect anonymous, non-personally-identifiable usage analytics to improve the app experience. No financial data is ever collected or transmitted.
            """.trimIndent())
            
            TermsSection("3. Security Notice", """
DEVICE SECURITY
The security of your data depends on your device's security. We strongly recommend:
• Using device lock (PIN, pattern, fingerprint, or face unlock)
• Keeping your device software up to date
• Only installing apps from trusted sources

⚠️ THIRD-PARTY APPS WARNING
Malicious applications installed on your device may potentially access data stored by Oikos. We are not responsible for data breaches caused by:
• Third-party malware or spyware
• Compromised or rooted devices
• Unauthorized physical access to your device

ENCRYPTION
Data stored locally is protected by Android's security measures. Cloud-synced data is encrypted in transit using industry-standard TLS.
            """.trimIndent())
            
            TermsSection("4. SMS Permissions", """
AUTO-TRACKING FEATURE
Oikos can optionally read your SMS messages to automatically detect and record transactions from bank notifications. This is a convenience feature.

LOCAL PROCESSING ONLY
SMS messages are processed entirely on your device. Message content is:
• Never sent to any server
• Never stored beyond parsing
• Never shared with any third party

MANUAL ALTERNATIVE
You can use the app fully without granting SMS permissions. Manual expense entry is always available.

SUPPORTED BANKS
The SMS parsing feature works with major Indian banks including HDFC, SBI, ICICI, Axis, Kotak, and many others.
            """.trimIndent())
            
            TermsSection("5. Disclaimers & Limitations", """
NO WARRANTY
The App is provided "as is" and "as available" without any warranties of any kind, either express or implied, including but not limited to merchantability, fitness for a particular purpose, or non-infringement.

DATA LOSS DISCLAIMER
We are NOT responsible for any loss of data due to:
• Device failure, damage, or theft
• App uninstallation (data is lost if not synced)
• Operating system updates or factory reset
• Cyberattacks, hacking, or malware
• User error or accidental deletion
• Cloud service outages
• Any other circumstances beyond our control

FINANCIAL ADVICE
Oikos is a tracking and management tool only. It does NOT provide:
• Financial advice or recommendations
• Investment guidance
• Tax advice or calculations
• Legal counsel

Please consult qualified professionals for such services.

ACCURACY
While we strive for accuracy in transaction detection and calculations, we are not responsible for errors in:
• Automatic SMS transaction parsing
• Currency conversions
• Budget calculations
• Split expense calculations
            """.trimIndent())
            
            TermsSection("6. Open Source License", """
APACHE LICENSE 2.0
Oikos is open-source software licensed under the Apache License, Version 2.0.

You may obtain a copy of the License at:
https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.

THIRD-PARTY LIBRARIES
The app uses various open-source libraries, each with their own licenses. A full list of dependencies and their licenses is available in the app's About section.

CONTRIBUTIONS
Contributions to the project are welcome under the same Apache 2.0 license terms.
            """.trimIndent())
            
            TermsSection("7. User Responsibilities", """
By using Oikos, you agree to:
• Provide accurate information when creating accounts
• Keep your login credentials secure
• Not use the app for any illegal purposes
• Not attempt to reverse engineer or tamper with the app
• Backup your data regularly if using without cloud sync
            """.trimIndent())
            
            TermsSection("8. Changes to Terms", """
We reserve the right to modify these Terms at any time. When we make changes:
• Significant changes will be notified within the app
• The version number will be updated
• Your continued use after changes constitutes acceptance

We recommend reviewing these terms periodically for updates.
            """.trimIndent())
            
            TermsSection("9. Contact & Support", """
For support, questions, bug reports, feature requests, or privacy inquiries, please contact us:

📧 Email: theblankstateteam@gmail.com

We aim to respond to all queries within 48-72 hours.

For security vulnerabilities, please email with "SECURITY" in the subject line for priority handling.
            """.trimIndent())
            
            Spacer(modifier = Modifier.height(Spacing.xxl))
        }
        
        HorizontalDivider()
        
        // Bottom buttons
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md)
        ) {
            if (!hasScrolledToBottom) {
                Text(
                    text = "↓ Scroll to read all terms",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = Spacing.xs)
                )
            }
            
            Button(
                onClick = onAccept,
                modifier = Modifier.fillMaxWidth(),
                enabled = hasScrolledToBottom
            ) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text("I Accept")
            }
        }
    }
}

@Composable
private fun TermsSection(title: String, content: String) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(Spacing.xs))
        Text(
            text = content,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrencyStep(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedCurrency: Currency?,
    onCurrencySelected: (Currency) -> Unit,
    onNext: () -> Unit,
    onRestore: () -> Unit,
    isRestoring: Boolean = false
) {
    val filteredCurrencies = remember(searchQuery) {
        if (searchQuery.isBlank()) CurrencyProvider.currencies
        else CurrencyProvider.search(searchQuery)
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.lg)
            .padding(top = 80.dp, bottom = Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "💱",
            fontSize = 48.sp
        )
        
        Spacer(modifier = Modifier.height(Spacing.md))
        
        Text(
            text = "Select Your Currency",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        // Manual Restore Option
        TextButton(
            onClick = onRestore,
            enabled = !isRestoring
        ) {
            if (isRestoring) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text("Checking backup...")
            } else {
                Icon(
                    Icons.Filled.CloudDownload,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text("Restore from Cloud")
            }
        }
        
        Spacer(modifier = Modifier.height(Spacing.xs))
        
        Surface(
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = "⚠️ This cannot be changed later",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs)
            )
        }
        
        Spacer(modifier = Modifier.height(Spacing.lg))
        
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Search by name, code, or symbol...") },
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )
        )
        
        Spacer(modifier = Modifier.height(Spacing.md))
        
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            items(filteredCurrencies, key = { it.code }) { currency ->
                CurrencyItem(
                    currency = currency,
                    isSelected = selectedCurrency == currency,
                    onClick = { onCurrencySelected(currency) }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(Spacing.md))
        
        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = selectedCurrency != null,
            shape = RoundedCornerShape(28.dp)
        ) {
            Text("Continue", fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.width(Spacing.xs))
            Icon(Icons.Filled.ArrowForward, null, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun CurrencyItem(
    currency: Currency,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val animatedElevation by animateDpAsState(
        targetValue = if (isSelected) 4.dp else 0.dp,
        animationSpec = tween(200),
        label = "elevation"
    )
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) 
            MaterialTheme.colorScheme.primaryContainer 
        else 
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shadowElevation = animatedElevation
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = currency.flag,
                fontSize = 28.sp
            )
            Spacer(modifier = Modifier.width(Spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = currency.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                )
                Text(
                    text = "${currency.code} • ${currency.symbol}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            AnimatedVisibility(
                visible = isSelected,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun ThemeStep(
    selectedTheme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit,
    themeViewModel: com.theblankstate.epmanager.ui.theme.ThemeViewModel,
    onNext: () -> Unit
) {
    // Predefined custom color options with ARGB colors for storage
    data class CustomColorOption(
        val name: String,
        val primary: Color,
        val light: Color,
        val primaryArgb: Int,
        val secondaryArgb: Int,
        val tertiaryArgb: Int
    )
    
    val customColors = remember {
        listOf(
            CustomColorOption("Blue", Blue, BlueLight, Blue.toArgb(), BlueLight.toArgb(), BlueDark.toArgb()),
            CustomColorOption("Purple", Purple, PurpleLight, Purple.toArgb(), PurpleLight.toArgb(), PurpleDark.toArgb()),
            CustomColorOption("Teal", Teal, TealLight, Teal.toArgb(), TealLight.toArgb(), TealDark.toArgb()),
            CustomColorOption("Amber", Amber, AmberLight, Amber.toArgb(), AmberLight.toArgb(), AmberDark.toArgb()),
            CustomColorOption("Green", Green, GreenLight, Green.toArgb(), GreenLight.toArgb(), GreenDark.toArgb())
        )
    }
    
    var selectedCustomColor by remember { mutableStateOf(customColors[0]) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.lg)
            .padding(top = 80.dp, bottom = Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "🎨",
            fontSize = 48.sp
        )
        
        Spacer(modifier = Modifier.height(Spacing.md))
        
        Text(
            text = "Choose Your Style",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(Spacing.xs))
        
        Text(
            text = "You can change this anytime in Settings",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(Spacing.lg))
        
        // Recommended Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(Spacing.xs))
            Text(
                text = "Recommended",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        Spacer(modifier = Modifier.height(Spacing.sm))
        
        // Rose Theme - Recommended
        ThemeOption(
            name = "Rose",
            description = "Warm & elegant • Best for daily use",
            colors = listOf(Rose, RoseLight),
            isSelected = selectedTheme == AppTheme.ROSE,
            isRecommended = true,
            onClick = { 
                onThemeSelected(AppTheme.ROSE)
                themeViewModel.setAppTheme(AppTheme.ROSE)
            }
        )
        
        Spacer(modifier = Modifier.height(Spacing.sm))
        
        // Monochrome Theme - Recommended
        ThemeOption(
            name = "Monochrome",
            description = "Clean & minimal • Easy on the eyes",
            colors = listOf(Color(0xFF374151), Color(0xFF6B7280)),
            isSelected = selectedTheme == AppTheme.MONOCHROME,
            isRecommended = true,
            onClick = { 
                onThemeSelected(AppTheme.MONOCHROME)
                themeViewModel.setAppTheme(AppTheme.MONOCHROME)
            }
        )
        
        Spacer(modifier = Modifier.height(Spacing.lg))
        
        // More Options Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Palette,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(Spacing.xs))
            Text(
                text = "More Colors",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.height(Spacing.sm))
        
        // Color picker grid
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (selectedTheme == AppTheme.CUSTOM) 
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(Spacing.md)
            ) {
                Text(
                    text = "Pick a color theme",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(Spacing.md))
                
                // Color circles in a row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    customColors.forEach { colorOption ->
                        val isColorSelected = selectedTheme == AppTheme.CUSTOM && 
                                            selectedCustomColor.name == colorOption.name
                        
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable {
                                selectedCustomColor = colorOption
                                onThemeSelected(AppTheme.CUSTOM)
                                // Apply custom colors via ThemeViewModel
                                themeViewModel.setAppTheme(AppTheme.CUSTOM)
                                themeViewModel.setCustomColors(
                                    colorOption.primaryArgb,
                                    colorOption.secondaryArgb,
                                    colorOption.tertiaryArgb
                                )
                            }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(colorOption.primary, colorOption.light)
                                        )
                                    )
                                    .then(
                                        if (isColorSelected) Modifier.background(
                                            color = Color.Transparent,
                                            shape = CircleShape
                                        ) else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isColorSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(CircleShape)
                                            .background(Color.Transparent)
                                    ) {
                                        Icon(
                                            Icons.Filled.Check,
                                            contentDescription = "Selected",
                                            tint = Color.White,
                                            modifier = Modifier
                                                .size(24.dp)
                                                .align(Alignment.Center)
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(Spacing.xs))
                            
                            Text(
                                text = colorOption.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isColorSelected) 
                                    colorOption.primary 
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (isColorSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp)
        ) {
            Text("Continue", fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.width(Spacing.xs))
            Icon(Icons.Filled.ArrowForward, null, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun ThemeOption(
    name: String,
    description: String,
    colors: List<Color>,
    isSelected: Boolean,
    isRecommended: Boolean,
    onClick: () -> Unit
) {
    val animatedElevation by animateDpAsState(
        targetValue = if (isSelected) 8.dp else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "theme_elevation"
    )
    
    val animatedScale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "theme_scale"
    )
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .scale(animatedScale)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) 
            MaterialTheme.colorScheme.primaryContainer 
        else 
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shadowElevation = animatedElevation
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Color preview
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(colors)
                    )
            )
            
            Spacer(modifier = Modifier.width(Spacing.md))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (isRecommended) {
                        Spacer(modifier = Modifier.width(Spacing.sm))
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "RECOMMENDED",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            AnimatedVisibility(
                visible = isSelected,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
private fun PermissionsStep(
    onRequestPermissions: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.lg)
            .padding(top = 80.dp, bottom = Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.LocationOn,
                contentDescription = null,
                modifier = Modifier.size(50.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        
        Spacer(modifier = Modifier.height(Spacing.xl))
        
        Text(
            text = "Enable Location",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(Spacing.md))
        
        Text(
            text = "Automatically tag where you make transactions to understand your spending patterns better.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = Spacing.lg)
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        Button(
            onClick = onRequestPermissions,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp)
        ) {
            Icon(Icons.Filled.LocationOn, null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(Spacing.sm))
            Text("Enable Location", fontWeight = FontWeight.SemiBold)
        }
        
        Spacer(modifier = Modifier.height(Spacing.md))
        
        TextButton(
            onClick = onSkip,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Maybe later", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CompleteStep(
    selectedCurrency: Currency?,
    hasCurrencyBeenSet: Boolean,
    onComplete: () -> Unit
) {
    // Animated entrance
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }
    
    val iconScale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "complete_icon"
    )
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.lg)
            .padding(top = 80.dp, bottom = Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .scale(iconScale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(60.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        
        Spacer(modifier = Modifier.height(Spacing.xl))
        
        Text(
            text = if (hasCurrencyBeenSet) "Welcome Back!" else "You're All Set!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(Spacing.sm))
        
        Text(
            text = if (hasCurrencyBeenSet) 
                "Your data is ready. Let's continue tracking!"
            else 
                "Start tracking your expenses now",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        // Currency warning only for new users
        if (!hasCurrencyBeenSet && selectedCurrency != null) {
            Spacer(modifier = Modifier.height(Spacing.xl))
            
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(Spacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = selectedCurrency.flag, fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Column {
                        Text(
                            text = "Currency: ${selectedCurrency.name}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Symbol: ${selectedCurrency.symbol}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(Spacing.huge))
        
        Button(
            onClick = onComplete,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp)
        ) {
            Text("Get Started", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Spacer(modifier = Modifier.width(Spacing.sm))
            Icon(Icons.Filled.ArrowForward, null, modifier = Modifier.size(20.dp))
        }
    }
}
