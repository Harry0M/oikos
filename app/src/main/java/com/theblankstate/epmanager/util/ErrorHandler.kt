package com.theblankstate.epmanager.util

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Result wrapper for operations that can fail.
 * Provides a type-safe way to handle success and error cases.
 */
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Throwable, val userMessage: String) : Result<Nothing>()
    
    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error
    
    fun getOrNull(): T? = (this as? Success)?.data
    fun exceptionOrNull(): Throwable? = (this as? Error)?.exception
    
    inline fun <R> map(transform: (T) -> R): Result<R> {
        return when (this) {
            is Success -> Success(transform(data))
            is Error -> this
        }
    }
    
    inline fun onSuccess(action: (T) -> Unit): Result<T> {
        if (this is Success) action(data)
        return this
    }
    
    inline fun onError(action: (Throwable, String) -> Unit): Result<T> {
        if (this is Error) action(exception, userMessage)
        return this
    }
}

/**
 * Centralized error handler for the application.
 * Logs errors to Crashlytics and provides user-friendly messages.
 */
@Singleton
class ErrorHandler @Inject constructor() {
    
    private val crashlytics = FirebaseCrashlytics.getInstance()
    
    /**
     * CoroutineExceptionHandler for use with viewModelScope
     */
    val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        handleError(throwable)
    }
    
    /**
     * Execute a suspend function safely and return a Result
     */
    suspend fun <T> safeCall(
        errorMessage: String = "An error occurred",
        block: suspend () -> T
    ): Result<T> {
        return try {
            Result.Success(block())
        } catch (e: CancellationException) {
            throw e // Don't catch cancellation
        } catch (e: Exception) {
            handleError(e)
            Result.Error(e, getUserFriendlyMessage(e, errorMessage))
        }
    }
    
    /**
     * Log an error without user notification
     */
    fun handleError(throwable: Throwable, context: String? = null) {
        // Don't log cancellation exceptions
        if (throwable is CancellationException) return
        
        // Log to Logcat for debugging
        Log.e(TAG, "Error${context?.let { " in $it" } ?: ""}", throwable)
        
        // Log to Crashlytics for production monitoring
        context?.let { crashlytics.setCustomKey("error_context", it) }
        crashlytics.recordException(throwable)
    }
    
    /**
     * Get a user-friendly error message based on exception type
     */
    fun getUserFriendlyMessage(throwable: Throwable, fallback: String = "An error occurred"): String {
        return when (throwable) {
            is java.net.UnknownHostException -> "No internet connection. Please check your network."
            is java.net.SocketTimeoutException -> "Connection timed out. Please try again."
            is java.net.ConnectException -> "Unable to connect. Please check your internet."
            is java.io.IOException -> "A network error occurred. Please try again."
            is IllegalArgumentException -> "Invalid input. Please check your data."
            is IllegalStateException -> "Something went wrong. Please restart the app."
            is SecurityException -> "Permission denied. Please check app permissions."
            is OutOfMemoryError -> "The app is running low on memory. Please close some apps."
            else -> fallback
        }
    }
    
    /**
     * Set user identifier for Crashlytics reports
     */
    fun setUserId(userId: String?) {
        if (userId != null) {
            crashlytics.setUserId(userId)
        }
    }
    
    /**
     * Log a custom key for debugging
     */
    fun logKey(key: String, value: String) {
        crashlytics.setCustomKey(key, value)
    }
    
    /**
     * Log a breadcrumb message for debugging
     */
    fun logBreadcrumb(message: String) {
        crashlytics.log(message)
    }
    
    companion object {
        private const val TAG = "ErrorHandler"
    }
}
