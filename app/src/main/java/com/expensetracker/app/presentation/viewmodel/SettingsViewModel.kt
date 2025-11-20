package com.expensetracker.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.data.preferences.UserPreferencesManager
import com.expensetracker.app.domain.model.Currency
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesManager: UserPreferencesManager,
    private val exportExpensesUseCase: com.expensetracker.app.domain.usecase.ExportExpensesUseCase
) : ViewModel() {

    val selectedCurrency: StateFlow<Currency> = userPreferencesManager.selectedCurrency
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Currency.INR
        )
    
    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

    fun setCurrency(currency: Currency) {
        viewModelScope.launch {
            userPreferencesManager.setCurrency(currency)
        }
    }
    
    fun exportExpenses() {
        viewModelScope.launch {
            _exportState.value = ExportState.Loading
            
            when (val result = exportExpensesUseCase()) {
                is com.expensetracker.app.domain.model.Result.Success -> {
                    _exportState.value = ExportState.Success(result.data)
                }
                is com.expensetracker.app.domain.model.Result.Error -> {
                    _exportState.value = ExportState.Error(result.message)
                }
            }
        }
    }
    
    fun resetExportState() {
        _exportState.value = ExportState.Idle
    }
}

sealed class ExportState {
    object Idle : ExportState()
    object Loading : ExportState()
    data class Success(val csvContent: String) : ExportState()
    data class Error(val message: String) : ExportState()
}
