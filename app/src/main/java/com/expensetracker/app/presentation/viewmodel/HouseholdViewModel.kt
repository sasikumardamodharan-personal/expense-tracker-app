package com.expensetracker.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.domain.model.Household
import com.expensetracker.app.domain.model.HouseholdMember
import com.expensetracker.app.domain.model.Result
import com.expensetracker.app.domain.repository.HouseholdRepository
import com.expensetracker.app.domain.usecase.HouseholdManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HouseholdViewModel @Inject constructor(
    private val householdManager: HouseholdManager,
    private val householdRepository: HouseholdRepository
) : ViewModel() {
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    // Observe current household
    val household: StateFlow<Household?> = householdManager.currentHousehold
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
    
    // Observe household members
    private val _members = MutableStateFlow<List<HouseholdMember>>(emptyList())
    val members: StateFlow<List<HouseholdMember>> = _members.asStateFlow()
    
    init {
        loadHouseholdMembers()
    }
    
    /**
     * Load household members
     */
    private fun loadHouseholdMembers() {
        viewModelScope.launch {
            household.collect { currentHousehold ->
                if (currentHousehold != null) {
                    _isLoading.value = true
                    _errorMessage.value = null
                    
                    when (val result = householdRepository.getHouseholdMembers(currentHousehold.id)) {
                        is Result.Success -> {
                            _members.value = result.data
                            _isLoading.value = false
                        }
                        is Result.Error -> {
                            _errorMessage.value = result.message
                            _isLoading.value = false
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Get invite code for current household
     */
    suspend fun getInviteCode(): Result<String> {
        return householdManager.getInviteCode()
    }
}
