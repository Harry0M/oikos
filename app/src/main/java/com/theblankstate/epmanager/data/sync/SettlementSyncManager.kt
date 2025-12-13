package com.theblankstate.epmanager.data.sync

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.theblankstate.epmanager.MainActivity
import com.theblankstate.epmanager.R
import com.theblankstate.epmanager.data.model.SettlementNotification
import com.theblankstate.epmanager.data.model.Transaction
import com.theblankstate.epmanager.data.model.TransactionType
import com.theblankstate.epmanager.data.repository.AccountRepository
import com.theblankstate.epmanager.data.repository.FriendsRepository
import com.theblankstate.epmanager.data.repository.TransactionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Listens for incoming settlement notifications from linked friends
 * and automatically records transactions.
 */
@Singleton
class SettlementSyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val friendsRepository: FriendsRepository,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var isListening = false
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    companion object {
        private const val TAG = "SettlementSync"
        private const val CHANNEL_ID = "settlement_notifications"
        private const val CHANNEL_NAME = "Settlement Notifications"
        private var notificationId = 1000
    }
    
    init {
        createNotificationChannel()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications when friends pay you"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Start listening for settlement notifications
     * Call this when user is logged in
     */
    fun startListening() {
        if (isListening) return
        isListening = true
        
        Log.d(TAG, "Starting settlement notification listener")
        
        scope.launch {
            friendsRepository.getSettlementNotifications()
                .catch { e ->
                    Log.e(TAG, "Error listening for settlements: ${e.message}")
                }
                .collectLatest { notifications ->
                    Log.d(TAG, "Received ${notifications.size} settlement notifications")
                    notifications.forEach { notification ->
                        processSettlement(notification)
                    }
                }
        }
    }
    
    /**
     * Process a settlement notification - create transaction and update balance
     */
    private suspend fun processSettlement(notification: SettlementNotification) {
        try {
            Log.d(TAG, "Processing settlement: ${notification.amount} from ${notification.fromDisplayName}")
            
            // Get default account to record the transaction
            val defaultAccount = accountRepository.getDefaultAccountSync()
            
            if (defaultAccount != null) {
                // When friend settles, I receive money - create INCOME transaction
                val transaction = Transaction(
                    amount = notification.amount,
                    type = TransactionType.INCOME,
                    categoryId = "split_payoff",
                    accountId = defaultAccount.id,
                    date = System.currentTimeMillis(),
                    note = "Split Payoff from ${notification.fromDisplayName} (${notification.groupName})"
                )
                
                transactionRepository.insertTransaction(transaction)
                accountRepository.updateBalance(defaultAccount.id, notification.amount)
                
                Log.d(TAG, "Created income transaction for ${notification.amount}")
                
                // Show notification
                showSettlementNotification(notification)
            } else {
                Log.w(TAG, "No default account found, skipping transaction recording")
            }
            
            // Mark as processed
            friendsRepository.markSettlementAsProcessed(notification.id)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing settlement: ${e.message}")
        }
    }
    
    private suspend fun showSettlementNotification(notification: SettlementNotification) {
        withContext(Dispatchers.Main) {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val amountFormatted = String.format("%.2f", notification.amount)
            val senderName = notification.fromDisplayName ?: "Someone"
            
            val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("💰 Payment Received")
                .setContentText("$senderName paid you ₹$amountFormatted")
                .setStyle(NotificationCompat.BigTextStyle()
                    .bigText("$senderName paid you ₹$amountFormatted from ${notification.groupName}. The amount has been added to your account."))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setVibrate(longArrayOf(0, 250, 250, 250))
            
            notificationManager.notify(notificationId++, notificationBuilder.build())
            
            // Also show toast for immediate feedback
            Toast.makeText(
                context,
                "💰 $senderName paid you ₹$amountFormatted",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    
    /**
     * Stop listening for settlements
     * Call this when user logs out
     */
    fun stopListening() {
        isListening = false
        Log.d(TAG, "Stopped settlement notification listener")
    }
}

