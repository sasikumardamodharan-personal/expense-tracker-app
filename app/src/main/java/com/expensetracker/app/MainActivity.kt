package com.expensetracker.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.expensetracker.app.domain.model.AuthState
import com.expensetracker.app.presentation.navigation.ExpenseTrackerNavHost
import com.expensetracker.app.presentation.navigation.NavigationRoutes
import com.expensetracker.app.presentation.viewmodel.AuthViewModel
import com.expensetracker.app.ui.theme.ExpenseTrackerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ExpenseTrackerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val authViewModel: AuthViewModel = hiltViewModel()
                    val authState by authViewModel.authState.collectAsStateWithLifecycle()
                    val navController = rememberNavController()
                    
                    // Determine start destination based on auth state
                    when (authState) {
                        is AuthState.Loading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                        is AuthState.Authenticated -> {
                            ExpenseTrackerNavHost(
                                navController = navController,
                                startDestination = NavigationRoutes.EXPENSE_LIST,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        is AuthState.Unauthenticated, is AuthState.Error -> {
                            ExpenseTrackerNavHost(
                                navController = navController,
                                startDestination = NavigationRoutes.SIGN_IN,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }
}
