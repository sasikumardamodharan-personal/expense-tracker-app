package com.expensetracker.app.domain.model

data class SpendingSummary(
    val totalAmount: Double,
    val categoryBreakdown: List<CategorySpending>,
    val period: String
)

data class CategorySpending(
    val category: com.expensetracker.app.data.local.entity.Category,
    val amount: Double,
    val percentage: Double
)
