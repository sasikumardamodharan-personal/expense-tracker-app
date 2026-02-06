package com.expensetracker.app.domain.sync

import android.util.Log
import com.expensetracker.app.data.firebase.models.FirestoreExpense
import com.expensetracker.app.data.local.entity.Expense
import com.expensetracker.app.data.local.entity.SyncOperation
import com.expensetracker.app.data.local.entity.SyncQueueItem
import com.expensetracker.app.data.preferences.HouseholdPreferencesManager
import com.expensetracker.app.di.SyncCoroutineScope
import com.expensetracker.app.domain.model.Result
import com.expensetracker.app.domain.model.SyncStatus
import com.expensetracker.app.domain.repository.ExpenseRepository
import com.expensetracker.app.domain.repository.FirestoreExpenseRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Coordinates bidirectional synchronization between local Room database and Firestore.
 * 
 * This class is responsible for:
 * - Syncing local expense changes to Firestore
 * - Syncing remote Firestore changes to local database
 * - Handling offline scenarios with automatic retry
 * - Implementing conflict resolution using last-write-wins strategy
 * - Throttling sync operations to prevent excessive network calls
 * - Managing sync status and error states
 * 
 * The coordinator uses a throttling mechanism to batch rapid changes and reduce
 * network overhead. Sync operations are automatically retried with exponential backoff
 * on failure.
 * 
 * @property localRepository Repository for local Room database operations
 * @property firestoreRepository Repository for Firestore cloud operations
 * @property householdPreferences Manager for household-related preferences
 * @property connectivityMonitor Monitor for network connectivity changes
 * @property syncThrottler Throttler to batch rapid sync operations
 * @property coroutineScope Coroutine scope for async operations
 */
