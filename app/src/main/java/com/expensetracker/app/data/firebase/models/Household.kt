package com.expensetracker.app.data.firebase.models

import com.google.firebase.Timestamp

/**
 * Firestore model for household data
 * 
 * A household is a shared group where multiple users can view and manage expenses together.
 * Each household has a unique ID, name, creator, and list of member IDs.
 * 
 * Firestore path: /households/{householdId}
 */
data class Household(
    val id: String = "",
    val name: String = "",
    val createdBy: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val memberIds: List<String> = emptyList(),
    val inviteCode: String = ""
) {
    companion object {
        const val COLLECTION_NAME = "households"
        const val MEMBERS_SUBCOLLECTION = "members"
        
        /**
         * Generate a random 6-character invite code
         */
        fun generateInviteCode(): String {
            val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
            return (1..6)
                .map { chars.random() }
                .joinToString("")
        }
    }
}

/**
 * Firestore model for household member data
 * 
 * Stored as a subcollection under each household document.
 * Firestore path: /households/{householdId}/members/{userId}
 */
data class HouseholdMember(
    val userId: String = "",
    val email: String = "",
    val displayName: String = "",
    val joinedAt: Timestamp = Timestamp.now()
)
