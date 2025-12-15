package com.theblankstate.epmanager.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.theblankstate.epmanager.data.repository.AppTheme
import com.theblankstate.epmanager.data.repository.DarkModePreference
import com.theblankstate.epmanager.data.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ThemeUiState(
    val darkModePreference: DarkModePreference = DarkModePreference.SYSTEM,
    val appTheme: AppTheme = AppTheme.MONOCHROME,
    val customPrimary: Int = 0xFF2196F3.toInt(),
    val customSecondary: Int = 0xFF03DAC6.toInt(),
    val customTertiary: Int = 0xFFFFC107.toInt()
)

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val themeState: StateFlow<ThemeUiState> = combine(
        userPreferencesRepository.darkModePreference,
        userPreferencesRepository.appTheme,
        userPreferencesRepository.customThemeColors
    ) { darkModePref, theme, customColors ->
        ThemeUiState(
            darkModePreference = darkModePref,
            appTheme = theme,
            customPrimary = customColors.first,
            customSecondary = customColors.second,
            customTertiary = customColors.third
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ThemeUiState()
    )

    fun setDarkModePreference(preference: DarkModePreference) {
        viewModelScope.launch {
            userPreferencesRepository.setDarkModePreference(preference)
        }
    }

    fun setAppTheme(theme: AppTheme) {
        viewModelScope.launch {
            userPreferencesRepository.setAppTheme(theme)
        }
    }
    
    fun setCustomColors(primary: Int, secondary: Int, tertiary: Int) {
        viewModelScope.launch {
            userPreferencesRepository.setCustomColors(primary, secondary, tertiary)
        }
    }
}
