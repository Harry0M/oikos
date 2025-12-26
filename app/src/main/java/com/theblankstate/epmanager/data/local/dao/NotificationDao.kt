package com.theblankstate.epmanager.data.local.dao

import androidx.room.*
import com.theblankstate.epmanager.data.model.AppNotification
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {
    
    @Query("SELECT * FROM app_notifications ORDER BY createdAt DESC")
    fun getAllNotifications(): Flow<List<AppNotification>>
    
    @Query("SELECT * FROM app_notifications WHERE isRead = 0 ORDER BY createdAt DESC")
    fun getUnreadNotifications(): Flow<List<AppNotification>>
    
    @Query("SELECT COUNT(*) FROM app_notifications WHERE isRead = 0")
    fun getUnreadCount(): Flow<Int>
    
    @Query("SELECT * FROM app_notifications WHERE id = :id")
    suspend fun getNotificationById(id: String): AppNotification?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: AppNotification)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotifications(notifications: List<AppNotification>)
    
    @Query("UPDATE app_notifications SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: String)
    
    @Query("UPDATE app_notifications SET isRead = 1")
    suspend fun markAllAsRead()
    
    @Delete
    suspend fun deleteNotification(notification: AppNotification)
    
    @Query("DELETE FROM app_notifications WHERE id = :id")
    suspend fun deleteById(id: String)
    
    @Query("DELETE FROM app_notifications")
    suspend fun deleteAllNotifications()
    
    @Query("DELETE FROM app_notifications WHERE createdAt < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long)
}
