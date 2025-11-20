package com.expensetracker.app.data.firebase.models

import com.google.firebase.Timestamp
import com.expensetracker.app.domain.model.User

/**
 * Firestore model for user data
 */
data class FirebaseUser(
    val id: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String? = null,
    val createdAt: Timestamp = Timestamp.now()
) {
    /**
     * Convert to domain model
     */
    fun toDomain(): User {
        return User(
            id = id,
            email = email,
            displayName = displayName,
            photoUrl = photoUrl,
            createdAt = createdAt.toDate().time
        )
    }
    
    companion object {
        /**
         * Convert from domain model
         */
        fun fromDomain(user: User): FirebaseUser {
            return FirebaseUser(
                id = user.id,
                email = user.email,
                displayName = user.displayName,
                photoUrl = user.photoUrl,
                createdAt = Timestamp(java.util.Date(user.createdAt))
            )
        }
    }
}
