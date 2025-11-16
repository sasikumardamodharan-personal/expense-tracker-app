package com.expensetracker.app.domain.usecase

import androidx.paging.PagingData
import androidx.paging.filter
import com.expensetracker.app.domain.model.ExpenseWithCategory
import com.expensetracker.app.domain.model.FilterCriteria
import com.expensetracker.app.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetFilteredExpensesUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository
) {
    operator fun invoke(filterCriteria: FilterCriteria): Flow<List<ExpenseWithCategory>> {
        // Start with all expenses
        val expensesFlow = expenseRepository.getAllExpenses()
        
        // Apply filters
        return expensesFlow.map { expenses ->
            var filteredExpenses = expenses
            
            // Apply date range filter when specified
            if (filterCriteria.startDate != null && filterCriteria.endDate != null) {
                filteredExpenses = filteredExpenses.filter { expense ->
                    expense.date >= filterCriteria.startDate && expense.date <= filterCriteria.endDate
                }
            } else if (filterCriteria.startDate != null) {
                filteredExpenses = filteredExpenses.filter { expense ->
                    expense.date >= filterCriteria.startDate
                }
            } else if (filterCriteria.endDate != null) {
                filteredExpenses = filteredExpenses.filter { expense ->
                    expense.date <= filterCriteria.endDate
                }
            }
            
            // Apply category filter when specified
            if (filterCriteria.categoryIds.isNotEmpty()) {
                filteredExpenses = filteredExpenses.filter { expense ->
                    filterCriteria.categoryIds.contains(expense.category.id)
                }
            }
            
            filteredExpenses
        }
    }
    
    fun invokePaged(filterCriteria: FilterCriteria): Flow<PagingData<ExpenseWithCategory>> {
        // Start with paged expenses
        val expensesFlow = expenseRepository.getAllExpensesPaged()
        
        // Apply filters to paging data
        return expensesFlow.map { pagingData ->
            pagingData.filter { expense ->
                var matches = true
                
                // Apply date range filter when specified
                if (filterCriteria.startDate != null && filterCriteria.endDate != null) {
                    matches = matches && expense.date >= filterCriteria.startDate && 
                              expense.date <= filterCriteria.endDate
                } else if (filterCriteria.startDate != null) {
                    matches = matches && expense.date >= filterCriteria.startDate
                } else if (filterCriteria.endDate != null) {
                    matches = matches && expense.date <= filterCriteria.endDate
                }
                
                // Apply category filter when specified
                if (filterCriteria.categoryIds.isNotEmpty()) {
                    matches = matches && filterCriteria.categoryIds.contains(expense.category.id)
                }
                
                matches
            }
        }
    }
}
