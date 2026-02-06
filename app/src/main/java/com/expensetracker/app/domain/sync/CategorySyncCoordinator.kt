package com.expensetracker.app.domain.sync

import android.util.Log
import com.expensetracker.app.data.firebase.models.FirestoreCategory
import com.expensetracker.app.data.local.dao.CategoryDao
import com.expensetracker.app.data.local.entity.Category
import com.expensetracker.app.data.preferences.HouseholdPreferencesManager
import com.expensetracker.app.di.SyncCoroutineScope
import com.expensetracker.app.domain.repository.FirestoreCategoryRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Coordinates category synchronization between local Room database and Firestore
 * 
 * This coordinator handles:
 * - Syncing local categories to Firestore
 * - Listening for remote category changes
 * - Merging default and household categories
 * - Conflict resolution using last-write-wins strategy
 */
@Singleton
class CategorySyncCoordinator @Inject constructor(
    private val firestoreCategoryRepository: FirestoreCategoryRepository,
    private val categoryDao: CategoryDao,
    private val householdPreferences: HouseholdPreferencesManager,
    private val auth: FirebaseAuth,
    @SyncCoroutineScope private val coroutineScope: CoroutineScope
) {
    companion object {
        private const val TAG = "CategorySyncCoordinator"
    }
    
    private var syncJob: Job? = null
    private var listenerJob: Job? = null
    
    /**
     * Start category synchronization
     * 
     * This will:
     * 1. Start listening for remote category changes
     * 2. Sync any pending local changes to Firestore
     */
    fun startSync() {
        Log.d(TAG, "Starting category sync")
        
        // Stop any existing sync
        stopSync()
        
        syncJob = coroutineScope.launch {
            try {
                val householdId = householdPreferences.householdId.first()
                if (householdId == null) {
                    Log.w(TAG, "Cannot start sync: No household configured")
                    return@launch
                }
                
                // Start listening for remote changes
                startListeningForRemoteChanges(householdId)
                
                // Sync any pending local changes
                syncPendingCategories()
                
                Log.d(TAG, "Category sync started successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start category sync", e)
            }
        }
    }
    
    /**
     * Stop category synchronization
     */
    fun stopSync() {
        Log.d(TAG, "Stopping category sync")
        syncJob?.cancel()
        listenerJob?.cancel()
        syncJob = null
        listenerJob = null
    }
    
    /**
     * Sync a category to Firestore with error handling
     * 
     * @param category The category to sync
     */
    suspend fun syncCategoryToCloud(category: Category) {
        try {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                Log.w(TAG, "Cannot sync category: User not authenticated")
                categoryDao.updateCategory(
                    category.copy(syncStatus = "ERROR")
                )
                return
            }
            
            val householdId = householdPreferences.householdId.first()
            if (householdId == null) {
                Log.w(TAG, "Cannot sync category: No household configured")
                // Mark as pending for later sync
                categoryDao.updateCategory(
                    category.copy(syncStatus = "PENDING")
                )
                return
            }
            
            Log.d(TAG, "Syncing category to cloud: ${category.name}")
            
            // Update modified timestamp and user
            val updatedCategory = category.copy(
                modifiedAt = System.currentTimeMillis(),
                modifiedBy = userId,
                syncStatus = "SYNCING"
            )
            categoryDao.updateCategory(updatedCategory)
            
            // Sync to Firestore
            val result = firestoreCategoryRepository.syncCategory(updatedCategory)
            
            when (result) {
                is com.expensetracker.app.domain.model.Result.Success -> {
                    // Mark as synced
                    categoryDao.updateCategory(
                        updatedCategory.copy(syncStatus = "SYNCED")
                    )
                    Log.d(TAG, "Category synced successfully: ${category.name}")
                }
                is com.expensetracker.app.domain.model.Result.Error -> {
                    val syncError = SyncErrorHandler.handleSyncError(result.exception)
                    Log.e(TAG, "Failed to sync category ${category.name}: ${syncError.message}", result.exception)
                    
                    // Mark as error
                    categoryDao.updateCategory(
                        updatedCategory.copy(syncStatus = "ERROR")
                    )
                }
            }
        } catch (e: Exception) {
            val syncError = SyncErrorHandler.handleSyncError(e)
            Log.e(TAG, "Exception syncing category to cloud: ${syncError.message}", e)
            
            // Mark as error
            try {
                categoryDao.updateCategory(
                    category.copy(syncStatus = "ERROR")
                )
            } catch (updateError: Exception) {
                Log.e(TAG, "Failed to update category sync status", updateError)
            }
        }
    }
    
    /**
     * Sync a category from Firestore to local database
     * 
     * @param firestoreCategory The category from Firestore
     */
    private suspend fun syncCategoryFromCloud(firestoreCategory: FirestoreCategory) {
        try {
            Log.d(TAG, "Syncing category from cloud: ${firestoreCategory.name}")
            
            // Check if category already exists locally by Firestore ID
            val existingCategories = categoryDao.getAllCategoriesOnce()
            val existingCategory = existingCategories.find { it.firestoreId == firestoreCategory.id }
            
            if (existingCategory != null) {
                // Update existing category
                // Use last-write-wins: compare timestamps
                val remoteModifiedAt = firestoreCategory.modifiedAt.toDate().time
                
                if (remoteModifiedAt > existingCategory.modifiedAt) {
                    Log.d(TAG, "Remote category is newer, updating local: ${firestoreCategory.name}")
                    
                    val updatedCategory = existingCategory.copy(
                        name = firestoreCategory.name,
                        iconName = firestoreCategory.iconName,
                        colorHex = firestoreCategory.colorHex,
                        isCustom = firestoreCategory.isCustom,
                        sortOrder = firestoreCategory.sortOrder,
                        modifiedAt = remoteModifiedAt,
                        modifiedBy = firestoreCategory.modifiedBy,
                        syncStatus = "SYNCED"
                    )
                    
                    categoryDao.updateCategory(updatedCategory)
                } else {
                    Log.d(TAG, "Local category is newer or same, skipping: ${firestoreCategory.name}")
                }
            } else {
                // Check if a category with the same name exists (might be a default category)
                val categoryByName = categoryDao.getCategoryByName(firestoreCategory.name)
                
                if (categoryByName != null && !categoryByName.isCustom) {
                    // This is a default category, update it with Firestore ID
                    Log.d(TAG, "Linking default category to Firestore: ${firestoreCategory.name}")
                    
                    val updatedCategory = categoryByName.copy(
                        firestoreId = firestoreCategory.id,
                        modifiedAt = firestoreCategory.modifiedAt.toDate().time,
                        modifiedBy = firestoreCategory.modifiedBy,
                        syncStatus = "SYNCED"
                    )
                    
                    categoryDao.updateCategory(updatedCategory)
                } else {
                    // New category from another household member, insert it
                    Log.d(TAG, "Inserting new category from cloud: ${firestoreCategory.name}")
                    
                    val newCategory = Category(
                        name = firestoreCategory.name,
                        iconName = firestoreCategory.iconName,
                        colorHex = firestoreCategory.colorHex,
                        isCustom = firestoreCategory.isCustom,
                        sortOrder = firestoreCategory.sortOrder,
                        firestoreId = firestoreCategory.id,
                        modifiedAt = firestoreCategory.modifiedAt.toDate().time,
                        createdBy = firestoreCategory.createdBy,
                        modifiedBy = firestoreCategory.modifiedBy,
                        syncStatus = "SYNCED"
                    )
                    
                    categoryDao.insertCategory(newCategory)
                }
            }
            
            Log.d(TAG, "Category synced from cloud successfully: ${firestoreCategory.name}")
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing category from cloud: ${firestoreCategory.name}", e)
        }
    }
    
    /**
     * Start listening for remote category changes
     * 
     * @param householdId The household ID to listen for
     */
    private fun startListeningForRemoteChanges(householdId: String) {
        Log.d(TAG, "Starting to listen for remote category changes for household: $householdId")
        
        listenerJob = firestoreCategoryRepository
            .observeHouseholdCategories(householdId)
            .onEach { firestoreCategories ->
                Log.d(TAG, "Received ${firestoreCategories.size} categories from Firestore")
                
                // Process each category
                firestoreCategories.forEach { firestoreCategory ->
                    syncCategoryFromCloud(firestoreCategory)
                }
                
                // Handle deleted categories
                handleDeletedCategories(firestoreCategories)
            }
            .launchIn(coroutineScope)
    }
    
    /**
     * Handle categories that were deleted remotely
     * 
     * @param firestoreCategories Current list of categories from Firestore
     */
    private suspend fun handleDeletedCategories(firestoreCategories: List<FirestoreCategory>) {
        try {
            val localCategories = categoryDao.getAllCategoriesOnce()
            val firestoreIds = firestoreCategories.map { it.id }.toSet()
            
            // Find local categories that have Firestore IDs but are not in the remote list
            val deletedCategories = localCategories.filter { localCategory ->
                localCategory.firestoreId != null && 
                localCategory.firestoreId !in firestoreIds &&
                localCategory.isCustom // Only delete custom categories, keep defaults
            }
            
            if (deletedCategories.isNotEmpty()) {
                Log.d(TAG, "Found ${deletedCategories.size} categories deleted remotely")
                
                deletedCategories.forEach { category ->
                    Log.d(TAG, "Deleting category: ${category.name}")
                    categoryDao.deleteCategory(category.id)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling deleted categories", e)
        }
    }
    
    /**
     * Sync any pending local categories to Firestore
     */
    private suspend fun syncPendingCategories() {
        try {
            Log.d(TAG, "Syncing pending categories")
            
            val allCategories = categoryDao.getAllCategoriesOnce()
            val pendingCategories = allCategories.filter { 
                it.syncStatus == "PENDING" || it.syncStatus == "ERROR"
            }
            
            if (pendingCategories.isEmpty()) {
                Log.d(TAG, "No pending categories to sync")
                return
            }
            
            Log.d(TAG, "Found ${pendingCategories.size} pending categories")
            
            pendingCategories.forEach { category ->
                syncCategoryToCloud(category)
            }
            
            Log.d(TAG, "Finished syncing pending categories")
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing pending categories", e)
        }
    }
    
    /**
     * Delete a category and sync the deletion to Firestore
     * 
     * @param category The category to delete
     */
    suspend fun deleteCategoryWithSync(category: Category) {
        try {
            Log.d(TAG, "Deleting category with sync: ${category.name}")
            
            // Delete from local database first
            categoryDao.deleteCategory(category.id)
            
            // If it has a Firestore ID, delete from Firestore
            if (category.firestoreId != null) {
                val householdId = householdPreferences.householdId.first()
                if (householdId != null) {
                    firestoreCategoryRepository.deleteCategory(category.firestoreId)
                    Log.d(TAG, "Category deleted from Firestore: ${category.name}")
                }
            }
            
            Log.d(TAG, "Category deleted successfully: ${category.name}")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting category with sync", e)
            throw e
        }
    }
    
    /**
     * Merge default categories with household categories
     * 
     * This ensures that:
     * 1. Default categories are always available
     * 2. Household-specific custom categories are included
     * 3. No duplicates exist
     */
    suspend fun mergeDefaultAndHouseholdCategories() {
        try {
            Log.d(TAG, "Merging default and household categories")
            
            val allCategories = categoryDao.getAllCategoriesOnce()
            
            // Separate default and custom categories
            val defaultCategories = allCategories.filter { !it.isCustom }
            val customCategories = allCategories.filter { it.isCustom }
            
            Log.d(TAG, "Found ${defaultCategories.size} default and ${customCategories.size} custom categories")
            
            // Default categories should always be present
            // Custom categories come from household sync
            // The merge is implicit - both types coexist in the database
            
            // Ensure default categories are marked as synced if they have Firestore IDs
            defaultCategories.forEach { category ->
                if (category.firestoreId != null && category.syncStatus != "SYNCED") {
                    categoryDao.updateCategory(
                        category.copy(syncStatus = "SYNCED")
                    )
                }
            }
            
            Log.d(TAG, "Category merge complete")
        } catch (e: Exception) {
            Log.e(TAG, "Error merging categories", e)
        }
    }
}
