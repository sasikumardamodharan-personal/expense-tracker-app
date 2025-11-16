package com.expensetracker.app.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.data.local.entity.Category
import com.expensetracker.app.data.local.entity.Expense
import com.expensetracker.app.domain.model.Result
import com.expensetracker.app.domain.repository.CategoryRepository
import com.expensetracker.app.domain.repository.ExpenseRepository
import com.expensetracker.app.domain.usecase.AddExpenseUseCase
import com.expensetracker.app.domain.usecase.UpdateExpenseUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddEditExpenseViewModel @Inject constructor(
    private val addExpenseUseCase: AddExpenseUseCase,
    private val updateExpenseUseCase: UpdateExpenseUseCase,
    private val categoryRepository: CategoryRepository,
    private val expenseRepository: ExpenseRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditExpenseUiState())
    val uiState: StateFlow<AddEditExpenseUiState> = _uiState.asStateFlow()

    private val _saveSuccess = MutableSharedFlow<Boolean>()
    val saveSuccess: SharedFlow<Boolean> = _saveSuccess.asSharedFlow()
    
    private val _errorMessage = MutableSharedFlow<String>()
    val errorMessage: SharedFlow<String> = _errorMessage.asSharedFlow()

    private val expenseId: Long? = savedStateHandle.get<Long>("expenseId")

    init {
        loadCategories()
        expenseId?.let { loadExpense(it) }
    }

    /**
     * Load all categories on initialization
     */
    private fun loadCategories() {
        viewModelScope.launch {
            categoryRepository.getAllCategories().first().let { categories ->
                _uiState.value = _uiState.value.copy(categories = categories)
            }
        }
    }

    /**
     * Load existing expense for edit mode
     */
    private fun loadExpense(id: Long) {
        viewModelScope.launch {
            expenseRepository.getExpenseById(id)?.let { expense ->
                // Find the category from loaded categories
                val category = _uiState.value.categories.find { it.id == expense.categoryId }
                
                _uiState.value = _uiState.value.copy(
                    amount = expense.amount.toString(),
                    selectedCategory = category,
                    date = expense.date,
                    description = expense.description,
                    isEditMode = true,
                    expenseId = id
                )
            }
        }
    }

    /**
     * Update the amount field
     */
    fun updateAmount(amount: String) {
        _uiState.value = _uiState.value.copy(
            amount = amount,
            validationErrors = _uiState.value.validationErrors - "amount"
        )
    }

    /**
     * Update the selected category
     */
    fun updateCategory(category: Category) {
        _uiState.value = _uiState.value.copy(
            selectedCategory = category,
            validationErrors = _uiState.value.validationErrors - "category"
        )
    }

    /**
     * Update the date
     */
    fun updateDate(date: Long) {
        _uiState.value = _uiState.value.copy(
            date = date,
            validationErrors = _uiState.value.validationErrors - "date"
        )
    }

    /**
     * Update the description
     */
    fun updateDescription(description: String) {
        _uiState.value = _uiState.value.copy(
            description = description,
            validationErrors = _uiState.value.validationErrors - "description"
        )
    }

    /**
     * Validate the form according to all validation rules
     */
    fun validateForm(): ValidationResult {
        val errors = mutableMapOf<String, String>()
        val state = _uiState.value

        // Validate amount: numeric, 2 decimals, > 0
        if (state.amount.isBlank()) {
            errors["amount"] = "Amount is required"
        } else {
            // Remove any whitespace
            val cleanAmount = state.amount.trim()
            val amountValue = cleanAmount.toDoubleOrNull()
            
            if (amountValue == null) {
                errors["amount"] = "Amount must be a valid number"
            } else if (amountValue.isNaN() || amountValue.isInfinite()) {
                errors["amount"] = "Amount must be a valid number"
            } else if (amountValue <= 0) {
                errors["amount"] = "Amount must be greater than 0"
            } else if (amountValue > 999999999.99) {
                errors["amount"] = "Amount is too large"
            } else {
                // Check for up to 2 decimal places
                val decimalPart = cleanAmount.substringAfter(".", "")
                if (decimalPart.length > 2) {
                    errors["amount"] = "Amount can have at most 2 decimal places"
                }
            }
        }

        // Validate category: required
        if (state.selectedCategory == null) {
            errors["category"] = "Category is required"
        }

        // Validate date: required and not too far in future
        if (state.date <= 0) {
            errors["date"] = "Date is required"
        } else {
            val currentTime = System.currentTimeMillis()
            if (state.date > currentTime + 86400000) { // Allow 1 day in future
                errors["date"] = "Date cannot be more than 1 day in the future"
            }
        }

        // Validate description: max 200 characters
        if (state.description.length > 200) {
            errors["description"] = "Description must be at most 200 characters (${state.description.length}/200)"
        }

        return if (errors.isEmpty()) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(errors)
        }
    }

    /**
     * Save the expense (add or update based on edit mode)
     * Prevents rapid repeated saves
     */
    fun saveExpense() {
        // Prevent rapid repeated saves
        if (_uiState.value.isSaving) {
            return
        }
        
        val validationResult = validateForm()
        
        if (validationResult is ValidationResult.Invalid) {
            _uiState.value = _uiState.value.copy(
                validationErrors = validationResult.errors
            )
            return
        }

        val state = _uiState.value
        _uiState.value = state.copy(isSaving = true)

        viewModelScope.launch {
            val expense = Expense(
                id = state.expenseId ?: 0,
                amount = state.amount.toDouble(),
                categoryId = state.selectedCategory!!.id,
                date = state.date,
                description = state.description,
                createdAt = if (state.isEditMode) 0 else System.currentTimeMillis(), // Will be set by repository
                updatedAt = System.currentTimeMillis()
            )

            val result = if (state.isEditMode) {
                updateExpenseUseCase(expense)
            } else {
                addExpenseUseCase(expense)
            }

            _uiState.value = state.copy(isSaving = false)

            when (result) {
                is Result.Success -> {
                    _saveSuccess.emit(true)
                }
                is Result.Error -> {
                    // Show error message as toast
                    _errorMessage.emit(result.message)
                    // Also update UI state for inline display
                    _uiState.value = _uiState.value.copy(
                        validationErrors = mapOf("general" to result.message)
                    )
                }
            }
        }
    }
}

/**
 * Validation result sealed class
 */
sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Invalid(val errors: Map<String, String>) : ValidationResult()
}
