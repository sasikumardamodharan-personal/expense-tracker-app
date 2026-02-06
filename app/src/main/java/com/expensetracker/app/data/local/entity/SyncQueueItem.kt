package com.expensetracker.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a pending sync operation in the queue
 */
@Entity(
    tableName = "sync_queue",
    indices = [
        Index(value = ["expense_id"]),
        Index(value = ["timestamp"])
    ]
)
data class SyncQueueItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "expense_id")
    val expenseId: Long,
    
    @ColumnInfo(name = "operation")
    val operation: SyncOperation,
    
    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "retry_count")
    val retryCount: Int = 0,
    
    @ColumnInfo(name = "last_error")
    val lastError: String? = null
)

/**
 * Types of sync operations
 */
enum class SyncOperation {
    CREATE,
    UPDATE,
    DELETE
}
