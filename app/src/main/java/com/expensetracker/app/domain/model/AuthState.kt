package com.expensetracker.app.domain.model

/**
 * Sealed class representing the authentication state of the user
 */
sealed class AuthState {
    /**
     * Authentication state is being determined
     */
    object Loading : AuthState()
    
    /**
     * User is not authenticated
     */
    object Unauthenticated : AuthState()
    
    /**
     * User is authenticated with their user data
     */
    data class Authenticated(val user: User) : AuthState()
    
    /**
     * Authentication error occurred
     */
    data class Error(val message: String) : AuthState()
}
