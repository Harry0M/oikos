package com.theblankstate.epmanager.data.local.dao

import androidx.room.*
import com.theblankstate.epmanager.data.model.Friend
import com.theblankstate.epmanager.data.model.FriendRequest
import com.theblankstate.epmanager.data.model.FriendStatus
import com.theblankstate.epmanager.data.model.RequestStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface FriendDao {
    
    // Friends
    @Query("SELECT * FROM friends WHERE status = :status ORDER BY displayName ASC")
    fun getFriendsByStatus(status: FriendStatus = FriendStatus.ACCEPTED): Flow<List<Friend>>
    
    @Query("SELECT * FROM friends WHERE status = 'ACCEPTED' ORDER BY displayName ASC")
    fun getAcceptedFriends(): Flow<List<Friend>>
    
    @Query("SELECT * FROM friends WHERE odiserId = :userId")
    suspend fun getFriendById(userId: String): Friend?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFriend(friend: Friend)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFriends(friends: List<Friend>)
    
    @Update
    suspend fun updateFriend(friend: Friend)
    
    @Delete
    suspend fun deleteFriend(friend: Friend)
    
    @Query("DELETE FROM friends WHERE odiserId = :userId")
    suspend fun deleteFriendById(userId: String)
    
    @Query("DELETE FROM friends")
    suspend fun deleteAllFriends()
    
    // Friend Requests
    @Query("SELECT * FROM friend_requests WHERE status = :status ORDER BY createdAt DESC")
    fun getRequestsByStatus(status: RequestStatus = RequestStatus.PENDING): Flow<List<FriendRequest>>
    
    @Query("SELECT * FROM friend_requests WHERE toUserId = :userId AND status = 'PENDING' ORDER BY createdAt DESC")
    fun getIncomingRequests(userId: String): Flow<List<FriendRequest>>
    
    @Query("SELECT * FROM friend_requests WHERE fromUserId = :userId AND status = 'PENDING' ORDER BY createdAt DESC")
    fun getSentRequests(userId: String): Flow<List<FriendRequest>>
    
    @Query("SELECT * FROM friend_requests WHERE id = :requestId")
    suspend fun getRequestById(requestId: String): FriendRequest?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRequest(request: FriendRequest)
    
    @Update
    suspend fun updateRequest(request: FriendRequest)
    
    @Delete
    suspend fun deleteRequest(request: FriendRequest)
    
    @Query("DELETE FROM friend_requests WHERE id = :requestId")
    suspend fun deleteRequestById(requestId: String)
    
    @Query("DELETE FROM friend_requests")
    suspend fun deleteAllRequests()
    
    // Count queries for badges
    @Query("SELECT COUNT(*) FROM friend_requests WHERE toUserId = :userId AND status = 'PENDING'")
    fun getPendingRequestCount(userId: String): Flow<Int>
    
    @Query("SELECT COUNT(*) FROM friends WHERE status = 'ACCEPTED'")
    fun getFriendCount(): Flow<Int>
}
