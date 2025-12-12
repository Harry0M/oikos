package com.theblankstate.epmanager.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.theblankstate.epmanager.data.repository.AuthRepository
import com.theblankstate.epmanager.data.repository.AuthResult
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
    private val syncManager: FirebaseSyncManager
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
                
                // Get last sync time if logged in
                if (user != null) {
                    val lastSync = syncManager.getLastSyncTime()
                    _uiState.update { it.copy(lastSyncTime = lastSync) }
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
    
    fun backupToCloud() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, error = null) }
            
            syncManager.backupToCloud()
                .onSuccess {
                    val lastSync = syncManager.getLastSyncTime()
                    _uiState.update { 
                        it.copy(
                            isSyncing = false,
                            lastSyncTime = lastSync,
                            successMessage = "Backup complete!"
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
    
    fun restoreFromCloud() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, error = null) }
            
            syncManager.restoreFromCloud()
                .onSuccess {
                    _uiState.update { 
                        it.copy(
                            isSyncing = false,
                            successMessage = "Restore complete!"
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
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    fun clearSuccess() {
        _uiState.update { it.copy(successMessage = null) }
    }
}
