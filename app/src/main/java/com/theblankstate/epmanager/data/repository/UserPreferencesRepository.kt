package com.theblankstate.epmanager.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import com.theblankstate.epmanager.data.model.CurrencyProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class AppTheme {
    MONOCHROME,
    ROSE,
    BLUE,
    SYSTEM,
    CUSTOM
}

enum class DarkModePreference {
    SYSTEM,
    LIGHT,
    DARK
}

@Singleton
class UserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private object PreferencesKeys {
        val DARK_MODE_PREF = stringPreferencesKey("dark_mode_preference")
        val APP_THEME = stringPreferencesKey("app_theme")
        
        // Custom Colors (ARGB Ints)
        val CUSTOM_PRIMARY = intPreferencesKey("custom_primary")
        val CUSTOM_SECONDARY = intPreferencesKey("custom_secondary")
        val CUSTOM_TERTIARY = intPreferencesKey("custom_tertiary")
        
        // Onboarding
        val HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")
        val SELECTED_CURRENCY_CODE = stringPreferencesKey("selected_currency_code")
        val HAS_CURRENCY_BEEN_SET = booleanPreferencesKey("has_currency_been_set")
    }

    // Default Custom Colors (e.g., a nice Blue as fallback)
    private val defaultPrimary = 0xFF2196F3.toInt()
    private val defaultSecondary = 0xFF03DAC6.toInt()
    private val defaultTertiary = 0xFFFFC107.toInt()

    val darkModePreference: Flow<DarkModePreference> = dataStore.data
        .map { preferences ->
            val prefName = preferences[PreferencesKeys.DARK_MODE_PREF] ?: DarkModePreference.SYSTEM.name
            try {
                DarkModePreference.valueOf(prefName)
            } catch (e: Exception) {
                DarkModePreference.SYSTEM
            }
        }

    // Default theme is now ROSE
    val appTheme: Flow<AppTheme> = dataStore.data
        .map { preferences ->
            val themeName = preferences[PreferencesKeys.APP_THEME] ?: AppTheme.ROSE.name
            try {
                AppTheme.valueOf(themeName)
            } catch (e: Exception) {
                AppTheme.ROSE
            }
        }

    val customThemeColors: Flow<Triple<Int, Int, Int>> = dataStore.data
        .map { preferences ->
            Triple(
                preferences[PreferencesKeys.CUSTOM_PRIMARY] ?: defaultPrimary,
                preferences[PreferencesKeys.CUSTOM_SECONDARY] ?: defaultSecondary,
                preferences[PreferencesKeys.CUSTOM_TERTIARY] ?: defaultTertiary
            )
        }

    // Onboarding status
    val hasCompletedOnboarding: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.HAS_COMPLETED_ONBOARDING] ?: false
        }

    // Selected currency code (default INR for backwards compatibility)
    val selectedCurrencyCode: Flow<String> = dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.SELECTED_CURRENCY_CODE] ?: "INR"
        }

    // Currency symbol helper
    val currencySymbol: Flow<String> = selectedCurrencyCode.map { code ->
        CurrencyProvider.getSymbol(code)
    }

    // Check if currency has ever been explicitly set
    val hasCurrencyBeenSet: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.HAS_CURRENCY_BEEN_SET] ?: false
        }

    suspend fun setDarkModePreference(preference: DarkModePreference) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DARK_MODE_PREF] = preference.name
        }
    }

    suspend fun setAppTheme(theme: AppTheme) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.APP_THEME] = theme.name
        }
    }
    
    suspend fun setCustomColors(primary: Int, secondary: Int, tertiary: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.CUSTOM_PRIMARY] = primary
            preferences[PreferencesKeys.CUSTOM_SECONDARY] = secondary
            preferences[PreferencesKeys.CUSTOM_TERTIARY] = tertiary
        }
    }

    suspend fun setCurrency(currencyCode: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SELECTED_CURRENCY_CODE] = currencyCode
            preferences[PreferencesKeys.HAS_CURRENCY_BEEN_SET] = true
        }
    }

    /**
     * Mark currency as set without changing the currency code.
     * Used for migrating existing users who already have data.
     */
    suspend fun markCurrencyAsSet() {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.HAS_CURRENCY_BEEN_SET] = true
        }
    }

    suspend fun completeOnboarding() {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.HAS_COMPLETED_ONBOARDING] = true
        }
    }
    
    /**
     * Clear all user preferences on logout.
     * This ensures the next user starts fresh and gets their own settings from Firebase.
     */
    suspend fun clearUserPreferences() {
        dataStore.edit { preferences ->
            // Clear currency preferences
            preferences.remove(PreferencesKeys.SELECTED_CURRENCY_CODE)
            preferences.remove(PreferencesKeys.HAS_CURRENCY_BEEN_SET)
            
            // Clear onboarding (so new user goes through onboarding)
            preferences.remove(PreferencesKeys.HAS_COMPLETED_ONBOARDING)
            
            // Note: Theme preferences are intentionally kept - they're device-specific, not user-specific
        }
    }
}

