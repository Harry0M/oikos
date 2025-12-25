package com.theblankstate.epmanager.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.theblankstate.epmanager.data.repository.AppTheme
import com.theblankstate.epmanager.data.repository.TermsRepository
import com.theblankstate.epmanager.data.repository.TransactionRepository
import com.theblankstate.epmanager.data.repository.UserPreferencesRepository
import com.theblankstate.epmanager.data.sync.FirebaseSyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(
    val isLoading: Boolean = true, // Start loading until we know status
    val isComplete: Boolean = false,
    val hasCurrencyBeenSet: Boolean = false
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val transactionRepository: TransactionRepository,
    private val firebaseSyncManager: FirebaseSyncManager,
    private val termsRepository: TermsRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()
    
    // Expose onboarding status as a flow
    val hasCompletedOnboarding: Flow<Boolean> = userPreferencesRepository.hasCompletedOnboarding
    
    init {
        checkAndMigrateExistingUser()
    }
    
    /**
     * Checks if user has existing data but hasn't set the currency flag yet.
     * First tries to restore currency from Firebase (for returning users).
     * Falls back to checking local transactions for migration.
     */
    private fun checkAndMigrateExistingUser() {
        viewModelScope.launch {
            val currencySet = userPreferencesRepository.hasCurrencyBeenSet.first()
            
            if (!currencySet) {
                // Check if user has existing transactions (local migration case)
                val transactions = transactionRepository.getAllTransactions().first()
                if (transactions.isNotEmpty()) {
                    // User has existing data - they're a returning user
                    // Mark currency as set so we skip the currency step
                    userPreferencesRepository.markCurrencyAsSet()
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        hasCurrencyBeenSet = true
                    )
                    return@launch
                }
            }
            
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                hasCurrencyBeenSet = currencySet
            )
        }
    }

    /**
     * explicit check for remote currency after login
     */
    /**
     * explicit check for remote currency after login
     * Returns true if currency was found and restored
     */
    suspend fun checkRemoteCurrency(): Result<Boolean> {
        _uiState.value = _uiState.value.copy(isLoading = true)
        return try {
            if (firebaseSyncManager.isLoggedIn) {
                val cloudCurrency = firebaseSyncManager.restoreCurrencyFromCloud()
                if (cloudCurrency != null) {
                    // Found currency in Firebase - restore it locally
                    userPreferencesRepository.setCurrency(cloudCurrency)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        hasCurrencyBeenSet = true
                    )
                    Result.success(true)
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    Result.success(false)
                }
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false)
                Result.failure(Exception("Not logged in"))
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(isLoading = false)
            Result.failure(e)
        }
    }
    
    fun setCurrency(currencyCode: String) {
        viewModelScope.launch {
            // Save locally
            userPreferencesRepository.setCurrency(currencyCode)
            
            // Backup to Firebase
            firebaseSyncManager.backupCurrencyToCloud(currencyCode)
        }
    }
    
    fun setTheme(theme: AppTheme) {
        viewModelScope.launch {
            userPreferencesRepository.setAppTheme(theme)
        }
    }
    
    fun completeOnboarding() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            userPreferencesRepository.completeOnboarding()
            _uiState.value = _uiState.value.copy(isLoading = false, isComplete = true)
        }
    }
    
    /**
     * Record Terms & Conditions acceptance in database
     */
    suspend fun acceptTerms() {
        termsRepository.acceptTerms(
            userId = firebaseSyncManager.getCurrentUserId(),
            deviceId = android.os.Build.MODEL
        )
    }
    
    /**
     * Check if user has already accepted current terms
     */
    suspend fun hasAcceptedTerms(): Boolean {
        return termsRepository.hasAcceptedCurrentTerms()
    }
}

