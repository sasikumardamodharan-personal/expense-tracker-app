package com.expensetracker.app.domain.usecase

import android.util.Log
import com.expensetracker.app.data.preferences.HouseholdPreferencesManager
import com.expensetracker.app.domain.model.MigrationProgress
import com.expensetracker.app.domain.model.Result
import com.expensetracker.app.domain.repository.ExpenseRepository
import com.expensetracker.app.domain.repository.FirestoreCategoryRepository
import com.expensetracker.app.domain.repository.FirestoreExpenseRepository
import com.expensetracker.app.domain.sync.ConnectivityMonitor
import com.expensetracker.app.domain.sync.SyncErrorHandler
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for migrating local expenses and categories to Firestore during initial setup.
 * 
 * This service handles the one-time migration of existing local data to the cloud when
 * a user first joins or creates a household. Key features include:
 * - Batch upload of expenses in configurable batch sizes
 * - Real-time progress tracking via StateFlow
 * - Automatic retry logic with exponential backoff
 * - Connectivity validation before migration
 * - Category migration alongside expenses
 * - Migration state persistence to prevent duplicate migrations
 * 
 * The migration process is designed to be resilient and user-friendly, providing
 * clear progress updates and handling errors gracefully.
 * 
 * @property localRepository Repository for local Room database operations
 * @property firestoreRepository Repository for Firestore expense operations
 * @property firestoreCategoryRepository Repository for Firestore category operations
 * @property householdPreferences Manager for household-related preferences
 * @property connectivityMonitor Monitor for network connectivity
 * @property auth Firebase authentication instance
 */
