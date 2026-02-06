package com.expensetracker.app.domain.model

/**
 * Represents the progress state of data migration to Firestore
 */
sealed class MigrationProgress {
    /**
     * Migration has not started
     */
    object Idle : MigrationProgress()
    
    /**
     * Migration is in progress
     * @param uploaded Number of expenses uploaded so far
     * @param total Total number of expenses to upload
     */
    data class InProgress(
        val uploaded: Int,
        val total: Int
    ) : MigrationProgress() {
        val percentage: Int
            get() = if (total > 0) (uploaded * 100) / total else 0
    }
    
    /**
     * Migration completed successfully
     * @param totalMigrated Total number of expenses migrated
     */
    data class Completed(val totalMigrated: Int) : MigrationProgress()
    
    /**
     * Migration failed with an error
     * @param message Error message describing what went wrong
     */
    data class Error(val message: String) : MigrationProgress()
}
