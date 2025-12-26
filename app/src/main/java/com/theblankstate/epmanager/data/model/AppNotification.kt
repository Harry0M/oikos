package com.theblankstate.epmanager.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Represents an in-app notification for history tracking
 */
@Entity(
    tableName = "app_notifications",
    indices = [
        Index("createdAt"),
        Index("isRead")
    ]
)
data class AppNotification(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val type: NotificationType,
    val title: String,
    val message: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val actionData: String? = null // JSON data for deep linking
)

enum class NotificationType {
    BUDGET_WARNING,
    BUDGET_EXCEEDED,
    RECURRING_REMINDER,
    DAILY_INSIGHT,
    WEEKLY_SUMMARY,
    SAVINGS_MILESTONE,
    SETTLEMENT_RECEIVED,
    FRIEND_REQUEST,
    DEBT_REMINDER,
    SYSTEM
}
