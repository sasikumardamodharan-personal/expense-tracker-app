package com.expensetracker.app.domain.usecase

import com.expensetracker.app.data.local.entity.Expense
import com.expensetracker.app.domain.model.Result
import com.expensetracker.app.domain.repository.ExpenseRepository
import javax.inject.Inject

class DeleteExpenseUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository
) {
    suspend operator fun invoke(expense: Expense): Result<Unit> {
        // Call repository to delete expense and handle deletion errors
        return expenseRepository.deleteExpense(expense)
    }
}
