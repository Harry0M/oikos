package com.theblankstate.epmanager.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Represents a friend connection between two users
 */
@Entity(tableName = "friends")
data class Friend(
    @PrimaryKey val odiserId: String, // Firebase UID of friend
    val email: String,
    val displayName: String? = null,
    val addedAt: Long = System.currentTimeMillis(),
    val status: FriendStatus = FriendStatus.ACCEPTED
)

enum class FriendStatus {
    PENDING_SENT,      // You sent the request
    PENDING_RECEIVED,  // You received the request
    ACCEPTED,          // Friends
    BLOCKED           // Blocked user
}

/**
 * Represents a friend request between users
 */
@Entity(tableName = "friend_requests")
data class FriendRequest(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val fromUserId: String,
    val fromEmail: String,
    val fromDisplayName: String? = null,
    val toUserId: String,
    val toEmail: String,
    val status: RequestStatus = RequestStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis()
)

enum class RequestStatus {
    PENDING,
    ACCEPTED,
    REJECTED
}

/**
 * User profile stored in Firebase for lookup
 */
data class UserProfile(
    val odiserId: String = "",
    val email: String = "",
    val displayName: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Settlement notification sent between users
 */
data class SettlementNotification(
    val id: String = UUID.randomUUID().toString(),
    val fromUserId: String = "",
    val fromDisplayName: String? = null,
    val toUserId: String = "",
    val amount: Double = 0.0,
    val groupId: String = "",
    val groupName: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)
