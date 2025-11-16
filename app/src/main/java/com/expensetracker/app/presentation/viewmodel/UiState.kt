package com.expensetracker.app.presentation.viewmodel

import com.expensetracker.app.data.local.entity.Category
import com.expensetracker.app.domain.model.ExpenseWithCategory
import com.expensetracker.app.domain.model.FilterCriteria
import com.expensetracker.app.domain.model.SpendingSummary

/**
 * UI state for the expense list screen
 */
sealed class ExpenseListUiState {
    object Loading : ExpenseListUiState()
    
    data class Success(
        val expenses: List<ExpenseWithCategory>,
        val activeFilters: FilterCriteria
    ) : ExpenseListUiState()
    
    data class Error(val message: String) : ExpenseListUiState()
    
    object Empty : ExpenseListUiState()
}

/**
 * UI state for the add/edit expense screen
 */
data class AddEditExpenseUiState(
    val amount: String = "",
    val selectedCategory: Category? = null,
    val date: Long = System.currentTimeMillis(),
    val description: String = "",
    val categories: List<Category> = emptyList(),
    val validationErrors: Map<String, String> = emptyMap(),
    val isSaving: Boolean = false,
    val isEditMode: Boolean = false,
    val expenseId: Long? = null
)

/**
 * UI state for the summary screen
 */
sealed class SummaryUiState {
    object Loading : SummaryUiState()
    
    data class Success(val summary: SpendingSummary) : SummaryUiState()
    
    data class Error(val message: String) : SummaryUiState()
}
