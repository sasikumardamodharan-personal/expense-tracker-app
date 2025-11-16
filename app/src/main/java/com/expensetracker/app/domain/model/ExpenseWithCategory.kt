package com.expensetracker.app.domain.model

import com.expensetracker.app.data.local.entity.Category

data class ExpenseWithCategory(
    val id: Long,
    val amount: Double,
    val category: Category,
    val date: Long,
    val description: String,
    val createdAt: Long,
    val updatedAt: Long
)
