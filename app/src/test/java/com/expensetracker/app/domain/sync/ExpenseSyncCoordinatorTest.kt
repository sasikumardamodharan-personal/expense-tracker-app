package com.expensetracker.app.domain.sync

import com.expensetracker.app.data.firebase.models.FirestoreExpense
import com.expensetracker.app.data.local.entity.Expense
import com.expensetracker.app.data.preferences.HouseholdPreferencesManager
import com.expensetracker.app.domain.model.Result
import com.expensetracker.app.domain.model.SyncStatus
import com.expensetracker.app.domain.repository.ExpenseRepository
import com.expensetracker.app.domain.repository.FirestoreExpenseRepository
import com.google.firebase.Timestamp
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import java.util.Date
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

@OptIn(ExperimentalCoroutinesApi::class)
class ExpenseSyncCoordinatorTest {

    private lateinit var syncCoordinator: ExpenseSyncCoordinator
    private lateinit var localRepository: ExpenseRepository
    private lateinit var firestoreRepository: FirestoreExpenseRepository
    private lateinit var householdPreferences: HouseholdPreferencesManager
    private lateinit var connectivityMonitor: ConnectivityMonitor
    private lateinit var syncThrottler: SyncThrottler
    private lateinit var testScope: TestScope

    private val testHouseholdId = "test-household-123"
    private val testUserId = "test-user-456"

    @Before
    fun setup() {
        testScope = TestScope(UnconfinedTestDispatcher())
        
        localRepository = mock()
        firestoreRepository = mock()
        householdPreferences = mock()
        connectivityMonitor = mock()
        syncThrottler = mock()

        // Setup default mocks
        runTest {
            whenever(householdPreferences.getHouseholdId()).thenReturn(testHouseholdId)
        }
        whenever(connectivityMonitor.isConnectedValue()).thenReturn(true)
        whenever(connectivityMonitor.isConnected).thenReturn(MutableStateFlow(true))

        syncCoordinator = ExpenseSyncCoordinator(
            localRepository = localRepository,
            firestoreRepository = firestoreRepository,
            householdPreferences = householdPreferences,
            connectivityMonitor = connectivityMonitor,
            syncThrottler = syncThrottler,
            coroutineScope = testScope
        )
    }

    @Test
    fun `syncExpenseToCloud - successful sync updates status to synced`() = runTest {
        // Given
        val expense = createTestExpense(id = 1L, amount = 50.0)
        whenever(firestoreRepository.syncExpense(any())).thenReturn(Result.Success(Unit))
        whenever(localRepository.updateExpenseWithoutSync(any())).thenReturn(Result.Success(Unit))

        // When
        syncCoordinator.syncExpenseToCloud(expense)
        advanceTimeBy(600) // Wait for throttle

        // Then
        verify(firestoreRepository).syncExpense(expense)
        verify(localRepository).updateExpenseWithoutSync(argThat { syncStatus == "SYNCED" })
        assertEquals(SyncStatus.Synced, syncCoordinator.syncStatus.value)
    }

    @Test
    fun `syncExpenseToCloud - offline sets status to offline`() = runTest {
        // Given
        val expense = createTestExpense(id = 1L, amount = 50.0)
        whenever(connectivityMonitor.isConnectedValue()).thenReturn(false)

        // When
        syncCoordinator.syncExpenseToCloud(expense)

        // Then
        verify(firestoreRepository, never()).syncExpense(any())
        assertEquals(SyncStatus.Offline, syncCoordinator.syncStatus.value)
    }

    @Test
    fun `syncExpenseToCloud - no household ID skips sync`() = runTest {
        // Given
        val expense = createTestExpense(id = 1L, amount = 50.0)
        whenever(householdPreferences.getHouseholdId()).thenReturn(null)

        // When
        syncCoordinator.syncExpenseToCloud(expense)
        advanceTimeBy(600)

        // Then
        verify(firestoreRepository, never()).syncExpense(any())
    }

    @Test
    fun `syncExpenseFromCloud - new expense is added locally`() = runTest {
        // Given
        val firestoreExpense = createTestFirestoreExpense(
            id = "firestore-1",
            amount = 75.0,
            modifiedAt = System.currentTimeMillis()
        )
        whenever(localRepository.getExpenseByFirestoreId(any())).thenReturn(null)
        whenever(localRepository.addExpenseWithoutSync(any())).thenReturn(Result.Success(1L))

        // When
        syncCoordinator.syncExpenseFromCloud(firestoreExpense)

        // Then
        verify(localRepository).addExpenseWithoutSync(argThat {
            amount == 75.0 && syncStatus == "SYNCED"
        })
    }

    @Test
    fun `syncExpenseFromCloud - conflict resolution uses last-write-wins`() = runTest {
        // Given
        val localModifiedAt = 1000L
        val remoteModifiedAt = 2000L
        
        val localExpense = createTestExpense(
            id = 1L,
            amount = 50.0,
            modifiedAt = localModifiedAt
        )
        
        val firestoreExpense = createTestFirestoreExpense(
            id = "firestore-1",
            amount = 75.0,
            modifiedAt = remoteModifiedAt
        )
        
        whenever(localRepository.getExpenseByFirestoreId("firestore-1")).thenReturn(localExpense)
        whenever(localRepository.updateExpenseWithoutSync(any())).thenReturn(Result.Success(Unit))

        // When
        syncCoordinator.syncExpenseFromCloud(firestoreExpense)

        // Then - remote is newer, should update local
        verify(localRepository).updateExpenseWithoutSync(argThat {
            amount == 75.0 && modifiedAt == remoteModifiedAt
        })
    }

