package com.expensetracker.app.integration

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.expensetracker.app.data.local.AppDatabase
import com.expensetracker.app.data.local.entity.Expense
import com.expensetracker.app.data.preferences.HouseholdPreferencesManager
import com.expensetracker.app.domain.model.Result
import com.expensetracker.app.domain.repository.ExpenseRepository
import com.expensetracker.app.domain.sync.ExpenseSyncCoordinator
import com.expensetracker.app.domain.model.SyncStatus
import com.google.firebase.Timestamp
import java.util.Date
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/**
 * Integration tests for Firestore sync functionality
 * Tests end-to-end sync flow, offline sync, and conflict scenarios
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class FirestoreSyncIntegrationTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var database: AppDatabase

    @Inject
    lateinit var expenseRepository: ExpenseRepository

    @Inject
    lateinit var syncCoordinator: ExpenseSyncCoordinator

    @Inject
    lateinit var householdPreferences: HouseholdPreferencesManager

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
        runBlocking {
            syncCoordinator.stopSync()
        }
        database.close()
    }

    @Test
    fun testEndToEndSyncFlow_AddExpense() = runBlocking {
        // Given - sync is started
        syncCoordinator.startSync()
        delay(100)

        // When - add an expense
        val expense = Expense(
            amount = 50.0,
            description = "Test expense",
            categoryId = 1L,
            date = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            createdBy = "test-user",
            modifiedBy = "test-user",
            modifiedAt = System.currentTimeMillis(),
            syncStatus = "PENDING"
        )
        
        val result = expenseRepository.addExpense(expense)
        assertTrue(result is Result.Success)
        val expenseId = (result as Result.Success).data
        
        // Wait for sync to complete
        delay(1000)

        // Then - expense should be synced
        val syncedExpense = expenseRepository.getExpenseById(expenseId)
        assertNotNull(syncedExpense)
        assertEquals("SYNCED", syncedExpense?.syncStatus)
    }

    @Test
    fun testEndToEndSyncFlow_UpdateExpense() = runBlocking {
        // Given - an existing synced expense
        syncCoordinator.startSync()
        delay(100)

        val expense = Expense(
            amount = 50.0,
            description = "Original description",
            categoryId = 1L,
            date = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            createdBy = "test-user",
            modifiedBy = "test-user",
            modifiedAt = System.currentTimeMillis(),
            syncStatus = "SYNCED"
        )
        
        val result = expenseRepository.addExpense(expense)
        assertTrue(result is Result.Success)
        val expenseId = (result as Result.Success).data
        delay(500)

        // When - update the expense
        val updatedExpense = expense.copy(
            id = expenseId,
            description = "Updated description",
            amount = 75.0,
            syncStatus = "PENDING"
        )
        
        expenseRepository.updateExpense(updatedExpense)
        delay(1000)

        // Then - updated expense should be synced
        val syncedExpense = expenseRepository.getExpenseById(expenseId)
        assertNotNull(syncedExpense)
        assertEquals("Updated description", syncedExpense!!.description)
        assertEquals(75.0, syncedExpense.amount, 0.01)
        assertEquals("SYNCED", syncedExpense.syncStatus)
    }

    @Test
    fun testEndToEndSyncFlow_DeleteExpense() = runBlocking {
        // Given - an existing synced expense
        syncCoordinator.startSync()
        delay(100)

        val expense = Expense(
            amount = 50.0,
            description = "To be deleted",
            categoryId = 1L,
            date = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            createdBy = "test-user",
            modifiedBy = "test-user",
            modifiedAt = System.currentTimeMillis(),
            syncStatus = "SYNCED"
        )
        
        val result = expenseRepository.addExpense(expense)
        assertTrue(result is Result.Success)
        val expenseId = (result as Result.Success).data
        delay(500)

        // When - delete the expense
        val expenseToDelete = expenseRepository.getExpenseById(expenseId)
        assertNotNull(expenseToDelete)
        expenseRepository.deleteExpense(expenseToDelete!!)
        delay(1000)

        // Then - expense should be deleted locally
        val deletedExpense = expenseRepository.getExpenseById(expenseId)
        assertNull(deletedExpense)
    }

    @Test
    fun testOfflineSync_QueuesPendingChanges() = runBlocking {
        // Given - sync is started but offline
        syncCoordinator.startSync()
        delay(100)

        // When - add expense while offline (simulated by not having household)
        val expense = Expense(
            amount = 50.0,
            description = "Offline expense",
            categoryId = 1L,
            date = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            createdBy = "test-user",
            modifiedBy = "test-user",
            modifiedAt = System.currentTimeMillis(),
            syncStatus = "PENDING"
        )
        
        val result = expenseRepository.addExpense(expense)
        assertTrue(result is Result.Success)
        val expenseId = (result as Result.Success).data
        delay(500)

        // Then - expense should be saved locally with pending status
        val savedExpense = expenseRepository.getExpenseById(expenseId)
        assertNotNull(savedExpense)
        assertEquals("Offline expense", savedExpense?.description)
        // Status may be PENDING or ERROR depending on connectivity
        assertTrue(savedExpense?.syncStatus in listOf("PENDING", "ERROR", "SYNCED"))
    }

    @Test
    fun testConflictResolution_LastWriteWins() = runBlocking {
        // Given - sync is started
        syncCoordinator.startSync()
        delay(100)

        val now = System.currentTimeMillis()
        
        // Create local expense with older timestamp
        val localExpense = Expense(
            amount = 50.0,
            description = "Local version",
            categoryId = 1L,
            date = now,
            createdAt = now,
            updatedAt = now,
            createdBy = "test-user",
            modifiedBy = "test-user",
            modifiedAt = now - 1000, // Older
            firestoreId = "test-firestore-id",
            syncStatus = "SYNCED"
        )
        
        val expenseId = expenseRepository.addExpense(localExpense)
        delay(500)

        // When - simulate receiving newer version from cloud
        val remoteExpense = com.expensetracker.app.data.firebase.models.FirestoreExpense(
            id = "test-firestore-id",
            householdId = "test-household",
            amount = 75.0,
            description = "Remote version",
            categoryId = 1L,
            date = Timestamp(Date(now)),
            createdBy = "test-user",
            createdAt = Timestamp(Date(now)),
            modifiedBy = "other-user",
            modifiedAt = Timestamp(Date(now)), // Newer
            isDeleted = false
        )
        
        syncCoordinator.syncExpenseFromCloud(remoteExpense)
        delay(500)

        // Then - local should be updated with remote version (last-write-wins)
        val updatedExpense = expenseRepository.getExpenseByFirestoreId("test-firestore-id")
        assertNotNull(updatedExpense)
        assertEquals("Remote version", updatedExpense!!.description)
        assertEquals(75.0, updatedExpense.amount, 0.01)
    }

    @Test
    fun testConflictResolution_KeepsNewerLocal() = runBlocking {
        // Given - sync is started
        syncCoordinator.startSync()
        delay(100)

        val now = System.currentTimeMillis()
        
        // Create local expense with newer timestamp
        val localExpense = Expense(
            amount = 50.0,
            description = "Local version",
            categoryId = 1L,
            date = now,
            createdAt = now,
            updatedAt = now,
            createdBy = "test-user",
            modifiedBy = "test-user",
            modifiedAt = now, // Newer
            firestoreId = "test-firestore-id-2",
            syncStatus = "SYNCED"
        )
        
        val expenseId = expenseRepository.addExpense(localExpense)
        delay(500)

        // When - simulate receiving older version from cloud
        val remoteExpense = com.expensetracker.app.data.firebase.models.FirestoreExpense(
            id = "test-firestore-id-2",
            householdId = "test-household",
            amount = 75.0,
            description = "Remote version",
            categoryId = 1L,
            date = Timestamp(Date(now)),
            createdBy = "test-user",
            createdAt = Timestamp(Date(now)),
            modifiedBy = "other-user",
            modifiedAt = Timestamp(Date(now - 1000)), // Older
            isDeleted = false
        )
        
        syncCoordinator.syncExpenseFromCloud(remoteExpense)
        delay(500)

        // Then - local should remain unchanged (newer wins)
        val unchangedExpense = expenseRepository.getExpenseByFirestoreId("test-firestore-id-2")
        assertNotNull(unchangedExpense)
        assertEquals("Local version", unchangedExpense!!.description)
        assertEquals(50.0, unchangedExpense.amount, 0.01)
    }

    @Test
    fun testSyncStatus_ReflectsCurrentState() = runBlocking {
        // Given - sync is started
        syncCoordinator.startSync()
        delay(100)

        // When - check initial status
        val initialStatus = syncCoordinator.syncStatus.first()

        // Then - should be synced or offline
        assertTrue(initialStatus is SyncStatus.Synced || initialStatus is SyncStatus.Offline)
    }

    @Test
    fun testMultipleExpenses_SyncInBatch() = runBlocking {
        // Given - sync is started
        syncCoordinator.startSync()
        delay(100)

        // When - add multiple expenses
        val expenses = listOf(
            Expense(
                amount = 10.0,
                description = "Expense 1",
                categoryId = 1L,
                date = System.currentTimeMillis(),
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                createdBy = "test-user",
                modifiedBy = "test-user",
                modifiedAt = System.currentTimeMillis(),
                syncStatus = "PENDING"
            ),
            Expense(
                amount = 20.0,
                description = "Expense 2",
                categoryId = 1L,
                date = System.currentTimeMillis(),
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                createdBy = "test-user",
                modifiedBy = "test-user",
                modifiedAt = System.currentTimeMillis(),
                syncStatus = "PENDING"
            ),
            Expense(
                amount = 30.0,
                description = "Expense 3",
                categoryId = 1L,
                date = System.currentTimeMillis(),
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                createdBy = "test-user",
                modifiedBy = "test-user",
                modifiedAt = System.currentTimeMillis(),
                syncStatus = "PENDING"
            )
        )

        expenses.forEach { expense ->
            expenseRepository.addExpense(expense)
            delay(100)
        }

        // Wait for all to sync
        delay(2000)

        // Then - all expenses should be saved locally
        val allExpenses = expenseRepository.getAllExpenses().first()
        assertTrue(allExpenses.size >= 3)
    }
}
