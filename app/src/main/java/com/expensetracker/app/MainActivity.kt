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
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var expenseSyncCoordinator: com.expensetracker.app.domain.sync.ExpenseSyncCoordinator
    
    @Inject
    lateinit var categorySyncCoordinator: com.expensetracker.app.domain.sync.CategorySyncCoordinator
    
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
                    val settingsViewModel: com.expensetracker.app.presentation.viewmodel.SettingsViewModel = hiltViewModel()
                    val household by settingsViewModel.household.collectAsStateWithLifecycle()
                    val navController = rememberNavController()
                    
                    // Start sync coordinators when household is set up
                    androidx.compose.runtime.LaunchedEffect(household) {
                        android.util.Log.d("DEBUG_SYNC", "=== MainActivity LaunchedEffect ===")
                        android.util.Log.d("DEBUG_SYNC", "Household: $household")
                        
                        if (household != null) {
                            android.util.Log.d("DEBUG_SYNC", "Starting sync coordinators...")
                            expenseSyncCoordinator.startSync()
                            categorySyncCoordinator.startSync()
                            android.util.Log.d("DEBUG_SYNC", "Sync coordinators started")
                        } else {
                            android.util.Log.d("DEBUG_SYNC", "Stopping sync coordinators...")
                            expenseSyncCoordinator.stopSync()
                            categorySyncCoordinator.stopSync()
                        }
                    }
                    
                    // Determine start destination based on auth state and household setup
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
                            // Check if household is set up
                            val startDestination = if (household == null) {
                                NavigationRoutes.HOUSEHOLD_SETUP
                            } else {
                                NavigationRoutes.EXPENSE_LIST
                            }
                            
                            ExpenseTrackerNavHost(
                                navController = navController,
                                startDestination = startDestination,
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
