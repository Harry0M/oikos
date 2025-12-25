package com.theblankstate.epmanager.data.repository

import com.theblankstate.epmanager.data.local.dao.TermsAcceptanceDao
import com.theblankstate.epmanager.data.model.TermsAcceptance
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TermsRepository @Inject constructor(
    private val termsAcceptanceDao: TermsAcceptanceDao
) {
    companion object {
        const val CURRENT_TERMS_VERSION = "1.0"
    }
    
    suspend fun hasAcceptedCurrentTerms(): Boolean {
        return termsAcceptanceDao.hasAcceptedVersion(CURRENT_TERMS_VERSION)
    }
    
    suspend fun getLatestAcceptance(): TermsAcceptance? {
        return termsAcceptanceDao.getLatestAcceptance()
    }
    
    fun getAllAcceptances(): Flow<List<TermsAcceptance>> {
        return termsAcceptanceDao.getAllAcceptances()
    }
    
    suspend fun acceptTerms(userId: String? = null, deviceId: String? = null) {
        val acceptance = TermsAcceptance(
            termsVersion = CURRENT_TERMS_VERSION,
            userId = userId,
            deviceId = deviceId
        )
        termsAcceptanceDao.insertAcceptance(acceptance)
    }
    
    suspend fun deleteAllAcceptances() {
        termsAcceptanceDao.deleteAllAcceptances()
    }
}
