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
    onboardingViewModel: OnboardingViewModel = hiltViewModel()
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
                        onSkip = { currentStep = OnboardingStep.CURRENCY }
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

@Composable
private fun WelcomeStep(
    isLoading: Boolean,
    onGoogleSignIn: () -> Unit,
    onSkip: () -> Unit
) {
    // Entrance animations
    var isVisible by remember { mutableStateOf(false) }
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
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.xl)
            .padding(bottom = Spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Animated logo
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
            text = "EPManager",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.graphicsLayer { alpha = contentAlpha }
        )
        
        Spacer(modifier = Modifier.height(Spacing.md))
        
        Text(
            text = "Track expenses effortlessly.\nSync across devices.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.graphicsLayer { alpha = contentAlpha }
        )
        
        Spacer(modifier = Modifier.height(Spacing.huge))
        
        // Google Sign In Button
        Button(
            onClick = onGoogleSignIn,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .graphicsLayer { alpha = contentAlpha },
            enabled = !isLoading,
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
            modifier = Modifier.graphicsLayer { alpha = contentAlpha }
        ) {
            Text(
                text = "Try without account",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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
    onNext: () -> Unit
) {
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
        
        Spacer(modifier = Modifier.height(Spacing.xxl))
        
        // Rose Theme
        ThemeOption(
            name = "Rose",
            description = "Warm & elegant",
            colors = listOf(Rose, RoseLight),
            isSelected = selectedTheme == AppTheme.ROSE,
            isRecommended = true,
            onClick = { onThemeSelected(AppTheme.ROSE) }
        )
        
        Spacer(modifier = Modifier.height(Spacing.md))
        
        // Monochrome Theme
        ThemeOption(
            name = "Monochrome",
            description = "Clean & minimal",
            colors = listOf(Color(0xFF374151), Color(0xFF6B7280)),
            isSelected = selectedTheme == AppTheme.MONOCHROME,
            isRecommended = false,
            onClick = { onThemeSelected(AppTheme.MONOCHROME) }
        )
        
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
