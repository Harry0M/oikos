package com.theblankstate.epmanager.data.repository

import com.theblankstate.epmanager.data.local.dao.AvailableBankDao
import com.theblankstate.epmanager.data.model.AvailableBank
import com.theblankstate.epmanager.data.model.AvailableBankSource
import com.theblankstate.epmanager.data.model.BankRegistry
import com.theblankstate.epmanager.sms.BankDiscoveryResult
import com.theblankstate.epmanager.sms.DiscoveredBank
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing user's available banks.
 * 
 * This replaces the in-memory BankDiscoveryResult with a persistent solution.
 * Banks are stored per-device and persist across app restarts.
 */
@Singleton
class AvailableBankRepository @Inject constructor(
    private val availableBankDao: AvailableBankDao
) {
    
    // ========== QUERIES ==========
    
    /**
     * Get all active available banks for this device
     */
    fun getAllBanks(): Flow<List<AvailableBank>> = 
        availableBankDao.getAllActiveBanks()
    
    /**
     * Get banks discovered from SMS scan (matched to known registry)
     */
    fun getScannedBanks(): Flow<List<AvailableBank>> = 
        availableBankDao.getScannedBanks()
    
    /**
     * Get custom banks (manually added by user)
     */
    fun getCustomBanks(): Flow<List<AvailableBank>> = 
        availableBankDao.getCustomBanks()
    
    /**
     * Get unknown sender banks (detected but not matched to registry)
     */
    fun getUnknownSenderBanks(): Flow<List<AvailableBank>> = 
        availableBankDao.getUnknownSenderBanks()
    
    /**
     * Get linkable banks for account linking (SCANNED + CUSTOM only)
     * Excludes UNKNOWN_SENDER entries which are individual sender IDs
     */
    fun getLinkableBanks(): Flow<List<AvailableBank>> = 
        availableBankDao.getLinkableBanks()
    
    /**
     * Get bank by ID
     */
    suspend fun getBankById(id: String): AvailableBank? = 
        availableBankDao.getBankById(id)
    
    /**
     * Get bank by code
     */
    suspend fun getBankByCode(bankCode: String): AvailableBank? = 
        availableBankDao.getBankByCode(bankCode)
    
    /**
     * Get bank that handles the given sender ID
     */
    suspend fun getBankBySenderId(senderId: String): AvailableBank? = 
        availableBankDao.getBankBySenderId(senderId)
    
    /**
     * Check if any banks have been discovered
     */
    suspend fun hasAvailableBanks(): Boolean = 
        availableBankDao.getActiveCount() > 0
    
    // ========== SAVE DISCOVERY RESULTS ==========
    
    /**
     * Save discovered banks from a scan result.
     * This will update existing banks or insert new ones.
     * 
     * @param result The BankDiscoveryResult from BankDiscoveryScanner
     * @param clearOldScannedBanks If true, removes previously scanned banks first
     */
    suspend fun saveDiscoveredBanks(result: BankDiscoveryResult, clearOldScannedBanks: Boolean = false) {
        if (clearOldScannedBanks) {
            availableBankDao.clearScannedBanks()
        }
        
        // Process detected banks (known banks from registry)
        for (discoveredBank in result.detectedBanks) {
            saveOrUpdateDiscoveredBank(discoveredBank, AvailableBankSource.SCANNED)
        }
        
        // Process unknown senders
        for (unknownSender in result.unknownSenders) {
            saveOrUpdateDiscoveredBank(unknownSender, AvailableBankSource.UNKNOWN_SENDER)
        }
    }
    
    /**
     * Save or update a single discovered bank
     */
    private suspend fun saveOrUpdateDiscoveredBank(
        discovered: DiscoveredBank,
        source: AvailableBankSource
    ) {
        // Check if we already have this bank by code
        val bankCode = discovered.bankInfo?.code ?: discovered.senderId.uppercase().take(10)
        val existing = availableBankDao.getBankByCode(bankCode)
        
        if (existing != null) {
            // Update existing bank
            val updatedSenderIds = mergeSenderIds(existing.senderIds, discovered.senderIds)
            val updated = existing.copy(
                senderIds = updatedSenderIds,
                transactionCount = maxOf(existing.transactionCount, discovered.transactionCount),
                lastTransactionDate = maxOf(existing.lastTransactionDate, discovered.lastTransactionDate),
                sampleSms = discovered.sampleSms ?: existing.sampleSms,
                updatedAt = System.currentTimeMillis()
            )
            availableBankDao.update(updated)
        } else {
            // Insert new bank
            val bank = AvailableBank.fromDiscoveredBank(
                bankCode = bankCode,
                bankName = discovered.bankName,
                senderIds = discovered.senderIds,
                color = discovered.bankInfo?.color ?: 0xFF3B82F6,
                transactionCount = discovered.transactionCount,
                lastTransactionDate = discovered.lastTransactionDate,
                sampleSms = discovered.sampleSms,
                isKnownBank = discovered.isKnownBank
            ).copy(source = source)
            availableBankDao.insert(bank)
        }
    }
    
    /**
     * Merge sender IDs, avoiding duplicates
     */
    private fun mergeSenderIds(existing: String, newIds: List<String>): String {
        val existingSet = existing.split(",").map { it.trim().uppercase() }.filter { it.isNotEmpty() }.toMutableSet()
        newIds.forEach { existingSet.add(it.trim().uppercase()) }
        return existingSet.joinToString(",")
    }
    
    // ========== CUSTOM BANK OPERATIONS ==========
    
    /**
     * Add a custom bank (user-created)
     */
    suspend fun addCustomBank(bankName: String, senderIds: String, linkedAccountId: String? = null): AvailableBank {
        // Check if bank with same name exists
        val existing = availableBankDao.getBankByName(bankName)
        
        if (existing != null) {
            // Merge sender IDs into existing
            val mergedSenderIds = mergeSenderIds(existing.senderIds, senderIds.split(","))
            val updated = existing.copy(
                senderIds = mergedSenderIds,
                linkedAccountId = linkedAccountId ?: existing.linkedAccountId,
                updatedAt = System.currentTimeMillis()
            )
            availableBankDao.update(updated)
            return updated
        } else {
            // Create new custom bank
            val bank = AvailableBank.createCustom(
                bankName = bankName,
                senderIds = senderIds,
                linkedAccountId = linkedAccountId
            )
            availableBankDao.insert(bank)
            return bank
        }
    }
    
    /**
     * Update an existing bank
     */
    suspend fun updateBank(
        bankId: String,
        bankName: String? = null,
        senderIds: String? = null,
        linkedAccountId: String? = null
    ): Boolean {
        val existing = availableBankDao.getBankById(bankId) ?: return false
        
        val updated = existing.copy(
            bankName = bankName ?: existing.bankName,
            senderIds = senderIds ?: existing.senderIds,
            linkedAccountId = linkedAccountId ?: existing.linkedAccountId,
            updatedAt = System.currentTimeMillis()
        )
        availableBankDao.update(updated)
        return true
    }
    
    /**
     * Add a sender ID to an existing bank
     */
    suspend fun addSenderIdToBank(bankId: String, senderId: String): Boolean {
        val existing = availableBankDao.getBankById(bankId) ?: return false
        
        val mergedSenderIds = mergeSenderIds(existing.senderIds, listOf(senderId))
        val updated = existing.copy(
            senderIds = mergedSenderIds,
            updatedAt = System.currentTimeMillis()
        )
        availableBankDao.update(updated)
        return true
    }
    
    /**
     * Delete a bank
     */
    suspend fun deleteBank(bank: AvailableBank) {
        availableBankDao.delete(bank)
    }
    
    /**
     * Delete a bank by ID
     */
    suspend fun deleteBankById(bankId: String) {
        availableBankDao.deleteById(bankId)
    }
    
    // ========== USAGE TRACKING ==========
    
    /**
     * Increment usage count when bank is used for SMS parsing
     */
    suspend fun incrementUsage(bankId: String) {
        availableBankDao.incrementUsage(bankId)
    }
    
    // ========== UTILITY ==========
    
    /**
     * Convert available banks to a format compatible with existing BankDiscoveryResult
     * for UI compatibility
     */
    suspend fun toBankDiscoveryResult(): BankDiscoveryResult {
        val allBanks = availableBankDao.getAllActiveBanks().first()
        
        val detectedBanks = allBanks
            .filter { it.source == AvailableBankSource.SCANNED }
            .map { it.toDiscoveredBank() }
        
        val unknownSenders = allBanks
            .filter { it.source == AvailableBankSource.UNKNOWN_SENDER }
            .map { it.toDiscoveredBank() }
        
        return BankDiscoveryResult(
            scannedCount = allBanks.sumOf { it.transactionCount },
            detectedBanks = detectedBanks,
            unknownSenders = unknownSenders,
            isComplete = true
        )
    }
    
    /**
     * Extension function to convert AvailableBank to DiscoveredBank for UI compatibility
     */
    private fun AvailableBank.toDiscoveredBank(): DiscoveredBank {
        val bankInfo = if (isKnownBank) BankRegistry.findBankByCode(bankCode) else null
        return DiscoveredBank(
            senderIds = getSenderIdList(),
            senderId = senderIds,
            newSenderIds = emptyList(), // Not applicable for persisted banks
            bankInfo = bankInfo,
            bankName = bankName,
            transactionCount = transactionCount,
            sampleSms = sampleSms,
            lastTransactionDate = lastTransactionDate,
            isKnownBank = isKnownBank,
            hasNewSenderIds = false
        )
    }
}
