# Design Document

## Overview

The Expense Tracker is an Android mobile application that enables users to record, manage, and analyze their personal expenses. The application follows modern Android development practices using Kotlin, Jetpack Compose for UI, and follows the MVVM (Model-View-ViewModel) architecture pattern with clean architecture principles.

### Key Design Principles

- **Offline-first**: All data is stored locally on the device with no cloud dependency
- **Reactive UI**: UI updates automatically when data changes using Flow/StateFlow
- **Single source of truth**: Room database serves as the single source of truth for all expense data
- **Separation of concerns**: Clear separation between UI, business logic, and data layers
- **Material Design 3**: Modern Android UI following Material Design 3 guidelines

## Architecture

### High-Level Architecture

The application follows a layered architecture with three main layers:

```
┌─────────────────────────────────────┐
│         Presentation Layer          │
│  (Composables + ViewModels)         │
└─────────────────────────────────────┘
              ↓ ↑
┌─────────────────────────────────────┐
│          Domain Layer               │
│     (Use Cases + Repositories)      │
└─────────────────────────────────────┘
              ↓ ↑
┌─────────────────────────────────────┐
│           Data Layer                │
│    (Room Database + DAOs)           │
└─────────────────────────────────────┘
```

### Technology Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM with Clean Architecture
- **Database**: Room (SQLite wrapper)
- **Dependency Injection**: Hilt
- **Asynchronous**: Coroutines + Flow
- **Navigation**: Compose Navigation
- **Charts**: Vico or MPAndroidChart

## Components and Interfaces

### 1. Data Layer

#### Database Schema

**Expense Entity**
```kotlin
@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    val categoryId: Long,
    val date: Long, // Unix timestamp
    val description: String,
    val createdAt: Long,
    val updatedAt: Long
)
```

**Category Entity**
```kotlin
@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val iconName: String,
    val colorHex: String,
    val isCustom: Boolean = false,
    val sortOrder: Int
)
```

#### Data Access Objects (DAOs)

**ExpenseDao**
```kotlin
@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<Expense>>
    
    @Query("SELECT * FROM expenses WHERE date BETWEEN :startDate AND :endDate")
    fun getExpensesByDateRange(startDate: Long, endDate: Long): Flow<List<Expense>>
    
    @Query("SELECT * FROM expenses WHERE categoryId = :categoryId")
    fun getExpensesByCategory(categoryId: Long): Flow<List<Expense>>
    
    @Insert
    suspend fun insertExpense(expense: Expense): Long
    
    @Update
    suspend fun updateExpense(expense: Expense)
    
    @Delete
    suspend fun deleteExpense(expense: Expense)
    
    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getExpenseById(id: Long): Expense?
}
```

**CategoryDao**
```kotlin
@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY sortOrder ASC")
    fun getAllCategories(): Flow<List<Category>>
    
    @Insert
    suspend fun insertCategory(category: Category): Long
    
    @Query("SELECT * FROM categories WHERE name = :name LIMIT 1")
    suspend fun getCategoryByName(name: String): Category?
    
    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: Long): Category?
}
```

#### Repository Interfaces

**ExpenseRepository**
```kotlin
interface ExpenseRepository {
    fun getAllExpenses(): Flow<List<ExpenseWithCategory>>
    fun getExpensesByDateRange(startDate: Long, endDate: Long): Flow<List<ExpenseWithCategory>>
    fun getExpensesByCategory(categoryId: Long): Flow<List<ExpenseWithCategory>>
    suspend fun addExpense(expense: Expense): Result<Long>
    suspend fun updateExpense(expense: Expense): Result<Unit>
    suspend fun deleteExpense(expense: Expense): Result<Unit>
    suspend fun getExpenseById(id: Long): Expense?
}
```

**CategoryRepository**
```kotlin
interface CategoryRepository {
    fun getAllCategories(): Flow<List<Category>>
    suspend fun addCategory(category: Category): Result<Long>
    suspend fun getCategoryByName(name: String): Category?
    suspend fun initializeDefaultCategories()
}
```

### 2. Domain Layer

#### Data Models

