# Sync Architecture Documentation

## Overview

This document provides technical documentation for developers working with the Firestore sync implementation in the Expense Tracker app.

## Architecture Components

### 1. Sync Coordinator (`ExpenseSyncCoordinator`)

**Purpose**: Orchestrates bidirectional synchronization between Room and Firestore.

**Key Responsibilities**:
- Syncing local changes to Firestore
- Syncing remote changes to local database
- Conflict resolution using last-write-wins
- Throttling sync operations
- Retry logic with exponential backoff
- Connectivity monitoring

**Usage Example**:
```kotlin
// Inject the coordinator
@Inject lateinit var syncCoordinator: ExpenseSyncCoordinator

// Start sync when household is set up
syncCoordinator.startSync()

// Sync an expense to cloud
lifecycleScope.launch {
    syncCoordinator.syncExpenseToCloud(expense)
}

// Observe sync status
syncCoordinator.syncStatus.collect { status ->
    when (status) {
        is SyncStatus.Synced -> // Show synced indicator
        is SyncStatus.Syncing -> // Show loading
        is SyncStatus.Offline -> // Show offline indicator
        is SyncStatus.Error -> // Show error message
    }
}
```

### 2. Household Manager (`HouseholdManager`)

**Purpose**: Manages household lifecycle and membership operations.

**Key Responsibilities**:
- Creating new households
- Joining existing households
- Managing household preferences
- Coordinating sync initialization
- Handling household leave operations

**Usage Example**:
```kotlin
// Create a new household
val result = householdManager.createNewHousehold("Smith Family")
when (result) {
    is Result.Success -> {
        val household = result.data
        // Household created with invite code
    }
    is Result.Error -> {
        // Handle error
    }
}

// Join an existing household
val result = householdManager.joinExistingHousehold("ABC12XYZ")

// Observe current household
householdManager.currentHousehold.collect { household ->
    // Update UI with household info
}
```

### 3. Data Migration Service (`DataMigrationService`)

**Purpose**: Handles one-time migration of local data to Firestore.

**Key Responsibilities**:
- Batch upload of expenses
- Progress tracking
- Error handling and retry
- Migration state persistence

**Usage Example**:
```kotlin
// Start migration
lifecycleScope.launch {
    val result = migrationService.migrateToFirestore()
    when (result) {
        is Result.Success -> // Migration complete
        is Result.Error -> // Handle error
    }
}

// Observe migration progress
migrationService.migrationProgress.collect { progress ->
    when (progress) {
        is MigrationProgress.Idle -> // Not started
        is MigrationProgress.InProgress -> {
            // Show progress: ${progress.current}/${progress.total}
        }
        is MigrationProgress.Complete -> // Migration done
        is MigrationProgress.Error -> // Show error
    }
}
```

### 4. Category Sync Coordinator (`CategorySyncCoordinator`)

**Purpose**: Synchronizes categories across household members.

**Key Responsibilities**:
- Syncing category changes to Firestore
- Listening for remote category changes
- Merging default and household categories
- Handling category conflicts

### 5. Firestore Repositories

#### `FirestoreExpenseRepository`
- CRUD operations for expenses in Firestore
- Batch upload for migration
- Real-time listeners for expense changes

#### `FirestoreHouseholdRepository`
- Household creation and management
- Member management
- Invite code generation and validation

#### `FirestoreCategoryRepository`
- Category CRUD operations
- Category synchronization

## Data Flow

### Adding an Expense

```
User adds expense
    ↓
ViewModel calls repository.addExpense()
    ↓
ExpenseRepository saves to Room
    ↓
ExpenseRepository triggers syncCoordinator.syncExpenseToCloud()
    ↓
SyncCoordinator throttles and queues sync
    ↓
FirestoreExpenseRepository uploads to Firestore
    ↓
Firestore broadcasts change to other devices
    ↓
Other devices receive via snapshot listener
    ↓
SyncCoordinator.syncExpenseFromCloud() called
    ↓
Local database updated on other devices
```

### Conflict Resolution

```
Device A edits expense (offline)
Device B edits same expense (offline)
    ↓
Both devices come online
    ↓
Both attempt to sync
    ↓
SyncCoordinator compares modifiedAt timestamps
    ↓
Last-write-wins: Most recent change is kept
    ↓
Losing device receives update from Firestore
    ↓
Local database updated with winning version
```

## Key Design Patterns

### 1. Repository Pattern

All data access goes through repository interfaces:
- `ExpenseRepository` (local)
- `FirestoreExpenseRepository` (remote)
- `HouseholdRepository` (remote)

