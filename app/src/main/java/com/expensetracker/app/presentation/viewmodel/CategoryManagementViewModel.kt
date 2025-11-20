package com.expensetracker.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.data.local.entity.Category
import com.expensetracker.app.domain.model.Result
import com.expensetracker.app.domain.repository.CategoryRepository
import com.expensetracker.app.domain.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoryManagementViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val expenseRepository: ExpenseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<CategoryManagementUiState>(CategoryManagementUiState.Loading)
    val uiState: StateFlow<CategoryManagementUiState> = _uiState.asStateFlow()

    private val _successMessage = MutableSharedFlow<String>()
    val successMessage: SharedFlow<String> = _successMessage.asSharedFlow()

    private val _errorMessage = MutableSharedFlow<String>()
    val errorMessage: SharedFlow<String> = _errorMessage.asSharedFlow()

    init {
        loadCategories()
    }

    fun loadCategories() {
        viewModelScope.launch {
            _uiState.value = CategoryManagementUiState.Loading
            categoryRepository.getAllCategories().collect { categories ->
                _uiState.value = CategoryManagementUiState.Success(categories)
            }
        }
    }

    fun addCategory(name: String, iconName: String, colorHex: String) {
        viewModelScope.launch {
            // Validate name
            if (name.isBlank() || name.length > 30) {
                _errorMessage.emit("Category name must be between 1 and 30 characters")
                return@launch
            }

            // Check if name already exists
            val existing = categoryRepository.getCategoryByName(name)
            if (existing != null) {
                _errorMessage.emit("Category '$name' already exists")
                return@launch
            }

            val category = Category(
                name = name,
                iconName = iconName,
                colorHex = colorHex,
                isCustom = true,
                sortOrder = 999 // Custom categories at the end
            )

            when (val result = categoryRepository.addCategory(category)) {
                is Result.Success -> {
                    _successMessage.emit("Category '$name' added successfully")
                }
                is Result.Error -> {
                    _errorMessage.emit(result.message)
                }
            }
        }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch {
            // Validate name
            if (category.name.isBlank() || category.name.length > 30) {
                _errorMessage.emit("Category name must be between 1 and 30 characters")
                return@launch
            }

            // Check if name already exists (excluding current category)
            val existing = categoryRepository.getCategoryByName(category.name)
            if (existing != null && existing.id != category.id) {
                _errorMessage.emit("Category '${category.name}' already exists")
                return@launch
            }

            when (val result = categoryRepository.updateCategory(category)) {
                is Result.Success -> {
                    _successMessage.emit("Category '${category.name}' updated successfully")
                }
                is Result.Error -> {
                    _errorMessage.emit(result.message)
                }
            }
        }
    }

    fun deleteCategory(categoryId: Long) {
        viewModelScope.launch {
            // Check if any expenses use this category
            expenseRepository.getExpensesByCategory(categoryId).collect { expenses ->
                if (expenses.isNotEmpty()) {
                    _errorMessage.emit("Cannot delete category with existing expenses")
                    return@collect
                }

                // Get category for success message
                val currentState = _uiState.value
                val categoryName = if (currentState is CategoryManagementUiState.Success) {
                    currentState.categories.find { it.id == categoryId }?.name ?: "Category"
                } else {
                    "Category"
                }

                when (val result = categoryRepository.deleteCategory(categoryId)) {
                    is Result.Success -> {
                        _successMessage.emit("'$categoryName' deleted successfully")
                    }
                    is Result.Error -> {
                        _errorMessage.emit(result.message)
                    }
                }
            }
        }
    }

    fun canDeleteCategory(categoryId: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            // Check if any expenses use this category
            expenseRepository.getExpensesByCategory(categoryId).collect { expenses ->
                onResult(expenses.isEmpty())
            }
        }
    }
}

sealed class CategoryManagementUiState {
    object Loading : CategoryManagementUiState()
    data class Success(val categories: List<Category>) : CategoryManagementUiState()
    data class Error(val message: String) : CategoryManagementUiState()
}