**ExpenseWithCategory** (Domain model combining expense and category)
```kotlin
data class ExpenseWithCategory(
    val id: Long,
    val amount: Double,
    val category: Category,
    val date: Long,
    val description: String,
    val createdAt: Long,
    val updatedAt: Long
)
```

**FilterCriteria**
```kotlin
data class FilterCriteria(
    val startDate: Long? = null,
    val endDate: Long? = null,
    val categoryIds: Set<Long> = emptySet()
)
```

**SpendingSummary**
```kotlin
data class SpendingSummary(
    val totalAmount: Double,
    val categoryBreakdown: List<CategorySpending>,
    val period: String
)

data class CategorySpending(
    val category: Category,
    val amount: Double,
    val percentage: Double
)
```

#### Use Cases

**AddExpenseUseCase**
- Validates expense data (amount format, required fields)
- Calls repository to persist expense
- Returns success or validation errors

**GetFilteredExpensesUseCase**
- Applies filter criteria to expense list
- Handles multiple simultaneous filters
- Returns filtered expense list

**CalculateSpendingSummaryUseCase**
- Calculates total spending for specified period
- Computes category breakdown with percentages
- Generates spending trends

**DeleteExpenseUseCase**
- Handles expense deletion with confirmation
- Ensures data integrity

### 3. Presentation Layer

#### Screen Components

**ExpenseListScreen**
- Displays paginated list of expenses
- Shows empty state when no expenses exist
- Provides navigation to add/edit screens
- Implements pull-to-refresh
- Shows filter chips for active filters

**AddEditExpenseScreen**
- Form for creating/editing expenses
- Amount input with decimal validation
- Category selector dropdown
- Date picker
- Optional description field
- Save and cancel actions

**FilterScreen**
- Date range picker (start and end date)
- Category multi-select
- Apply and clear filter actions
- Shows active filter count

**SummaryScreen**
- Displays current month total
- Category breakdown with percentages
- Visual charts (pie chart for categories)
- Time period selector
- Spending trends comparison

#### ViewModels

**ExpenseListViewModel**
```kotlin
class ExpenseListViewModel @Inject constructor(
    private val getFilteredExpensesUseCase: GetFilteredExpensesUseCase,
    private val deleteExpenseUseCase: DeleteExpenseUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<ExpenseListUiState>(ExpenseListUiState.Loading)
    val uiState: StateFlow<ExpenseListUiState> = _uiState.asStateFlow()
    
    private val _filterCriteria = MutableStateFlow(FilterCriteria())
    
    fun loadExpenses()
    fun applyFilter(criteria: FilterCriteria)
    fun clearFilters()
    fun deleteExpense(expense: ExpenseWithCategory)
}
```

**AddEditExpenseViewModel**
```kotlin
class AddEditExpenseViewModel @Inject constructor(
    private val addExpenseUseCase: AddExpenseUseCase,
    private val categoryRepository: CategoryRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AddEditExpenseUiState())
    val uiState: StateFlow<AddEditExpenseUiState> = _uiState.asStateFlow()
    
    fun updateAmount(amount: String)
    fun updateCategory(category: Category)
    fun updateDate(date: Long)
    fun updateDescription(description: String)
    fun saveExpense()
    fun validateForm(): ValidationResult
}
```

**SummaryViewModel**
```kotlin
class SummaryViewModel @Inject constructor(
    private val calculateSpendingSummaryUseCase: CalculateSpendingSummaryUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<SummaryUiState>(SummaryUiState.Loading)
    val uiState: StateFlow<SummaryUiState> = _uiState.asStateFlow()
    
    fun loadSummary(period: TimePeriod)
    fun changePeriod(period: TimePeriod)
}
```

#### UI State Models

```kotlin
sealed class ExpenseListUiState {
    object Loading : ExpenseListUiState()
    data class Success(
        val expenses: List<ExpenseWithCategory>,
        val activeFilters: FilterCriteria
    ) : ExpenseListUiState()
    data class Error(val message: String) : ExpenseListUiState()
    object Empty : ExpenseListUiState()
}

data class AddEditExpenseUiState(
    val amount: String = "",
    val selectedCategory: Category? = null,
    val date: Long = System.currentTimeMillis(),
    val description: String = "",
    val categories: List<Category> = emptyList(),
    val validationErrors: Map<String, String> = emptyMap(),
    val isSaving: Boolean = false
)

sealed class SummaryUiState {
    object Loading : SummaryUiState()
    data class Success(val summary: SpendingSummary) : SummaryUiState()
    data class Error(val message: String) : SummaryUiState()
}
```

