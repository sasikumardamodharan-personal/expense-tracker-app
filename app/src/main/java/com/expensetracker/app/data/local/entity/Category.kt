package com.expensetracker.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
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
    val sortOrder: Int
)
