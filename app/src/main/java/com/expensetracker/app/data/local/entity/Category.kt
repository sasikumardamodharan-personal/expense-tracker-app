package com.expensetracker.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "categories",
    indices = [
        Index(value = ["firestore_id"]),
        Index(value = ["sync_status"])
    ]
)
data class Category(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "name")
    val name: String,
    
    @ColumnInfo(name = "icon_name")
    val iconName: String,
    
    @ColumnInfo(name = "color_hex")
    val colorHex: String,
    
    @ColumnInfo(name = "is_custom")
    val isCustom: Boolean = false,
    
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int,
    
    // Sync-related fields
    @ColumnInfo(name = "firestore_id")
    val firestoreId: String? = null,
    
    @ColumnInfo(name = "modified_at")
    val modifiedAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "created_by")
    val createdBy: String? = null,
    
    @ColumnInfo(name = "modified_by")
    val modifiedBy: String? = null,
    
    @ColumnInfo(name = "sync_status")
    val syncStatus: String = "PENDING" // PENDING, SYNCED, ERROR
)
