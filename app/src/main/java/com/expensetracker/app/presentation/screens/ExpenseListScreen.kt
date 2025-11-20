package com.expensetracker.app.presentation.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.expensetracker.app.domain.model.ExpenseWithCategory
import com.expensetracker.app.presentation.screens.components.*
import com.expensetracker.app.presentation.viewmodel.ExpenseListUiState
import com.expensetracker.app.presentation.viewmodel.ExpenseListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseListScreen(
    onNavigateToAddExpense: () -> Unit,
    onNavigateToEditExpense: (Long) -> Unit,
    onNavigateToFilter: () -> Unit,
    onNavigateToSummary: () -> Unit,
    onNavigateToSettings: () -> Unit,
    shouldRefresh: Boolean = false,
    viewModel: ExpenseListViewModel = hiltViewModel()
) {
    // Use collectAsStateWithLifecycle for lifecycle-aware collection
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pagedExpenses = viewModel.pagedExpenses.collectAsLazyPagingItems()
    val filterCriteria by viewModel.filterCriteria.collectAsStateWithLifecycle()
    val selectedCurrency by viewModel.selectedCurrency.collectAsStateWithLifecycle()
    
    val context = LocalContext.current
    val haptic = com.expensetracker.app.util.rememberHapticFeedback()
    
    // Use derivedStateOf for expensive calculations to avoid unnecessary recomposition
    val usePagination by remember {
        derivedStateOf {
            filterCriteria.startDate == null && 
            filterCriteria.endDate == null && 
            filterCriteria.categoryIds.isEmpty()
        }
    }
    
    LaunchedEffect(Unit) {
        viewModel.errorMessage.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }
    
    // Initial load
    LaunchedEffect(Unit) {
        viewModel.refreshExpenses()
    }
    
    // Refresh when returning from add/edit with saved expense
    LaunchedEffect(shouldRefresh) {
        if (shouldRefresh) {
            viewModel.refreshExpenses()
            pagedExpenses.refresh()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Expense Tracker",
                        modifier = Modifier.semantics { heading() }
                    ) 
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.refreshExpenses()
                            pagedExpenses.refresh()
                        },
                        modifier = Modifier.semantics {
                            contentDescription = "Refresh expenses button"
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh"
                        )
                    }
                    IconButton(
                        onClick = onNavigateToFilter,
                        modifier = Modifier.semantics {
                            contentDescription = "Filter expenses button"
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Filter expenses"
                        )
                    }
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.semantics {
                            contentDescription = "Settings button"
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                    TextButton(
                        onClick = onNavigateToSummary,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .semantics {
                                contentDescription = "View spending summary button"
                            }
                    ) {
                        Text("Summary")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddExpense,
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics {
                    contentDescription = "Add new expense button"
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add expense"
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (usePagination) {
                // Use pagination when no filters are active
                PaginatedExpenseList(
                    pagedExpenses = pagedExpenses,
                    filterCriteria = filterCriteria,
                    onNavigateToAddExpense = onNavigateToAddExpense,
                    onNavigateToEditExpense = onNavigateToEditExpense,
                    onClearFilters = { viewModel.clearFilters() },
                    onDeleteExpense = { expense ->
                        haptic.performStrongTap()
                        viewModel.deleteExpense(expense)
                        viewModel.refreshExpenses()
                        pagedExpenses.refresh()
                    },
                    currency = selectedCurrency,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Use regular list when filters are active
                when (val state = uiState) {
                    is ExpenseListUiState.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .semantics {
                                    contentDescription = "Loading expenses"
                                }
                        )
                    }
                    
                    is ExpenseListUiState.Empty -> {
                        EmptyState(
                            message = "No expenses match your filters.\nTry adjusting your criteria.",
                            actionText = "Clear Filters",
                            onAction = { viewModel.clearFilters() },
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    
                    is ExpenseListUiState.Success -> {
                        Column(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // Active filters chips
                            if (hasActiveFilters(state)) {
                                FilterChipsRow(
                                    activeFilters = state.activeFilters,
                                    onClearFilters = { viewModel.clearFilters() },
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                        .semantics(mergeDescendants = true) {
                                            contentDescription = "Active filters section"
                                        }
                                )
                            }
                            
                            // Expense list
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .semantics {
                                        contentDescription = "Expense list"
                                    },
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(
                                    items = state.expenses,
                                    key = { it.id }
                                ) { expense ->
                                    SwipeableExpenseItem(
                                        expense = expense,
                                        onClick = { onNavigateToEditExpense(expense.id) },
                                        onDelete = {
                                            haptic.performStrongTap()
                                            viewModel.deleteExpense(expense)
                                            viewModel.refreshExpenses()
                                            pagedExpenses.refresh()
                                        },
                                        currency = selectedCurrency
                                    )
                                }
                            }
                        }
                    }
                    
                    is ExpenseListUiState.Error -> {
                        ErrorMessage(
                            message = state.message,
                            onRetry = { viewModel.loadExpenses() },
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
        }
    }
    
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterChipsRow(
    activeFilters: com.expensetracker.app.domain.model.FilterCriteria,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Filters:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        if (activeFilters.startDate != null || activeFilters.endDate != null) {
            FilterChip(
                selected = true,
                onClick = { },
                label = { Text("Date Range") },
                modifier = Modifier.semantics {
                    contentDescription = "Date range filter active"
                }
            )
        }
        
        if (activeFilters.categoryIds.isNotEmpty()) {
            FilterChip(
                selected = true,
                onClick = { },
                label = { Text("${activeFilters.categoryIds.size} Categories") },
                modifier = Modifier.semantics {
                    contentDescription = "${activeFilters.categoryIds.size} categories filter active"
                }
            )
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        TextButton(
            onClick = onClearFilters,
            modifier = Modifier
                .heightIn(min = 48.dp)
                .semantics {
                    contentDescription = "Clear all filters button"
                }
        ) {
            Text("Clear All")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableExpenseItem(
    expense: ExpenseWithCategory,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    currency: com.expensetracker.app.domain.model.Currency,
    modifier: Modifier = Modifier
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    val dismissState = rememberDismissState(
        confirmValueChange = {
            if (it == DismissValue.DismissedToStart) {
                showDeleteDialog = true
                false // Don't dismiss yet, wait for dialog
            } else {
                false
            }
        },
        positionalThreshold = { distance -> distance * 0.5f }
    )
    
    // Reset dismiss state when dialog is dismissed
    LaunchedEffect(showDeleteDialog) {
        if (!showDeleteDialog && dismissState.currentValue != DismissValue.Default) {
            dismissState.reset()
        }
    }
    
    SwipeToDismiss(
        state = dismissState,
        modifier = modifier.semantics {
            contentDescription = "Swipe left to delete expense"
        },
        directions = setOf(DismissDirection.EndToStart),
        background = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .semantics {
                        contentDescription = "Delete action"
                    },
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete icon",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissContent = {
            ExpenseListItem(
                expense = expense,
                onClick = onClick,
                currency = currency
            )
        }
    )
    
    // Delete confirmation dialog (single dialog, no double confirmation)
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
            },
            title = { Text("Delete Expense") },
            text = { Text("Are you sure you want to delete this expense?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete() // This directly deletes, no second dialog
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun hasActiveFilters(state: ExpenseListUiState.Success): Boolean {
    return state.activeFilters.startDate != null ||
            state.activeFilters.endDate != null ||
            state.activeFilters.categoryIds.isNotEmpty()
}

@Composable
private fun PaginatedExpenseList(
    pagedExpenses: LazyPagingItems<ExpenseWithCategory>,
    filterCriteria: com.expensetracker.app.domain.model.FilterCriteria,
    onNavigateToAddExpense: () -> Unit,
    onNavigateToEditExpense: (Long) -> Unit,
    onClearFilters: () -> Unit,
    onDeleteExpense: (ExpenseWithCategory) -> Unit,
    currency: com.expensetracker.app.domain.model.Currency,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Active filters chips (if any)
        val hasFilters = filterCriteria.startDate != null || 
                        filterCriteria.endDate != null || 
                        filterCriteria.categoryIds.isNotEmpty()
        
        if (hasFilters) {
            FilterChipsRow(
                activeFilters = filterCriteria,
                onClearFilters = onClearFilters,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .semantics(mergeDescendants = true) {
                        contentDescription = "Active filters section"
                    }
            )
        }
        
        // Handle different load states
        when {
            pagedExpenses.loadState.refresh is LoadState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.semantics {
                            contentDescription = "Loading expenses"
                        }
                    )
                }
            }
            
            pagedExpenses.loadState.refresh is LoadState.Error -> {
                val error = (pagedExpenses.loadState.refresh as LoadState.Error).error
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    ErrorMessage(
                        message = error.message ?: "Failed to load expenses",
                        onRetry = { pagedExpenses.retry() }
                    )
                }
            }
            
            pagedExpenses.itemCount == 0 -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        message = "No expenses yet.\nStart tracking your spending!",
                        actionText = "Add First Expense",
                        onAction = onNavigateToAddExpense
                    )
                }
            }
            
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .semantics {
                            contentDescription = "Expense list"
                        },
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        count = pagedExpenses.itemCount,
                        key = pagedExpenses.itemKey { it.id }
                    ) { index ->
                        val expense = pagedExpenses[index]
                        if (expense != null) {
                            SwipeableExpenseItem(
                                expense = expense,
                                onClick = { onNavigateToEditExpense(expense.id) },
                                onDelete = { onDeleteExpense(expense) },
                                currency = currency
                            )
                        }
                    }
                    
                    // Loading indicator at the bottom when loading more items
                    if (pagedExpenses.loadState.append is LoadState.Loading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.semantics {
                                        contentDescription = "Loading more expenses"
                                    }
                                )
                            }
                        }
                    }
                    
                    // Error indicator at the bottom when loading more items fails
                    if (pagedExpenses.loadState.append is LoadState.Error) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                TextButton(
                                    onClick = { pagedExpenses.retry() },
                                    modifier = Modifier.semantics {
                                        contentDescription = "Retry loading more expenses"
                                    }
                                ) {
                                    Text("Failed to load more. Tap to retry.")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
