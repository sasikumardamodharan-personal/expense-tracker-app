package com.expensetracker.app.domain.repository

import com.expensetracker.app.data.firebase.models.FirestoreCategory
import com.expensetracker.app.data.local.entity.Category
import com.expensetracker.app.domain.model.Result
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Firestore category operations
 * 
 * This repository handles syncing categories between the local Room database
 * and Firebase Firestore for multi-device synchronization.
 */
interface FirestoreCategoryRepository {
    /**
     * Sync a local category to Firestore
     * 
     * @param category The local category to sync
     * @return Result indicating success or failure
     */
    suspend fun syncCategory(category: Category): Result<Unit>
    
    /**
     * Delete a category from Firestore
     * 
     * @param categoryId The Firestore document ID of the category to delete
     * @return Result indicating success or failure
     */
    suspend fun deleteCategory(categoryId: String): Result<Unit>
    
    /**
     * Observe all categories for a household in real-time
     * 
     * @param householdId The household ID to observe categories for
     * @return Flow of category lists that updates in real-time
     */
    fun observeHouseholdCategories(householdId: String): Flow<List<FirestoreCategory>>
    
    /**
     * Upload all custom local categories to Firestore (for initial migration)
     * 
     * @param householdId The household ID to upload categories to
     * @return Result indicating success or failure
     */
    suspend fun uploadAllCustomCategories(householdId: String): Result<Unit>
}
