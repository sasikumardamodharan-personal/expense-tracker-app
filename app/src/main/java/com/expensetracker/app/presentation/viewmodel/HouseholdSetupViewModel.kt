package com.expensetracker.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.domain.model.Household
import com.expensetracker.app.domain.model.Result
import com.expensetracker.app.domain.usecase.HouseholdManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for household setup screen
 */
@HiltViewModel
class HouseholdSetupViewModel @Inject constructor(
    private val householdManager: HouseholdManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<HouseholdSetupUiState>(HouseholdSetupUiState.Initial)
    val uiState: StateFlow<HouseholdSetupUiState> = _uiState.asStateFlow()
    
    /**
     * Create a new household
     */
    fun createHousehold(name: String) {
        viewModelScope.launch {
            _uiState.value = HouseholdSetupUiState.Loading
            
            when (val result = householdManager.createNewHousehold(name)) {
                is Result.Success -> {
                    _uiState.value = HouseholdSetupUiState.Success(result.data)
                }
                is Result.Error -> {
                    _uiState.value = HouseholdSetupUiState.Error(result.message)
                }
            }
        }
    }
    
    /**
     * Join an existing household
     */
    fun joinHousehold(inviteCode: String) {
        viewModelScope.launch {
            _uiState.value = HouseholdSetupUiState.Loading
            
            when (val result = householdManager.joinExistingHousehold(inviteCode)) {
                is Result.Success -> {
                    _uiState.value = HouseholdSetupUiState.Success(result.data)
                }
                is Result.Error -> {
                    _uiState.value = HouseholdSetupUiState.Error(result.message)
                }
            }
        }
    }
    
    /**
     * Reset UI state
     */
    fun resetState() {
        _uiState.value = HouseholdSetupUiState.Initial
    }
}

/**
 * UI state for household setup
 */
sealed class HouseholdSetupUiState {
    object Initial : HouseholdSetupUiState()
    object Loading : HouseholdSetupUiState()
    data class Success(val household: Household) : HouseholdSetupUiState()
    data class Error(val message: String) : HouseholdSetupUiState()
}