This abstraction allows for:
- Easy testing with mocks
- Separation of concerns
- Flexibility to change implementations

### 2. Coordinator Pattern

The `ExpenseSyncCoordinator` acts as a mediator between local and remote repositories, handling:
- Sync orchestration
- Conflict resolution
- Error handling
- Status management

### 3. Observer Pattern

StateFlows and Flows are used throughout for reactive updates:
- `syncStatus: StateFlow<SyncStatus>`
- `migrationProgress: StateFlow<MigrationProgress>`
- `currentHousehold: Flow<Household?>`

### 4. Retry Pattern

The `SyncRetryManager` implements exponential backoff:
```kotlin
class SyncRetryManager(private val maxRetries: Int = 3) {
    suspend fun <T> executeWithRetry(
        operation: suspend () -> Result<T>,
        onRetry: (attemptCount: Int, error: Exception) -> Unit = { _, _ -> }
    ): Result<T>
}
```

### 5. Throttling Pattern

The `SyncThrottler` prevents excessive sync operations:
```kotlin
class SyncThrottler {
    fun throttleSync(
        key: String,
        delayMs: Long = 500,
        action: suspend () -> Unit
    )
}
```

## Database Schema

### Room Database

#### Expense Entity
```kotlin
@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val description: String,
    val categoryId: Long,
    val date: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val firestoreId: String? = null,      // Links to Firestore document
    val syncStatus: String = "PENDING",    // PENDING, SYNCED, ERROR
    val modifiedAt: Long,                  // For conflict resolution
    val createdBy: String? = null,         // User ID who created
    val modifiedBy: String? = null         // User ID who last modified
)
```

### Firestore Schema

#### Households Collection
```
/households/{householdId}
  - name: String
  - createdBy: String (userId)
  - createdAt: Timestamp
  - inviteCode: String
  - memberIds: List<String>
```

#### Expenses Collection
```
/expenses/{householdId}/expenses/{expenseId}
  - id: String
  - amount: Double
  - description: String
  - categoryId: Long
  - date: Timestamp
  - createdBy: String
  - createdAt: Timestamp
  - modifiedBy: String
  - modifiedAt: Timestamp
  - isDeleted: Boolean
```

#### Categories Collection
```
/categories/{householdId}/categories/{categoryId}
  - id: String
  - name: String
  - icon: String
  - color: String
  - createdBy: String
  - createdAt: Timestamp
  - isDefault: Boolean
```

## Sync Status States

```kotlin
sealed class SyncStatus {
    object Synced : SyncStatus()           // All changes synced
    object Syncing : SyncStatus()          // Sync in progress
    data class Error(val message: String) : SyncStatus()  // Sync failed
    object Offline : SyncStatus()          // No connectivity
}
```

## Error Handling

### Error Types

1. **Network Errors**: No internet connection
   - Status: `SyncStatus.Offline`
   - Action: Queue changes, retry when online

2. **Authentication Errors**: Token expired
   - Status: `SyncStatus.Error`
   - Action: Prompt user to re-authenticate

3. **Permission Errors**: Firestore rules denied access
   - Status: `SyncStatus.Error`
   - Action: Check household membership

4. **Quota Errors**: Firestore limits exceeded
   - Status: `SyncStatus.Error`
   - Action: Inform user, suggest reducing frequency

### Error Handler

```kotlin
object SyncErrorHandler {
    fun handleSyncError(exception: Exception): SyncError {
        return when (exception) {
            is FirebaseNetworkException -> 
                SyncError.NetworkError("No internet connection")
            is FirebaseAuthException -> 
                SyncError.AuthError("Please sign in again")
            is FirebaseFirestoreException -> {
                when (exception.code) {
                    FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                        SyncError.PermissionError("Access denied")
                    FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED ->
                        SyncError.QuotaError("Quota exceeded")
                    else -> SyncError.UnknownError(exception.message)
                }
            }
            else -> SyncError.UnknownError(exception.message)
        }
    }
}
```

## Testing

### Unit Tests

Test sync coordinator logic:
```kotlin
@Test
fun `syncExpenseToCloud updates sync status`() = runTest {
    // Given
    val expense = createTestExpense()
    
    // When
    syncCoordinator.syncExpenseToCloud(expense)
    
    // Then
    assertEquals(SyncStatus.Syncing, syncCoordinator.syncStatus.value)
}
```

### Integration Tests

