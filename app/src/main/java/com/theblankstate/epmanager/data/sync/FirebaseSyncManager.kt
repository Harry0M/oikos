package com.theblankstate.epmanager.data.sync

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.theblankstate.epmanager.data.model.*
import com.theblankstate.epmanager.data.repository.TransactionRepository
import com.theblankstate.epmanager.data.repository.CategoryRepository
import com.theblankstate.epmanager.data.repository.AccountRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseSyncManager @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository
) {
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    
    private val userId: String?
        get() = auth.currentUser?.uid
    
    private fun getUserRef(): DatabaseReference? {
        return userId?.let { database.reference.child("users").child(it) }
    }
    
    /**
     * Backup all local data to Firebase
     */
    suspend fun backupToCloud(): Result<Unit> {
        val userRef = getUserRef() ?: return Result.failure(Exception("Not logged in"))
        
        return try {
            // Backup transactions
            val transactions = transactionRepository.getAllTransactions().first()
            val transactionsMap = transactions.associate { it.id to transactionToMap(it) }
            userRef.child("transactions").setValue(transactionsMap).await()
            
            // Backup categories (custom only)
            val categories = categoryRepository.getAllCategories().first()
                .filter { !it.isDefault }
            val categoriesMap = categories.associate { it.id to categoryToMap(it) }
            userRef.child("categories").setValue(categoriesMap).await()
            
            // Backup accounts (custom only)
            val accounts = accountRepository.getAllAccounts().first()
                .filter { !it.isDefault }
            val accountsMap = accounts.associate { it.id to accountToMap(it) }
            userRef.child("accounts").setValue(accountsMap).await()
            
            // Update last sync time
            userRef.child("lastSync").setValue(System.currentTimeMillis()).await()
            
            // Mark all transactions as synced
            transactions.forEach { 
                transactionRepository.markAsSynced(it.id)
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Restore data from Firebase to local database
     */
    suspend fun restoreFromCloud(): Result<Unit> {
        val userRef = getUserRef() ?: return Result.failure(Exception("Not logged in"))
        
        return try {
            // Restore transactions
            val transactionsSnapshot = userRef.child("transactions").get().await()
            transactionsSnapshot.children.forEach { snapshot ->
                val map = snapshot.value as? Map<*, *>
                if (map != null) {
                    val transaction = mapToTransaction(map)
                    transactionRepository.insertTransaction(transaction)
                }
            }
            
            // Restore custom categories
            val categoriesSnapshot = userRef.child("categories").get().await()
            categoriesSnapshot.children.forEach { snapshot ->
                val map = snapshot.value as? Map<*, *>
                if (map != null) {
                    val category = mapToCategory(map)
                    categoryRepository.insertCategory(category)
                }
            }
            
            // Restore custom accounts
            val accountsSnapshot = userRef.child("accounts").get().await()
            accountsSnapshot.children.forEach { snapshot ->
                val map = snapshot.value as? Map<*, *>
                if (map != null) {
                    val account = mapToAccount(map)
                    accountRepository.insertAccount(account)
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Sync only unsynced transactions
     */
    suspend fun syncUnsyncedTransactions(): Result<Int> {
        val userRef = getUserRef() ?: return Result.failure(Exception("Not logged in"))
        
        return try {
            val unsyncedTransactions = transactionRepository.getUnsyncedTransactions()
            
            unsyncedTransactions.forEach { transaction ->
                userRef.child("transactions").child(transaction.id)
                    .setValue(transactionToMap(transaction)).await()
                transactionRepository.markAsSynced(transaction.id)
            }
            
            Result.success(unsyncedTransactions.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get last sync time
     */
    suspend fun getLastSyncTime(): Long? {
        val userRef = getUserRef() ?: return null
        
        return try {
            val snapshot = userRef.child("lastSync").get().await()
            snapshot.getValue(Long::class.java)
        } catch (e: Exception) {
            null
        }
    }
    
    // Conversion functions
    private fun transactionToMap(transaction: Transaction): Map<String, Any?> = mapOf(
        "id" to transaction.id,
        "amount" to transaction.amount,
        "type" to transaction.type.name,
        "categoryId" to transaction.categoryId,
        "accountId" to transaction.accountId,
        "date" to transaction.date,
        "note" to transaction.note,
        "isRecurring" to transaction.isRecurring,
        "recurringId" to transaction.recurringId,
        "createdAt" to transaction.createdAt,
        "updatedAt" to transaction.updatedAt
    )
    
    private fun mapToTransaction(map: Map<*, *>): Transaction = Transaction(
        id = map["id"] as? String ?: "",
        amount = (map["amount"] as? Number)?.toDouble() ?: 0.0,
        type = try { 
            TransactionType.valueOf(map["type"] as? String ?: "EXPENSE") 
        } catch (e: Exception) { 
            TransactionType.EXPENSE 
        },
        categoryId = map["categoryId"] as? String,
        accountId = map["accountId"] as? String,
        date = (map["date"] as? Number)?.toLong() ?: System.currentTimeMillis(),
        note = map["note"] as? String,
        isRecurring = map["isRecurring"] as? Boolean ?: false,
        recurringId = map["recurringId"] as? String,
        createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
        updatedAt = (map["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
        isSynced = true
    )
    
    private fun categoryToMap(category: Category): Map<String, Any?> = mapOf(
        "id" to category.id,
        "name" to category.name,
        "icon" to category.icon,
        "color" to category.color,
        "type" to category.type.name,
        "isDefault" to category.isDefault,
        "createdAt" to category.createdAt
    )
    
    private fun mapToCategory(map: Map<*, *>): Category = Category(
        id = map["id"] as? String ?: "",
        name = map["name"] as? String ?: "",
        icon = map["icon"] as? String ?: "",
        color = (map["color"] as? Number)?.toLong() ?: 0xFF9CA3AF,
        type = try { 
            CategoryType.valueOf(map["type"] as? String ?: "EXPENSE") 
        } catch (e: Exception) { 
            CategoryType.EXPENSE 
        },
        isDefault = map["isDefault"] as? Boolean ?: false,
        createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
    )
    
    private fun accountToMap(account: Account): Map<String, Any?> = mapOf(
        "id" to account.id,
        "name" to account.name,
        "type" to account.type.name,
        "icon" to account.icon,
        "color" to account.color,
        "balance" to account.balance,
        "isDefault" to account.isDefault,
        "createdAt" to account.createdAt
    )
    
    private fun mapToAccount(map: Map<*, *>): Account = Account(
        id = map["id"] as? String ?: "",
        name = map["name"] as? String ?: "",
        type = try { 
            AccountType.valueOf(map["type"] as? String ?: "CASH") 
        } catch (e: Exception) { 
            AccountType.CASH 
        },
        icon = map["icon"] as? String ?: "",
        color = (map["color"] as? Number)?.toLong() ?: 0xFF22C55E,
        balance = (map["balance"] as? Number)?.toDouble() ?: 0.0,
        isDefault = map["isDefault"] as? Boolean ?: false,
        createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
    )
}
