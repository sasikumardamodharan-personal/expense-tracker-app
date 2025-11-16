package com.expensetracker.app.data.repository

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.expensetracker.app.data.local.dao.CategoryDao
import com.expensetracker.app.data.local.dao.ExpenseDao
import com.expensetracker.app.domain.model.ExpenseWithCategory

class ExpenseWithCategoryPagingSource(
    private val expenseDao: ExpenseDao,
    private val categoryDao: CategoryDao
) : PagingSource<Int, ExpenseWithCategory>() {

    companion object {
        private const val TAG = "ExpenseWithCategoryPagingSource"
        private const val STARTING_PAGE_INDEX = 0
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ExpenseWithCategory> {
        return try {
            val page = params.key ?: STARTING_PAGE_INDEX
            val pageSize = params.loadSize
            val offset = page * pageSize

            // Get expenses from database
            val expenses = expenseDao.getExpensesPaged(limit = pageSize, offset = offset)

            // Map to ExpenseWithCategory
            val expensesWithCategory = expenses.mapNotNull { expense ->
                try {
                    val category = categoryDao.getCategoryById(expense.categoryId)
                    category?.let {
                        ExpenseWithCategory(
                            id = expense.id,
                            amount = expense.amount,
                            category = it,
                            date = expense.date,
                            description = expense.description,
                            createdAt = expense.createdAt,
                            updatedAt = expense.updatedAt
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error mapping expense ${expense.id} with category", e)
                    null
                }
            }

            LoadResult.Page(
                data = expensesWithCategory,
                prevKey = if (page == STARTING_PAGE_INDEX) null else page - 1,
                nextKey = if (expensesWithCategory.isEmpty()) null else page + 1
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error loading page", e)
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, ExpenseWithCategory>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }
}