@Singleton
class ExpenseSyncCoordinator @Inject constructor(
    private val localRepository: ExpenseRepository,
    private val firestoreRepository: FirestoreExpenseRepository,
    private val householdPreferences: HouseholdPreferencesManager,
    private val connectivityMonitor: ConnectivityMonitor,
    private val syncThrottler: SyncThrottler,
    private val realtimeSyncListener: RealtimeSyncListener,
    @SyncCoroutineScope private val coroutineScope: CoroutineScope
) {
    
    companion object {
        private const val TAG = "ExpenseSyncCoordinator"
        private const val SYNC_THROTTLE_MS = 500L
        private const val MAX_RETRY_ATTEMPTS = 3
    }
    
    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Synced)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()
    
    private val retryManager = SyncRetryManager(maxRetries = MAX_RETRY_ATTEMPTS)
    
    private var syncJob: Job? = null
    private var isRunning = false
    
    /**
     * Starts the sync coordinator and begins monitoring connectivity changes.
     * 
     * This method should be called when the user joins or creates a household.
     * It sets up connectivity monitoring and updates sync status based on
     * network availability. Safe to call multiple times (idempotent).
     */
    fun startSync() {
        if (isRunning) {
            Log.d(TAG, "Sync already running")
            return
        }
        
        isRunning = true
        Log.d(TAG, "Starting sync coordinator")
        
        // Start realtime listener for incoming changes from Firestore
        Log.d(TAG, "Starting realtime sync listener")
        realtimeSyncListener.startListening()
        
        // Monitor connectivity changes
        syncJob = coroutineScope.launch {
            connectivityMonitor.isConnected.collect { isConnected ->
                if (isConnected) {
                    Log.d(TAG, "Connectivity restored, updating status")
                    if (_syncStatus.value is SyncStatus.Offline) {
                        _syncStatus.value = SyncStatus.Synced
                    }
                } else {
                    Log.d(TAG, "Connectivity lost")
                    _syncStatus.value = SyncStatus.Offline
                }
            }
        }
    }
    
    /**
     * Stops the sync coordinator and cancels all ongoing sync operations.
     * 
     * This method should be called when the user leaves a household or signs out.
     * It cancels connectivity monitoring and cleans up resources.
     */
    fun stopSync() {
        Log.d(TAG, "Stopping sync coordinator")
        isRunning = false
        
        // Stop realtime listener
        Log.d(TAG, "Stopping realtime sync listener")
        realtimeSyncListener.stopListening()
        
        syncJob?.cancel()
        syncJob = null
    }
    
    /**
     * Syncs an expense to Firestore with automatic throttling and retry logic.
     * 
     * This method queues the expense for sync and applies throttling to prevent
     * excessive network calls. If offline, the sync is deferred until connectivity
     * is restored. Failed syncs are automatically retried with exponential backoff.
     * 
     * @param expense The expense to sync to Firestore
     */
    suspend fun syncExpenseToCloud(expense: Expense) {
        Log.d("DEBUG_SYNC", "=== syncExpenseToCloud CALLED ===")
        Log.d("DEBUG_SYNC", "Expense: id=${expense.id}, amount=${expense.amount}")
        
        val householdId = householdPreferences.getHouseholdId()
        Log.d("DEBUG_SYNC", "Household ID: $householdId")
        
        if (householdId == null) {
            Log.w(TAG, "No household ID, skipping sync")
            Log.e("DEBUG_SYNC", "ERROR: No household ID!")
            return
        }
        
        val isConnected = connectivityMonitor.isConnectedValue()
        Log.d("DEBUG_SYNC", "Is connected: $isConnected")
        
        if (!isConnected) {
            Log.d(TAG, "Offline, sync will occur when connectivity is restored")
            Log.e("DEBUG_SYNC", "ERROR: Offline!")
            _syncStatus.value = SyncStatus.Offline
            return
        }
        
        Log.d("DEBUG_SYNC", "Calling throttleSync...")
        syncThrottler.throttleSync("expense_${expense.id}", SYNC_THROTTLE_MS) {
            performSyncToCloud(expense, householdId)
        }
    }
    
    /**
     * Perform the actual sync to cloud with retry logic
     */
    private suspend fun performSyncToCloud(expense: Expense, householdId: String) {
        Log.d("DEBUG_SYNC", "=== performSyncToCloud CALLED ===")
        Log.d("DEBUG_SYNC", "Expense: ${expense.id}, Household: $householdId")
        
        _syncStatus.value = SyncStatus.Syncing
        Log.d(TAG, "Syncing expense ${expense.id} to Firestore")
        
        val result = retryManager.executeWithRetry(
            operation = {
                firestoreRepository.syncExpense(expense)
            },
            onRetry = { attemptCount, error ->
                Log.w(TAG, "Retry attempt $attemptCount for expense ${expense.id}: ${error.message}")
                _syncStatus.value = SyncStatus.Error("Retrying... (${attemptCount + 1}/$MAX_RETRY_ATTEMPTS)")
            }
        )
        
        when (result) {
            is Result.Success -> {
                Log.d(TAG, "Successfully synced expense ${expense.id}")
                
                // Update local expense sync status to SYNCED without triggering another sync
                try {
                    val syncedExpense = expense.copy(syncStatus = "SYNCED")
                    localRepository.updateExpenseWithoutSync(syncedExpense)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to update expense sync status", e)
                }
                
                _syncStatus.value = SyncStatus.Synced
            }
            
            is Result.Error -> {
                val syncError = SyncErrorHandler.handleSyncError(result.exception)
                Log.e(TAG, "Failed to sync expense ${expense.id} after retries: ${syncError.message}", result.exception)
                
                // Update local expense sync status to ERROR without triggering another sync
                try {
                    val errorExpense = expense.copy(syncStatus = "ERROR")
                    localRepository.updateExpenseWithoutSync(errorExpense)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to update expense sync status", e)
                }
                
                _syncStatus.value = SyncStatus.Error(syncError.message)
            }
        }
    }
    
    /**
     * Syncs an expense from Firestore to the local database with conflict resolution.
     * 
     * Implements last-write-wins conflict resolution strategy by comparing modification
     * timestamps. If the remote version is newer, it overwrites the local version.
     * If the local version is newer or equal, it is preserved. New expenses from
     * Firestore are added to the local database without triggering a sync back.
     * 
     * @param firestoreExpense The expense received from Firestore
     */
    suspend fun syncExpenseFromCloud(firestoreExpense: FirestoreExpense) {
        try {
            Log.d(TAG, "Syncing expense from Firestore: ${firestoreExpense.id}")
            
            // Check if expense exists locally
            val localExpense = if (firestoreExpense.id.isNotEmpty()) {
                localRepository.getExpenseByFirestoreId(firestoreExpense.id)
            } else {
                null
            }
            
            if (localExpense != null) {
                // Conflict resolution: last-write-wins
                val remoteModifiedAtMillis = firestoreExpense.modifiedAt.toDate().time
                if (remoteModifiedAtMillis > localExpense.modifiedAt) {
                    Log.d(TAG, "Remote version is newer, updating local")
                    val updatedExpense = localExpense.copy(
                        amount = firestoreExpense.amount,
                        description = firestoreExpense.description,
                        categoryId = firestoreExpense.categoryId,
                        date = firestoreExpense.date.toDate().time,
                        modifiedAt = remoteModifiedAtMillis,
                        modifiedBy = firestoreExpense.modifiedBy,
                        syncStatus = "SYNCED"
                    )
                    localRepository.updateExpenseWithoutSync(updatedExpense)
                } else {
                    Log.d(TAG, "Local version is newer or equal, keeping local")
                }
            } else {
                // New expense from cloud, add to local without triggering sync back to cloud
                Log.d(TAG, "New expense from cloud, adding to local")
                val newExpense = Expense(
                    amount = firestoreExpense.amount,
                    description = firestoreExpense.description,
                    categoryId = firestoreExpense.categoryId,
                    date = firestoreExpense.date.toDate().time,
                    createdAt = firestoreExpense.createdAt.toDate().time,
                    updatedAt = System.currentTimeMillis(),
                    firestoreId = firestoreExpense.id,
                    modifiedAt = firestoreExpense.modifiedAt.toDate().time,
                    createdBy = firestoreExpense.createdBy,
                    modifiedBy = firestoreExpense.modifiedBy,
                    syncStatus = "SYNCED"
                )
                localRepository.addExpenseWithoutSync(newExpense)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing expense from cloud", e)
        }
    }
    
    /**
     * Deletes an expense from Firestore with automatic retry logic.
     * 
     * This method attempts to delete the expense from Firestore with automatic
     * retries on failure. If offline, the deletion is deferred until connectivity
     * is restored. The local expense should be deleted separately by the caller.
     * 
     * @param expenseId The ID of the expense to delete from Firestore
     */
    suspend fun deleteExpenseFromCloud(expenseId: Long) {
        val householdId = householdPreferences.getHouseholdId()
        if (householdId == null) {
            Log.w(TAG, "No household ID, skipping deletion sync")
            return
        }
        
        if (!connectivityMonitor.isConnectedValue()) {
            Log.d(TAG, "Offline, deletion will sync when connectivity is restored")
            _syncStatus.value = SyncStatus.Offline
            return
        }
        
        _syncStatus.value = SyncStatus.Syncing
        Log.d(TAG, "Deleting expense $expenseId from Firestore")
        
        val result = retryManager.executeWithRetry(
            operation = {
                firestoreRepository.deleteExpense(expenseId.toString())
            },
            onRetry = { attemptCount, error ->
                Log.w(TAG, "Retry attempt $attemptCount for deleting expense $expenseId: ${error.message}")
                _syncStatus.value = SyncStatus.Error("Retrying deletion... (${attemptCount + 1}/$MAX_RETRY_ATTEMPTS)")
            }
        )
        
        when (result) {
            is Result.Success -> {
                Log.d(TAG, "Successfully deleted expense $expenseId from Firestore")
                _syncStatus.value = SyncStatus.Synced
            }
            
            is Result.Error -> {
                val syncError = SyncErrorHandler.handleSyncError(result.exception)
                Log.e(TAG, "Failed to delete expense $expenseId after retries: ${syncError.message}", result.exception)
                _syncStatus.value = SyncStatus.Error(syncError.message)
            }
        }
    }
    
    /**
     * Migrates all local expenses to Firestore during initial household setup.
     * 
     * This method uploads all existing local expenses to Firestore when a user
     * first joins or creates a household. The migration is performed with automatic
     * retry logic and requires an active internet connection. Progress can be
     * monitored through the syncStatus StateFlow.
     * 
     * @return Result.Success if migration completes successfully, Result.Error otherwise
     */
    suspend fun migrateLocalExpenses(): Result<Unit> {
        val householdId = householdPreferences.getHouseholdId()
        if (householdId == null) {
            val error = "Please join or create a household first"
            Log.w(TAG, error)
            return Result.Error(
                exception = IllegalStateException("No household ID"),
                message = error
            )
        }
        
        if (!connectivityMonitor.isConnectedValue()) {
            val error = "Please connect to the internet to migrate data"
            Log.w(TAG, error)
            _syncStatus.value = SyncStatus.Offline
            return Result.Error(
                exception = IllegalStateException("No connectivity"),
                message = error
            )
        }
        
        Log.d(TAG, "Starting migration of local expenses")
        _syncStatus.value = SyncStatus.Syncing
        
        val result = retryManager.executeWithRetry(
            operation = {
                firestoreRepository.uploadAllLocalExpenses(householdId)
            },
            onRetry = { attemptCount, error ->
                Log.w(TAG, "Retry attempt $attemptCount for migration: ${error.message}")
                _syncStatus.value = SyncStatus.Error("Retrying migration... (${attemptCount + 1}/$MAX_RETRY_ATTEMPTS)")
            }
        )
        
        return when (result) {
            is Result.Success -> {
                Log.d(TAG, "Migration completed successfully")
                _syncStatus.value = SyncStatus.Synced
                Result.Success(Unit)
            }
            
            is Result.Error -> {
                val syncError = SyncErrorHandler.handleSyncError(result.exception)
                Log.e(TAG, "Migration failed after retries: ${syncError.message}", result.exception)
                _syncStatus.value = SyncStatus.Error(syncError.message)
                Result.Error(
                    exception = syncError.exception,
                    message = syncError.message
                )
            }
        }
    }
}