@Singleton
class DataMigrationService @Inject constructor(
    private val localRepository: ExpenseRepository,
    private val firestoreRepository: FirestoreExpenseRepository,
    private val firestoreCategoryRepository: FirestoreCategoryRepository,
    private val householdPreferences: HouseholdPreferencesManager,
    private val connectivityMonitor: ConnectivityMonitor,
    private val auth: FirebaseAuth
) {
    
    companion object {
        private const val TAG = "DataMigrationService"
        private const val BATCH_SIZE = 50
        private const val MAX_RETRY_ATTEMPTS = 3
    }
    
    private val _migrationProgress = MutableStateFlow<MigrationProgress>(MigrationProgress.Idle)
    val migrationProgress: StateFlow<MigrationProgress> = _migrationProgress.asStateFlow()
    
    /**
     * Migrate all local expenses to Firestore
     * 
     * This method:
     * 1. Validates prerequisites (auth, household, connectivity)
     * 2. Fetches all local expenses
     * 3. Uploads them in batches to Firestore
     * 4. Tracks progress and handles errors
     * 5. Marks migration as complete
     */
    suspend fun migrateToFirestore(): Result<Unit> {
        return try {
            Log.d(TAG, "Starting data migration to Firestore")
            
            // Validate user is authenticated
            val userId = auth.currentUser?.uid
            if (userId == null) {
                val errorMsg = "User not authenticated"
                Log.w(TAG, errorMsg)
                _migrationProgress.value = MigrationProgress.Error(errorMsg)
                return Result.Error(
                    exception = IllegalStateException(errorMsg),
                    message = "Please sign in to migrate your data"
                )
            }
            
            // Validate household is configured
            val householdId = householdPreferences.householdId.first()
            if (householdId == null) {
                val errorMsg = "No household configured"
                Log.w(TAG, errorMsg)
                _migrationProgress.value = MigrationProgress.Error(errorMsg)
                return Result.Error(
                    exception = IllegalStateException(errorMsg),
                    message = "Please join or create a household first"
                )
            }
            
            // Validate connectivity
            if (!connectivityMonitor.isConnectedValue()) {
                val errorMsg = "No internet connection"
                Log.w(TAG, errorMsg)
                _migrationProgress.value = MigrationProgress.Error(errorMsg)
                return Result.Error(
                    exception = IllegalStateException(errorMsg),
                    message = "Please connect to the internet to migrate your data"
                )
            }
            
            // Check if migration already complete
            val isMigrationComplete = householdPreferences.isMigrationComplete.first()
            if (isMigrationComplete) {
                Log.d(TAG, "Migration already completed")
                _migrationProgress.value = MigrationProgress.Completed(0)
                return Result.Success(Unit)
            }
            
            // Get all local expenses
            Log.d(TAG, "Fetching all local expenses")
            val localExpenses = localRepository.getAllExpensesOnce()
            val totalExpenses = localExpenses.size
            
            Log.d(TAG, "Found $totalExpenses local expenses to migrate")
            
            if (totalExpenses == 0) {
                Log.d(TAG, "No expenses to migrate")
                _migrationProgress.value = MigrationProgress.Completed(0)
                householdPreferences.setMigrationComplete(true)
                return Result.Success(Unit)
            }
            
            // Start migration
            _migrationProgress.value = MigrationProgress.InProgress(0, totalExpenses)
            
            // Upload categories first
            Log.d(TAG, "Uploading custom categories")
            val categoryResult = firestoreCategoryRepository.uploadAllCustomCategories(householdId)
            if (categoryResult is Result.Error) {
                Log.e(TAG, "Failed to upload categories: ${categoryResult.message}")
                _migrationProgress.value = MigrationProgress.Error(
                    "Failed to upload categories: ${categoryResult.message}"
                )
                return categoryResult
            }
            
            // Upload expenses with retry logic
            val result = uploadWithRetry(householdId, totalExpenses)
            
            when (result) {
                is Result.Success -> {
                    Log.d(TAG, "Migration completed successfully")
                    _migrationProgress.value = MigrationProgress.Completed(totalExpenses)
                    householdPreferences.setMigrationComplete(true)
                    Result.Success(Unit)
                }
                is Result.Error -> {
                    Log.e(TAG, "Migration failed: ${result.message}", result.exception)
                    _migrationProgress.value = MigrationProgress.Error(
                        result.message ?: "Migration failed"
                    )
                    result
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during migration", e)
            _migrationProgress.value = MigrationProgress.Error(e.message ?: "Unknown error")
            Result.Error(
                exception = e,
                message = "Migration failed: ${e.message}"
            )
        }
    }
    
    /**
     * Upload expenses with retry logic and error handling
     */
    private suspend fun uploadWithRetry(
        householdId: String,
        totalExpenses: Int
    ): Result<Unit> {
        var attempt = 0
        var lastSyncError: com.expensetracker.app.domain.sync.SyncError? = null
        
        while (attempt < MAX_RETRY_ATTEMPTS) {
            attempt++
            Log.d(TAG, "Upload attempt $attempt of $MAX_RETRY_ATTEMPTS")
            
            val result = firestoreRepository.uploadAllLocalExpenses(householdId)
            
            when (result) {
                is Result.Success -> {
                    return result
                }
                is Result.Error -> {
                    val syncError = SyncErrorHandler.handleSyncError(result.exception)
                    lastSyncError = syncError
                    
                    Log.w(TAG, "Upload attempt $attempt failed: ${syncError.message}")
                    
                    // Check if error is retryable
                    if (!SyncErrorHandler.shouldRetry(syncError, attempt - 1, MAX_RETRY_ATTEMPTS)) {
                        Log.e(TAG, "Error is not retryable, aborting migration")
                        return Result.Error(
                            exception = syncError.exception,
                            message = syncError.message
                        )
                    }
                    
                    if (attempt < MAX_RETRY_ATTEMPTS) {
                        // Calculate exponential backoff delay
                        val delayMs = SyncErrorHandler.calculateBackoffDelay(attempt - 1)
                        Log.d(TAG, "Retrying in ${delayMs}ms")
                        kotlinx.coroutines.delay(delayMs)
                    }
                }
            }
        }
        
        return Result.Error(
            exception = lastSyncError?.exception ?: Exception("Max retry attempts exceeded"),
            message = lastSyncError?.message ?: "Failed to upload expenses after $MAX_RETRY_ATTEMPTS attempts"
        )
    }
    
    /**
     * Reset migration status (for testing or re-migration)
     */
    suspend fun resetMigrationStatus() {
        Log.d(TAG, "Resetting migration status")
        householdPreferences.setMigrationComplete(false)
        _migrationProgress.value = MigrationProgress.Idle
    }
    
    /**
     * Check if migration is needed
     */
    suspend fun isMigrationNeeded(): Boolean {
        val isMigrationComplete = householdPreferences.isMigrationComplete.first()
        if (isMigrationComplete) {
            return false
        }
        
        val localExpenses = localRepository.getAllExpensesOnce()
        return localExpenses.isNotEmpty()
    }
}
