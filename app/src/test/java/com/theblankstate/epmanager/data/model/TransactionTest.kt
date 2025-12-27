package com.theblankstate.epmanager.data.model

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for Transaction data model.
 */
class TransactionTest {
    
    @Test
    fun `Transaction should have unique ID by default`() {
        val transaction1 = Transaction(
            amount = 100.0,
            type = TransactionType.EXPENSE,
            categoryId = "cat1",
            accountId = "acc1",
            date = System.currentTimeMillis()
        )
        
        val transaction2 = Transaction(
            amount = 200.0,
            type = TransactionType.INCOME,
            categoryId = "cat2",
            accountId = "acc2",
            date = System.currentTimeMillis()
        )
        
        assertNotEquals(transaction1.id, transaction2.id)
    }
    
    @Test
    fun `Transaction should have correct type`() {
        val expense = Transaction(
            amount = 50.0,
            type = TransactionType.EXPENSE,
            categoryId = null,
            accountId = null,
            date = System.currentTimeMillis()
        )
        
        val income = Transaction(
            amount = 100.0,
            type = TransactionType.INCOME,
            categoryId = null,
            accountId = null,
            date = System.currentTimeMillis()
        )
        
        assertEquals(TransactionType.EXPENSE, expense.type)
        assertEquals(TransactionType.INCOME, income.type)
    }
    
    @Test
    fun `Transaction should have default values for optional fields`() {
        val transaction = Transaction(
            amount = 100.0,
            type = TransactionType.EXPENSE,
            categoryId = "cat1",
            accountId = "acc1",
            date = System.currentTimeMillis()
        )
        
        assertNull(transaction.note)
        assertFalse(transaction.isRecurring)
        assertNull(transaction.recurringId)
        assertNull(transaction.goalId)
        assertNull(transaction.debtId)
        assertFalse(transaction.isSynced)
        assertNull(transaction.latitude)
        assertNull(transaction.longitude)
    }
    
    @Test
    fun `Transaction createdAt and updatedAt should be set by default`() {
        val before = System.currentTimeMillis()
        val transaction = Transaction(
            amount = 100.0,
            type = TransactionType.EXPENSE,
            categoryId = null,
            accountId = null,
            date = before
        )
        val after = System.currentTimeMillis()
        
        assertTrue(transaction.createdAt >= before)
        assertTrue(transaction.createdAt <= after)
        assertTrue(transaction.updatedAt >= before)
        assertTrue(transaction.updatedAt <= after)
    }
}
