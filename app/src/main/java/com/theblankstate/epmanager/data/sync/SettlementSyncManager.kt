package com.theblankstate.epmanager.data.sync

import android.content.Context
import android.util.Log
import android.widget.Toast
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
    
    companion object {
        private const val TAG = "SettlementSync"
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
                // The notification is sent TO me, meaning someone settled with me
                // If I was owed money, this is INCOME
                // If I owed them, this is already handled on my side when I initiated settle
                
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
                
                // Show toast notification
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "💰 ${notification.fromDisplayName} paid you ₹${String.format("%.2f", notification.amount)}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } else {
                Log.w(TAG, "No default account found, skipping transaction recording")
            }
            
            // Mark as processed
            friendsRepository.markSettlementAsProcessed(notification.id)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing settlement: ${e.message}")
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
