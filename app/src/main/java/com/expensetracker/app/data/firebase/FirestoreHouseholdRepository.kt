package com.expensetracker.app.data.firebase

import android.util.Log
import com.expensetracker.app.data.firebase.models.Household as FirestoreHousehold
import com.expensetracker.app.data.firebase.models.HouseholdMember as FirestoreHouseholdMember
import com.expensetracker.app.domain.model.Household
import com.expensetracker.app.domain.model.HouseholdMember
import com.expensetracker.app.domain.model.Result
import com.expensetracker.app.domain.repository.HouseholdRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firestore implementation of HouseholdRepository
 */
@Singleton
class FirestoreHouseholdRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) : HouseholdRepository {
    
    private val householdsCollection = firestore.collection(FirestoreHousehold.COLLECTION_NAME)
    
    companion object {
        private const val TAG = "FirestoreHouseholdRepo"
    }
    
    override suspend fun createHousehold(name: String, userId: String): Result<Household> {
        return try {
            Log.d(TAG, "Creating household: $name for user: $userId")
            
            // Generate unique invite code
            val inviteCode = FirestoreHousehold.generateInviteCode()
            
            // Create household document
            val householdRef = householdsCollection.document()
            val householdId = householdRef.id
            
            val firestoreHousehold = FirestoreHousehold(
                id = householdId,
                name = name,
                createdBy = userId,
                createdAt = Timestamp.now(),
                memberIds = listOf(userId),
                inviteCode = inviteCode
            )
            
            // Save household
            householdRef.set(firestoreHousehold).await()
            
            // Add creator as first member
            val memberRef = householdRef
                .collection(FirestoreHousehold.MEMBERS_SUBCOLLECTION)
                .document(userId)
            
            // Get user info from users collection
            val userDoc = firestore.collection("users").document(userId).get().await()
            val email = userDoc.getString("email") ?: ""
            val displayName = userDoc.getString("displayName") ?: "User"
            
            val member = FirestoreHouseholdMember(
                userId = userId,
                email = email,
                displayName = displayName,
                joinedAt = Timestamp.now()
            )
            
            memberRef.set(member).await()
            
            Log.d(TAG, "Household created successfully: $householdId")
            Result.Success(firestoreHousehold.toDomain())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create household: ${e.javaClass.simpleName} - ${e.message}", e)
            Result.Error(
                exception = e,
                message = e.message ?: "Failed to create household"
            )
        }
    }
    
    override suspend fun joinHousehold(inviteCode: String, userId: String): Result<Household> {
        return try {
            Log.d(TAG, "User $userId joining household with invite code: $inviteCode")
            
            // Find household by invite code
            val querySnapshot = householdsCollection
                .whereEqualTo("inviteCode", inviteCode)
                .limit(1)
                .get()
                .await()
            
            if (querySnapshot.isEmpty) {
                Log.w(TAG, "No household found with invite code: $inviteCode")
                return Result.Error(
                    exception = IllegalArgumentException("Invalid invite code"),
                    message = "No household found with this invite code. Please check the code and try again."
                )
            }
            
            val householdDoc = querySnapshot.documents[0]
            val householdId = householdDoc.id
            val household = householdDoc.toObject(FirestoreHousehold::class.java)
                ?: return Result.Error(
                    exception = IllegalStateException("Failed to parse household"),
                    message = "Failed to load household data. Please try again."
                )
            
            // Check if user is already a member
            if (household.memberIds.contains(userId)) {
                Log.d(TAG, "User $userId is already a member of household $householdId")
                return Result.Success(household.toDomain())
            }
            
            // Add user to memberIds array
            householdDoc.reference.update("memberIds", FieldValue.arrayUnion(userId)).await()
            
            // Add member document
            val memberRef = householdDoc.reference
                .collection(FirestoreHousehold.MEMBERS_SUBCOLLECTION)
                .document(userId)
            
            // Get user info
            val userDoc = firestore.collection("users").document(userId).get().await()
            val email = userDoc.getString("email") ?: ""
            val displayName = userDoc.getString("displayName") ?: "User"
            
            val member = FirestoreHouseholdMember(
                userId = userId,
                email = email,
                displayName = displayName,
                joinedAt = Timestamp.now()
            )
            
            memberRef.set(member).await()
            
            // Get updated household
            val updatedHousehold = household.copy(
                memberIds = household.memberIds + userId
            )
            
            Log.d(TAG, "User $userId successfully joined household $householdId")
            Result.Success(updatedHousehold.toDomain())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to join household: ${e.javaClass.simpleName} - ${e.message}", e)
            Result.Error(
                exception = e,
                message = e.message ?: "Failed to join household"
            )
        }
    }
    
