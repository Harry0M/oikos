package com.theblankstate.epmanager.ui.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.theblankstate.epmanager.data.model.Friend
import com.theblankstate.epmanager.data.model.FriendRequest
import com.theblankstate.epmanager.data.repository.FriendsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FriendsUiState(
    val friends: List<Friend> = emptyList(),
    val incomingRequests: List<FriendRequest> = emptyList(),
    val isLoading: Boolean = true,
    val showAddFriendSheet: Boolean = false,
    val searchEmail: String = "",
    val isSearching: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class FriendsViewModel @Inject constructor(
    private val friendsRepository: FriendsRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(FriendsUiState())
    val uiState: StateFlow<FriendsUiState> = _uiState.asStateFlow()
    
    init {
        loadFriends()
        loadIncomingRequests()
        ensureUserProfile()
    }
    
    private fun ensureUserProfile() {
        viewModelScope.launch {
            friendsRepository.ensureUserProfile()
        }
    }
    
    private fun loadFriends() {
        viewModelScope.launch {
            friendsRepository.getFriends().collect { friends ->
                _uiState.update { it.copy(friends = friends, isLoading = false) }
            }
        }
    }
    
    private fun loadIncomingRequests() {
        viewModelScope.launch {
            friendsRepository.getIncomingRequests().collect { requests ->
                _uiState.update { it.copy(incomingRequests = requests) }
            }
        }
    }
    
    fun showAddFriendSheet() {
        _uiState.update { it.copy(showAddFriendSheet = true, searchEmail = "", error = null) }
    }
    
    fun hideAddFriendSheet() {
        _uiState.update { it.copy(showAddFriendSheet = false, searchEmail = "", error = null) }
    }
    
    fun updateSearchEmail(email: String) {
        _uiState.update { it.copy(searchEmail = email, error = null) }
    }
    
    fun sendFriendRequest() {
        val email = _uiState.value.searchEmail.trim().lowercase()
        if (email.isBlank()) {
            _uiState.update { it.copy(error = "Please enter an email") }
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true, error = null) }
            
            friendsRepository.sendFriendRequest(email).fold(
                onSuccess = {
                    _uiState.update { 
                        it.copy(
                            isSearching = false,
                            showAddFriendSheet = false,
                            successMessage = "Friend request sent!"
                        ) 
                    }
                },
                onFailure = { e ->
                    _uiState.update { 
                        it.copy(isSearching = false, error = e.message) 
                    }
                }
            )
        }
    }
    
    fun acceptRequest(requestId: String) {
        viewModelScope.launch {
            friendsRepository.acceptFriendRequest(requestId).fold(
                onSuccess = {
                    _uiState.update { it.copy(successMessage = "Friend added!") }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
            )
        }
    }
    
    fun rejectRequest(requestId: String) {
        viewModelScope.launch {
            friendsRepository.rejectFriendRequest(requestId)
        }
    }
    
    fun removeFriend(friendId: String) {
        viewModelScope.launch {
            friendsRepository.removeFriend(friendId).fold(
                onSuccess = {
                    _uiState.update { it.copy(successMessage = "Friend removed") }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(error = e.message) }
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
