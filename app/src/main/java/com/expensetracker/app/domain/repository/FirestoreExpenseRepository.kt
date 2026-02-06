package com.expensetracker.app.domain.repository

import com.expensetracker.app.data.firebase.models.FirestoreExpense
import com.expensetracker.app.data.local.entity.Expense
import com.expensetracker.app.domain.model.Result
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Firestore expense operations
 * 
 * This repository handles syncing expenses between the local Room database
 * and Firebase Firestore for multi-device synchronization.
 */
interface FirestoreExpenseRepository {
    /**
     * Sync a local expense to Firestore
     * 
     * @param expense The local expense to sync
     * @return Result indicating success or failure
     */
    suspend fun syncExpense(expense: Expense): Result<Unit>
    
    /**
     * Delete an expense from Firestore
     * 
     * @param expenseId The Firestore document ID of the expense to delete
     * @return Result indicating success or failure
     */
    suspend fun deleteExpense(expenseId: String): Result<Unit>
    
    /**
     * Observe all expenses for a household in real-time
     * 
     * @param householdId The household ID to observe expenses for
     * @return Flow of expense lists that updates in real-time
     */
    fun observeHouseholdExpenses(householdId: String): Flow<List<FirestoreExpense>>
    
    /**
     * Upload all local expenses to Firestore (for initial migration)
     * 
     * @param householdId The household ID to upload expenses to
     * @return Result indicating success or failure
     */
    suspend fun uploadAllLocalExpenses(householdId: String): Result<Unit>
}
