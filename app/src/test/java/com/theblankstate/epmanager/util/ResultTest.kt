package com.theblankstate.epmanager.util

import com.theblankstate.epmanager.util.Result
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for the Result wrapper class.
 */
class ResultTest {
    
    @Test
    fun `Success result should contain data`() {
        val result = Result.Success("test data")
        
        assertTrue(result.isSuccess)
        assertFalse(result.isError)
        assertEquals("test data", result.getOrNull())
        assertNull(result.exceptionOrNull())
    }
    
    @Test
    fun `Error result should contain exception and message`() {
        val exception = IllegalArgumentException("test error")
        val result = Result.Error(exception, "User-friendly message")
        
        assertTrue(result.isError)
        assertFalse(result.isSuccess)
        assertNull(result.getOrNull())
        assertEquals(exception, result.exceptionOrNull())
    }
    
    @Test
    fun `map should transform Success data`() {
        val result = Result.Success(5)
        val mapped = result.map { it * 2 }
        
        assertTrue(mapped.isSuccess)
        assertEquals(10, mapped.getOrNull())
    }
    
    @Test
    fun `map should preserve Error`() {
        val exception = RuntimeException("error")
        val result: Result<Int> = Result.Error(exception, "Error message")
        val mapped = result.map { it * 2 }
        
        assertTrue(mapped.isError)
        assertEquals(exception, mapped.exceptionOrNull())
    }
    
    @Test
    fun `onSuccess should be called for Success`() {
        var called = false
        val result = Result.Success("data")
        
        result.onSuccess { called = true }
        
        assertTrue(called)
    }
    
    @Test
    fun `onSuccess should not be called for Error`() {
        var called = false
        val result: Result<String> = Result.Error(RuntimeException(), "Error")
        
        result.onSuccess { called = true }
        
        assertFalse(called)
    }
    
    @Test
    fun `onError should be called for Error`() {
        var errorType: Throwable? = null
        var errorMessage: String? = null
        val exception = RuntimeException("test")
        val result: Result<String> = Result.Error(exception, "User message")
        
        result.onError { e, msg -> 
            errorType = e
            errorMessage = msg
        }
        
        assertEquals(exception, errorType)
        assertEquals("User message", errorMessage)
    }
    
    @Test
    fun `onError should not be called for Success`() {
        var called = false
        val result = Result.Success("data")
        
        result.onError { _, _ -> called = true }
        
        assertFalse(called)
    }
}
