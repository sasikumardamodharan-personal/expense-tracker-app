package com.expensetracker.app.data.firebase.models

import com.google.firebase.Timestamp
import com.expensetracker.app.data.local.entity.Category

/**
 * Firestore model for category data
 * 
 * This model represents a category stored in Firestore, with additional metadata
 * for tracking creation, modification, and household association.
 * 
 * Firestore path: /categories/{householdId}/categories/{categoryId}
 */
data class FirestoreCategory(
    val id: String = "",
    val householdId: String = "",
    val name: String = "",
    val iconName: String = "",
    val colorHex: String = "",
    val isCustom: Boolean = false,
    val sortOrder: Int = 0,
    val createdBy: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val modifiedBy: String = "",
    val modifiedAt: Timestamp = Timestamp.now(),
    val isDeleted: Boolean = false
) {
    companion object {
        const val COLLECTION_NAME = "categories"
        const val CATEGORIES_SUBCOLLECTION = "categories"
        
        /**
         * Convert from Room Category entity to FirestoreCategory
         * 
         * @param category The local Room category entity
         * @param householdId The household ID this category belongs to
         * @param userId The user ID who created/modified the category
         * @param firestoreId Optional Firestore document ID (if already synced)
         * @return FirestoreCategory model ready for Firestore upload
         */
        fun fromCategory(
            category: Category,
            householdId: String,
            userId: String,
            firestoreId: String? = null
        ): FirestoreCategory {
            val now = Timestamp.now()
            return FirestoreCategory(
                id = firestoreId ?: "",
                householdId = householdId,
                name = category.name,
                iconName = category.iconName,
                colorHex = category.colorHex,
                isCustom = category.isCustom,
                sortOrder = category.sortOrder,
                createdBy = userId,
                createdAt = now,
                modifiedBy = userId,
                modifiedAt = now,
                isDeleted = false
            )
        }
    }
    
    /**
     * Convert FirestoreCategory to Room Category entity
     * 
     * @param localId Optional local Room database ID (for updates)
     * @return Room Category entity
     */
    fun toCategory(localId: Long = 0L): Category {
        return Category(
            id = localId,
            name = name,
            iconName = iconName,
            colorHex = colorHex,
            isCustom = isCustom,
            sortOrder = sortOrder
        )
    }
}
