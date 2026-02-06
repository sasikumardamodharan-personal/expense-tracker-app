package com.expensetracker.app.data.repository

import android.util.Log
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.expensetracker.app.data.local.dao.CategoryDao
import com.expensetracker.app.data.local.dao.ExpenseDao
import com.expensetracker.app.data.local.entity.Expense
import com.expensetracker.app.domain.model.ExpenseWithCategory
import com.expensetracker.app.domain.model.Result
import com.expensetracker.app.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ExpenseRepositoryImpl @Inject constructor(
    private val expenseDao: ExpenseDao,
    private val categoryDao: CategoryDao
) : ExpenseRepository {
    
    companion object {
        private const val TAG = "ExpenseRepository"
        private const val PAGE_SIZE = 20
    }
    
    override fun getAllExpenses(): Flow<List<ExpenseWithCategory>> {
        return expenseDao.getAllExpenses()
            .map { expenses ->
                try {
                    expenses.mapNotNull { expense ->
                        val category = categoryDao.getCategoryById(expense.categoryId)
                        category?.let {
                            ExpenseWithCategory(
                                id = expense.id,
                                amount = expense.amount,
                                category = it,
                                date = expense.date,
                                description = expense.description,
                                createdAt = expense.createdAt,
                                updatedAt = expense.updatedAt,
                                syncStatus = expense.syncStatus
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error mapping expenses with categories", e)
                    emptyList()
                }
            }
            .catch { exception ->
                Log.e(TAG, "Error loading all expenses", exception)
                emit(emptyList())
            }
    }
    
    override fun getAllExpensesPaged(): Flow<PagingData<ExpenseWithCategory>> {
        return Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                enablePlaceholders = false,
                initialLoadSize = PAGE_SIZE
            ),
            pagingSourceFactory = { ExpenseWithCategoryPagingSource(expenseDao, categoryDao) }
        ).flow
    }
    
    override fun getExpensesByDateRange(
        startDate: Long,
        endDate: Long
    ): Flow<List<ExpenseWithCategory>> {
        return expenseDao.getExpensesByDateRange(startDate, endDate)
            .map { expenses ->
                try {
                    expenses.mapNotNull { expense ->
                        val category = categoryDao.getCategoryById(expense.categoryId)
                        category?.let {
                            ExpenseWithCategory(
                                id = expense.id,
                                amount = expense.amount,
                                category = it,
                                date = expense.date,
                                description = expense.description,
                                createdAt = expense.createdAt,
                                updatedAt = expense.updatedAt,
                                syncStatus = expense.syncStatus
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error mapping expenses by date range", e)
                    emptyList()
                }
            }
            .catch { exception ->
                Log.e(TAG, "Error loading expenses by date range: $startDate to $endDate", exception)
                emit(emptyList())
            }
    }
    
    override fun getExpensesByCategory(categoryId: Long): Flow<List<ExpenseWithCategory>> {
        return expenseDao.getExpensesByCategory(categoryId)
            .map { expenses ->
                try {
                    expenses.mapNotNull { expense ->
                        val category = categoryDao.getCategoryById(expense.categoryId)
                        category?.let {
                            ExpenseWithCategory(
                                id = expense.id,
                                amount = expense.amount,
                                category = it,
                                date = expense.date,
                                description = expense.description,
                                createdAt = expense.createdAt,
                                updatedAt = expense.updatedAt,
                                syncStatus = expense.syncStatus
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error mapping expenses by category", e)
                    emptyList()
                }
            }
            .catch { exception ->
                Log.e(TAG, "Error loading expenses by category: $categoryId", exception)
                emit(emptyList())
            }
    }
    
    override suspend fun addExpense(expense: Expense): Result<Long> {
        return try {
            Log.d(TAG, "Adding expense: amount=${expense.amount}, categoryId=${expense.categoryId}")
            
            // Set sync status to PENDING and update modifiedAt timestamp
            val expenseWithSync = expense.copy(
                syncStatus = "PENDING",
                modifiedAt = System.currentTimeMillis()
            )
            
            val id = expenseDao.insertExpense(expenseWithSync)
            Log.d(TAG, "Expense added successfully with id: $id")
            
            Result.Success(id)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add expense", e)
            Result.Error(
                exception = e,
                message = "Unable to save expense. Please try again."
            )
        }
    }
    
    override suspend fun updateExpense(expense: Expense): Result<Unit> {
        return try {
            Log.d(TAG, "Updating expense: id=${expense.id}, amount=${expense.amount}")
            
            // Set sync status to PENDING and update modifiedAt timestamp
            val expenseWithSync = expense.copy(
                syncStatus = "PENDING",
                modifiedAt = System.currentTimeMillis()
            )
            
            expenseDao.updateExpense(expenseWithSync)
            Log.d(TAG, "Expense updated successfully")
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update expense with id: ${expense.id}", e)
            Result.Error(
                exception = e,
                message = "Unable to update expense. Please try again."
            )
        }
    }
    
    override suspend fun deleteExpense(expense: Expense): Result<Unit> {
        return try {
            Log.d(TAG, "Deleting expense: id=${expense.id}")
            
            // Delete from local database
            expenseDao.deleteExpense(expense)
            Log.d(TAG, "Expense deleted from local database")
            
            // Note: Firestore sync for deletion should be handled by the caller
            // by calling ExpenseSyncCoordinator.markExpenseAsDeleted()
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete expense with id: ${expense.id}", e)
            Result.Error(
                exception = e,
                message = "Unable to delete expense. Please try again."
            )
        }
    }
    
    override suspend fun getExpenseById(id: Long): Expense? {
        return try {
            Log.d(TAG, "Fetching expense by id: $id")
            expenseDao.getExpenseById(id)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch expense by id: $id", e)
            null
        }
    }
    
    override suspend fun getExpenseByFirestoreId(firestoreId: String): Expense? {
        return try {
            Log.d(TAG, "Fetching expense by Firestore ID: $firestoreId")
            expenseDao.getExpenseByFirestoreId(firestoreId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch expense by Firestore ID: $firestoreId", e)
            null
        }
    }
    
    override suspend fun getAllExpensesOnce(): List<Expense> {
        return try {
            Log.d(TAG, "Fetching all expenses once")
            expenseDao.getAllExpensesOnce()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch all expenses", e)
            emptyList()
        }
    }
    
    override suspend fun updateExpenseWithoutSync(expense: Expense): Result<Unit> {
        return try {
            Log.d(TAG, "Updating expense without sync: id=${expense.id}")
            expenseDao.updateExpense(expense)
            Log.d(TAG, "Expense updated successfully without triggering sync")
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update expense without sync: ${expense.id}", e)
            Result.Error(
                exception = e,
                message = "Unable to update expense. Please try again."
            )
        }
    }
    
    override suspend fun addExpenseWithoutSync(expense: Expense): Result<Long> {
        return try {
            Log.d(TAG, "Adding expense without sync: amount=${expense.amount}, categoryId=${expense.categoryId}")
            val id = expenseDao.insertExpense(expense)
            Log.d(TAG, "Expense added successfully with id: $id (no sync triggered)")
            Result.Success(id)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add expense without sync", e)
            Result.Error(
                exception = e,
                message = "Unable to save expense. Please try again."
            )
        }
    }
}
