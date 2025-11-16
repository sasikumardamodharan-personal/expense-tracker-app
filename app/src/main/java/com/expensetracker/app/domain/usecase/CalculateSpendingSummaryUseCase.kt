package com.expensetracker.app.domain.usecase

import com.expensetracker.app.domain.model.CategorySpending
import com.expensetracker.app.domain.model.SpendingSummary
import com.expensetracker.app.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CalculateSpendingSummaryUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository
) {
    operator fun invoke(startDate: Long, endDate: Long, period: String): Flow<SpendingSummary> {
        // Get expenses for the specified period
        return expenseRepository.getExpensesByDateRange(startDate, endDate).map { expenses ->
            // Calculate total spending
            val totalAmount = expenses.sumOf { it.amount }
            
            // Group expenses by category and calculate amounts
            val categoryMap = expenses.groupBy { it.category }
            
            // Compute category breakdown with amounts and percentages
            val categoryBreakdown = categoryMap.map { (category, expenseList) ->
                val categoryAmount = expenseList.sumOf { it.amount }
                val percentage = if (totalAmount > 0) {
                    (categoryAmount / totalAmount) * 100
                } else {
                    0.0
                }
                
                CategorySpending(
                    category = category,
                    amount = categoryAmount,
                    percentage = percentage
                )
            }.sortedByDescending { it.amount }
            
            // Generate SpendingSummary model
            SpendingSummary(
                totalAmount = totalAmount,
                categoryBreakdown = categoryBreakdown,
                period = period
            )
        }
    }
}
