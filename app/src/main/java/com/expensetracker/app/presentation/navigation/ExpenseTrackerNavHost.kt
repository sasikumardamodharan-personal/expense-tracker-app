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
import com.expensetracker.app.presentation.screens.ExpenseListScreen
import com.expensetracker.app.presentation.screens.FilterScreen
import com.expensetracker.app.presentation.screens.SummaryScreen
import com.expensetracker.app.ui.theme.*

/**
 * Main navigation host for the Expense Tracker app with smooth animations
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ExpenseTrackerNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = NavigationRoutes.EXPENSE_LIST,
        modifier = modifier,
        enterTransition = { slideInFromRight() },
        exitTransition = { slideOutToLeft() },
        popEnterTransition = { slideInFromRight() },
        popExitTransition = { slideOutToLeft() }
    ) {
        // Expense List Screen (Start Destination)
        composable(
            route = NavigationRoutes.EXPENSE_LIST,
            enterTransition = { fadeIn(animationSpec = fadeInSpec()) },
            exitTransition = { fadeOut(animationSpec = fadeOutSpec()) }
        ) {
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
                }
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
    }
}
