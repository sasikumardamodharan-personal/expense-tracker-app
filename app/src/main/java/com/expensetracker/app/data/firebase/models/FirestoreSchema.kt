package com.expensetracker.app.data.firebase.models

/**
 * Firestore Collection Structure and Schema Definition
 * 
 * This file documents the Firestore database structure for the expense tracker app.
 * 
 * Collection Hierarchy:
 * 
 * firestore/
 * ├── households/
 * │   └── {householdId}/
 * │       ├── id: String
 * │       ├── name: String
 * │       ├── createdBy: String (userId)
 * │       ├── createdAt: Timestamp
 * │       ├── memberIds: List<String>
 * │       ├── inviteCode: String
 * │       └── members/ (subcollection)
 * │           └── {userId}/
 * │               ├── userId: String
 * │               ├── email: String
 * │               ├── displayName: String
 * │               └── joinedAt: Timestamp
 * │
 * ├── expenses/
 * │   └── {householdId}/
 * │       └── expenses/ (subcollection)
 * │           └── {expenseId}/
 * │               ├── id: String
 * │               ├── householdId: String
 * │               ├── amount: Double
 * │               ├── description: String
 * │               ├── categoryId: Long
 * │               ├── date: Timestamp
 * │               ├── createdBy: String (userId)
 * │               ├── createdAt: Timestamp
 * │               ├── modifiedBy: String (userId)
 * │               ├── modifiedAt: Timestamp
 * │               └── isDeleted: Boolean
 * │
 * └── categories/
 *     └── {householdId}/
 *         └── categories/ (subcollection)
 *             └── {categoryId}/
 *                 ├── id: String
 *                 ├── householdId: String
 *                 ├── name: String
 *                 ├── iconName: String
 *                 ├── colorHex: String
 *                 ├── isCustom: Boolean
 *                 ├── sortOrder: Int
 *                 ├── createdBy: String (userId)
 *                 ├── createdAt: Timestamp
 *                 ├── modifiedBy: String (userId)
 *                 ├── modifiedAt: Timestamp
 *                 └── isDeleted: Boolean
 */
object FirestoreSchema {
    
    // Root collections
    object Collections {
        const val HOUSEHOLDS = "households"
        const val EXPENSES = "expenses"
        const val CATEGORIES = "categories"
        const val USERS = "users"
    }
    
    // Subcollections
    object Subcollections {
        const val MEMBERS = "members"
        const val EXPENSES = "expenses"
        const val CATEGORIES = "categories"
    }
    
    // Field names for Household documents
    object HouseholdFields {
        const val ID = "id"
        const val NAME = "name"
        const val CREATED_BY = "createdBy"
        const val CREATED_AT = "createdAt"
        const val MEMBER_IDS = "memberIds"
        const val INVITE_CODE = "inviteCode"
    }
    
    // Field names for HouseholdMember documents
    object MemberFields {
        const val USER_ID = "userId"
        const val EMAIL = "email"
        const val DISPLAY_NAME = "displayName"
        const val JOINED_AT = "joinedAt"
    }
    
    // Field names for FirestoreExpense documents
    object ExpenseFields {
        const val ID = "id"
        const val HOUSEHOLD_ID = "householdId"
        const val AMOUNT = "amount"
        const val DESCRIPTION = "description"
        const val CATEGORY_ID = "categoryId"
        const val DATE = "date"
        const val CREATED_BY = "createdBy"
        const val CREATED_AT = "createdAt"
        const val MODIFIED_BY = "modifiedBy"
        const val MODIFIED_AT = "modifiedAt"
        const val IS_DELETED = "isDeleted"
    }
    
    // Field names for FirestoreCategory documents
    object CategoryFields {
        const val ID = "id"
        const val HOUSEHOLD_ID = "householdId"
        const val NAME = "name"
        const val ICON_NAME = "iconName"
        const val COLOR_HEX = "colorHex"
        const val IS_CUSTOM = "isCustom"
        const val SORT_ORDER = "sortOrder"
        const val CREATED_BY = "createdBy"
        const val CREATED_AT = "createdAt"
        const val MODIFIED_BY = "modifiedBy"
        const val MODIFIED_AT = "modifiedAt"
        const val IS_DELETED = "isDeleted"
    }
    
    // Field names for FirebaseUser documents
    object UserFields {
        const val ID = "id"
        const val EMAIL = "email"
        const val DISPLAY_NAME = "displayName"
        const val PHOTO_URL = "photoUrl"
        const val CREATED_AT = "createdAt"
    }
    
    /**
     * Get the Firestore path for a household document
     */
    fun getHouseholdPath(householdId: String): String {
        return "${Collections.HOUSEHOLDS}/$householdId"
    }
    
    /**
     * Get the Firestore path for a household member document
     */
    fun getHouseholdMemberPath(householdId: String, userId: String): String {
        return "${Collections.HOUSEHOLDS}/$householdId/${Subcollections.MEMBERS}/$userId"
    }
    
    /**
     * Get the Firestore path for an expense document
     */
    fun getExpensePath(householdId: String, expenseId: String): String {
        return "${Collections.EXPENSES}/$householdId/${Subcollections.EXPENSES}/$expenseId"
    }
    
    /**
     * Get the Firestore collection reference path for household expenses
     */
    fun getHouseholdExpensesCollectionPath(householdId: String): String {
        return "${Collections.EXPENSES}/$householdId/${Subcollections.EXPENSES}"
    }
    
    /**
     * Get the Firestore collection reference path for household members
     */
    fun getHouseholdMembersCollectionPath(householdId: String): String {
        return "${Collections.HOUSEHOLDS}/$householdId/${Subcollections.MEMBERS}"
    }
    
    /**
     * Get the Firestore path for a category document
     */
    fun getCategoryPath(householdId: String, categoryId: String): String {
        return "${Collections.CATEGORIES}/$householdId/${Subcollections.CATEGORIES}/$categoryId"
    }
    
    /**
     * Get the Firestore collection reference path for household categories
     */
    fun getHouseholdCategoriesCollectionPath(householdId: String): String {
        return "${Collections.CATEGORIES}/$householdId/${Subcollections.CATEGORIES}"
    }
}
