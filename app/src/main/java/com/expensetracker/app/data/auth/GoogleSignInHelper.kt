package com.expensetracker.app.data.auth

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.common.api.ApiException
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper class for Google Sign-In using One Tap
 */
@Singleton
class GoogleSignInHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val oneTapClient: SignInClient = Identity.getSignInClient(context)
    
    /**
     * Build the sign-in request
     */
    fun buildSignInRequest(serverClientId: String): BeginSignInRequest {
        return BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setServerClientId(serverClientId)
                    .setFilterByAuthorizedAccounts(false)
                    .build()
            )
            .setAutoSelectEnabled(true)
            .build()
    }
    
    /**
     * Begin sign-in flow
     */
    suspend fun beginSignIn(
        serverClientId: String,
        launcher: ActivityResultLauncher<IntentSenderRequest>
    ): Result<Unit> {
        return try {
            val result = oneTapClient.beginSignIn(buildSignInRequest(serverClientId))
                .addOnSuccessListener { signInResult ->
                    val intentSenderRequest = IntentSenderRequest.Builder(
                        signInResult.pendingIntent.intentSender
                    ).build()
                    launcher.launch(intentSenderRequest)
                }
                .addOnFailureListener { e ->
                    // Sign-in failed
                }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get ID token from sign-in result
     */
    fun getIdTokenFromIntent(data: Intent?): String? {
        return try {
            val credential = oneTapClient.getSignInCredentialFromIntent(data)
            credential.googleIdToken
        } catch (e: ApiException) {
            null
        }
    }
}