    override suspend fun getHousehold(householdId: String): Result<Household> {
        return try {
            Log.d(TAG, "Getting household: $householdId")
            
            val document = householdsCollection.document(householdId).get().await()
            
            if (!document.exists()) {
                Log.w(TAG, "Household not found: $householdId")
                return Result.Error(
                    exception = IllegalArgumentException("Household not found"),
                    message = "Household not found. It may have been deleted."
                )
            }
            
            val firestoreHousehold = document.toObject(FirestoreHousehold::class.java)
                ?: return Result.Error(
                    exception = IllegalStateException("Failed to parse household"),
                    message = "Failed to load household data. Please try again."
                )
            
            Log.d(TAG, "Household retrieved successfully: $householdId")
            Result.Success(firestoreHousehold.toDomain())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get household: ${e.javaClass.simpleName} - ${e.message}", e)
            Result.Error(
                exception = e,
                message = e.message ?: "Failed to get household"
            )
        }
    }
    
    override fun observeHousehold(householdId: String): Flow<Household?> = callbackFlow {
        Log.d(TAG, "Starting to observe household: $householdId")
        
        val listener = householdsCollection.document(householdId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error observing household", error)
                    trySend(null)
                    return@addSnapshotListener
                }
                
                if (snapshot != null && snapshot.exists()) {
                    val firestoreHousehold = snapshot.toObject(FirestoreHousehold::class.java)
                    trySend(firestoreHousehold?.toDomain())
                } else {
                    trySend(null)
                }
            }
        
        awaitClose {
            Log.d(TAG, "Stopping observation of household: $householdId")
            listener.remove()
        }
    }
    
    override suspend fun getHouseholdMembers(householdId: String): Result<List<HouseholdMember>> {
        return try {
            Log.d(TAG, "Getting members for household: $householdId")
            
            val membersSnapshot = householdsCollection
                .document(householdId)
                .collection(FirestoreHousehold.MEMBERS_SUBCOLLECTION)
                .get()
                .await()
            
            val members = membersSnapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(FirestoreHouseholdMember::class.java)?.toDomain()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse member document ${doc.id}", e)
                    null
                }
            }
            
            Log.d(TAG, "Retrieved ${members.size} members for household: $householdId")
            Result.Success(members)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get household members: ${e.javaClass.simpleName} - ${e.message}", e)
            Result.Error(
                exception = e,
                message = e.message ?: "Failed to get household members"
            )
        }
    }
    
    override suspend fun removeMember(householdId: String, userId: String): Result<Unit> {
        return try {
            Log.d(TAG, "Removing user $userId from household $householdId")
            
            // Remove from memberIds array
            householdsCollection.document(householdId)
                .update("memberIds", FieldValue.arrayRemove(userId))
                .await()
            
            // Delete member document
            householdsCollection
                .document(householdId)
                .collection(FirestoreHousehold.MEMBERS_SUBCOLLECTION)
                .document(userId)
                .delete()
                .await()
            
            Log.d(TAG, "User $userId removed from household $householdId")
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove member: ${e.javaClass.simpleName} - ${e.message}", e)
            Result.Error(
                exception = e,
                message = e.message ?: "Failed to remove member"
            )
        }
    }
}

/**
 * Extension functions to convert between Firestore and domain models
 */
private fun FirestoreHousehold.toDomain(): Household {
    return Household(
        id = id,
        name = name,
        createdBy = createdBy,
        createdAt = createdAt.toDate().time,
        memberIds = memberIds,
        inviteCode = inviteCode
    )
}

private fun FirestoreHouseholdMember.toDomain(): HouseholdMember {
    return HouseholdMember(
        userId = userId,
        email = email,
        displayName = displayName,
        joinedAt = joinedAt.toDate().time
    )
}
