package com.theblankstate.epmanager.ui.notifications

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.theblankstate.epmanager.notifications.NotificationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationSettingsUiState(
    val hasPermission: Boolean = false,
    val budgetAlerts: Boolean = true,
    val recurringReminders: Boolean = true,
    val debtReminders: Boolean = true,
    val savingsReminders: Boolean = true,
    val splitReminders: Boolean = true,
    val dailyInsights: Boolean = false
)

@HiltViewModel
class NotificationSettingsViewModel @Inject constructor(
    private val notificationManager: NotificationManager,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(NotificationSettingsUiState())
    val uiState: StateFlow<NotificationSettingsUiState> = _uiState.asStateFlow()
    
    init {
        loadPreferences()
    }
    
    private fun loadPreferences() {
        val prefs = context.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)
        _uiState.update {
            it.copy(
                budgetAlerts = prefs.getBoolean("budget_alerts", true),
                recurringReminders = prefs.getBoolean("recurring_reminders", true),
                debtReminders = prefs.getBoolean("debt_reminders", true),
                savingsReminders = prefs.getBoolean("savings_reminders", true),
                splitReminders = prefs.getBoolean("split_reminders", true),
                dailyInsights = prefs.getBoolean("daily_insights", false)
            )
        }
    }
    
    private fun savePreference(key: String, value: Boolean) {
        context.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)
            .edit()
            .putBoolean(key, value)
            .apply()
    }
    
    fun updatePermissionStatus(hasPermission: Boolean) {
        _uiState.update { it.copy(hasPermission = hasPermission) }
    }
    
    fun setBudgetAlerts(enabled: Boolean) {
        _uiState.update { it.copy(budgetAlerts = enabled) }
        savePreference("budget_alerts", enabled)
    }
    
    fun setRecurringReminders(enabled: Boolean) {
        _uiState.update { it.copy(recurringReminders = enabled) }
        savePreference("recurring_reminders", enabled)
    }
    
    fun setDebtReminders(enabled: Boolean) {
        _uiState.update { it.copy(debtReminders = enabled) }
        savePreference("debt_reminders", enabled)
    }
    
    fun setSavingsReminders(enabled: Boolean) {
        _uiState.update { it.copy(savingsReminders = enabled) }
        savePreference("savings_reminders", enabled)
    }
    
    fun setSplitReminders(enabled: Boolean) {
        _uiState.update { it.copy(splitReminders = enabled) }
        savePreference("split_reminders", enabled)
    }
    
    fun setDailyInsights(enabled: Boolean) {
        _uiState.update { it.copy(dailyInsights = enabled) }
        savePreference("daily_insights", enabled)
    }
    
    fun sendTestNotification() {
        viewModelScope.launch {
            notificationManager.sendTestNotification()
        }
    }
}
