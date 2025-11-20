package com.expensetracker.app.data.auth

import android.content.Context
import com.expensetracker.app.domain.model.Result
import com.expensetracker.app.domain.model.User
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages Firebase Authentication
 */
@Singleton
class AuthManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val auth: FirebaseAuth
) {
    
    /**
     * Flow of current Firebase user
     */
    val currentUser: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }
    
    /**
     * Get current user synchronously
     */
    fun getCurrentUser(): FirebaseUser? = auth.currentUser
    
    /**
     * Sign in with Google ID token
     */
    suspend fun signInWithGoogle(idToken: String): Result<User> {
        return try {
            android.util.Log.d("AuthManager", "Creating credential with ID token")
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            
            android.util.Log.d("AuthManager", "Signing in with Firebase")
            val authResult = auth.signInWithCredential(credential).await()
            
            val firebaseUser = authResult.user
            if (firebaseUser != null) {
                android.util.Log.d("AuthManager", "Firebase sign in successful: ${firebaseUser.email}")
                val user = User(
                    id = firebaseUser.uid,
                    email = firebaseUser.email ?: "",
                    displayName = firebaseUser.displayName ?: "User",
                    photoUrl = firebaseUser.photoUrl?.toString()
                )
                Result.Success(user)
            } else {
                android.util.Log.e("AuthManager", "Firebase user is null after sign in")
                Result.Error(
                    exception = Exception("Sign in failed"),
                    message = "Failed to sign in with Google"
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("AuthManager", "Sign in exception: ${e.message}", e)
            Result.Error(
                exception = e,
                message = "Sign in failed: ${e.message}"
            )
        }
    }
    
    /**
     * Sign out
     */
    suspend fun signOut(): Result<Unit> {
        return try {
            // Sign out from Firebase
            auth.signOut()
            
            // Sign out from Google
            val googleSignInClient = GoogleSignIn.getClient(
                context,
                GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
            )
            googleSignInClient.signOut().await()
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(
                exception = e,
                message = "Sign out failed: ${e.message}"
            )
        }
    }
    
    /**
     * Convert Firebase user to domain User
     */
    fun firebaseUserToDomain(firebaseUser: FirebaseUser): User {
        return User(
            id = firebaseUser.uid,
            email = firebaseUser.email ?: "",
            displayName = firebaseUser.displayName ?: "User",
            photoUrl = firebaseUser.photoUrl?.toString()
        )
    }
}
