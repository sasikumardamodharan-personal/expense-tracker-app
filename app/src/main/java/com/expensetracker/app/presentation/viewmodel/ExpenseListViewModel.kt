package com.expensetracker.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.expensetracker.app.data.local.entity.Category
import com.expensetracker.app.data.local.entity.Expense
import com.expensetracker.app.domain.model.ExpenseWithCategory
import com.expensetracker.app.domain.model.FilterCriteria
import com.expensetracker.app.domain.model.Result
import com.expensetracker.app.domain.repository.CategoryRepository
import com.expensetracker.app.domain.usecase.DeleteExpenseUseCase
import com.expensetracker.app.domain.usecase.GetFilteredExpensesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExpenseListViewModel @Inject constructor(
    private val getFilteredExpensesUseCase: GetFilteredExpensesUseCase,
    private val deleteExpenseUseCase: DeleteExpenseUseCase,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ExpenseListUiState>(ExpenseListUiState.Loading)
    val uiState: StateFlow<ExpenseListUiState> = _uiState.asStateFlow()

    private val _filterCriteria = MutableStateFlow(FilterCriteria())
    val filterCriteria: StateFlow<FilterCriteria> = _filterCriteria.asStateFlow()
    
    private val _errorMessage = MutableSharedFlow<String>()
    val errorMessage: SharedFlow<String> = _errorMessage.asSharedFlow()
    
    private val _pagedExpenses = MutableStateFlow<PagingData<ExpenseWithCategory>>(PagingData.empty())
    val pagedExpenses: StateFlow<PagingData<ExpenseWithCategory>> = _pagedExpenses.asStateFlow()

    init {
        loadExpenses()
        loadPagedExpenses()
    }
    
    /**
     * Load paged expenses based on current filter criteria
     */
    private fun loadPagedExpenses() {
        viewModelScope.launch {
            _filterCriteria
                .flatMapLatest { criteria ->
                    getFilteredExpensesUseCase.invokePaged(criteria)
                }
                .cachedIn(viewModelScope)
                .collect { pagingData ->
                    _pagedExpenses.value = pagingData
                }
        }
    }
    
    /**
     * Refresh the expense list
     */
    fun refreshExpenses() {
        loadExpenses()
        // Trigger reload by updating filter criteria (even if unchanged)
        _filterCriteria.value = _filterCriteria.value.copy()
    }

    /**
     * Load expenses based on current filter criteria
     */
    fun loadExpenses() {
        viewModelScope.launch {
            _uiState.value = ExpenseListUiState.Loading
            
            getFilteredExpensesUseCase(_filterCriteria.value)
                .catch { exception ->
                    _uiState.value = ExpenseListUiState.Error(
                        exception.message ?: "Failed to load expenses"
                    )
                }
                .collect { expenses ->
                    _uiState.value = if (expenses.isEmpty()) {
                        ExpenseListUiState.Empty
                    } else {
                        ExpenseListUiState.Success(
                            expenses = expenses,
                            activeFilters = _filterCriteria.value
                        )
                    }
                }
        }
    }

    /**
     * Apply filter criteria to the expense list
     */
    fun applyFilter(criteria: FilterCriteria) {
        _filterCriteria.value = criteria
        loadExpenses()
    }

    /**
     * Clear all active filters and reload expenses
     */
    fun clearFilters() {
        _filterCriteria.value = FilterCriteria()
        loadExpenses()
    }

    /**
     * Delete an expense with confirmation
     */
    fun deleteExpense(expenseWithCategory: ExpenseWithCategory) {
        viewModelScope.launch {
            // Convert ExpenseWithCategory to Expense entity
            val expense = Expense(
                id = expenseWithCategory.id,
                amount = expenseWithCategory.amount,
                categoryId = expenseWithCategory.category.id,
                date = expenseWithCategory.date,
                description = expenseWithCategory.description,
                createdAt = expenseWithCategory.createdAt,
                updatedAt = expenseWithCategory.updatedAt
            )
            
            when (val result = deleteExpenseUseCase(expense)) {
                is Result.Success -> {
                    // Expense deleted successfully, list will update automatically via Flow
                }
                is Result.Error -> {
                    // Show error message as toast
                    _errorMessage.emit(result.message)
                    // Keep current state but show error
                    val currentState = _uiState.value
                    if (currentState is ExpenseListUiState.Success) {
                        // Keep the list visible but notify about error
                        _uiState.value = currentState
                    } else {
                        _uiState.value = ExpenseListUiState.Error(result.message)
                    }
                }
            }
        }
    }

    /**
     * Load all categories for filtering
     */
    fun loadCategories(): Flow<List<Category>> {
        return categoryRepository.getAllCategories()
    }
}
