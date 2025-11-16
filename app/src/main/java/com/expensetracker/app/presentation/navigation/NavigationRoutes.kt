package com.expensetracker.app.presentation.navigation

/**
 * Navigation routes for the Expense Tracker app
 */
object NavigationRoutes {
    const val EXPENSE_LIST = "expense_list"
    const val ADD_EXPENSE = "add_expense"
    const val EDIT_EXPENSE = "edit_expense/{expenseId}"
    const val FILTER = "filter"
    const val SUMMARY = "summary"
    
    /**
     * Creates the route for editing an expense with the given ID
     */
    fun editExpense(expenseId: Long): String {
        return "edit_expense/$expenseId"
    }
}
