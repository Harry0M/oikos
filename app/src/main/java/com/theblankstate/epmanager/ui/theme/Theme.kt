package com.theblankstate.epmanager.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

// ==========================================
// LIGHT COLOR SCHEME
// ==========================================
private val LightColorScheme = lightColorScheme(
    // Primary - Rose
    primary = Rose,
    onPrimary = White,
    primaryContainer = RoseLight,
    onPrimaryContainer = RoseDark,
    
    // Secondary - Teal (for income)
    secondary = Teal,
    onSecondary = White,
    secondaryContainer = TealLight,
    onSecondaryContainer = TealDark,
    
    // Tertiary - Purple
    tertiary = Purple,
    onTertiary = White,
    tertiaryContainer = PurpleLight,
    onTertiaryContainer = PurpleDark,
    
    // Background & Surface
    background = Gray50,
    onBackground = Black,
    surface = White,
    onSurface = Black,
    surfaceVariant = Gray100,
    onSurfaceVariant = Gray600,
    
    // Surface containers
    surfaceContainer = White,
    surfaceContainerHigh = Gray100,
    surfaceContainerHighest = Gray200,
    surfaceContainerLow = Gray50,
    surfaceContainerLowest = White,
    
    // Outline
    outline = Gray300,
    outlineVariant = Gray200,
    
    // Inverse
    inverseSurface = Gray900,
    inverseOnSurface = White,
    inversePrimary = RoseLight,
    
    // Error
    error = Error,
    onError = White,
    errorContainer = ErrorLight,
    onErrorContainer = ErrorDark
)

// ==========================================
// DARK COLOR SCHEME
// ==========================================
private val DarkColorScheme = darkColorScheme(
    // Primary - Rose
    primary = Rose,
    onPrimary = White,
    primaryContainer = RoseDark,
    onPrimaryContainer = RoseLight,
    
    // Secondary - Teal (for income)
    secondary = Teal,
    onSecondary = Black,
    secondaryContainer = TealDark,
    onSecondaryContainer = TealLight,
    
    // Tertiary - Purple
    tertiary = Purple,
    onTertiary = White,
    tertiaryContainer = PurpleDark,
    onTertiaryContainer = PurpleLight,
    
    // Background & Surface
    background = Black,
    onBackground = White,
    surface = Gray900,
    onSurface = White,
    surfaceVariant = Gray800,
    onSurfaceVariant = Gray300,
    
    // Surface containers
    surfaceContainer = Gray900,
    surfaceContainerHigh = Gray800,
    surfaceContainerHighest = Gray700,
    surfaceContainerLow = Gray900,
    surfaceContainerLowest = Black,
    
    // Outline
    outline = Gray600,
    outlineVariant = Gray700,
    
    // Inverse
    inverseSurface = Gray100,
    inverseOnSurface = Black,
    inversePrimary = RoseDark,
    
    // Error
    error = Error,
    onError = Black,
    errorContainer = ErrorDark,
    onErrorContainer = ErrorLight
)

// ==========================================
// SPACING
// ==========================================
object Spacing {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 20.dp
    val xl = 24.dp
    val xxl = 32.dp
    val xxxl = 40.dp
    val huge = 48.dp
    val section = 64.dp
    val page = 80.dp
}

// ==========================================
// ELEVATION
// ==========================================
object Elevation {
    val none = 0.dp
    val xs = 1.dp
    val sm = 2.dp
    val md = 4.dp
    val lg = 8.dp
    val xl = 16.dp
    val xxl = 24.dp
}

// ==========================================
// MONOCHROME SCHEMES
// ==========================================
private val MonochromeLightScheme = lightColorScheme(
    primary = MonoBlack,
    onPrimary = MonoWhite,
    primaryContainer = MonoGrayDark,
    onPrimaryContainer = MonoWhite,
    secondary = MonoGrayDark,
    onSecondary = MonoWhite,
    secondaryContainer = MonoGrayLight,
    onSecondaryContainer = MonoBlack,
    tertiary = MonoGrayDark,
    onTertiary = MonoWhite,
    background = White,
    onBackground = Black,
    surface = White,
    onSurface = Black,
    surfaceVariant = Gray100,
    onSurfaceVariant = Black,
    outline = Gray400,
    error = MonoBlack,
    onError = MonoWhite,
    errorContainer = MonoGrayLight,
    onErrorContainer = MonoBlack
)

private val MonochromeDarkScheme = darkColorScheme(
    primary = MonoWhite,
    onPrimary = MonoBlack,
    primaryContainer = MonoGrayLight,
    onPrimaryContainer = MonoBlack,
    secondary = MonoGrayLight,
    onSecondary = MonoBlack,
    secondaryContainer = MonoGrayDark,
    onSecondaryContainer = MonoWhite,
    tertiary = MonoGrayLight,
    onTertiary = MonoBlack,
    background = Black,
    onBackground = White,
    surface = Black,
    onSurface = White,
    surfaceVariant = Gray900,
    onSurfaceVariant = White,
    outline = Gray600,
    error = MonoWhite,
    onError = MonoBlack,
    errorContainer = MonoGrayDark,
    onErrorContainer = MonoWhite
)

// ==========================================
// THEME COMPOSABLE
// ==========================================
@Composable
fun EpmanagerTheme(
    darkModePreference: com.theblankstate.epmanager.data.repository.DarkModePreference = com.theblankstate.epmanager.data.repository.DarkModePreference.SYSTEM,
    appTheme: com.theblankstate.epmanager.data.repository.AppTheme = com.theblankstate.epmanager.data.repository.AppTheme.MONOCHROME,
    customPrimary: Int = 0xFF2196F3.toInt(),
    customSecondary: Int = 0xFF03DAC6.toInt(),
    customTertiary: Int = 0xFFFFC107.toInt(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = when(darkModePreference) {
        com.theblankstate.epmanager.data.repository.DarkModePreference.SYSTEM -> isSystemInDarkTheme()
        com.theblankstate.epmanager.data.repository.DarkModePreference.DARK -> true
        com.theblankstate.epmanager.data.repository.DarkModePreference.LIGHT -> false
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) 
            else dynamicLightColorScheme(context)
        }
        appTheme == com.theblankstate.epmanager.data.repository.AppTheme.CUSTOM -> {
            // Generate scheme from custom colors
            // Note: In a real app we'd use utilities to generate a full tonal palette
            // For now, we'll map the picked colors to main slots and use defaults for containers
            if (darkTheme) {
                darkColorScheme(
                    primary = Color(customPrimary),
                    onPrimary = Color.White, // Simplified
                    secondary = Color(customSecondary),
                    onSecondary = Color.Black,
                    tertiary = Color(customTertiary)
                    // ... other slots would ideally be derived
                )
            } else {
                lightColorScheme(
                    primary = Color(customPrimary),
                    onPrimary = Color.White,
                    secondary = Color(customSecondary),
                    onSecondary = Color.Black,
                    tertiary = Color(customTertiary)
                )
            }
        }
        darkTheme -> when(appTheme) {
            com.theblankstate.epmanager.data.repository.AppTheme.MONOCHROME -> MonochromeDarkScheme
            else -> DarkColorScheme // Default to Rose (original Dark)
        }
        else -> when(appTheme) {
            com.theblankstate.epmanager.data.repository.AppTheme.MONOCHROME -> MonochromeLightScheme
            else -> LightColorScheme // Default to Rose (original Light)
        }
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Make status bar transparent for edge-to-edge
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}