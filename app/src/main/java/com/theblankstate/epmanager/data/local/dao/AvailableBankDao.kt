package com.theblankstate.epmanager.data.local.dao

import androidx.room.*
import com.theblankstate.epmanager.data.model.AvailableBank
import com.theblankstate.epmanager.data.model.AvailableBankSource
import kotlinx.coroutines.flow.Flow

@Dao
interface AvailableBankDao {
    
    // ========== QUERIES ==========
    
    /**
     * Get all available banks ordered by transaction count (most used first)
     */
    @Query("SELECT * FROM available_banks WHERE isActive = 1 ORDER BY transactionCount DESC")
    fun getAllActiveBanks(): Flow<List<AvailableBank>>
    
    /**
     * Get all banks including inactive
     */
    @Query("SELECT * FROM available_banks ORDER BY createdAt DESC")
    fun getAllBanks(): Flow<List<AvailableBank>>
    
    /**
     * Get banks by source type
     */
    @Query("SELECT * FROM available_banks WHERE source = :source AND isActive = 1 ORDER BY transactionCount DESC")
    fun getBanksBySource(source: AvailableBankSource): Flow<List<AvailableBank>>
    
    /**
     * Get scanned banks (from known registry)
     */
    @Query("SELECT * FROM available_banks WHERE source = 'SCANNED' AND isActive = 1 ORDER BY transactionCount DESC")
    fun getScannedBanks(): Flow<List<AvailableBank>>
    
    /**
     * Get custom banks (user-added)
     */
    @Query("SELECT * FROM available_banks WHERE source = 'CUSTOM' AND isActive = 1 ORDER BY createdAt DESC")
    fun getCustomBanks(): Flow<List<AvailableBank>>
    
    /**
     * Get unknown sender banks
     */
    @Query("SELECT * FROM available_banks WHERE source = 'UNKNOWN_SENDER' AND isActive = 1 ORDER BY transactionCount DESC")
    fun getUnknownSenderBanks(): Flow<List<AvailableBank>>
    
    /**
     * Get bank by ID
     */
    @Query("SELECT * FROM available_banks WHERE id = :id")
    suspend fun getBankById(id: String): AvailableBank?
    
    /**
     * Get bank by code
     */
    @Query("SELECT * FROM available_banks WHERE bankCode = :bankCode LIMIT 1")
    suspend fun getBankByCode(bankCode: String): AvailableBank?
    
    /**
     * Get bank by sender ID (search in comma-separated list)
     */
    @Query("SELECT * FROM available_banks WHERE senderIds LIKE '%' || :senderId || '%' AND isActive = 1 LIMIT 1")
    suspend fun getBankBySenderId(senderId: String): AvailableBank?
    
    /**
     * Get bank by name (case insensitive)
     */
    @Query("SELECT * FROM available_banks WHERE LOWER(bankName) = LOWER(:bankName) LIMIT 1")
    suspend fun getBankByName(bankName: String): AvailableBank?
    
    /**
     * Check if any banks exist
     */
    @Query("SELECT COUNT(*) FROM available_banks WHERE isActive = 1")
    suspend fun getActiveCount(): Int
    
    /**
     * Get count by source
     */
    @Query("SELECT COUNT(*) FROM available_banks WHERE source = :source AND isActive = 1")
    suspend fun getCountBySource(source: AvailableBankSource): Int
    
    // ========== INSERT/UPDATE ==========
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bank: AvailableBank)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(banks: List<AvailableBank>)
    
    @Update
    suspend fun update(bank: AvailableBank)
    
    /**
     * Update usage stats when bank is used for SMS parsing
     */
    @Query("UPDATE available_banks SET usageCount = usageCount + 1, lastUsedAt = :timestamp WHERE id = :id")
    suspend fun incrementUsage(id: String, timestamp: Long = System.currentTimeMillis())
    
    /**
     * Update transaction count after discovery scan
     */
    @Query("UPDATE available_banks SET transactionCount = :count, lastTransactionDate = :lastDate, updatedAt = :now WHERE id = :id")
    suspend fun updateTransactionStats(id: String, count: Int, lastDate: Long, now: Long = System.currentTimeMillis())
    
    // ========== DELETE ==========
    
    @Delete
    suspend fun delete(bank: AvailableBank)
    
    @Query("DELETE FROM available_banks WHERE id = :id")
    suspend fun deleteById(id: String)
    
    /**
     * Delete all banks from a specific source
     */
    @Query("DELETE FROM available_banks WHERE source = :source")
    suspend fun deleteBySource(source: AvailableBankSource)
    
    /**
     * Clear all scanned banks (to refresh from new scan)
     */
    @Query("DELETE FROM available_banks WHERE source = 'SCANNED' OR source = 'UNKNOWN_SENDER'")
    suspend fun clearScannedBanks()
}
