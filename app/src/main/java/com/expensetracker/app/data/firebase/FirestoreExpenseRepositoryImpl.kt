package com.expensetracker.app.data.firebase

import android.util.Log
import com.expensetracker.app.data.firebase.models.FirestoreExpense
import com.expensetracker.app.data.local.dao.ExpenseDao
import com.expensetracker.app.data.local.entity.Expense
import com.expensetracker.app.data.preferences.HouseholdPreferencesManager
import com.expensetracker.app.domain.model.Result
import com.expensetracker.app.domain.repository.FirestoreExpenseRepository
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
 * Firestore implementation of FirestoreExpenseRepository
 * 
 * Handles syncing expenses between local Room database and Firebase Firestore
 * for multi-device synchronization within a household.
 */
@Singleton
class FirestoreExpenseRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val expenseDao: ExpenseDao,
    private val householdPreferences: HouseholdPreferencesManager
) : FirestoreExpenseRepository {
    
    companion object {
        private const val TAG = "FirestoreExpenseRepo"
        private const val EXPENSES_COLLECTION = "expenses"
        private const val EXPENSES_SUBCOLLECTION = "expenses"
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
     * Get reference to household expenses collection
     */
    private fun getHouseholdExpensesCollection(householdId: String) =
        firestore.collection(EXPENSES_COLLECTION)
            .document(householdId)
            .collection(EXPENSES_SUBCOLLECTION)
    
    override suspend fun syncExpense(expense: Expense): Result<Unit> {
        return try {
            Log.d("DEBUG_SYNC", "=== FirestoreExpenseRepo.syncExpense CALLED ===")
            
            val userId = getCurrentUserId()
            Log.d("DEBUG_SYNC", "User ID: $userId")
            
            if (userId == null) {
                Log.w(TAG, "Cannot sync expense: User not authenticated")
                Log.e("DEBUG_SYNC", "ERROR: User not authenticated")
                return Result.Error(
                    exception = IllegalStateException("User not authenticated"),
                    message = "Your session has expired. Please sign in again."
                )
            }
            
            val householdId = getCurrentHouseholdId()
            Log.d("DEBUG_SYNC", "Household ID: $householdId")
            
            if (householdId == null) {
                Log.w(TAG, "Cannot sync expense: No household configured")
                Log.e("DEBUG_SYNC", "ERROR: No household configured")
                return Result.Error(
                    exception = IllegalStateException("No household configured"),
                    message = "Please join or create a household first"
                )
            }
            
            Log.d(TAG, "Syncing expense ${expense.id} to household $householdId")
            Log.d("DEBUG_SYNC", "Creating Firestore document...")
            
            val expensesCollection = getHouseholdExpensesCollection(householdId)
            Log.d("DEBUG_SYNC", "Collection path: expenses/$householdId/expenses")
            
            // Create or update document
            // Use expense.id as document ID for consistency
            val docRef = expensesCollection.document(expense.id.toString())
            
            // Convert to Firestore model with the document ID
            val firestoreExpense = FirestoreExpense.fromExpense(
                expense = expense,
                householdId = householdId,
                userId = userId,
                firestoreId = docRef.id
            )
            Log.d("DEBUG_SYNC", "Firestore expense created: $firestoreExpense")
            Log.d("DEBUG_SYNC", "Document ID: ${expense.id}")
            Log.d("DEBUG_SYNC", "Document path: ${docRef.path}")
            Log.d("DEBUG_SYNC", "Firestore data to write: amount=${firestoreExpense.amount}, desc=${firestoreExpense.description}, isDeleted=${firestoreExpense.isDeleted}")
            Log.d("DEBUG_SYNC", "Calling Firestore set()...")
            
            docRef.set(firestoreExpense).await()
            
            Log.d(TAG, "Successfully synced expense ${expense.id}")
            Log.d("DEBUG_SYNC", "=== SYNC SUCCESS ===")
            Log.d("DEBUG_SYNC", "Document written to: ${docRef.path}")
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync expense: ${e.javaClass.simpleName} - ${e.message}", e)
            Result.Error(
                exception = e,
                message = e.message ?: "Failed to sync expense"
            )
        }
    }
    
    override suspend fun deleteExpense(expenseId: String): Result<Unit> {
        return try {
            val userId = getCurrentUserId()
            if (userId == null) {
                Log.w(TAG, "Cannot delete expense: User not authenticated")
                return Result.Error(
                    exception = IllegalStateException("User not authenticated"),
                    message = "Your session has expired. Please sign in again."
                )
            }
            
            val householdId = getCurrentHouseholdId()
            if (householdId == null) {
                Log.w(TAG, "Cannot delete expense: No household configured")
                return Result.Error(
                    exception = IllegalStateException("No household configured"),
                    message = "Please join or create a household first"
                )
            }
            
            Log.d(TAG, "Soft deleting expense $expenseId from household $householdId")
            
            val expensesCollection = getHouseholdExpensesCollection(householdId)
            
            // Soft delete: update the document to mark it as deleted
            val updates = hashMapOf<String, Any>(
                "deleted" to true,
                "modifiedBy" to userId,
                "modifiedAt" to com.google.firebase.Timestamp.now()
            )
            
            expensesCollection.document(expenseId).update(updates).await()
            
            Log.d(TAG, "Successfully marked expense $expenseId as deleted")
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete expense: ${e.javaClass.simpleName} - ${e.message}", e)
            Result.Error(
                exception = e,
                message = e.message ?: "Failed to delete expense"
            )
        }
    }
    
    override fun observeHouseholdExpenses(householdId: String): Flow<List<FirestoreExpense>> = callbackFlow {
        Log.d(TAG, "Starting to observe expenses for household: $householdId")
        Log.d("DEBUG_SYNC", "=== observeHouseholdExpenses ===")
        Log.d("DEBUG_SYNC", "Household ID: $householdId")
        
        val expensesCollection = getHouseholdExpensesCollection(householdId)
        Log.d("DEBUG_SYNC", "Collection path: expenses/$householdId/expenses")
        Log.d("DEBUG_SYNC", "Query: whereEqualTo('isDeleted', false)")
        
        val listener = expensesCollection
            .whereEqualTo("deleted", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error observing household expenses", error)
                    Log.e("DEBUG_SYNC", "Listener error: ${error.message}", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    Log.d("DEBUG_SYNC", "Snapshot received: ${snapshot.documents.size} documents")
                    snapshot.documents.forEach { doc ->
                        Log.d("DEBUG_SYNC", "  Document: ${doc.id}, data: ${doc.data}")
                    }
                    
                    val expenses = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(FirestoreExpense::class.java)
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to parse expense document ${doc.id}", e)
                            Log.e("DEBUG_SYNC", "Parse error for ${doc.id}: ${e.message}", e)
                            null
                        }
                    }
                    Log.d(TAG, "Received ${expenses.size} expenses from Firestore")
                    Log.d("DEBUG_SYNC", "Parsed ${expenses.size} expenses successfully")
                    trySend(expenses)
                } else {
                    Log.d("DEBUG_SYNC", "Snapshot is null")
                    trySend(emptyList())
                }
            }
        
        awaitClose {
            Log.d(TAG, "Stopping observation of household expenses: $householdId")
            listener.remove()
        }
    }
    
    override suspend fun uploadAllLocalExpenses(householdId: String): Result<Unit> {
        return try {
            val userId = getCurrentUserId()
            if (userId == null) {
                Log.w(TAG, "Cannot upload expenses: User not authenticated")
                return Result.Error(
                    exception = IllegalStateException("User not authenticated"),
                    message = "Your session has expired. Please sign in again."
                )
            }
            
            Log.d(TAG, "Starting migration: Uploading all local expenses to household $householdId")
            
            // Get all local expenses
            val localExpenses = expenseDao.getAllExpensesOnce()
            Log.d(TAG, "Found ${localExpenses.size} local expenses to upload")
            
            if (localExpenses.isEmpty()) {
                Log.d(TAG, "No local expenses to upload")
                return Result.Success(Unit)
            }
            
            val expensesCollection = getHouseholdExpensesCollection(householdId)
            
            // Upload in batches of 50 (Firestore batch limit is 500)
            val batchSize = 50
            val totalBatches = (localExpenses.size + batchSize - 1) / batchSize
            
            localExpenses.chunked(batchSize).forEachIndexed { batchIndex, batch ->
                Log.d(TAG, "Uploading batch ${batchIndex + 1} of $totalBatches")
                
                val firestoreBatch = firestore.batch()
                
                batch.forEach { expense ->
                    val firestoreExpense = FirestoreExpense.fromExpense(
                        expense = expense,
                        householdId = householdId,
                        userId = userId
                    )
                    
                    val docRef = expensesCollection.document(expense.id.toString())
                    firestoreBatch.set(docRef, firestoreExpense)
                }
                
                firestoreBatch.commit().await()
                Log.d(TAG, "Batch ${batchIndex + 1} uploaded successfully")
            }
            
            Log.d(TAG, "Successfully uploaded ${localExpenses.size} expenses to Firestore")
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload local expenses: ${e.javaClass.simpleName} - ${e.message}", e)
            Result.Error(
                exception = e,
                message = e.message ?: "Failed to upload expenses"
            )
        }
    }
}