### 4. Navigation

**Navigation Graph**
```
ExpenseListScreen (Start Destination)
    ├─> AddExpenseScreen
    ├─> EditExpenseScreen/{expenseId}
    ├─> FilterScreen
    └─> SummaryScreen
```

## Data Models

### Validation Rules

**Expense Validation**
- Amount: Required, numeric, up to 2 decimal places, greater than 0
- Category: Required, must exist in categories table
- Date: Required, valid Unix timestamp
- Description: Optional, max 200 characters

**Category Validation**
- Name: Required, 1-30 characters, unique
- Icon: Required, valid icon name from predefined set
- Color: Required, valid hex color code

### Default Categories

The application will initialize with these predefined categories:

1. Food (🍔, #FF6B6B)
2. Transport (🚗, #4ECDC4)
3. Entertainment (🎬, #45B7D1)
4. Shopping (🛍️, #FFA07A)
5. Bills (📄, #98D8C8)
6. Healthcare (⚕️, #F7DC6F)
7. Other (📦, #B19CD9)

## Error Handling

### Error Categories

**Validation Errors**
- Display inline error messages below form fields
- Prevent form submission until resolved
- Clear errors when user corrects input

**Storage Errors**
- Show toast notification for save failures
- Retry mechanism for transient failures
- Log errors for debugging

**Data Loading Errors**
- Display error state with retry button
- Graceful degradation when data unavailable
- Offline-first approach minimizes errors

### Error Handling Strategy

```kotlin
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Exception, val message: String) : Result<Nothing>()
}
```

All repository methods return `Result<T>` to handle errors consistently across the application.

## Testing Strategy

### Unit Tests

**Repository Tests**
- Test CRUD operations with in-memory database
- Verify data transformations
- Test error handling scenarios

**Use Case Tests**
- Test business logic in isolation
- Mock repository dependencies
- Verify validation rules
- Test filter logic

**ViewModel Tests**
- Test state management
- Verify UI state transitions
- Test user interaction handling
- Use TestCoroutineDispatcher for coroutine testing

### Integration Tests

**Database Tests**
- Test Room database migrations
- Verify query correctness
- Test relationships between entities

**End-to-End Tests**
- Test complete user flows (add expense, filter, view summary)
- Use Compose testing framework
- Verify navigation between screens

### UI Tests

**Compose UI Tests**
- Test individual composable rendering
- Verify user interactions
- Test accessibility features
- Validate theme support (light/dark)

## Performance Considerations

### Database Optimization

- Index on `date` column for faster date range queries
- Index on `categoryId` for category filtering
- Pagination for large expense lists (20 items per page)

### UI Performance

- LazyColumn for efficient list rendering
- Remember and derivedStateOf for expensive calculations
- Avoid recomposition with stable data classes
- Use Flow.collectAsStateWithLifecycle for lifecycle-aware collection

### Memory Management

- Limit in-memory expense list size
- Use paging for historical data
- Clear unused resources in ViewModel onCleared()

## Accessibility

- Content descriptions for all interactive elements
- Semantic properties for screen readers
- Sufficient color contrast ratios (WCAG AA)
- Touch target sizes minimum 48dp
- Support for TalkBack navigation

## Theme Support

**Light and Dark Themes**
- Follow system theme preference
- Material Design 3 dynamic color scheme
- Consistent color usage across themes
- Test all UI components in both themes

## Security Considerations

- No sensitive data encryption required (local-only app)
- Input validation to prevent SQL injection (Room handles this)
- No network communication, no API security needed
- Data remains on device, no cloud backup

## Future Enhancements (Out of Scope)

- Export data to CSV/PDF
- Budget setting and alerts
- Recurring expense support
- Multi-currency support
- Cloud backup and sync
- Widgets for quick expense entry

