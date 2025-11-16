package com.expensetracker.app.data.initialization

import com.expensetracker.app.data.local.datastore.InitializationPreferences
import com.expensetracker.app.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseInitializer @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val initializationPreferences: InitializationPreferences
) {
    
    /**
     * Initializes the database with default categories on first app launch.
     * This method is idempotent and safe to call multiple times.
     */
    suspend fun initialize() {
        try {
            // Check if database has already been initialized
            val isInitialized = initializationPreferences.isDatabaseInitialized.first()
            
            if (!isInitialized) {
                // Initialize default categories
                categoryRepository.initializeDefaultCategories()
                
                // Mark as initialized
                initializationPreferences.setDatabaseInitialized(true)
            }
        } catch (e: Exception) {
            // Log error but don't crash the app
            e.printStackTrace()
        }
    }
}
