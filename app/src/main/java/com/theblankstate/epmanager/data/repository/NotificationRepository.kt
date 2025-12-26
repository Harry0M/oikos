package com.theblankstate.epmanager.data.repository

import com.theblankstate.epmanager.data.local.dao.NotificationDao
import com.theblankstate.epmanager.data.model.AppNotification
import com.theblankstate.epmanager.data.model.NotificationType
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(
    private val notificationDao: NotificationDao
) {
    
    fun getAllNotifications(): Flow<List<AppNotification>> =
        notificationDao.getAllNotifications()
    
    fun getUnreadNotifications(): Flow<List<AppNotification>> =
        notificationDao.getUnreadNotifications()
    
    fun getUnreadCount(): Flow<Int> =
        notificationDao.getUnreadCount()
    
    suspend fun getNotificationById(id: String): AppNotification? =
        notificationDao.getNotificationById(id)
    
    suspend fun saveNotification(notification: AppNotification) {
        notificationDao.insertNotification(notification)
    }
    
    suspend fun saveNotification(
        type: NotificationType,
        title: String,
        message: String,
        actionData: String? = null
    ) {
        val notification = AppNotification(
            type = type,
            title = title,
            message = message,
            actionData = actionData
        )
        notificationDao.insertNotification(notification)
    }
    
    suspend fun markAsRead(id: String) {
        notificationDao.markAsRead(id)
    }
    
    suspend fun markAllAsRead() {
        notificationDao.markAllAsRead()
    }
    
    suspend fun deleteNotification(id: String) {
        notificationDao.deleteById(id)
    }
    
    suspend fun deleteAllNotifications() {
        notificationDao.deleteAllNotifications()
    }
    
    /**
     * Clean up old notifications (older than 30 days)
     */
    suspend fun cleanupOldNotifications() {
        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        notificationDao.deleteOlderThan(thirtyDaysAgo)
    }
}
