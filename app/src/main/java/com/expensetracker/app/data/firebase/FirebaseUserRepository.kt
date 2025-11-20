package com.expensetracker.app.data.firebase

import com.expensetracker.app.data.firebase.models.FirebaseUser
import com.expensetracker.app.domain.model.Result
import com.expensetracker.app.domain.model.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for user data in Firestore
 */
@Singleton
class FirebaseUserRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    
    private val usersCollection = firestore.collection("users")
    
    /**
     * Create or update user in Firestore
     */
    suspend fun saveUser(user: User): Result<Unit> {
        return try {
            val firebaseUser = FirebaseUser.fromDomain(user)
            usersCollection.document(user.id)
                .set(firebaseUser)
                .await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(
                exception = e,
                message = "Failed to save user: ${e.message}"
            )
        }
    }
    
    /**
     * Get user from Firestore
     */
    suspend fun getUser(userId: String): Result<User?> {
        return try {
            val document = usersCollection.document(userId).get().await()
            val firebaseUser = document.toObject(FirebaseUser::class.java)
            Result.Success(firebaseUser?.toDomain())
        } catch (e: Exception) {
            Result.Error(
                exception = e,
                message = "Failed to get user: ${e.message}"
            )
        }
    }
    
    /**
     * Update user in Firestore
     */
    suspend fun updateUser(user: User): Result<Unit> {
        return saveUser(user) // Same as save for now
    }
    
    /**
     * Delete user from Firestore
     */
    suspend fun deleteUser(userId: String): Result<Unit> {
        return try {
            usersCollection.document(userId).delete().await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(
                exception = e,
                message = "Failed to delete user: ${e.message}"
            )
        }
    }
}