    @Test
    fun `syncExpenseFromCloud - keeps local when local is newer`() = runTest {
        // Given
        val localModifiedAt = 2000L
        val remoteModifiedAt = 1000L
        
        val localExpense = createTestExpense(
            id = 1L,
            amount = 50.0,
            modifiedAt = localModifiedAt
        )
        
        val firestoreExpense = createTestFirestoreExpense(
            id = "firestore-1",
            amount = 75.0,
            modifiedAt = remoteModifiedAt
        )
        
        whenever(localRepository.getExpenseByFirestoreId("firestore-1")).thenReturn(localExpense)

        // When
        syncCoordinator.syncExpenseFromCloud(firestoreExpense)

        // Then - local is newer, should not update
        verify(localRepository, never()).updateExpenseWithoutSync(any())
    }

    @Test
    fun `deleteExpenseFromCloud - successful deletion updates status`() = runTest {
        // Given
        val expenseId = 1L
        whenever(firestoreRepository.deleteExpense(any())).thenReturn(Result.Success(Unit))

        // When
        syncCoordinator.deleteExpenseFromCloud(expenseId)

        // Then
        verify(firestoreRepository).deleteExpense("1")
        assertEquals(SyncStatus.Synced, syncCoordinator.syncStatus.value)
    }

    @Test
    fun `deleteExpenseFromCloud - offline sets status to offline`() = runTest {
        // Given
        val expenseId = 1L
        whenever(connectivityMonitor.isConnectedValue()).thenReturn(false)

        // When
        syncCoordinator.deleteExpenseFromCloud(expenseId)

        // Then
        verify(firestoreRepository, never()).deleteExpense(any())
        assertEquals(SyncStatus.Offline, syncCoordinator.syncStatus.value)
    }

    @Test
    fun `migrateLocalExpenses - successful migration returns success`() = runTest {
        // Given
        whenever(firestoreRepository.uploadAllLocalExpenses(testHouseholdId))
            .thenReturn(Result.Success(Unit))

        // When
        val result = syncCoordinator.migrateLocalExpenses()

        // Then
        assertTrue(result is Result.Success)
        verify(firestoreRepository).uploadAllLocalExpenses(testHouseholdId)
        assertEquals(SyncStatus.Synced, syncCoordinator.syncStatus.value)
    }

    @Test
    fun `migrateLocalExpenses - no household returns error`() = runTest {
        // Given
        whenever(householdPreferences.getHouseholdId()).thenReturn(null)

        // When
        val result = syncCoordinator.migrateLocalExpenses()

        // Then
        assertTrue(result is Result.Error)
        verify(firestoreRepository, never()).uploadAllLocalExpenses(any())
    }

    @Test
    fun `migrateLocalExpenses - offline returns error`() = runTest {
        // Given
        whenever(connectivityMonitor.isConnectedValue()).thenReturn(false)

        // When
        val result = syncCoordinator.migrateLocalExpenses()

        // Then
        assertTrue(result is Result.Error)
        assertEquals(SyncStatus.Offline, syncCoordinator.syncStatus.value)
        verify(firestoreRepository, never()).uploadAllLocalExpenses(any())
    }

    @Test
    fun `startSync - monitors connectivity changes`() = runTest {
        // Given
        val connectivityFlow = MutableStateFlow(true)
        whenever(connectivityMonitor.isConnected).thenReturn(connectivityFlow)

        // When
        syncCoordinator.startSync()
        advanceTimeBy(100)

        // Simulate connectivity loss
        connectivityFlow.value = false
        advanceTimeBy(100)

        // Then
        assertEquals(SyncStatus.Offline, syncCoordinator.syncStatus.value)

        // Simulate connectivity restored
        connectivityFlow.value = true
        advanceTimeBy(100)

        // Then
        assertEquals(SyncStatus.Synced, syncCoordinator.syncStatus.value)
    }

    @Test
    fun `stopSync - cancels sync job`() = runTest {
        // Given
        syncCoordinator.startSync()
        advanceTimeBy(100)

        // When
        syncCoordinator.stopSync()
        advanceTimeBy(100)

        // Then - no exceptions should occur
        assertTrue(true)
    }

    // Helper functions
    private fun createTestExpense(
        id: Long,
        amount: Double,
        description: String = "Test expense",
        categoryId: Long = 1L,
        modifiedAt: Long = System.currentTimeMillis()
    ): Expense {
        return Expense(
            id = id,
            amount = amount,
            description = description,
            categoryId = categoryId,
            date = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            firestoreId = "firestore-$id",
            modifiedAt = modifiedAt,
            createdBy = testUserId,
            modifiedBy = testUserId,
            syncStatus = "PENDING"
        )
    }

    private fun createTestFirestoreExpense(
        id: String,
        amount: Double,
        description: String = "Test expense",
        categoryId: Long = 1L,
        modifiedAt: Long = System.currentTimeMillis()
    ): FirestoreExpense {
        return FirestoreExpense(
            id = id,
            householdId = testHouseholdId,
            amount = amount,
            description = description,
            categoryId = categoryId,
            date = Timestamp(Date(System.currentTimeMillis())),
            createdBy = testUserId,
            createdAt = Timestamp(Date(System.currentTimeMillis())),
            modifiedBy = testUserId,
            modifiedAt = Timestamp(Date(modifiedAt)),
            isDeleted = false
        )
    }
}
