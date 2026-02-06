package com.expensetracker.app.domain.sync

import android.util.Log
import com.expensetracker.app.data.firebase.models.FirestoreExpense
import com.expensetracker.app.data.local.entity.Expense
import com.expensetracker.app.data.preferences.HouseholdPreferencesManager
import com.expensetracker.app.di.SyncCoroutineScope
import com.expensetracker.app.domain.repository.ExpenseRepository
import com.expensetracker.app.domain.repository.FirestoreExpenseRepository
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Listens for real-time changes from Firestore and updates local database
 */
@Singleton
class RealtimeSyncListener @Inject constructor(
    private val firestoreRepository: FirestoreExpenseRepository,
    private val localRepository: ExpenseRepository,
    private val householdPreferences: HouseholdPreferencesManager,
    @SyncCoroutineScope private val coroutineScope: CoroutineScope
) {
    
    companion object {
        private const val TAG = "RealtimeSyncListener"
    }
    
    private var listenerRegistration: ListenerRegistration? = null
    private var isListening = false
    
    /**
     * Start listening for real-time changes
     */
    fun startListening() {
        if (isListening) {
            Log.d(TAG, "Already listening")
            return
        }
        
        Log.d(TAG, "Starting real-time sync listener")
        Log.d("DEBUG_SYNC", "=== RealtimeSyncListener.startListening ===")
        isListening = true
        
        // Start observing household expenses
        coroutineScope.launch {
            val householdId = householdPreferences.getHouseholdId()
            Log.d("DEBUG_SYNC", "Household ID for listening: $householdId")
            
            if (householdId == null) {
                Log.w(TAG, "No household ID, cannot start listening")
                Log.e("DEBUG_SYNC", "ERROR: No household ID for listening")
                isListening = false
                return@launch
            }
            
            Log.d(TAG, "Listening for household: $householdId")
            Log.d("DEBUG_SYNC", "Starting to observe Firestore expenses...")
            
            firestoreRepository.observeHouseholdExpenses(householdId).collect { expenses ->
                Log.d(TAG, "Received ${expenses.size} expenses from Firestore")
                Log.d("DEBUG_SYNC", "=== RECEIVED ${expenses.size} EXPENSES FROM FIRESTORE ===")
                expenses.forEach { exp ->
                    Log.d("DEBUG_SYNC", "  - Expense: id=${exp.id}, amount=${exp.amount}, desc=${exp.description}")
                }
                handleExpenseChanges(expenses)
            }
        }
    }
    
    /**
     * Stop listening for real-time changes
     */
    fun stopListening() {
        Log.d(TAG, "Stopping real-time sync listener")
        isListening = false
        listenerRegistration?.remove()
        listenerRegistration = null
    }
    
    /**
     * Handle incoming expense changes from Firestore
     */
    private suspend fun handleExpenseChanges(firestoreExpenses: List<FirestoreExpense>) {
        try {
            for (firestoreExpense in firestoreExpenses) {
                handleExpenseChange(firestoreExpense)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling expense changes", e)
        }
    }
    
    /**
     * Handle a single expense change
     */
    private suspend fun handleExpenseChange(firestoreExpense: FirestoreExpense) {
        try {
            if (firestoreExpense.isDeleted) {
                handleExpenseDeleted(firestoreExpense)
            } else {
                handleExpenseAddedOrModified(firestoreExpense)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling expense change for ${firestoreExpense.id}", e)
        }
    }
    
    /**
     * Handle expense added or modified
     */
    private suspend fun handleExpenseAddedOrModified(firestoreExpense: FirestoreExpense) {
        val firestoreId = firestoreExpense.id
        if (firestoreId.isEmpty()) return
        
        Log.d(TAG, "Handling expense added/modified: $firestoreId")
        Log.d("DEBUG_SYNC", "=== handleExpenseAddedOrModified: $firestoreId ===")
        
        // Check if expense exists locally by firestoreId or by local ID
        var localExpense = localRepository.getExpenseByFirestoreId(firestoreId)
        
        // If not found by firestoreId, try by local ID (document ID matches local ID)
        if (localExpense == null) {
            try {
                val localId = firestoreId.toLongOrNull()
                if (localId != null) {
                    localExpense = localRepository.getExpenseById(localId)
                }
            } catch (e: Exception) {
                Log.d(TAG, "Could not parse firestoreId as Long: $firestoreId")
            }
        }
        
        Log.d("DEBUG_SYNC", "Local expense found: ${localExpense != null}")
        
        if (localExpense != null) {
            // Expense exists, check if we need to update (last-write-wins)
            val remoteModifiedAtMillis = firestoreExpense.modifiedAt.toDate().time
            if (remoteModifiedAtMillis > localExpense.modifiedAt) {
                Log.d(TAG, "Remote version is newer, updating local expense ${localExpense.id}")
                
                val updatedExpense = localExpense.copy(
                    amount = firestoreExpense.amount,
                    description = firestoreExpense.description,
                    categoryId = firestoreExpense.categoryId,
                    date = firestoreExpense.date.toDate().time,
                    updatedAt = System.currentTimeMillis(),
                    modifiedAt = remoteModifiedAtMillis,
                    modifiedBy = firestoreExpense.modifiedBy,
                    syncStatus = "SYNCED"
                )
                
                localRepository.updateExpenseWithoutSync(updatedExpense)
            } else {
                Log.d(TAG, "Local version is newer or equal, keeping local expense ${localExpense.id}")
            }
        } else {
            // New expense from cloud, add to local
            Log.d(TAG, "New expense from cloud, adding to local: $firestoreId")
            
            val newExpense = Expense(
                amount = firestoreExpense.amount,
                description = firestoreExpense.description,
                categoryId = firestoreExpense.categoryId,
                date = firestoreExpense.date.toDate().time,
                createdAt = firestoreExpense.createdAt.toDate().time,
                updatedAt = System.currentTimeMillis(),
                firestoreId = firestoreId,
                modifiedAt = firestoreExpense.modifiedAt.toDate().time,
                createdBy = firestoreExpense.createdBy,
                modifiedBy = firestoreExpense.modifiedBy,
                syncStatus = "SYNCED"
            )
            
            localRepository.addExpenseWithoutSync(newExpense)
        }
    }
    
    /**
     * Handle expense deleted
     */
    private suspend fun handleExpenseDeleted(firestoreExpense: FirestoreExpense) {
        val firestoreId = firestoreExpense.id
        if (firestoreId.isEmpty()) return
        
        Log.d(TAG, "Handling expense deleted: $firestoreId")
        
        // Find and delete local expense
        val localExpense = localRepository.getExpenseByFirestoreId(firestoreId)
        if (localExpense != null) {
            Log.d(TAG, "Deleting local expense ${localExpense.id}")
            localRepository.deleteExpense(localExpense)
        } else {
            Log.d(TAG, "Expense not found locally, nothing to delete")
        }
    }
    
    /**
     * Check if currently listening
     */
    fun isListening(): Boolean = isListening
}
