package com.expensetracker.app.data.repository

import android.util.Log
import com.expensetracker.app.data.local.dao.CategoryDao
import com.expensetracker.app.data.local.entity.Category
import com.expensetracker.app.domain.model.Result
import com.expensetracker.app.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import javax.inject.Inject

class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao
) : CategoryRepository {
    
    companion object {
        private const val TAG = "CategoryRepository"
    }
    
    override fun getAllCategories(): Flow<List<Category>> {
        return categoryDao.getAllCategories()
            .catch { exception ->
                Log.e(TAG, "Error loading categories", exception)
                emit(emptyList())
            }
    }
    
    override suspend fun addCategory(category: Category): Result<Long> {
        return try {
            Log.d(TAG, "Adding category: ${category.name}")
            
            // Validate name length
            if (category.name.isEmpty() || category.name.length > 30) {
                Log.w(TAG, "Invalid category name length: ${category.name.length}")
                return Result.Error(
                    Exception("Invalid category name"),
                    "Category name must be between 1 and 30 characters"
                )
            }
            
            // Check for uniqueness
            val existing = categoryDao.getCategoryByName(category.name)
            if (existing != null) {
                Log.w(TAG, "Category already exists: ${category.name}")
                return Result.Error(
                    Exception("Category already exists"),
                    "A category with the name '${category.name}' already exists"
                )
            }
            
            val id = categoryDao.insertCategory(category)
            Log.d(TAG, "Category added successfully with id: $id")
            Result.Success(id)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add category: ${category.name}", e)
            Result.Error(
                exception = e,
                message = "Unable to add category. Please try again."
            )
        }
    }
    
    override suspend fun getCategoryByName(name: String): Category? {
        return try {
            categoryDao.getCategoryByName(name)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch category by name: $name", e)
            null
        }
    }
    
    override suspend fun initializeDefaultCategories() {
        try {
            Log.d(TAG, "Initializing default categories")
            
            // Check if categories already exist
            val existingCategories = categoryDao.getCategoryByName("Food")
            if (existingCategories != null) {
                Log.d(TAG, "Default categories already initialized")
                return // Already initialized
            }
            
            // Define default categories based on design document
            val defaultCategories = listOf(
                Category(
                    name = "Food",
                    iconName = "🍔",
                    colorHex = "#FF6B6B",
                    isCustom = false,
                    sortOrder = 1
                ),
                Category(
                    name = "Transport",
                    iconName = "🚗",
                    colorHex = "#4ECDC4",
                    isCustom = false,
                    sortOrder = 2
                ),
                Category(
                    name = "Entertainment",
                    iconName = "🎬",
                    colorHex = "#45B7D1",
                    isCustom = false,
                    sortOrder = 3
                ),
                Category(
                    name = "Shopping",
                    iconName = "🛍️",
                    colorHex = "#FFA07A",
                    isCustom = false,
                    sortOrder = 4
                ),
                Category(
                    name = "Bills",
                    iconName = "📄",
                    colorHex = "#98D8C8",
                    isCustom = false,
                    sortOrder = 5
                ),
                Category(
                    name = "Healthcare",
                    iconName = "⚕️",
                    colorHex = "#F7DC6F",
                    isCustom = false,
                    sortOrder = 6
                ),
                Category(
                    name = "Other",
                    iconName = "📦",
                    colorHex = "#B19CD9",
                    isCustom = false,
                    sortOrder = 7
                )
            )
            
            // Insert all default categories
            defaultCategories.forEach { category ->
                categoryDao.insertCategory(category)
            }
            
            Log.d(TAG, "Default categories initialized successfully")
        } catch (e: Exception) {
            // Log error but don't throw - initialization failure shouldn't crash the app
            Log.e(TAG, "Failed to initialize default categories", e)
        }
    }
}
