package com.expensetracker.app.domain.sync

import android.util.Log
import com.expensetracker.app.data.local.dao.SyncQueueDao
import com.expensetracker.app.data.local.entity.SyncOperation
import com.expensetracker.app.data.local.entity.SyncQueueItem
import com.expensetracker.app.di.SyncCoroutineScope
import com.expensetracker.app.domain.repository.ExpenseRepository
import com.expensetracker.app.domain.repository.FirestoreExpenseRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow

/**
 * Manages offline sync queue with auto-retry logic
 */
@Singleton
class OfflineQueueManager @Inject constructor(
    private val syncQueueDao: SyncQueueDao,
    private val localRepository: ExpenseRepository,
    private val firestoreRepository: FirestoreExpenseRepository,
    private val connectivityMonitor: ConnectivityMonitor,
    @SyncCoroutineScope private val coroutineScope: CoroutineScope
) {
    
    companion object {
        private const val TAG = "OfflineQueueManager"
        private const val MAX_RETRY_COUNT = 5
        private const val BASE_DELAY_MS = 1000L
    }
    
    private var processingJob: Job? = null
    private var isProcessing = false
    
    /**
     * Get the current queue size as a Flow
     */
    fun getQueueSize(): Flow<Int> = syncQueueDao.getQueueSize()
    
    /**
     * Add an expense operation to the sync queue
     */
    suspend fun enqueueOperation(expenseId: Long, operation: SyncOperation) {
        try {
            Log.d(TAG, "Enqueuing operation: $operation for expense $expenseId")
            
            // Remove any existing queue items for this expense
            syncQueueDao.deleteQueueItemsByExpenseId(expenseId)
            
            // Add new queue item
            val queueItem = SyncQueueItem(
                expenseId = expenseId,
                operation = operation,
                timestamp = System.currentTimeMillis()
            )
            syncQueueDao.insertQueueItem(queueItem)
            
            // Try to process queue if online
            if (connectivityMonitor.isConnectedValue()) {
                processQueue()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enqueue operation", e)
        }
    }
    
    /**
     * Start monitoring connectivity and process queue when online
     */
    fun startMonitoring() {
        if (processingJob != null) {
            Log.d(TAG, "Already monitoring")
            return
        }
        
        Log.d(TAG, "Starting queue monitoring")
        processingJob = coroutineScope.launch {
            connectivityMonitor.isConnected.collect { isConnected ->
                if (isConnected && !isProcessing) {
                    Log.d(TAG, "Connectivity restored, processing queue")
                    processQueue()
                }
            }
        }
    }
    
    /**
     * Stop monitoring
     */
    fun stopMonitoring() {
        Log.d(TAG, "Stopping queue monitoring")
        processingJob?.cancel()
        processingJob = null
    }
    
    /**
     * Process all items in the queue
     */
    suspend fun processQueue() {
        if (isProcessing) {
            Log.d(TAG, "Queue already being processed")
            return
        }
        
        if (!connectivityMonitor.isConnectedValue()) {
            Log.d(TAG, "No connectivity, skipping queue processing")
            return
        }
        
        isProcessing = true
        
        try {
            val queueItems = syncQueueDao.getAllQueueItemsOnce()
            Log.d(TAG, "Processing ${queueItems.size} queue items")
            
            for (item in queueItems) {
                processQueueItem(item)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing queue", e)
        } finally {
            isProcessing = false
        }
    }
    
    /**
     * Process a single queue item with retry logic
     */
    private suspend fun processQueueItem(item: SyncQueueItem) {
        try {
            Log.d(TAG, "Processing queue item: ${item.id}, operation: ${item.operation}, retry: ${item.retryCount}")
            
            // Check if max retries exceeded
            if (item.retryCount >= MAX_RETRY_COUNT) {
                Log.w(TAG, "Max retries exceeded for item ${item.id}, removing from queue")
                syncQueueDao.deleteQueueItem(item)
                return
            }
            
            // Get the expense
            val expense = localRepository.getExpenseById(item.expenseId)
            if (expense == null) {
                Log.w(TAG, "Expense ${item.expenseId} not found, removing from queue")
                syncQueueDao.deleteQueueItem(item)
                return
            }
            
            // Perform the operation
            val success = when (item.operation) {
                SyncOperation.CREATE, SyncOperation.UPDATE -> {
                    val result = firestoreRepository.syncExpense(expense)
                    result is com.expensetracker.app.domain.model.Result.Success
                }
                SyncOperation.DELETE -> {
                    expense.firestoreId?.let { firestoreId ->
                        val result = firestoreRepository.deleteExpense(firestoreId)
                        result is com.expensetracker.app.domain.model.Result.Success
                    } ?: false
                }
            }
            
            if (success) {
                Log.d(TAG, "Successfully processed queue item ${item.id}")
                syncQueueDao.deleteQueueItem(item)
            } else {
                // Retry with exponential backoff
                val retryCount = item.retryCount + 1
                val delayMs = calculateBackoffDelay(retryCount)
                
                Log.d(TAG, "Failed to process item ${item.id}, will retry in ${delayMs}ms (attempt $retryCount)")
                
                delay(delayMs)
                
                val updatedItem = item.copy(
                    retryCount = retryCount,
                    lastError = "Sync failed, retry $retryCount"
                )
                syncQueueDao.updateQueueItem(updatedItem)
                
                // Retry immediately
                processQueueItem(updatedItem)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception processing queue item ${item.id}", e)
            
            // Update retry count
            val updatedItem = item.copy(
                retryCount = item.retryCount + 1,
                lastError = e.message
            )
            syncQueueDao.updateQueueItem(updatedItem)
        }
    }
    
    /**
     * Calculate exponential backoff delay
     */
    private fun calculateBackoffDelay(retryCount: Int): Long {
        return (BASE_DELAY_MS * 2.0.pow(retryCount - 1)).toLong().coerceAtMost(60000L)
    }
    
    /**
     * Clear all items from the queue
     */
    suspend fun clearQueue() {
        Log.d(TAG, "Clearing sync queue")
        syncQueueDao.clearQueue()
    }
}
