package com.expensetracker.app.data.firebase.models

import com.google.firebase.Timestamp
import com.expensetracker.app.data.local.entity.Expense

/**
 * Firestore model for expense data
 * 
 * This model represents an expense stored in Firestore, with additional metadata
 * for tracking creation, modification, and soft deletion.
 * 
 * Firestore path: /expenses/{householdId}/expenses/{expenseId}
 */
data class FirestoreExpense(
    val id: String = "",
    val householdId: String = "",
    val amount: Double = 0.0,
    val description: String = "",
    val categoryId: Long = 0L,
    val date: Timestamp = Timestamp.now(),
    val createdBy: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val modifiedBy: String = "",
    val modifiedAt: Timestamp = Timestamp.now(),
    val isDeleted: Boolean = false
) {
    companion object {
        const val COLLECTION_NAME = "expenses"
        const val EXPENSES_SUBCOLLECTION = "expenses"
        
        /**
         * Convert from Room Expense entity to FirestoreExpense
         * 
         * @param expense The local Room expense entity
         * @param householdId The household ID this expense belongs to
         * @param userId The user ID who created/modified the expense
         * @param firestoreId Optional Firestore document ID (if already synced)
         * @return FirestoreExpense model ready for Firestore upload
         */
        fun fromExpense(
            expense: Expense,
            householdId: String,
            userId: String,
            firestoreId: String? = null
        ): FirestoreExpense {
            val now = Timestamp.now()
            return FirestoreExpense(
                id = firestoreId ?: "",
                householdId = householdId,
                amount = expense.amount,
                description = expense.description,
                categoryId = expense.categoryId,
                date = Timestamp(java.util.Date(expense.date)),
                createdBy = userId,
                createdAt = Timestamp(java.util.Date(expense.createdAt)),
                modifiedBy = userId,
                modifiedAt = Timestamp(java.util.Date(expense.updatedAt)),
                isDeleted = false
            )
        }
    }
    
    /**
     * Convert FirestoreExpense to Room Expense entity
     * 
     * @param localId Optional local Room database ID (for updates)
     * @return Room Expense entity
     */
    fun toExpense(localId: Long = 0L): Expense {
        return Expense(
            id = localId,
            amount = amount,
            categoryId = categoryId,
            date = date.toDate().time,
            description = description,
            createdAt = createdAt.toDate().time,
            updatedAt = modifiedAt.toDate().time
        )
    }
}
