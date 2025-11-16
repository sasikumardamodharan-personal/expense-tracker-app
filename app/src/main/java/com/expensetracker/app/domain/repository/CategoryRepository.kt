package com.expensetracker.app.domain.repository

import com.expensetracker.app.data.local.entity.Category
import com.expensetracker.app.domain.model.Result
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun getAllCategories(): Flow<List<Category>>
    suspend fun addCategory(category: Category): Result<Long>
    suspend fun getCategoryByName(name: String): Category?
    suspend fun initializeDefaultCategories()
}
