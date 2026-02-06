package com.expensetracker.app.domain.sync

import com.expensetracker.app.di.SyncCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Throttles sync operations to prevent excessive network calls
 */
@Singleton
class SyncThrottler @Inject constructor(
    @SyncCoroutineScope private val coroutineScope: CoroutineScope
) {
    private val syncJobs = mutableMapOf<String, Job>()
    
    /**
     * Throttle a sync operation by key
     * If called multiple times with the same key within delayMs, only the last call will execute
     */
    fun throttleSync(key: String, delayMs: Long = 500, action: suspend () -> Unit) {
        // Cancel any existing job for this key
        syncJobs[key]?.cancel()
        
        // Create new job with delay
        syncJobs[key] = coroutineScope.launch {
            delay(delayMs)
            action()
            syncJobs.remove(key)
        }
    }
    
    /**
     * Cancel all pending sync operations
     */
    fun cancelAll() {
        syncJobs.values.forEach { it.cancel() }
        syncJobs.clear()
    }
    
    /**
     * Cancel sync operation for a specific key
     */
    fun cancel(key: String) {
        syncJobs[key]?.cancel()
        syncJobs.remove(key)
    }
}
