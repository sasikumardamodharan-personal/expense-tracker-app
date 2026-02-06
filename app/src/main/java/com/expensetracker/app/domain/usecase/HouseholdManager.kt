package com.expensetracker.app.domain.usecase

import android.util.Log
import com.expensetracker.app.data.auth.AuthManager
import com.expensetracker.app.data.preferences.HouseholdPreferencesManager
import com.expensetracker.app.domain.model.Household
import com.expensetracker.app.domain.model.Result
import com.expensetracker.app.domain.repository.HouseholdRepository
import com.expensetracker.app.domain.sync.CategorySyncCoordinator
import com.expensetracker.app.domain.sync.ExpenseSyncCoordinator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages household setup, membership, and lifecycle operations.
 * 
 * This service is the primary interface for household-related operations including:
 * - Creating new households with unique invite codes
 * - Joining existing households using invite codes
 * - Managing household membership and preferences
 * - Coordinating sync initialization when households are set up
 * - Handling household leave operations
 * 
 * The manager integrates with authentication, preferences, and sync coordinators
 * to provide a complete household management solution.
 * 
 * @property householdRepository Repository for Firestore household operations
 * @property householdPreferences Manager for household-related preferences
 * @property authManager Manager for user authentication
 * @property expenseSyncCoordinator Coordinator for expense synchronization
 * @property categorySyncCoordinator Coordinator for category synchronization
 */
