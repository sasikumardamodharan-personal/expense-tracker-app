package com.expensetracker.app.data.firebase

import android.util.Log
import com.expensetracker.app.data.firebase.models.FirestoreCategory
import com.expensetracker.app.data.local.dao.CategoryDao
import com.expensetracker.app.data.local.entity.Category
import com.expensetracker.app.data.preferences.HouseholdPreferencesManager
import com.expensetracker.app.domain.model.Result
import com.expensetracker.app.domain.repository.FirestoreCategoryRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firestore implementation of FirestoreCategoryRepository
 * 
 * Handles syncing categories between local Room database and Firebase Firestore
 * for multi-device synchronization within a household.
 */
@Singleton
class FirestoreCategoryRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val categoryDao: CategoryDao,
    private val householdPreferences: HouseholdPreferencesManager
) : FirestoreCategoryRepository {
    
    companion object {
        private const val TAG = "FirestoreCategoryRepo"
        private const val CATEGORIES_COLLECTION = "categories"
        private const val CATEGORIES_SUBCOLLECTION = "categories"
    }
    
    /**
     * Get the current user ID from Firebase Auth
     */
    private fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }
    
    /**
     * Get the current household ID from preferences
     */
    private suspend fun getCurrentHouseholdId(): String? {
        return householdPreferences.householdId.first()
    }
    
    /**
     * Get reference to household categories collection
     */
    private fun getHouseholdCategoriesCollection(householdId: String) =
        firestore.collection(CATEGORIES_COLLECTION)
            .document(householdId)
            .collection(CATEGORIES_SUBCOLLECTION)
    
    override suspend fun syncCategory(category: Category): Result<Unit> {
        return try {
            val userId = getCurrentUserId()
            if (userId == null) {
                Log.w(TAG, "Cannot sync category: User not authenticated")
                return Result.Error(
                    exception = Exception("User not authenticated"),
                    message = "Please sign in to sync categories"
                )
            }
            
            val householdId = getCurrentHouseholdId()
            if (householdId == null) {
                Log.w(TAG, "Cannot sync category: No household configured")
                return Result.Error(
                    exception = Exception("No household configured"),
                    message = "Please join or create a household first"
                )
            }
            
            Log.d(TAG, "Syncing category ${category.id} to household $householdId")
            
            val categoriesCollection = getHouseholdCategoriesCollection(householdId)
            
            // Convert to Firestore model
            val firestoreCategory = FirestoreCategory.fromCategory(
                category = category,
                householdId = householdId,
                userId = userId,
                firestoreId = category.firestoreId
            )
            
            // Create or update document
            val docRef = if (category.firestoreId != null) {
                categoriesCollection.document(category.firestoreId)
            } else {
                categoriesCollection.document()
            }
            
            val updatedCategory = firestoreCategory.copy(id = docRef.id)
            docRef.set(updatedCategory).await()
            
            // Update local category with Firestore ID if it's new
            if (category.firestoreId == null) {
                categoryDao.updateCategory(
                    category.copy(
                        firestoreId = docRef.id,
                        syncStatus = "SYNCED"
                    )
                )
            }
            
            Log.d(TAG, "Successfully synced category ${category.id}")
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync category", e)
            Result.Error(
                exception = e,
                message = "Failed to sync category: ${e.message}"
            )
        }
    }
    
    override suspend fun deleteCategory(categoryId: String): Result<Unit> {
        return try {
            val householdId = getCurrentHouseholdId()
            if (householdId == null) {
                Log.w(TAG, "Cannot delete category: No household configured")
                return Result.Error(
                    exception = Exception("No household configured"),
                    message = "Please join or create a household first"
                )
            }
            
            Log.d(TAG, "Deleting category $categoryId from household $householdId")
            
            val categoriesCollection = getHouseholdCategoriesCollection(householdId)
            categoriesCollection.document(categoryId).delete().await()
            
            Log.d(TAG, "Successfully deleted category $categoryId")
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete category", e)
            Result.Error(
                exception = e,
                message = "Failed to delete category: ${e.message}"
            )
        }
    }
    
    override fun observeHouseholdCategories(householdId: String): Flow<List<FirestoreCategory>> = callbackFlow {
        Log.d(TAG, "Starting to observe categories for household: $householdId")
        
        val categoriesCollection = getHouseholdCategoriesCollection(householdId)
        
        val listener = categoriesCollection
            .whereEqualTo("isDeleted", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error observing household categories", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val categories = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(FirestoreCategory::class.java)
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to parse category document ${doc.id}", e)
                            null
                        }
                    }
                    Log.d(TAG, "Received ${categories.size} categories from Firestore")
                    trySend(categories)
                } else {
                    trySend(emptyList())
                }
            }
        
        awaitClose {
            Log.d(TAG, "Stopping observation of household categories: $householdId")
            listener.remove()
        }
    }
    
    override suspend fun uploadAllCustomCategories(householdId: String): Result<Unit> {
        return try {
            val userId = getCurrentUserId()
            if (userId == null) {
                Log.w(TAG, "Cannot upload categories: User not authenticated")
                return Result.Error(
                    exception = Exception("User not authenticated"),
                    message = "Please sign in to upload categories"
                )
            }
            
            Log.d(TAG, "Starting migration: Uploading all custom categories to household $householdId")
            
            // Get all custom categories (not default ones)
            val allCategories = categoryDao.getAllCategoriesOnce()
            val customCategories = allCategories.filter { it.isCustom }
            Log.d(TAG, "Found ${customCategories.size} custom categories to upload")
            
            if (customCategories.isEmpty()) {
                Log.d(TAG, "No custom categories to upload")
                return Result.Success(Unit)
            }
            
            val categoriesCollection = getHouseholdCategoriesCollection(householdId)
            
            // Upload in batches
            val batchSize = 50
            customCategories.chunked(batchSize).forEachIndexed { batchIndex, batch ->
                Log.d(TAG, "Uploading batch ${batchIndex + 1} of ${(customCategories.size + batchSize - 1) / batchSize}")
                
                val firestoreBatch = firestore.batch()
                
                batch.forEach { category ->
                    val firestoreCategory = FirestoreCategory.fromCategory(
                        category = category,
                        householdId = householdId,
                        userId = userId
                    )
                    
                    val docRef = categoriesCollection.document()
                    val updatedCategory = firestoreCategory.copy(id = docRef.id)
                    firestoreBatch.set(docRef, updatedCategory)
                    
                    // Update local category with Firestore ID
                    val localUpdate = category.copy(
                        firestoreId = docRef.id,
                        syncStatus = "SYNCED"
                    )
                    categoryDao.updateCategory(localUpdate)
                }
                
                firestoreBatch.commit().await()
            }
            
            Log.d(TAG, "Successfully uploaded ${customCategories.size} categories to Firestore")
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload custom categories", e)
            Result.Error(
                exception = e,
                message = "Failed to upload categories: ${e.message}"
            )
        }
    }
}
