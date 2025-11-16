package com.expensetracker.app.domain.usecase

import com.expensetracker.app.data.local.entity.Expense
import com.expensetracker.app.domain.model.Result
import com.expensetracker.app.domain.repository.ExpenseRepository
import javax.inject.Inject

class AddExpenseUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository
) {
    suspend operator fun invoke(expense: Expense): Result<Long> {
        // Validate amount format (numeric, 2 decimal places, greater than 0)
        val validationError = validateExpense(expense)
        if (validationError != null) {
            return Result.Error(
                exception = IllegalArgumentException(validationError),
                message = validationError
            )
        }
        
        // Call repository to persist expense
        return expenseRepository.addExpense(expense)
    }
    
    private fun validateExpense(expense: Expense): String? {
        // Validate amount is greater than 0
        if (expense.amount <= 0) {
            return "Amount must be greater than 0"
        }
        
        // Validate amount is a valid number (not NaN or Infinity)
        if (expense.amount.isNaN() || expense.amount.isInfinite()) {
            return "Amount must be a valid number"
        }
        
        // Validate amount has at most 2 decimal places
        // Use BigDecimal for precise decimal validation
        val amountString = expense.amount.toString()
        val decimalIndex = amountString.indexOf('.')
        if (decimalIndex != -1) {
            val decimalPart = amountString.substring(decimalIndex + 1)
            // Remove scientific notation suffix if present
            val cleanDecimalPart = decimalPart.takeWhile { it.isDigit() }
            if (cleanDecimalPart.length > 2) {
                return "Amount must have at most 2 decimal places"
            }
        }
        
        // Validate category is provided (categoryId > 0)
        if (expense.categoryId <= 0) {
            return "Category is required"
        }
        
        // Validate date is provided (valid timestamp)
        if (expense.date <= 0) {
            return "Date is required"
        }
        
        // Validate date is not in the future (reasonable check)
        val currentTime = System.currentTimeMillis()
        if (expense.date > currentTime + 86400000) { // Allow 1 day in future for timezone differences
            return "Date cannot be more than 1 day in the future"
        }
        
        // Validate description length (max 200 characters)
        if (expense.description.length > 200) {
            return "Description must not exceed 200 characters"
        }
        
        return null
    }
}
