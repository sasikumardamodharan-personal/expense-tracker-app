package com.expensetracker.app.integration

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.expensetracker.app.MainActivity
import com.expensetracker.app.data.local.AppDatabase
import com.expensetracker.app.data.local.entity.Category
import com.expensetracker.app.data.local.entity.Expense
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class EdgeCaseIntegrationTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var database: AppDatabase

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setup() {
        hiltRule.inject()
        runBlocking {
            database.clearAllTables()
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testEmptyDatabase_ShowsEmptyState() {
        composeTestRule.waitForIdle()

        // Verify empty state is displayed
        composeTestRule.onNodeWithText("No expenses yet").assertIsDisplayed()
        composeTestRule.onNodeWithText("Add your first expense to start tracking").assertIsDisplayed()
    }

    @Test
    fun testLargeNumberOfExpenses_Performance() {
        // Add 100 expenses programmatically
        runBlocking {
            val categoryDao = database.categoryDao()
            val expenseDao = database.expenseDao()

            // Ensure categories exist
            val foodCategory = categoryDao.getCategoryByName("Food") 
                ?: Category(name = "Food", iconName = "🍔", colorHex = "#FF6B6B", sortOrder = 1).also {
                    categoryDao.insertCategory(it)
                }

            // Add 100 expenses
            repeat(100) { index ->
                val expense = Expense(
                    amount = (10.0 + index),
                    categoryId = foodCategory.id,
                    date = System.currentTimeMillis() - (index * 86400000L), // Different days
                    description = "Test expense $index",
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                expenseDao.insertExpense(expense)
            }
        }

        composeTestRule.waitForIdle()

        // Verify list loads and is scrollable
        composeTestRule.onNodeWithText("Test expense 0").assertIsDisplayed()

        // Scroll to verify pagination/lazy loading works
        composeTestRule.onNodeWithTag("expense_list").performScrollToIndex(50)
        composeTestRule.waitForIdle()

        // Verify we can see expenses further down
        composeTestRule.onNodeWithText("Test expense 50").assertIsDisplayed()
    }

    @Test
    fun testAllCategoriesFilteredOut_ShowsEmptyState() {
        // Add expenses in Food category
        runBlocking {
            val categoryDao = database.categoryDao()
            val expenseDao = database.expenseDao()

            val foodCategory = categoryDao.getCategoryByName("Food")
                ?: Category(name = "Food", iconName = "🍔", colorHex = "#FF6B6B", sortOrder = 1).also {
                    categoryDao.insertCategory(it)
                }

            expenseDao.insertExpense(
                Expense(
                    amount = 50.0,
                    categoryId = foodCategory.id,
                    date = System.currentTimeMillis(),
                    description = "Food expense",
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            )
        }

        composeTestRule.waitForIdle()

        // Open filter screen
        composeTestRule.onNodeWithContentDescription("Filter expenses").performClick()

        // Select Transport category (not Food)
        composeTestRule.onNodeWithText("Transport").performClick()

        // Apply filter
        composeTestRule.onNodeWithText("Apply").performClick()

        composeTestRule.waitForIdle()

        // Verify empty state for filtered results
        composeTestRule.onNodeWithText("No expenses found").assertIsDisplayed()
        composeTestRule.onNodeWithText("Try adjusting your filters").assertIsDisplayed()
    }

    @Test
    fun testDateRangeWithNoExpenses_ShowsEmptyState() {
        // Add an expense today
        runBlocking {
            val categoryDao = database.categoryDao()
            val expenseDao = database.expenseDao()

            val category = categoryDao.getCategoryByName("Food")
                ?: Category(name = "Food", iconName = "🍔", colorHex = "#FF6B6B", sortOrder = 1).also {
                    categoryDao.insertCategory(it)
                }

            expenseDao.insertExpense(
                Expense(
                    amount = 50.0,
                    categoryId = category.id,
                    date = System.currentTimeMillis(),
                    description = "Today's expense",
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            )
        }

        composeTestRule.waitForIdle()

        // Open filter screen
        composeTestRule.onNodeWithContentDescription("Filter expenses").performClick()

        // Select date range from 30 days ago to 10 days ago (no expenses in this range)
        composeTestRule.onNodeWithText("Start Date").performClick()
        // Select date 30 days ago (implementation depends on date picker)
        composeTestRule.onNodeWithContentDescription("Select date").performClick()

        composeTestRule.onNodeWithText("End Date").performClick()
        // Select date 10 days ago
        composeTestRule.onNodeWithContentDescription("Select date").performClick()

        // Apply filter
        composeTestRule.onNodeWithText("Apply").performClick()

        composeTestRule.waitForIdle()

        // Verify empty state
        composeTestRule.onNodeWithText("No expenses found").assertIsDisplayed()
    }

    @Test
    fun testRapidUserInteractions_NoErrors() {
        // Add a test expense first
        runBlocking {
            val categoryDao = database.categoryDao()
            val expenseDao = database.expenseDao()

            val category = categoryDao.getCategoryByName("Food")
                ?: Category(name = "Food", iconName = "🍔", colorHex = "#FF6B6B", sortOrder = 1).also {
                    categoryDao.insertCategory(it)
                }

            expenseDao.insertExpense(
                Expense(
                    amount = 50.0,
                    categoryId = category.id,
                    date = System.currentTimeMillis(),
                    description = "Test expense",
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            )
        }

        composeTestRule.waitForIdle()

        // Rapidly click between screens
        repeat(5) {
            composeTestRule.onNodeWithContentDescription("View summary").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithContentDescription("Navigate back").performClick()
            composeTestRule.waitForIdle()
        }

        // Verify app is still functional
        composeTestRule.onNodeWithText("$50.00").assertIsDisplayed()
    }

    @Test
    fun testEmptyFormSubmission_ShowsValidationErrors() {
        composeTestRule.waitForIdle()

        // Click FAB to add expense
        composeTestRule.onNodeWithContentDescription("Add expense").performClick()

        // Try to save without filling anything
        composeTestRule.onNodeWithText("Save").performClick()

        composeTestRule.waitForIdle()

        // Verify validation errors are shown
        composeTestRule.onNodeWithText("Amount is required").assertIsDisplayed()
        composeTestRule.onNodeWithText("Category is required").assertIsDisplayed()
    }

    @Test
    fun testInvalidAmountFormat_ShowsValidationError() {
        composeTestRule.waitForIdle()

        // Click FAB to add expense
        composeTestRule.onNodeWithContentDescription("Add expense").performClick()

        // Enter invalid amount
        composeTestRule.onNodeWithText("Amount").performTextInput("abc")

        // Select category
        composeTestRule.onNodeWithText("Category").performClick()
        composeTestRule.onNodeWithText("Food").performClick()

        // Try to save
        composeTestRule.onNodeWithText("Save").performClick()

        composeTestRule.waitForIdle()

        // Verify validation error
        composeTestRule.onNodeWithText("Invalid amount format").assertIsDisplayed()
    }

    @Test
    fun testZeroAmount_ShowsValidationError() {
        composeTestRule.waitForIdle()

        // Click FAB to add expense
        composeTestRule.onNodeWithContentDescription("Add expense").performClick()

        // Enter zero amount
        composeTestRule.onNodeWithText("Amount").performTextInput("0.00")

        // Select category
        composeTestRule.onNodeWithText("Category").performClick()
        composeTestRule.onNodeWithText("Food").performClick()

        // Try to save
        composeTestRule.onNodeWithText("Save").performClick()

        composeTestRule.waitForIdle()

        // Verify validation error
        composeTestRule.onNodeWithText("Amount must be greater than 0").assertIsDisplayed()
    }
}
