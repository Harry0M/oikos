package com.theblankstate.epmanager.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.theblankstate.epmanager.data.local.dao.FriendDao
import com.theblankstate.epmanager.data.model.Friend
import com.theblankstate.epmanager.data.model.FriendRequest
import com.theblankstate.epmanager.data.model.FriendStatus
import com.theblankstate.epmanager.data.model.RequestStatus
import com.theblankstate.epmanager.data.model.SettlementNotification
import com.theblankstate.epmanager.data.model.UserProfile
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FriendsRepository @Inject constructor(
    private val friendDao: FriendDao
) {
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    
    private val usersRef = database.getReference("users")
    private val requestsRef = database.getReference("friend_requests")
    private val friendsRef = database.getReference("friends")
    
    private val currentUserId: String?
        get() = auth.currentUser?.uid
    
    private val currentUserEmail: String?
        get() = auth.currentUser?.email
    
    // ==================== USER PROFILE ====================
    
    /**
     * Ensure current user's profile exists in Firebase
     * Also stores email lookup for friend search
     */
    suspend fun ensureUserProfile() {
        val uid = currentUserId ?: return
        val email = currentUserEmail ?: return
        
        try {
            val snapshot = usersRef.child(uid).get().await()
            if (!snapshot.exists()) {
                val profile = UserProfile(
                    odiserId = uid,
                    email = email.lowercase(),
                    displayName = email.substringBefore("@")
                )
                usersRef.child(uid).setValue(profile).await()
                
                // Store email->userId mapping for lookup (avoid index requirement)
                val emailKey = emailToKey(email)
                database.getReference("email_lookup").child(emailKey).setValue(uid).await()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Convert email to a valid Firebase key (replace invalid characters)
     */
    private fun emailToKey(email: String): String {
        return email.lowercase()
            .replace(".", "_dot_")
            .replace("@", "_at_")
            .replace("#", "_hash_")
            .replace("$", "_dollar_")
            .replace("[", "_lb_")
            .replace("]", "_rb_")
    }
    
    /**
     * Search for a user by email using email_lookup path
     */
    suspend fun searchUserByEmail(email: String): UserProfile? {
        return try {
            val emailKey = emailToKey(email)
            android.util.Log.d("FriendsRepo", "Searching for email: $email, key: $emailKey")
            
            val lookupSnapshot = database.getReference("email_lookup")
                .child(emailKey)
                .get()
                .await()
            
            val userId = lookupSnapshot.getValue(String::class.java)
            android.util.Log.d("FriendsRepo", "Found userId: $userId")
            
            if (userId == null) {
                android.util.Log.d("FriendsRepo", "No user found for email: $email")
                return null
            }
            
            // Get user profile
            val userSnapshot = usersRef.child(userId).get().await()
            if (userSnapshot.exists()) {
                UserProfile(
                    odiserId = userId,
                    email = userSnapshot.child("email").getValue(String::class.java) ?: email,
                    displayName = userSnapshot.child("displayName").getValue(String::class.java)
                )
            } else {
                android.util.Log.d("FriendsRepo", "User profile not found for userId: $userId")
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("FriendsRepo", "Error searching for user: ${e.message}")
            e.printStackTrace()
            null
        }
    }
    
    // ==================== FRIEND REQUESTS ====================
    
    /**
     * Send a friend request to a user by email
     */
    suspend fun sendFriendRequest(toEmail: String): Result<Unit> {
        val fromUserId = currentUserId ?: return Result.failure(Exception("Not logged in"))
        val fromEmail = currentUserEmail ?: return Result.failure(Exception("No email"))
        
        return try {
            // Find user by email
            val toUser = searchUserByEmail(toEmail)
                ?: return Result.failure(Exception("User not found with this email"))
            
            if (toUser.odiserId == fromUserId) {
                return Result.failure(Exception("Cannot add yourself"))
            }
            
            // Check if already friends
            val existingFriend = friendDao.getFriendById(toUser.odiserId)
            if (existingFriend != null) {
                return Result.failure(Exception("Already friends"))
            }
            
            // Check for existing pending request under SENDER's path (we can read our own path)
            val requestId = "${fromUserId}_${toUser.odiserId}"
            val existingRequest = requestsRef.child(fromUserId).child(requestId).get().await()
            if (existingRequest.exists()) {
                val status = existingRequest.child("status").getValue(String::class.java)
                if (status == "PENDING") {
                    return Result.failure(Exception("Request already sent"))
                }
            }
            
            // Create friend request
            val request = FriendRequest(
                id = requestId,
                fromUserId = fromUserId,
                fromEmail = fromEmail,
                fromDisplayName = fromEmail.substringBefore("@"),
                toUserId = toUser.odiserId,
                toEmail = toUser.email,
                status = RequestStatus.PENDING
            )
            
            // Save to Firebase under RECIPIENT's path (so they can read it)
            requestsRef.child(toUser.odiserId).child(requestId).setValue(requestToMap(request)).await()
            
            // Also save under sender's path for tracking sent requests
            requestsRef.child(fromUserId).child(requestId).setValue(requestToMap(request)).await()
            
            // Save locally as sent
            friendDao.insertRequest(request)
            
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(Exception("Failed to send request: ${e.message}"))
        }
    }
    
    /**
     * Accept a friend request
     */
    suspend fun acceptFriendRequest(requestId: String): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(Exception("Not logged in"))
        
        // Get request from Firebase (under current user's path)
        val snapshot = requestsRef.child(userId).child(requestId).get().await()
        if (!snapshot.exists()) {
            return Result.failure(Exception("Request not found"))
        }
        
        val fromUserId = snapshot.child("fromUserId").getValue(String::class.java) ?: return Result.failure(Exception("Invalid request"))
        val fromEmail = snapshot.child("fromEmail").getValue(String::class.java) ?: ""
        val fromDisplayName = snapshot.child("fromDisplayName").getValue(String::class.java)
        
        // Update request status in both paths
        requestsRef.child(userId).child(requestId).child("status").setValue("ACCEPTED").await()
        requestsRef.child(fromUserId).child(requestId).child("status").setValue("ACCEPTED").await()
        
        // Add to friends list (both users)
        val timestamp = System.currentTimeMillis()
        
        // Add to my friends
        val myFriend = Friend(
            odiserId = fromUserId,
            email = fromEmail,
            displayName = fromDisplayName,
            addedAt = timestamp,
            status = FriendStatus.ACCEPTED
        )
        friendsRef.child(userId).child(fromUserId).setValue(friendToMap(myFriend)).await()
        friendDao.insertFriend(myFriend)
        
        // Add me to their friends
        val theirFriend = mapOf(
            "odiserId" to userId,
            "email" to (currentUserEmail ?: ""),
            "displayName" to (currentUserEmail?.substringBefore("@") ?: "Unknown"),
            "addedAt" to timestamp,
            "status" to "ACCEPTED"
        )
        friendsRef.child(fromUserId).child(userId).setValue(theirFriend).await()
        
        // Remove local request
        friendDao.deleteRequestById(requestId)
        
        return Result.success(Unit)
    }
    
    /**
     * Reject a friend request
     */
    suspend fun rejectFriendRequest(requestId: String): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(Exception("Not logged in"))
        
        // Get request to find fromUserId
        val snapshot = requestsRef.child(userId).child(requestId).get().await()
        val fromUserId = snapshot.child("fromUserId").getValue(String::class.java)
        
        // Update request status in both paths
        requestsRef.child(userId).child(requestId).child("status").setValue("REJECTED").await()
        if (fromUserId != null) {
            requestsRef.child(fromUserId).child(requestId).child("status").setValue("REJECTED").await()
        }
        
        // Remove local request
        friendDao.deleteRequestById(requestId)
        
        return Result.success(Unit)
    }
    
    /**
     * Get incoming friend requests (real-time) - now listens to current user's path
     */
    fun getIncomingRequests(): Flow<List<FriendRequest>> = callbackFlow {
        val userId = currentUserId ?: run {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val requests = mutableListOf<FriendRequest>()
                snapshot.children.forEach { child ->
                    val toUserId = child.child("toUserId").getValue(String::class.java)
                    val status = child.child("status").getValue(String::class.java)
                    
                    // Only show pending requests where I am the recipient
                    if (toUserId == userId && status == "PENDING") {
                        requests.add(mapToRequest(child))
                    }
                }
                trySend(requests)
            }
            
            override fun onCancelled(error: DatabaseError) {
                android.util.Log.e("FriendsRepo", "Error getting incoming requests: ${error.message}")
                trySend(emptyList())
            }
        }
        
        // Listen to current user's requests path
        requestsRef.child(userId).addValueEventListener(listener)
        awaitClose { requestsRef.child(userId).removeEventListener(listener) }
    }
    
    // ==================== FRIENDS ====================
    
    /**
     * Get all friends (real-time from Firebase)
     */
    fun getFriends(): Flow<List<Friend>> = callbackFlow {
        val userId = currentUserId ?: run {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val friends = mutableListOf<Friend>()
                snapshot.children.forEach { child ->
                    val friend = Friend(
                        odiserId = child.key ?: "",
                        email = child.child("email").getValue(String::class.java) ?: "",
                        displayName = child.child("displayName").getValue(String::class.java),
                        addedAt = child.child("addedAt").getValue(Long::class.java) ?: 0L,
                        status = FriendStatus.ACCEPTED
                    )
                    friends.add(friend)
                }
                trySend(friends)
            }
            
            override fun onCancelled(error: DatabaseError) {
                android.util.Log.e("FriendsRepo", "Error getting friends: ${error.message}")
                trySend(emptyList())
            }
        }
        
        friendsRef.child(userId).addValueEventListener(listener)
        awaitClose { friendsRef.child(userId).removeEventListener(listener) }
    }
    
    /**
     * Remove a friend
     */
    suspend fun removeFriend(friendId: String): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(Exception("Not logged in"))
        
        // Remove from both users' lists
        friendsRef.child(userId).child(friendId).removeValue().await()
        friendsRef.child(friendId).child(userId).removeValue().await()
        
        // Remove locally
        friendDao.deleteFriendById(friendId)
        
        return Result.success(Unit)
    }
    
    /**
     * Get local friends (cached)
     */
    fun getLocalFriends(): Flow<List<Friend>> = friendDao.getAcceptedFriends()
    
    /**
     * Get friend count
     */
    fun getFriendCount(): Flow<Int> = friendDao.getFriendCount()
    
    /**
     * Get pending request count for current user
     */
    fun getPendingRequestCount(): Flow<Int> {
        val userId = currentUserId ?: return kotlinx.coroutines.flow.flowOf(0)
        return friendDao.getPendingRequestCount(userId)
    }
    
    // ==================== SETTLEMENT NOTIFICATIONS ====================
    
    private val settlementNotificationsRef = database.getReference("settlement_notifications")
    
    /**
     * Send a settlement notification to a linked friend
     */
    suspend fun sendSettlementNotification(
        toUserId: String,
        amount: Double,
        groupId: String,
        groupName: String
    ): Result<Unit> {
        val fromUserId = currentUserId ?: return Result.failure(Exception("Not logged in"))
        val fromEmail = currentUserEmail ?: return Result.failure(Exception("No email"))
        
        return try {
            val notificationId = java.util.UUID.randomUUID().toString()
            val notification = SettlementNotification(
                id = notificationId,
                fromUserId = fromUserId,
                fromDisplayName = fromEmail.substringBefore("@"),
                toUserId = toUserId,
                amount = amount,
                groupId = groupId,
                groupName = groupName
            )
            
            // Save to Firebase under recipient's notifications
            settlementNotificationsRef
                .child(toUserId)
                .child(notificationId)
                .setValue(notificationToMap(notification))
                .await()
            
            android.util.Log.d("FriendsRepo", "Settlement notification sent to $toUserId for $amount")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FriendsRepo", "Failed to send settlement notification: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Listen for incoming settlement notifications (real-time)
     */
    fun getSettlementNotifications(): Flow<List<SettlementNotification>> = callbackFlow {
        val userId = currentUserId ?: run {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val notifications = mutableListOf<SettlementNotification>()
                snapshot.children.forEach { child ->
                    val notification = SettlementNotification(
                        id = child.key ?: "",
                        fromUserId = child.child("fromUserId").getValue(String::class.java) ?: "",
                        fromDisplayName = child.child("fromDisplayName").getValue(String::class.java),
                        toUserId = child.child("toUserId").getValue(String::class.java) ?: "",
                        amount = child.child("amount").getValue(Double::class.java) ?: 0.0,
                        groupId = child.child("groupId").getValue(String::class.java) ?: "",
                        groupName = child.child("groupName").getValue(String::class.java) ?: "",
                        createdAt = child.child("createdAt").getValue(Long::class.java) ?: 0L,
                        isRead = child.child("isRead").getValue(Boolean::class.java) ?: false
                    )
                    if (!notification.isRead) {
                        notifications.add(notification)
                    }
                }
                android.util.Log.d("FriendsRepo", "Received ${notifications.size} unread settlement notifications")
                trySend(notifications)
            }
            
            override fun onCancelled(error: DatabaseError) {
                android.util.Log.e("FriendsRepo", "Error getting settlement notifications: ${error.message}")
                trySend(emptyList())
            }
        }
        
        settlementNotificationsRef.child(userId).addValueEventListener(listener)
        awaitClose { settlementNotificationsRef.child(userId).removeEventListener(listener) }
    }
    
    /**
     * Mark a settlement notification as read/processed
     */
    suspend fun markSettlementAsProcessed(notificationId: String): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(Exception("Not logged in"))
        
        return try {
            settlementNotificationsRef
                .child(userId)
                .child(notificationId)
                .child("isRead")
                .setValue(true)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun notificationToMap(notification: SettlementNotification): Map<String, Any?> = mapOf(
        "id" to notification.id,
        "fromUserId" to notification.fromUserId,
        "fromDisplayName" to notification.fromDisplayName,
        "toUserId" to notification.toUserId,
        "amount" to notification.amount,
        "groupId" to notification.groupId,
        "groupName" to notification.groupName,
        "createdAt" to notification.createdAt,
        "isRead" to notification.isRead
    )
    
    // ==================== CONVERSION HELPERS ====================
    
    private fun requestToMap(request: FriendRequest): Map<String, Any?> = mapOf(
        "id" to request.id,
        "fromUserId" to request.fromUserId,
        "fromEmail" to request.fromEmail,
        "fromDisplayName" to request.fromDisplayName,
        "toUserId" to request.toUserId,
        "toEmail" to request.toEmail,
        "status" to request.status.name,
        "createdAt" to request.createdAt
    )
    
    private fun mapToRequest(snapshot: DataSnapshot): FriendRequest = FriendRequest(
        id = snapshot.key ?: "",
        fromUserId = snapshot.child("fromUserId").getValue(String::class.java) ?: "",
        fromEmail = snapshot.child("fromEmail").getValue(String::class.java) ?: "",
        fromDisplayName = snapshot.child("fromDisplayName").getValue(String::class.java),
        toUserId = snapshot.child("toUserId").getValue(String::class.java) ?: "",
        toEmail = snapshot.child("toEmail").getValue(String::class.java) ?: "",
        status = try {
            RequestStatus.valueOf(snapshot.child("status").getValue(String::class.java) ?: "PENDING")
        } catch (e: Exception) { RequestStatus.PENDING },
        createdAt = snapshot.child("createdAt").getValue(Long::class.java) ?: System.currentTimeMillis()
    )
    
    private fun friendToMap(friend: Friend): Map<String, Any?> = mapOf(
        "odiserId" to friend.odiserId,
        "email" to friend.email,
        "displayName" to friend.displayName,
        "addedAt" to friend.addedAt,
        "status" to friend.status.name
    )
}

