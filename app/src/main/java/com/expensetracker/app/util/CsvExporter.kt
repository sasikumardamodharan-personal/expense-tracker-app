package com.expensetracker.app.util

import com.expensetracker.app.domain.model.ExpenseWithCategory
import java.text.SimpleDateFormat
import java.util.*

object CsvExporter {
    
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val dateTimeFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    
    /**
     * Convert list of expenses to CSV format
     */
    fun exportToCsv(expenses: List<ExpenseWithCategory>): String {
        val csv = StringBuilder()
        
        // Header row
        csv.append("Date,Amount,Category,Description,Created,Updated\n")
        
        // Data rows
        expenses.forEach { expense ->
            csv.append(escapeCsvValue(dateFormatter.format(Date(expense.date))))
            csv.append(",")
            csv.append(expense.amount)
            csv.append(",")
            csv.append(escapeCsvValue(expense.category.name))
            csv.append(",")
            csv.append(escapeCsvValue(expense.description))
            csv.append(",")
            csv.append(escapeCsvValue(dateTimeFormatter.format(Date(expense.createdAt))))
            csv.append(",")
            csv.append(escapeCsvValue(dateTimeFormatter.format(Date(expense.updatedAt))))
            csv.append("\n")
        }
        
        return csv.toString()
    }
    
    /**
     * Escape special characters in CSV values
     */
    private fun escapeCsvValue(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }
    
    /**
     * Generate filename with current date
     */
    fun generateFilename(): String {
        val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return "expenses_$dateStr.csv"
    }
}
