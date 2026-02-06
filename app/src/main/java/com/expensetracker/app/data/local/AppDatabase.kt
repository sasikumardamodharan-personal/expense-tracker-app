package com.expensetracker.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.expensetracker.app.data.local.dao.CategoryDao
import com.expensetracker.app.data.local.dao.ExpenseDao
import com.expensetracker.app.data.local.dao.SyncQueueDao
import com.expensetracker.app.data.local.entity.Category
import com.expensetracker.app.data.local.entity.Expense
import com.expensetracker.app.data.local.entity.SyncQueueItem

@Database(
    entities = [Expense::class, Category::class, SyncQueueItem::class],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun expenseDao(): ExpenseDao
    abstract fun categoryDao(): CategoryDao
    abstract fun syncQueueDao(): SyncQueueDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        /**
         * Migration from version 3 to 4: Add sync_status field to expenses table
         */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add sync_status column with default value "PENDING"
                database.execSQL(
                    "ALTER TABLE expenses ADD COLUMN sync_status TEXT NOT NULL DEFAULT 'PENDING'"
                )
                
                // Create index on sync_status for better query performance
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_expenses_sync_status ON expenses(sync_status)"
                )
            }
        }
        
        /**
         * Migration from version 4 to 5: Add sync-related fields to categories table
         */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add sync-related columns to categories table
                database.execSQL(
                    "ALTER TABLE categories ADD COLUMN firestore_id TEXT"
                )
                database.execSQL(
                    "ALTER TABLE categories ADD COLUMN modified_at INTEGER NOT NULL DEFAULT ${System.currentTimeMillis()}"
                )
                database.execSQL(
                    "ALTER TABLE categories ADD COLUMN created_by TEXT"
                )
                database.execSQL(
                    "ALTER TABLE categories ADD COLUMN modified_by TEXT"
                )
                database.execSQL(
                    "ALTER TABLE categories ADD COLUMN sync_status TEXT NOT NULL DEFAULT 'PENDING'"
                )
                
                // Create indices for better query performance
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_categories_firestore_id ON categories(firestore_id)"
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_categories_sync_status ON categories(sync_status)"
                )
            }
        }
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "expense_tracker_database"
                )
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
