package com.expensetracker.app.domain.usecase

import com.expensetracker.app.domain.model.ExpenseWithCategory
import com.expensetracker.app.domain.model.Result
import com.expensetracker.app.domain.repository.ExpenseRepository
import com.expensetracker.app.util.CsvExporter
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ExportExpensesUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository
) {
    
    /**
     * Export all expenses to CSV format
     * Returns the CSV content as a string
     */
    suspend operator fun invoke(): Result<String> {
        return try {
            // Get all expenses
            val expenses = expenseRepository.getAllExpenses().first()
            
            if (expenses.isEmpty()) {
                return Result.Error(
                    exception = Exception("No expenses to export"),
                    message = "No expenses found. Add some expenses first."
                )
            }
            
            // Convert to CSV
            val csvContent = CsvExporter.exportToCsv(expenses)
            
            Result.Success(csvContent)
        } catch (e: Exception) {
            Result.Error(
                exception = e,
                message = "Failed to export expenses: ${e.message}"
            )
        }
    }
}
