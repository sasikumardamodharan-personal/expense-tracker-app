package com.expensetracker.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.data.auth.AuthManager
import com.expensetracker.app.data.firebase.FirebaseUserRepository
import com.expensetracker.app.domain.model.AuthState
import com.expensetracker.app.domain.model.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authManager: AuthManager,
    private val userRepository: FirebaseUserRepository
) : ViewModel() {
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    init {
        checkAuthState()
    }
    
    /**
     * Check current authentication state
     */
    private fun checkAuthState() {
        viewModelScope.launch {
            authManager.currentUser.collect { firebaseUser ->
                _authState.value = if (firebaseUser != null) {
                    val user = authManager.firebaseUserToDomain(firebaseUser)
                    AuthState.Authenticated(user)
                } else {
                    AuthState.Unauthenticated
                }
            }
        }
    }
    
    /**
     * Sign in with Google
     */
    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            android.util.Log.d("AuthViewModel", "Starting sign in with Google")
            _authState.value = AuthState.Loading
            
            when (val result = authManager.signInWithGoogle(idToken)) {
                is Result.Success -> {
                    android.util.Log.d("AuthViewModel", "Sign in successful: ${result.data.email}")
                    // Save user to Firestore
                    val saveResult = userRepository.saveUser(result.data)
                    when (saveResult) {
                        is Result.Success -> {
                            android.util.Log.d("AuthViewModel", "User saved to Firestore")
                        }
                        is Result.Error -> {
                            android.util.Log.e("AuthViewModel", "Failed to save user: ${saveResult.message}")
                        }
                    }
                    _authState.value = AuthState.Authenticated(result.data)
                }
                is Result.Error -> {
                    android.util.Log.e("AuthViewModel", "Sign in failed: ${result.message}")
                    _authState.value = AuthState.Error(result.message)
                }
            }
        }
    }
    
    /**
     * Sign out
     */
    fun signOut() {
        viewModelScope.launch {
            when (authManager.signOut()) {
                is Result.Success -> {
                    _authState.value = AuthState.Unauthenticated
                }
                is Result.Error -> {
                    // Still set to unauthenticated even if sign out fails
                    _authState.value = AuthState.Unauthenticated
                }
            }
        }
    }
    
    /**
     * Reset error state
     */
    fun resetError() {
        if (_authState.value is AuthState.Error) {
            _authState.value = AuthState.Unauthenticated
        }
    }
}
