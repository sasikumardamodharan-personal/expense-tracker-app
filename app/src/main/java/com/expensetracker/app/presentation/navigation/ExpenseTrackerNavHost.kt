package com.expensetracker.app.presentation.navigation

import androidx.compose.animation.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.expensetracker.app.presentation.screens.AddEditExpenseScreen
import com.expensetracker.app.presentation.screens.CategoryManagementScreen
import com.expensetracker.app.presentation.screens.ExpenseListScreen
import com.expensetracker.app.presentation.screens.FilterScreen
import com.expensetracker.app.presentation.screens.SettingsScreen
import com.expensetracker.app.presentation.screens.SignInScreen
import com.expensetracker.app.presentation.screens.SummaryScreen
import com.expensetracker.app.ui.theme.*

/**
 * Main navigation host for the Expense Tracker app with smooth animations
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ExpenseTrackerNavHost(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = { slideInFromRight() },
        exitTransition = { slideOutToLeft() },
        popEnterTransition = { slideInFromRight() },
        popExitTransition = { slideOutToLeft() }
    ) {
        // Sign In Screen
        composable(
            route = NavigationRoutes.SIGN_IN,
            enterTransition = { fadeIn(animationSpec = fadeInSpec()) },
            exitTransition = { fadeOut(animationSpec = fadeOutSpec()) }
        ) {
            SignInScreen(
                onSignInSuccess = {
                    navController.navigate(NavigationRoutes.EXPENSE_LIST) {
                        popUpTo(NavigationRoutes.SIGN_IN) { inclusive = true }
                    }
                }
            )
        }
        
        // Expense List Screen (Start Destination)
        composable(
            route = NavigationRoutes.EXPENSE_LIST,
            enterTransition = { fadeIn(animationSpec = fadeInSpec()) },
            exitTransition = { fadeOut(animationSpec = fadeOutSpec()) }
        ) { backStackEntry ->
            // Check if we're returning from add/edit with a result
            val shouldRefresh = navController.currentBackStackEntry
                ?.savedStateHandle
                ?.get<Boolean>("expense_saved") ?: false
            
            if (shouldRefresh) {
                navController.currentBackStackEntry
                    ?.savedStateHandle
                    ?.remove<Boolean>("expense_saved")
            }
            
            ExpenseListScreen(
                onNavigateToAddExpense = {
                    navController.navigate(NavigationRoutes.ADD_EXPENSE)
                },
                onNavigateToEditExpense = { expenseId ->
                    navController.navigate(NavigationRoutes.editExpense(expenseId))
                },
                onNavigateToFilter = {
                    navController.navigate(NavigationRoutes.FILTER)
                },
                onNavigateToSummary = {
                    navController.navigate(NavigationRoutes.SUMMARY)
                },
                onNavigateToSettings = {
                    navController.navigate(NavigationRoutes.SETTINGS)
                },
                shouldRefresh = shouldRefresh
            )
        }
        
        // Add Expense Screen
        composable(
            route = NavigationRoutes.ADD_EXPENSE,
            enterTransition = { slideInFromBottom() },
            exitTransition = { slideOutToBottom() },
            popExitTransition = { slideOutToBottom() }
        ) {
            AddEditExpenseScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onExpenseSaved = {
                    // Set result to trigger refresh
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("expense_saved", true)
                    navController.popBackStack()
                }
            )
        }
        
        // Edit Expense Screen (with expenseId parameter)
        composable(
            route = NavigationRoutes.EDIT_EXPENSE,
            arguments = listOf(
                navArgument("expenseId") {
                    type = NavType.LongType
                }
            ),
            enterTransition = { slideInFromBottom() },
            exitTransition = { slideOutToBottom() },
            popExitTransition = { slideOutToBottom() }
        ) {
            AddEditExpenseScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onExpenseSaved = {
                    // Set result to trigger refresh
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("expense_saved", true)
                    navController.popBackStack()
                }
            )
        }
        
        // Filter Screen
        composable(route = NavigationRoutes.FILTER) {
            FilterScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        // Summary Screen
        composable(route = NavigationRoutes.SUMMARY) {
            SummaryScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        // Category Management Screen
        composable(
            route = NavigationRoutes.CATEGORY_MANAGEMENT,
            enterTransition = { slideInFromRight() },
            exitTransition = { slideOutToLeft() },
            popExitTransition = { slideOutToLeft() }
        ) {
            CategoryManagementScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        // Settings Screen
        composable(
            route = NavigationRoutes.SETTINGS,
            enterTransition = { slideInFromRight() },
            exitTransition = { slideOutToLeft() },
            popExitTransition = { slideOutToLeft() }
        ) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToCategoryManagement = {
                    navController.navigate(NavigationRoutes.CATEGORY_MANAGEMENT)
                },
                onSignOut = {
                    navController.navigate(NavigationRoutes.SIGN_IN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
