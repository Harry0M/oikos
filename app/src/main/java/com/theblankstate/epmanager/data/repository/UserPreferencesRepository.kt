package com.theblankstate.epmanager.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
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
        val CUSTOM_PRIMARY = androidx.datastore.preferences.core.intPreferencesKey("custom_primary")
        val CUSTOM_SECONDARY = androidx.datastore.preferences.core.intPreferencesKey("custom_secondary")
        val CUSTOM_TERTIARY = androidx.datastore.preferences.core.intPreferencesKey("custom_tertiary")
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

    val appTheme: Flow<AppTheme> = dataStore.data
        .map { preferences ->
            val themeName = preferences[PreferencesKeys.APP_THEME] ?: AppTheme.MONOCHROME.name
            try {
                AppTheme.valueOf(themeName)
            } catch (e: Exception) {
                AppTheme.MONOCHROME
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
}
