package com.expensetracker.app.domain.sync

import android.util.Log
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreException.Code

/**
 * Handles sync errors and provides user-friendly error messages
 */
object SyncErrorHandler {
    
    private const val TAG = "SyncErrorHandler"
    
    /**
     * Categorizes and handles sync errors
     */
    fun handleSyncError(exception: Exception): SyncError {
        Log.e(TAG, "Handling sync error: ${exception.javaClass.simpleName}", exception)
        
        return when (exception) {
            // Network errors
            is FirebaseNetworkException -> {
                SyncError.NetworkError(
                    exception = exception,
                    message = "No internet connection. Changes will sync when you're back online.",
                    isRetryable = true
                )
            }
            
            // Authentication errors
            is FirebaseAuthException -> {
                handleAuthError(exception)
            }
            
            // Firestore errors
            is FirebaseFirestoreException -> {
                handleFirestoreError(exception)
            }
            
            // Generic errors
            else -> {
                SyncError.UnknownError(
                    exception = exception,
                    message = "Sync failed: ${exception.message ?: "Unknown error"}",
                    isRetryable = true
                )
            }
        }
    }
    
    /**
     * Handles Firebase Authentication errors
     */
    private fun handleAuthError(exception: FirebaseAuthException): SyncError {
        return when (exception.errorCode) {
            "ERROR_USER_NOT_FOUND",
            "ERROR_INVALID_USER_TOKEN",
            "ERROR_USER_TOKEN_EXPIRED" -> {
                SyncError.AuthenticationError(
                    exception = exception,
                    message = "Your session has expired. Please sign in again.",
                    isRetryable = false
                )
            }
            
            "ERROR_NETWORK_REQUEST_FAILED" -> {
                SyncError.NetworkError(
                    exception = exception,
                    message = "No internet connection. Changes will sync when you're back online.",
                    isRetryable = true
                )
            }
            
            else -> {
                SyncError.AuthenticationError(
                    exception = exception,
                    message = "Authentication error: ${exception.message}",
                    isRetryable = false
                )
            }
        }
    }
    
    /**
     * Handles Firestore-specific errors
     */
    private fun handleFirestoreError(exception: FirebaseFirestoreException): SyncError {
        return when (exception.code) {
            Code.PERMISSION_DENIED -> {
                SyncError.PermissionError(
                    exception = exception,
                    message = "You don't have permission to access this data. Please check your household membership.",
                    isRetryable = false
                )
            }
            
            Code.UNAVAILABLE -> {
                SyncError.NetworkError(
                    exception = exception,
                    message = "Firestore service is temporarily unavailable. Retrying...",
                    isRetryable = true
                )
            }
            
            Code.DEADLINE_EXCEEDED -> {
                SyncError.NetworkError(
                    exception = exception,
                    message = "Request timed out. Please check your internet connection.",
                    isRetryable = true
                )
            }
            
            Code.RESOURCE_EXHAUSTED -> {
                SyncError.QuotaExceededError(
                    exception = exception,
                    message = "Firestore quota exceeded. Please try again later or contact support.",
                    isRetryable = false
                )
            }
            
            Code.UNAUTHENTICATED -> {
                SyncError.AuthenticationError(
                    exception = exception,
                    message = "Your session has expired. Please sign in again.",
                    isRetryable = false
                )
            }
            
            Code.NOT_FOUND -> {
                SyncError.DataNotFoundError(
                    exception = exception,
                    message = "The requested data was not found.",
                    isRetryable = false
                )
            }
            
            Code.ALREADY_EXISTS -> {
                SyncError.ConflictError(
                    exception = exception,
                    message = "This data already exists. Refreshing...",
                    isRetryable = false
                )
            }
            
            Code.ABORTED -> {
                SyncError.ConflictError(
                    exception = exception,
                    message = "Sync conflict detected. Retrying...",
                    isRetryable = true
                )
            }
            
            else -> {
                SyncError.UnknownError(
                    exception = exception,
                    message = "Sync error: ${exception.message ?: "Unknown Firestore error"}",
                    isRetryable = true
                )
            }
        }
    }
    
    /**
     * Determines if an error should trigger a retry
     */
    fun shouldRetry(error: SyncError, attemptCount: Int, maxAttempts: Int): Boolean {
        if (attemptCount >= maxAttempts) {
            Log.d(TAG, "Max retry attempts ($maxAttempts) reached")
            return false
        }
        
        return error.isRetryable
    }
    
    /**
     * Calculates exponential backoff delay in milliseconds
     */
    fun calculateBackoffDelay(attemptCount: Int, baseDelayMs: Long = 1000): Long {
        // Exponential backoff: 1s, 2s, 4s, 8s, 16s (capped at 30s)
        val delay = (baseDelayMs * Math.pow(2.0, attemptCount.toDouble())).toLong()
        return minOf(delay, 30000L)
    }
}

/**
 * Sealed class representing different types of sync errors
 */
sealed class SyncError(
    open val exception: Exception,
    open val message: String,
    open val isRetryable: Boolean
) {
    /**
     * Network connectivity errors
     */
    data class NetworkError(
        override val exception: Exception,
        override val message: String,
        override val isRetryable: Boolean = true
    ) : SyncError(exception, message, isRetryable)
    
    /**
     * Firebase Authentication errors
     */
    data class AuthenticationError(
        override val exception: Exception,
        override val message: String,
        override val isRetryable: Boolean = false
    ) : SyncError(exception, message, isRetryable)
    
    /**
     * Permission denied errors
     */
    data class PermissionError(
        override val exception: Exception,
        override val message: String,
        override val isRetryable: Boolean = false
    ) : SyncError(exception, message, isRetryable)
    
    /**
     * Firestore quota exceeded errors
     */
    data class QuotaExceededError(
        override val exception: Exception,
        override val message: String,
        override val isRetryable: Boolean = false
    ) : SyncError(exception, message, isRetryable)
    
    /**
     * Data not found errors
     */
    data class DataNotFoundError(
        override val exception: Exception,
        override val message: String,
        override val isRetryable: Boolean = false
    ) : SyncError(exception, message, isRetryable)
    
    /**
     * Conflict errors (concurrent modifications)
     */
    data class ConflictError(
        override val exception: Exception,
        override val message: String,
        override val isRetryable: Boolean = true
    ) : SyncError(exception, message, isRetryable)
    
    /**
     * Unknown or unhandled errors
     */
    data class UnknownError(
        override val exception: Exception,
        override val message: String,
        override val isRetryable: Boolean = true
    ) : SyncError(exception, message, isRetryable)
}
