package com.expensetracker.app.integration

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.expensetracker.app.MainActivity
import com.expensetracker.app.data.local.AppDatabase
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
class UserFlowIntegrationTest {

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
        // Clear database before each test
        runBlocking {
            database.clearAllTables()
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testAddExpenseFlow_EndToEnd() {
        // Wait for the app to load
        composeTestRule.waitForIdle()

        // Verify empty state is shown
        composeTestRule.onNodeWithText("No expenses yet").assertIsDisplayed()

        // Click FAB to add expense
        composeTestRule.onNodeWithContentDescription("Add expense").performClick()

        // Fill in expense form
        composeTestRule.onNodeWithText("Amount").performTextInput("50.00")
        
        // Select category (assuming Food is first)
        composeTestRule.onNodeWithText("Category").performClick()
        composeTestRule.onNodeWithText("Food").performClick()

        // Add description
        composeTestRule.onNodeWithText("Description (Optional)").performTextInput("Lunch at restaurant")

        // Save expense
        composeTestRule.onNodeWithText("Save").performClick()

        // Wait for navigation back
        composeTestRule.waitForIdle()

        // Verify expense appears in list
        composeTestRule.onNodeWithText("$50.00").assertIsDisplayed()
        composeTestRule.onNodeWithText("Lunch at restaurant").assertIsDisplayed()
    }

    @Test
    fun testEditExpenseFlow_EndToEnd() {
        // First add an expense
        addTestExpense(amount = "30.00", category = "Transport", description = "Bus fare")

        composeTestRule.waitForIdle()

        // Click on the expense to edit
        composeTestRule.onNodeWithText("$30.00").performClick()

        // Verify we're in edit mode
        composeTestRule.onNodeWithText("Edit Expense").assertIsDisplayed()

        // Update amount
        composeTestRule.onNodeWithText("Amount").performTextClearance()
        composeTestRule.onNodeWithText("Amount").performTextInput("35.00")

        // Update description
        composeTestRule.onNodeWithText("Description (Optional)").performTextClearance()
        composeTestRule.onNodeWithText("Description (Optional)").performTextInput("Taxi fare")

        // Save changes
        composeTestRule.onNodeWithText("Save").performClick()

        composeTestRule.waitForIdle()

        // Verify updated expense
        composeTestRule.onNodeWithText("$35.00").assertIsDisplayed()
        composeTestRule.onNodeWithText("Taxi fare").assertIsDisplayed()
    }

    @Test
    fun testDeleteExpenseFlow_EndToEnd() {
        // Add an expense
        addTestExpense(amount = "25.00", category = "Entertainment", description = "Movie ticket")

        composeTestRule.waitForIdle()

        // Long press or swipe to delete (implementation depends on UI)
        composeTestRule.onNodeWithText("$25.00").performTouchInput {
            swipeLeft()
        }

        // Confirm deletion
        composeTestRule.onNodeWithText("Delete").performClick()
        composeTestRule.onNodeWithText("Confirm").performClick()

        composeTestRule.waitForIdle()

        // Verify expense is removed
        composeTestRule.onNodeWithText("$25.00").assertDoesNotExist()
        composeTestRule.onNodeWithText("No expenses yet").assertIsDisplayed()
    }

    @Test
    fun testFilteringFlow_EndToEnd() {
        // Add multiple expenses
        addTestExpense(amount = "50.00", category = "Food", description = "Groceries")
        addTestExpense(amount = "30.00", category = "Transport", description = "Gas")
        addTestExpense(amount = "20.00", category = "Food", description = "Coffee")

        composeTestRule.waitForIdle()

        // Open filter screen
        composeTestRule.onNodeWithContentDescription("Filter expenses").performClick()

        // Select Food category
        composeTestRule.onNodeWithText("Food").performClick()

        // Apply filter
        composeTestRule.onNodeWithText("Apply").performClick()

        composeTestRule.waitForIdle()

        // Verify only Food expenses are shown
        composeTestRule.onNodeWithText("$50.00").assertIsDisplayed()
        composeTestRule.onNodeWithText("$20.00").assertIsDisplayed()
        composeTestRule.onNodeWithText("$30.00").assertDoesNotExist()

        // Clear filters
        composeTestRule.onNodeWithContentDescription("Clear filters").performClick()

        composeTestRule.waitForIdle()

        // Verify all expenses are shown again
        composeTestRule.onNodeWithText("$50.00").assertIsDisplayed()
        composeTestRule.onNodeWithText("$30.00").assertIsDisplayed()
        composeTestRule.onNodeWithText("$20.00").assertIsDisplayed()
    }

    @Test
    fun testSummaryCalculations_EndToEnd() {
        // Add expenses in different categories
        addTestExpense(amount = "100.00", category = "Food", description = "Groceries")
        addTestExpense(amount = "50.00", category = "Food", description = "Restaurant")
        addTestExpense(amount = "75.00", category = "Transport", description = "Gas")
        addTestExpense(amount = "25.00", category = "Entertainment", description = "Movie")

        composeTestRule.waitForIdle()

        // Navigate to summary screen
        composeTestRule.onNodeWithContentDescription("View summary").performClick()

        composeTestRule.waitForIdle()

        // Verify total spending
        composeTestRule.onNodeWithText("$250.00").assertIsDisplayed()

        // Verify category breakdown
        composeTestRule.onNodeWithText("Food").assertIsDisplayed()
        composeTestRule.onNodeWithText("$150.00").assertIsDisplayed() // Food total
        composeTestRule.onNodeWithText("60%").assertIsDisplayed() // Food percentage

        composeTestRule.onNodeWithText("Transport").assertIsDisplayed()
        composeTestRule.onNodeWithText("$75.00").assertIsDisplayed()
        composeTestRule.onNodeWithText("30%").assertIsDisplayed()
    }

    @Test
    fun testAppLifecycle_DataPersistence() {
        // Add an expense
        addTestExpense(amount = "45.00", category = "Shopping", description = "Clothes")

        composeTestRule.waitForIdle()

        // Verify expense is displayed
        composeTestRule.onNodeWithText("$45.00").assertIsDisplayed()

        // Simulate app restart by recreating activity
        composeTestRule.activityRule.scenario.recreate()

        composeTestRule.waitForIdle()

        // Verify expense persists after restart
        composeTestRule.onNodeWithText("$45.00").assertIsDisplayed()
        composeTestRule.onNodeWithText("Clothes").assertIsDisplayed()
    }

    // Helper function to add test expense programmatically
    private fun addTestExpense(amount: String, category: String, description: String) {
        composeTestRule.onNodeWithContentDescription("Add expense").performClick()
        composeTestRule.onNodeWithText("Amount").performTextInput(amount)
        composeTestRule.onNodeWithText("Category").performClick()
        composeTestRule.onNodeWithText(category).performClick()
        composeTestRule.onNodeWithText("Description (Optional)").performTextInput(description)
        composeTestRule.onNodeWithText("Save").performClick()
        composeTestRule.waitForIdle()
    }
}
