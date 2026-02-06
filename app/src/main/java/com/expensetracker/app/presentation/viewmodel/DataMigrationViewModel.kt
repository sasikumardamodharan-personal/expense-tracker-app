package com.expensetracker.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.domain.model.MigrationProgress
import com.expensetracker.app.domain.model.Result
import com.expensetracker.app.domain.usecase.DataMigrationService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for data migration screen
 */
@HiltViewModel
class DataMigrationViewModel @Inject constructor(
    private val dataMigrationService: DataMigrationService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<DataMigrationUiState>(DataMigrationUiState.Initial)
    val uiState: StateFlow<DataMigrationUiState> = _uiState.asStateFlow()
    
    val migrationProgress: StateFlow<MigrationProgress> = dataMigrationService.migrationProgress
    
    init {
        checkMigrationStatus()
    }
    
    /**
     * Check if migration is needed
     */
    private fun checkMigrationStatus() {
        viewModelScope.launch {
            val isNeeded = dataMigrationService.isMigrationNeeded()
            if (!isNeeded) {
                _uiState.value = DataMigrationUiState.AlreadyMigrated
            }
        }
    }
    
    /**
     * Start the migration process
     */
    fun startMigration() {
        viewModelScope.launch {
            _uiState.value = DataMigrationUiState.Migrating
            
            when (val result = dataMigrationService.migrateToFirestore()) {
                is Result.Success -> {
                    _uiState.value = DataMigrationUiState.Success
                }
                is Result.Error -> {
                    _uiState.value = DataMigrationUiState.Error(result.message)
                }
            }
        }
    }
    
    /**
     * Retry migration after error
     */
    fun retryMigration() {
        startMigration()
    }
    
    /**
     * Skip migration (user can do it later)
     */
    fun skipMigration() {
        _uiState.value = DataMigrationUiState.Skipped
    }
    
    /**
     * Reset UI state
     */
    fun resetState() {
        _uiState.value = DataMigrationUiState.Initial
    }
}

/**
 * UI state for data migration
 */
sealed class DataMigrationUiState {
    object Initial : DataMigrationUiState()
    object Migrating : DataMigrationUiState()
    object Success : DataMigrationUiState()
    object Skipped : DataMigrationUiState()
    object AlreadyMigrated : DataMigrationUiState()
    data class Error(val message: String) : DataMigrationUiState()
}
