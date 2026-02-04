package com.theblankstate.epmanager.data.local.dao

import androidx.room.*
import com.theblankstate.epmanager.data.model.PendingSms
import com.theblankstate.epmanager.data.model.PendingSmsStatus
import com.theblankstate.epmanager.data.model.SmsTemplate
import kotlinx.coroutines.flow.Flow

@Dao
interface SmsTemplateDao {
    
    // ========== SMS TEMPLATES ==========
    
    @Query("SELECT * FROM sms_templates WHERE isActive = 1 ORDER BY usageCount DESC")
    fun getAllActiveTemplates(): Flow<List<SmsTemplate>>
    
    @Query("SELECT * FROM sms_templates ORDER BY createdAt DESC")
    fun getAllTemplates(): Flow<List<SmsTemplate>>
    
    @Query("SELECT * FROM sms_templates WHERE id = :id")
    suspend fun getTemplateById(id: String): SmsTemplate?
    
    @Query("SELECT * FROM sms_templates WHERE senderIds LIKE '%' || :senderId || '%' AND isActive = 1 LIMIT 1")
    suspend fun getTemplateBySenderId(senderId: String): SmsTemplate?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: SmsTemplate)
    
    @Update
    suspend fun updateTemplate(template: SmsTemplate)
    
    @Delete
    suspend fun deleteTemplate(template: SmsTemplate)
    
    @Query("UPDATE sms_templates SET usageCount = usageCount + 1, lastUsedAt = :timestamp WHERE id = :id")
    suspend fun incrementUsage(id: String, timestamp: Long = System.currentTimeMillis())
    
    @Query("SELECT COUNT(*) FROM sms_templates")
    suspend fun getTemplateCount(): Int
    
    @Query("SELECT * FROM sms_templates WHERE LOWER(bankName) = LOWER(:bankName) LIMIT 1")
    suspend fun getTemplateByBankName(bankName: String): SmsTemplate?
    
    // ========== PENDING SMS ==========
    
    @Query("SELECT * FROM pending_sms WHERE status = :status ORDER BY receivedAt DESC")
    fun getPendingSmsByStatus(status: PendingSmsStatus): Flow<List<PendingSms>>
    
    @Query("SELECT * FROM pending_sms WHERE status = 'PENDING' ORDER BY receivedAt DESC")
    fun getAllPendingSms(): Flow<List<PendingSms>>
    
    @Query("SELECT * FROM pending_sms WHERE id = :id")
    suspend fun getPendingSmsById(id: String): PendingSms?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPendingSms(pendingSms: PendingSms)
    
    @Update
    suspend fun updatePendingSms(pendingSms: PendingSms)
    
    @Delete
    suspend fun deletePendingSms(pendingSms: PendingSms)
    
    @Query("DELETE FROM pending_sms WHERE status != 'PENDING'")
    suspend fun clearProcessedSms()
    
    @Query("SELECT COUNT(*) FROM pending_sms WHERE status = 'PENDING'")
    fun getPendingCount(): Flow<Int>
}
