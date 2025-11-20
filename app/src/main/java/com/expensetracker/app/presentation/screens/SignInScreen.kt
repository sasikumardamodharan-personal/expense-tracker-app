package com.expensetracker.app.presentation.screens

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expensetracker.app.R
import com.expensetracker.app.domain.model.AuthState
import com.expensetracker.app.presentation.viewmodel.AuthViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

@Composable
fun SignInScreen(
    onSignInSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    // Google Sign-In launcher
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        android.util.Log.d("SignInScreen", "Result code: ${result.resultCode}")
        
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                android.util.Log.d("SignInScreen", "Account: ${account.email}, ID Token: ${account.idToken != null}")
                
                if (account.idToken != null) {
                    viewModel.signInWithGoogle(account.idToken!!)
                } else {
                    Toast.makeText(
                        context,
                        "Failed to get ID token. Please try again.",
                        Toast.LENGTH_LONG
                    ).show()
                    android.util.Log.e("SignInScreen", "ID Token is null")
                }
            } catch (e: ApiException) {
                val errorMessage = "Google sign in failed: ${e.statusCode} - ${e.message}"
                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                android.util.Log.e("SignInScreen", errorMessage, e)
            }
        } else if (result.resultCode == Activity.RESULT_CANCELED) {
            Toast.makeText(context, "Sign in cancelled", Toast.LENGTH_SHORT).show()
            android.util.Log.d("SignInScreen", "Sign in cancelled by user")
        } else {
            Toast.makeText(context, "Sign in failed with code: ${result.resultCode}", Toast.LENGTH_LONG).show()
            android.util.Log.e("SignInScreen", "Sign in failed with result code: ${result.resultCode}")
        }
    }
    
    // Handle auth state changes
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Authenticated -> {
                onSignInSuccess()
            }
            is AuthState.Error -> {
                Toast.makeText(
                    context,
                    (authState as AuthState.Error).message,
                    Toast.LENGTH_LONG
                ).show()
                viewModel.resetError()
            }
            else -> {}
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // App Icon/Logo
            Text(
                text = "💰",
                style = MaterialTheme.typography.displayLarge,
                fontSize = MaterialTheme.typography.displayLarge.fontSize * 2
            )
            
            // App Name
            Text(
                text = "Expense Tracker",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Welcome Message
            Text(
                text = "Track your expenses with family and friends",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Sign In Button or Loading
            when (authState) {
                is AuthState.Loading -> {
                    CircularProgressIndicator()
                }
                is AuthState.Unauthenticated, is AuthState.Error -> {
                    GoogleSignInButton(
                        onClick = {
                            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                .requestIdToken(context.getString(R.string.default_web_client_id))
                                .requestEmail()
                                .build()
                            
                            val googleSignInClient = GoogleSignIn.getClient(context, gso)
                            launcher.launch(googleSignInClient.signInIntent)
                        },
                        enabled = authState !is AuthState.Loading
                    )
                }
                else -> {}
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Privacy Note
            Text(
                text = "By signing in, you agree to sync your data with Firebase",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun GoogleSignInButton(
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Google Icon (using emoji for simplicity)
            Text(
                text = "G",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Sign in with Google",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
