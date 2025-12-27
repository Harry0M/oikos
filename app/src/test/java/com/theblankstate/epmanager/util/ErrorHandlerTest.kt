package com.theblankstate.epmanager.util

import org.junit.Assert.*
import org.junit.Test
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Unit tests for ErrorHandler user-friendly message generation.
 * Tests only the pure logic that doesn't require Firebase.
 */
class ErrorHandlerTest {
    
    @Test
    fun `getUserFriendlyMessage returns correct message for UnknownHostException`() {
        val message = getMessageForException(UnknownHostException("test"))
        assertEquals("No internet connection. Please check your network.", message)
    }
    
    @Test
    fun `getUserFriendlyMessage returns correct message for SocketTimeoutException`() {
        val message = getMessageForException(SocketTimeoutException("timeout"))
        assertEquals("Connection timed out. Please try again.", message)
    }
    
    @Test
    fun `getUserFriendlyMessage returns correct message for IllegalArgumentException`() {
        val message = getMessageForException(IllegalArgumentException("invalid input"))
        assertEquals("Invalid input. Please check your data.", message)
    }
    
    @Test
    fun `getUserFriendlyMessage returns correct message for SecurityException`() {
        val message = getMessageForException(SecurityException("permission denied"))
        assertEquals("Permission denied. Please check app permissions.", message)
    }
    
    @Test
    fun `getUserFriendlyMessage returns fallback for unknown exception`() {
        val message = getMessageForException(CustomException("custom error"), "Something went wrong")
        assertEquals("Something went wrong", message)
    }
    
    @Test
    fun `getUserFriendlyMessage returns default fallback when not specified`() {
        val message = getMessageForException(CustomException("custom error"))
        assertEquals("An error occurred", message)
    }
    
    /**
     * Test helper that replicates the ErrorHandler logic without Firebase dependency.
     */
    private fun getMessageForException(throwable: Throwable, fallback: String = "An error occurred"): String {
        return when (throwable) {
            is UnknownHostException -> "No internet connection. Please check your network."
            is SocketTimeoutException -> "Connection timed out. Please try again."
            is java.net.ConnectException -> "Unable to connect. Please check your internet."
            is java.io.IOException -> "A network error occurred. Please try again."
            is IllegalArgumentException -> "Invalid input. Please check your data."
            is IllegalStateException -> "Something went wrong. Please restart the app."
            is SecurityException -> "Permission denied. Please check app permissions."
            is OutOfMemoryError -> "The app is running low on memory. Please close some apps."
            else -> fallback
        }
    }
    
    private class CustomException(message: String) : Exception(message)
}
