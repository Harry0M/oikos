package com.theblankstate.epmanager.data.repository

import com.theblankstate.epmanager.data.local.dao.DebtDao
import com.theblankstate.epmanager.data.model.Debt
import com.theblankstate.epmanager.data.model.DebtPayment
import com.theblankstate.epmanager.data.model.DebtType
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DebtRepository @Inject constructor(
    private val debtDao: DebtDao
) {
    // Debt queries
    fun getActiveDebts(): Flow<List<Debt>> = debtDao.getActiveDebts()
    
    fun getActiveDebtsByType(type: DebtType): Flow<List<Debt>> = 
        debtDao.getActiveDebtsByType(type)
    
    fun getSettledDebts(): Flow<List<Debt>> = debtDao.getSettledDebts()
    
    fun getAllDebts(): Flow<List<Debt>> = debtDao.getAllDebts()
    
    suspend fun getDebtById(id: String): Debt? = debtDao.getDebtById(id)
    
    fun getDebtsByFriend(friendId: String): Flow<List<Debt>> = 
        debtDao.getDebtsByFriend(friendId)
    
    fun getTotalDebtOwed(): Flow<Double?> = 
        debtDao.getTotalRemainingByType(DebtType.DEBT)
    
    fun getTotalCreditOwed(): Flow<Double?> = 
        debtDao.getTotalRemainingByType(DebtType.CREDIT)
    
    // Debt operations
    suspend fun createDebt(debt: Debt) {
        debtDao.insertDebt(debt)
    }
    
    suspend fun updateDebt(debt: Debt) {
        debtDao.updateDebt(debt.copy(updatedAt = System.currentTimeMillis()))
    }
    
    suspend fun deleteDebt(debt: Debt) {
        debtDao.deleteDebt(debt)
    }
    
    // Payment operations
    fun getPaymentsForDebt(debtId: String): Flow<List<DebtPayment>> =
        debtDao.getPaymentsForDebt(debtId)
    
    suspend fun addPayment(debtId: String, amount: Double, transactionId: String? = null, note: String? = null) {
        val debt = debtDao.getDebtById(debtId) ?: return
        
        // Record the payment
        val payment = DebtPayment(
            debtId = debtId,
            amount = amount,
            linkedTransactionId = transactionId,
            note = note
        )
        debtDao.insertPayment(payment)
        
        // Update debt remaining amount
        val newRemaining = (debt.remainingAmount - amount).coerceAtLeast(0.0)
        val isSettled = newRemaining <= 0
        
        debtDao.updateDebtBalance(debtId, newRemaining, isSettled)
    }
    
    suspend fun deletePayment(payment: DebtPayment) {
        // Re-add amount to debt
        val debt = debtDao.getDebtById(payment.debtId) ?: return
        val newRemaining = (debt.remainingAmount + payment.amount).coerceAtMost(debt.totalAmount)
        
        debtDao.deletePayment(payment)
        debtDao.updateDebtBalance(payment.debtId, newRemaining, false)
    }
    
    suspend fun settleDebt(debtId: String) {
        debtDao.updateDebtBalance(debtId, 0.0, true)
    }
}
