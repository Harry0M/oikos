package com.theblankstate.epmanager.data.local.dao

import androidx.room.*
import com.theblankstate.epmanager.data.model.Account
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    
    @Query("SELECT * FROM accounts ORDER BY isDefault DESC, name ASC")
    fun getAllAccounts(): Flow<List<Account>>
    
    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getAccountById(id: String): Account?
    
    @Query("SELECT * FROM accounts WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultAccount(): Account?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: Account)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccounts(accounts: List<Account>)
    
    @Update
    suspend fun updateAccount(account: Account)
    
    @Query("UPDATE accounts SET balance = balance + :amount WHERE id = :accountId")
    suspend fun updateBalance(accountId: String, amount: Double)
    
    @Delete
    suspend fun deleteAccount(account: Account)
    
    @Query("SELECT COUNT(*) FROM accounts")
    suspend fun getAccountCount(): Int
    
    @Query("SELECT SUM(balance) FROM accounts")
    fun getTotalBalance(): Flow<Double?>
    
    @Query("DELETE FROM accounts")
    suspend fun deleteAllAccounts()
}