Test end-to-end sync flow:
```kotlin
@Test
fun `expense syncs between devices`() = runTest {
    // Given two devices in same household
    val device1 = createTestDevice()
    val device2 = createTestDevice()
    
    // When device1 adds expense
    device1.addExpense(expense)
    
    // Then device2 receives it
    delay(5000) // Wait for sync
    val expenses = device2.getExpenses()
    assertTrue(expenses.contains(expense))
}
```

## Performance Considerations

### Throttling

Sync operations are throttled to 500ms to prevent excessive network calls:
```kotlin
syncThrottler.throttleSync("expense_${expense.id}", 500L) {
    performSyncToCloud(expense, householdId)
}
```

### Batching

Migration uploads expenses in batches of 50:
```kotlin
expenses.chunked(BATCH_SIZE).forEach { batch ->
    batch.forEach { expense ->
        firestoreRepository.syncExpense(expense)
    }
}
```

### Offline Persistence

Firestore offline persistence is enabled to cache data locally:
```kotlin
FirebaseFirestore.getInstance().apply {
    firestoreSettings = firestoreSettings.toBuilder()
        .setPersistenceEnabled(true)
        .build()
}
```

### Real-time Listeners

Use Firestore snapshot listeners instead of polling:
```kotlin
fun observeHouseholdExpenses(householdId: String): Flow<List<FirestoreExpense>> {
    return callbackFlow {
        val listener = expensesCollection(householdId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val expenses = snapshot?.toObjects(FirestoreExpense::class.java) ?: emptyList()
                trySend(expenses)
            }
        awaitClose { listener.remove() }
    }
}
```

## Security

### Firestore Security Rules

Rules enforce household-based access control:
```javascript
match /expenses/{householdId}/expenses/{expenseId} {
  allow read, write: if request.auth != null && 
    request.auth.uid in get(/databases/$(database)/documents/households/$(householdId)).data.memberIds;
}
```

### Data Validation

Validate all input before Firestore write:
```kotlin
private fun validateExpense(expense: Expense): Boolean {
    return expense.amount > 0 &&
           expense.description.isNotBlank() &&
           expense.description.length <= 500 &&
           expense.categoryId > 0
}
```

## Monitoring and Logging

### Logging Strategy

Use structured logging with tags:
```kotlin
Log.d(TAG, "Syncing expense ${expense.id} to Firestore")
Log.w(TAG, "Retry attempt $attemptCount for expense ${expense.id}")
Log.e(TAG, "Failed to sync expense ${expense.id}", exception)
```

### Key Metrics to Monitor

- Sync success rate
- Average sync latency
- Conflict frequency
- Offline queue size
- Error rates by type
- Migration completion time

## Common Issues and Solutions

### Issue: Expenses not syncing

**Diagnosis**:
1. Check connectivity: `connectivityMonitor.isConnected`
2. Check sync status: `syncCoordinator.syncStatus`
3. Check household ID: `householdPreferences.getHouseholdId()`
4. Check authentication: `auth.currentUser`

**Solution**:
- Ensure user is authenticated
- Verify household membership
- Check internet connection
- Review Firestore security rules

### Issue: Duplicate expenses

**Diagnosis**:
- Check if expense has `firestoreId` set
- Review sync logs for conflicts

**Solution**:
- Ensure `firestoreId` is properly set after first sync
- Verify conflict resolution logic is working

### Issue: Slow sync performance

**Diagnosis**:
- Check batch sizes
- Review throttling settings
- Monitor network latency

**Solution**:
- Adjust `BATCH_SIZE` constant
- Tune `SYNC_THROTTLE_MS` value
- Enable Firestore offline persistence

## Future Enhancements

### Planned Features

1. **Selective Sync**: Sync only recent expenses
2. **Delta Sync**: Sync only changed fields
3. **Compression**: Compress large text fields
4. **Multi-Household**: Support multiple households per user
5. **Conflict UI**: Show conflicts to user for manual resolution

### Extensibility Points

The architecture is designed for extensibility:

1. **Custom Conflict Resolution**: Implement `ConflictResolver` interface
2. **Custom Sync Strategies**: Extend `SyncCoordinator`
3. **Additional Data Types**: Follow repository pattern
4. **Custom Error Handling**: Extend `SyncErrorHandler`

## References

- [Firestore Documentation](https://firebase.google.com/docs/firestore)
- [Room Database Guide](https://developer.android.com/training/data-storage/room)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)
- [Hilt Dependency Injection](https://developer.android.com/training/dependency-injection/hilt-android)

---

*For user-facing documentation, see [HOUSEHOLD_SETUP_GUIDE.md](HOUSEHOLD_SETUP_GUIDE.md)*
