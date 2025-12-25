package com.theblankstate.epmanager.data.local.dao

import androidx.room.*
import com.theblankstate.epmanager.data.model.TermsAcceptance
import kotlinx.coroutines.flow.Flow

@Dao
interface TermsAcceptanceDao {
    
    @Query("SELECT * FROM terms_acceptances ORDER BY acceptedAt DESC LIMIT 1")
    suspend fun getLatestAcceptance(): TermsAcceptance?
    
    @Query("SELECT * FROM terms_acceptances ORDER BY acceptedAt DESC")
    fun getAllAcceptances(): Flow<List<TermsAcceptance>>
    
    @Query("SELECT COUNT(*) > 0 FROM terms_acceptances WHERE termsVersion = :version")
    suspend fun hasAcceptedVersion(version: String): Boolean
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAcceptance(acceptance: TermsAcceptance)
    
    @Query("DELETE FROM terms_acceptances")
    suspend fun deleteAllAcceptances()
}
