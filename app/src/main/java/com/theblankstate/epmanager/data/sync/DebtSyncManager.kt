package com.theblankstate.epmanager.data.sync

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.theblankstate.epmanager.data.model.Debt
import com.theblankstate.epmanager.data.model.DebtType
import com.theblankstate.epmanager.data.repository.DebtRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Notification sent when a debt is created/updated for a linked friend
 */
data class DebtNotification(
    val id: String = "",
    val originalDebtId: String = "",
    val fromUserId: String = "",
    val fromDisplayName: String = "",
    val debtType: String = "", // "DEBT" or "CREDIT" (from sender's perspective)
    val personName: String = "",
    val totalAmount: Double = 0.0,
    val remainingAmount: Double = 0.0,
    val dueDate: Long? = null,
    val notes: String? = null,
    val isProcessed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Manages synchronization of debt entries between linked friends via Firebase
 */
@Singleton
class DebtSyncManager @Inject constructor(
    private val debtRepository: DebtRepository
) {
    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private var debtListener: ChildEventListener? = null
    
    companion object {
        private const val TAG = "DebtSyncManager"
    }
    
    /**
     * Start listening for incoming debt notifications
     */
    fun startListening() {
        val userId = auth.currentUser?.uid ?: return
        
        Log.d(TAG, "Starting debt notification listener for user: $userId")
        
        val notificationsRef = database.reference
            .child("users")
            .child(userId)
            .child("debt_notifications")
        
        debtListener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                scope.launch {
                    processNotification(snapshot)
                }
            }
            
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                // Handle updates if needed
            }
            
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Listener cancelled: ${error.message}")
            }
        }
        
        notificationsRef.addChildEventListener(debtListener!!)
    }
    
    /**
     * Stop listening for notifications
     */
    fun stopListening() {
        val userId = auth.currentUser?.uid ?: return
        debtListener?.let {
            database.reference
                .child("users")
                .child(userId)
                .child("debt_notifications")
                .removeEventListener(it)
        }
        debtListener = null
    }
    
    /**
     * Process an incoming debt notification
     */
    private suspend fun processNotification(snapshot: DataSnapshot) {
        try {
            val notification = snapshot.getValue(DebtNotification::class.java) ?: return
            
            if (notification.isProcessed) return
            
            Log.d(TAG, "Processing debt notification: ${notification.totalAmount} from ${notification.fromDisplayName}")
            
            // Create reverse debt entry (their debt becomes my credit and vice versa)
            val reverseType = if (notification.debtType == "DEBT") DebtType.CREDIT else DebtType.DEBT
            
            val debt = Debt(
                type = reverseType,
                personName = notification.fromDisplayName,
                totalAmount = notification.totalAmount,
                remainingAmount = notification.remainingAmount,
                linkedFriendId = notification.fromUserId,
                dueDate = notification.dueDate,
                notes = "Synced from ${notification.fromDisplayName}: ${notification.notes ?: ""}"
            )
            
            debtRepository.createDebt(debt)
            
            // Mark as processed
            snapshot.ref.child("isProcessed").setValue(true)
            
            Log.d(TAG, "Created reverse debt entry: ${debt.type} - ${debt.totalAmount}")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing debt notification: ${e.message}")
        }
    }
    
    /**
     * Send debt notification to a linked friend
     */
    suspend fun syncDebtToFriend(debt: Debt, friendUserId: String, myDisplayName: String) {
        val myUserId = auth.currentUser?.uid ?: return
        
        try {
            val notification = DebtNotification(
                id = "${debt.id}_notification",
                originalDebtId = debt.id,
                fromUserId = myUserId,
                fromDisplayName = myDisplayName,
                debtType = debt.type.name,
                personName = debt.personName,
                totalAmount = debt.totalAmount,
                remainingAmount = debt.remainingAmount,
                dueDate = debt.dueDate,
                notes = debt.notes
            )
            
            // Send to friend's notifications
            database.reference
                .child("users")
                .child(friendUserId)
                .child("debt_notifications")
                .child(notification.id)
                .setValue(notification)
                .await()
            
            Log.d(TAG, "Sent debt notification to friend: $friendUserId")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing debt to friend: ${e.message}")
        }
    }
    
    /**
     * Remove/dismiss a synced debt (user disagrees with it)
     */
    suspend fun dismissSyncedDebt(debt: Debt) {
        // Just delete locally - the original user keeps their entry
        debtRepository.deleteDebt(debt)
    }
}
