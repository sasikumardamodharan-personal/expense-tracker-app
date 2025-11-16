package com.expensetracker.app.domain.repository

import androidx.paging.PagingData
import com.expensetracker.app.data.local.entity.Expense
import com.expensetracker.app.domain.model.ExpenseWithCategory
import com.expensetracker.app.domain.model.Result
import kotlinx.coroutines.flow.Flow

interface ExpenseRepository {
    fun getAllExpenses(): Flow<List<ExpenseWithCategory>>
    fun getAllExpensesPaged(): Flow<PagingData<ExpenseWithCategory>>
    fun getExpensesByDateRange(startDate: Long, endDate: Long): Flow<List<ExpenseWithCategory>>
    fun getExpensesByCategory(categoryId: Long): Flow<List<ExpenseWithCategory>>
    suspend fun addExpense(expense: Expense): Result<Long>
    suspend fun updateExpense(expense: Expense): Result<Unit>
    suspend fun deleteExpense(expense: Expense): Result<Unit>
    suspend fun getExpenseById(id: Long): Expense?
}
