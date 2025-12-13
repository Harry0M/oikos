package com.theblankstate.epmanager.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.theblankstate.epmanager.data.repository.AuthRepository
import com.theblankstate.epmanager.data.repository.AuthResult
import com.theblankstate.epmanager.data.repository.FriendsRepository
import com.theblankstate.epmanager.data.sync.FirebaseSyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val user: FirebaseUser? = null,
    val error: String? = null,
    val successMessage: String? = null,
    val lastSyncTime: Long? = null,
    val isSyncing: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val syncManager: FirebaseSyncManager,
    private val friendsRepository: FriendsRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()
    
    init {
        observeAuthState()
    }
    
    private fun observeAuthState() {
        viewModelScope.launch {
            authRepository.observeAuthState().collect { user ->
                _uiState.update { 
                    it.copy(
                        isLoggedIn = user != null,
                        user = user
                    ) 
                }
                
                // Get last sync times if logged in
                if (user != null) {
                    val (settingsSync, transactionSync) = syncManager.getLastSyncTimes()
                    _uiState.update { it.copy(lastSyncTime = settingsSync) }
                }
            }
        }
    }
    
    fun signInWithEmail(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(error = "Please fill in all fields") }
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            when (val result = authRepository.signInWithEmail(email, password)) {
                is AuthResult.Success -> {
                    // Register user for friend lookup
                    friendsRepository.ensureUserProfile()
                    
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            isLoggedIn = true,
                            user = result.user,
                            successMessage = "Welcome back!"
                        ) 
                    }
                }
                is AuthResult.Error -> {
                    _uiState.update { 
                        it.copy(isLoading = false, error = result.message) 
                    }
                }
                AuthResult.Loading -> {}
            }
        }
    }
    
    fun signUpWithEmail(email: String, password: String, confirmPassword: String) {
        if (email.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
            _uiState.update { it.copy(error = "Please fill in all fields") }
            return
        }
        
        if (password != confirmPassword) {
            _uiState.update { it.copy(error = "Passwords do not match") }
            return
        }
        
        if (password.length < 6) {
            _uiState.update { it.copy(error = "Password must be at least 6 characters") }
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            when (val result = authRepository.signUpWithEmail(email, password)) {
                is AuthResult.Success -> {
                    // Register user for friend lookup
                    friendsRepository.ensureUserProfile()
                    
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            isLoggedIn = true,
                            user = result.user,
                            successMessage = "Account created successfully!"
                        ) 
                    }
                }
                is AuthResult.Error -> {
                    _uiState.update { 
                        it.copy(isLoading = false, error = result.message) 
                    }
                }
                AuthResult.Loading -> {}
            }
        }
    }
    
    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            when (val result = authRepository.signInWithGoogle(idToken)) {
                is AuthResult.Success -> {
                    // Register user for friend lookup
                    friendsRepository.ensureUserProfile()
                    
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            isLoggedIn = true,
                            user = result.user,
                            successMessage = "Signed in with Google!"
                        ) 
                    }
                }
                is AuthResult.Error -> {
                    _uiState.update { 
                        it.copy(isLoading = false, error = result.message) 
                    }
                }
                AuthResult.Loading -> {}
            }
        }
    }
    
    fun sendPasswordReset(email: String) {
        if (email.isBlank()) {
            _uiState.update { it.copy(error = "Please enter your email") }
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            authRepository.sendPasswordResetEmail(email)
                .onSuccess {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            successMessage = "Password reset email sent!"
                        ) 
                    }
                }
                .onFailure { e ->
                    _uiState.update { 
                        it.copy(isLoading = false, error = e.message) 
                    }
                }
        }
    }
    
    fun signOut() {
        authRepository.signOut()
        _uiState.update { 
            it.copy(
                isLoggedIn = false,
                user = null,
                lastSyncTime = null
            ) 
        }
    }
    
    /**
     * Backup settings data to cloud (auto-sync data)
     * This is also done automatically by BackgroundSyncWorker
     */
    fun backupSettingsToCloud() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, error = null) }
            
            syncManager.backupSettingsToCloud()
                .onSuccess {
                    val (settingsSync, _) = syncManager.getLastSyncTimes()
                    _uiState.update { 
                        it.copy(
                            isSyncing = false,
                            lastSyncTime = settingsSync,
                            successMessage = "Settings backup complete!"
                        ) 
                    }
                }
                .onFailure { e ->
                    _uiState.update { 
                        it.copy(isSyncing = false, error = e.message) 
                    }
                }
        }
    }
    
    /**
     * Restore settings data from cloud
     * Call this after reinstalling the app
     */
    fun restoreSettingsFromCloud() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, error = null) }
            
            syncManager.restoreSettingsFromCloud()
                .onSuccess {
                    _uiState.update { 
                        it.copy(
                            isSyncing = false,
                            successMessage = "Settings restored!"
                        ) 
                    }
                }
                .onFailure { e ->
                    _uiState.update { 
                        it.copy(isSyncing = false, error = e.message) 
                    }
                }
        }
    }
    
    /**
     * Manual backup of transactions (NOT auto-synced)
     */
    fun backupTransactions() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, error = null) }
            
            syncManager.backupTransactionsToCloud()
                .onSuccess {
                    _uiState.update { 
                        it.copy(
                            isSyncing = false,
                            successMessage = "Transactions backed up!"
                        ) 
                    }
                }
                .onFailure { e ->
                    _uiState.update { 
                        it.copy(isSyncing = false, error = e.message) 
                    }
                }
        }
    }
    
    /**
     * Manual restore of transactions
     */
    fun restoreTransactions() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, error = null) }
            
            syncManager.restoreTransactionsFromCloud()
                .onSuccess { count ->
                    _uiState.update { 
                        it.copy(
                            isSyncing = false,
                            successMessage = "Restored $count transactions!"
                        ) 
                    }
                }
                .onFailure { e ->
                    _uiState.update { 
                        it.copy(isSyncing = false, error = e.message) 
                    }
                }
        }
    }
    
    /**
     * Backup everything - both settings and transactions
     */
    fun backupEverything() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, error = null) }
            
            // First backup settings
            val settingsResult = syncManager.backupSettingsToCloud()
            if (settingsResult.isFailure) {
                _uiState.update { 
                    it.copy(isSyncing = false, error = settingsResult.exceptionOrNull()?.message) 
                }
                return@launch
            }
            
            // Then backup transactions
            val transactionsResult = syncManager.backupTransactionsToCloud()
            
            transactionsResult.fold(
                onSuccess = {
                    val (settingsSync, _) = syncManager.getLastSyncTimes()
                    _uiState.update { 
                        it.copy(
                            isSyncing = false,
                            lastSyncTime = settingsSync,
                            successMessage = "All data backed up!"
                        ) 
                    }
                },
                onFailure = { e ->
                    _uiState.update { 
                        it.copy(isSyncing = false, error = e.message) 
                    }
                }
            )
        }
    }
    
    /**
     * Restore everything - both settings and transactions
     */
    fun restoreEverything() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, error = null) }
            
            // First restore settings
            val settingsResult = syncManager.restoreSettingsFromCloud()
            if (settingsResult.isFailure) {
                _uiState.update { 
                    it.copy(isSyncing = false, error = "Settings restore failed: ${settingsResult.exceptionOrNull()?.message}") 
                }
                return@launch
            }
            
            // Then restore transactions
            val transactionsResult = syncManager.restoreTransactionsFromCloud()
            
            transactionsResult.fold(
                onSuccess = { count ->
                    _uiState.update { 
                        it.copy(
                            isSyncing = false,
                            successMessage = "Restored settings + $count transactions!"
                        ) 
                    }
                },
                onFailure = { e ->
                    _uiState.update { 
                        it.copy(isSyncing = false, error = e.message) 
                    }
                }
            )
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    fun clearSuccess() {
        _uiState.update { it.copy(successMessage = null) }
    }
}