@Singleton
class HouseholdManager @Inject constructor(
    private val householdRepository: HouseholdRepository,
    private val householdPreferences: HouseholdPreferencesManager,
    private val authManager: AuthManager,
    private val expenseSyncCoordinator: ExpenseSyncCoordinator,
    private val categorySyncCoordinator: CategorySyncCoordinator
) {
    
    companion object {
        private const val TAG = "HouseholdManager"
    }
    
    /**
     * Flow of the current household
     * 
     * Observes the household ID from preferences and fetches the household data
     */
    val currentHousehold: Flow<Household?> = householdPreferences.householdId
        .flatMapLatest { householdId ->
            if (householdId != null) {
                householdRepository.observeHousehold(householdId)
            } else {
                flowOf(null)
            }
        }
    
    /**
     * Check if household setup is complete
     */
    val isHouseholdSetupComplete: Flow<Boolean> = householdPreferences.isHouseholdSetupComplete
    
    /**
     * Setup household - guides user through create or join flow
     * 
     * This method checks if the user already has a household set up.
     * If not, it returns an error indicating setup is needed.
     * 
     * @return Result containing the Household or an error
     */
    suspend fun setupHousehold(): Result<Household> {
        val currentUser = authManager.getCurrentUser()
        if (currentUser == null) {
            Log.e(TAG, "Cannot setup household: user not authenticated")
            return Result.Error(
                exception = Exception("User not authenticated"),
                message = "Please sign in to setup household"
            )
        }
        
        // Check if household is already set up
        val householdId = householdPreferences.householdId.firstOrNull()
        if (householdId != null) {
            Log.d(TAG, "Household already set up: $householdId")
            return householdRepository.getHousehold(householdId)
        }
        
        Log.w(TAG, "Household not set up")
        return Result.Error(
            exception = Exception("Household not set up"),
            message = "Please create or join a household"
        )
    }
    
    /**
     * Create a new household
     * 
     * @param name The name for the new household
     * @return Result containing the created Household or an error
     */
    suspend fun createNewHousehold(name: String): Result<Household> {
        val currentUser = authManager.getCurrentUser()
        if (currentUser == null) {
            Log.e(TAG, "Cannot create household: user not authenticated")
            return Result.Error(
                exception = Exception("User not authenticated"),
                message = "Please sign in to create a household"
            )
        }
        
        if (name.isBlank()) {
            Log.w(TAG, "Cannot create household: name is blank")
            return Result.Error(
                exception = Exception("Invalid household name"),
                message = "Household name cannot be empty"
            )
        }
        
        Log.d(TAG, "Creating new household: $name")
        
        return when (val result = householdRepository.createHousehold(name, currentUser.uid)) {
            is Result.Success -> {
                val household = result.data
                // Save household to preferences
                householdPreferences.setHousehold(household.id, household.name)
                Log.d(TAG, "Household created and saved to preferences: ${household.id}")
                
                // Start sync coordinators
                startSyncCoordinators()
                
                Result.Success(household)
            }
            is Result.Error -> {
                Log.e(TAG, "Failed to create household", result.exception)
                result
            }
        }
    }
    
    /**
     * Join an existing household using an invite code
     * 
     * @param inviteCode The invite code for the household
     * @return Result containing the joined Household or an error
     */
    suspend fun joinExistingHousehold(inviteCode: String): Result<Household> {
        val currentUser = authManager.getCurrentUser()
        if (currentUser == null) {
            Log.e(TAG, "Cannot join household: user not authenticated")
            return Result.Error(
                exception = Exception("User not authenticated"),
                message = "Please sign in to join a household"
            )
        }
        
        if (inviteCode.isBlank()) {
            Log.w(TAG, "Cannot join household: invite code is blank")
            return Result.Error(
                exception = Exception("Invalid invite code"),
                message = "Invite code cannot be empty"
            )
        }
        
        Log.d(TAG, "Joining household with invite code: $inviteCode")
        
        return when (val result = householdRepository.joinHousehold(inviteCode, currentUser.uid)) {
            is Result.Success -> {
                val household = result.data
                // Save household to preferences
                householdPreferences.setHousehold(household.id, household.name)
                Log.d(TAG, "Joined household and saved to preferences: ${household.id}")
                
                // Start sync coordinators
                startSyncCoordinators()
                
                Result.Success(household)
            }
            is Result.Error -> {
                Log.e(TAG, "Failed to join household", result.exception)
                result
            }
        }
    }
    
    /**
     * Leave the current household
     * 
     * This removes the user from the household and clears local preferences.
     * 
     * @return Result indicating success or error
     */
    suspend fun leaveHousehold(): Result<Unit> {
        val currentUser = authManager.getCurrentUser()
        if (currentUser == null) {
            Log.e(TAG, "Cannot leave household: user not authenticated")
            return Result.Error(
                exception = Exception("User not authenticated"),
                message = "User not authenticated"
            )
        }
        
        val householdId = householdPreferences.householdId.firstOrNull()
        if (householdId == null) {
            Log.w(TAG, "Cannot leave household: no household set up")
            return Result.Error(
                exception = Exception("No household"),
                message = "Not part of any household"
            )
        }
        
        Log.d(TAG, "Leaving household: $householdId")
        
        return when (val result = householdRepository.removeMember(householdId, currentUser.uid)) {
            is Result.Success -> {
                // Stop sync coordinators
                stopSyncCoordinators()
                
                // Clear local preferences
                householdPreferences.clearHouseholdData()
                Log.d(TAG, "Left household and cleared preferences")
                Result.Success(Unit)
            }
            is Result.Error -> {
                Log.e(TAG, "Failed to leave household", result.exception)
                result
            }
        }
    }
    
    /**
     * Start sync coordinators for expenses and categories
     */
    private fun startSyncCoordinators() {
        Log.d(TAG, "Starting sync coordinators")
        expenseSyncCoordinator.startSync()
        categorySyncCoordinator.startSync()
    }
    
    /**
     * Stop sync coordinators
     */
    private fun stopSyncCoordinators() {
        Log.d(TAG, "Stopping sync coordinators")
        expenseSyncCoordinator.stopSync()
        categorySyncCoordinator.stopSync()
    }
    
    /**
     * Get the invite code for the current household
     * 
     * @return Result containing the invite code or an error
     */
    suspend fun getInviteCode(): Result<String> {
        val householdId = householdPreferences.householdId.firstOrNull()
        if (householdId == null) {
            return Result.Error(
                exception = Exception("No household"),
                message = "Not part of any household"
            )
        }
        
        return when (val result = householdRepository.getHousehold(householdId)) {
            is Result.Success -> Result.Success(result.data.inviteCode)
            is Result.Error -> result
        }
    }
}
