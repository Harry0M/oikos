package com.theblankstate.epmanager.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.theblankstate.epmanager.data.model.AppNotification
import com.theblankstate.epmanager.data.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationCenterViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository
) : ViewModel() {
    
    val notifications: StateFlow<List<AppNotification>> = 
        notificationRepository.getAllNotifications()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    
    val unreadCount: StateFlow<Int> = 
        notificationRepository.getUnreadCount()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = 0
            )
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    fun markAsRead(notification: AppNotification) {
        viewModelScope.launch {
            notificationRepository.markAsRead(notification.id)
        }
    }
    
    fun markAllAsRead() {
        viewModelScope.launch {
            notificationRepository.markAllAsRead()
        }
    }
    
    fun deleteNotification(notification: AppNotification) {
        viewModelScope.launch {
            notificationRepository.deleteNotification(notification.id)
        }
    }
    
    fun clearAllNotifications() {
        viewModelScope.launch {
            notificationRepository.deleteAllNotifications()
        }
    }
}
