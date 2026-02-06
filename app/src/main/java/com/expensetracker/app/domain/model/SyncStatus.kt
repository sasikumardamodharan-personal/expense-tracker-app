package com.expensetracker.app.domain.model

/**
 * Represents the synchronization status of an expense or the overall sync state
 */
sealed class SyncStatus {
    /**
     * Data is successfully synced with Firestore
     */
    object Synced : SyncStatus()
    
    /**
     * Data is currently being synced to Firestore
     */
    object Syncing : SyncStatus()
    
    /**
     * Sync operation failed with an error
     */
    data class Error(val message: String) : SyncStatus()
    
    /**
     * Device is offline, sync will occur when connectivity is restored
     */
    object Offline : SyncStatus()
    
    /**
     * Data is pending sync (queued for upload)
     */
    object Pending : SyncStatus()
}
