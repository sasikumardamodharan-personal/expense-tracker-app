package com.expensetracker.app.domain.repository

import com.expensetracker.app.domain.model.Household
import com.expensetracker.app.domain.model.HouseholdMember
import com.expensetracker.app.domain.model.Result
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for household operations
 */
interface HouseholdRepository {
    
    /**
     * Create a new household
     * 
     * @param name The name of the household
     * @param userId The ID of the user creating the household
     * @return Result containing the created Household or an error
     */
    suspend fun createHousehold(name: String, userId: String): Result<Household>
    
    /**
     * Join an existing household using an invite code
     * 
     * @param inviteCode The invite code for the household
     * @param userId The ID of the user joining
     * @return Result containing the joined Household or an error
     */
    suspend fun joinHousehold(inviteCode: String, userId: String): Result<Household>
    
    /**
     * Get a household by ID
     * 
     * @param householdId The ID of the household
     * @return Result containing the Household or an error
     */
    suspend fun getHousehold(householdId: String): Result<Household>
    
    /**
     * Observe changes to a household in real-time
     * 
     * @param householdId The ID of the household to observe
     * @return Flow emitting household updates
     */
    fun observeHousehold(householdId: String): Flow<Household?>
    
    /**
     * Get all members of a household
     * 
     * @param householdId The ID of the household
     * @return Result containing list of HouseholdMembers or an error
     */
    suspend fun getHouseholdMembers(householdId: String): Result<List<HouseholdMember>>
    
    /**
     * Remove a member from a household
     * 
     * @param householdId The ID of the household
     * @param userId The ID of the user to remove
     * @return Result indicating success or error
     */
    suspend fun removeMember(householdId: String, userId: String): Result<Unit>
}
