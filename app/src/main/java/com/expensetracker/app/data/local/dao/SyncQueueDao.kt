package com.expensetracker.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.expensetracker.app.data.local.entity.SyncQueueItem
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncQueueDao {
    
    @Query("SELECT * FROM sync_queue ORDER BY timestamp ASC")
    fun getAllQueueItems(): Flow<List<SyncQueueItem>>
    
    @Query("SELECT * FROM sync_queue ORDER BY timestamp ASC")
    suspend fun getAllQueueItemsOnce(): List<SyncQueueItem>
    
    @Query("SELECT * FROM sync_queue WHERE expense_id = :expenseId")
    suspend fun getQueueItemsByExpenseId(expenseId: Long): List<SyncQueueItem>
    
    @Query("SELECT COUNT(*) FROM sync_queue")
    fun getQueueSize(): Flow<Int>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQueueItem(item: SyncQueueItem): Long
    
    @Update
    suspend fun updateQueueItem(item: SyncQueueItem)
    
    @Delete
    suspend fun deleteQueueItem(item: SyncQueueItem)
    
    @Query("DELETE FROM sync_queue WHERE expense_id = :expenseId")
    suspend fun deleteQueueItemsByExpenseId(expenseId: Long)
    
    @Query("DELETE FROM sync_queue")
    suspend fun clearQueue()
}
