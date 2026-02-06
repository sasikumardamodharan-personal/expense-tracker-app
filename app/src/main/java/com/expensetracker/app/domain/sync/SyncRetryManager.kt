package com.expensetracker.app.domain.sync

import android.util.Log
import com.expensetracker.app.domain.model.Result
import kotlinx.coroutines.delay

/**
 * Manages retry logic for sync operations with exponential backoff
 */
class SyncRetryManager(
    private val maxRetries: Int = 3,
    private val baseDelayMs: Long = 1000
) {
    
    companion object {
        private const val TAG = "SyncRetryManager"
    }
    
    /**
     * Execute an operation with automatic retry on failure
     * 
     * @param operation The suspend function to execute
     * @param onRetry Optional callback invoked before each retry attempt
     * @return Result of the operation
     */
    suspend fun <T> executeWithRetry(
        operation: suspend () -> Result<T>,
        onRetry: ((attemptCount: Int, error: SyncError) -> Unit)? = null
    ): Result<T> {
        var attemptCount = 0
        var lastError: SyncError? = null
        
        while (attemptCount <= maxRetries) {
            try {
                Log.d(TAG, "Executing operation (attempt ${attemptCount + 1}/${maxRetries + 1})")
                
                val result = operation()
                
                when (result) {
                    is Result.Success -> {
                        if (attemptCount > 0) {
                            Log.d(TAG, "Operation succeeded after $attemptCount retries")
                        }
                        return result
                    }
                    
                    is Result.Error -> {
                        val syncError = SyncErrorHandler.handleSyncError(result.exception)
                        lastError = syncError
                        
                        Log.w(TAG, "Operation failed: ${syncError.message}")
                        
                        // Check if we should retry
                        if (!SyncErrorHandler.shouldRetry(syncError, attemptCount, maxRetries)) {
                            Log.d(TAG, "Error is not retryable or max attempts reached")
                            return Result.Error(
                                exception = syncError.exception,
                                message = syncError.message
                            )
                        }
                        
                        // Calculate backoff delay
                        val delayMs = SyncErrorHandler.calculateBackoffDelay(attemptCount, baseDelayMs)
                        Log.d(TAG, "Retrying in ${delayMs}ms...")
                        
                        // Notify retry callback
                        onRetry?.invoke(attemptCount, syncError)
                        
                        // Wait before retry
                        delay(delayMs)
                        
                        attemptCount++
                    }
                }
            } catch (e: Exception) {
                val syncError = SyncErrorHandler.handleSyncError(e)
                lastError = syncError
                
                Log.e(TAG, "Unexpected exception during operation", e)
                
                // Check if we should retry
                if (!SyncErrorHandler.shouldRetry(syncError, attemptCount, maxRetries)) {
                    Log.d(TAG, "Exception is not retryable or max attempts reached")
                    return Result.Error(
                        exception = syncError.exception,
                        message = syncError.message
                    )
                }
                
                // Calculate backoff delay
                val delayMs = SyncErrorHandler.calculateBackoffDelay(attemptCount, baseDelayMs)
                Log.d(TAG, "Retrying in ${delayMs}ms...")
                
                // Notify retry callback
                onRetry?.invoke(attemptCount, syncError)
                
                // Wait before retry
                delay(delayMs)
                
                attemptCount++
            }
        }
        
        // All retries exhausted
        Log.e(TAG, "All retry attempts exhausted")
        return Result.Error(
            exception = lastError?.exception ?: Exception("Max retries exceeded"),
            message = lastError?.message ?: "Operation failed after $maxRetries retries"
        )
    }
    
    /**
     * Execute an operation with retry, but return Unit on success
     */
    suspend fun executeWithRetryUnit(
        operation: suspend () -> Result<Unit>,
        onRetry: ((attemptCount: Int, error: SyncError) -> Unit)? = null
    ): Result<Unit> {
        return executeWithRetry(operation, onRetry)
    }
}
