package com.expensetracker.app.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expensetracker.app.data.local.entity.Category
import com.expensetracker.app.domain.model.FilterCriteria
import com.expensetracker.app.presentation.screens.components.CategoryIcon
import com.expensetracker.app.presentation.screens.components.DatePickerDialog
import com.expensetracker.app.presentation.viewmodel.ExpenseListViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterScreen(
    onNavigateBack: () -> Unit,
    viewModel: ExpenseListViewModel = hiltViewModel()
) {
    // Use collectAsStateWithLifecycle for lifecycle-aware collection
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    // Use derivedStateOf to avoid recomposition when getting current filters
    val currentFilters by remember {
        derivedStateOf {
            when (val state = uiState) {
                is com.expensetracker.app.presentation.viewmodel.ExpenseListUiState.Success -> state.activeFilters
                else -> FilterCriteria()
            }
        }
    }
    
    var startDate by remember { mutableStateOf(currentFilters.startDate) }
    var endDate by remember { mutableStateOf(currentFilters.endDate) }
    var selectedCategoryIds by remember { mutableStateOf(currentFilters.categoryIds) }
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    
    // Load categories
    LaunchedEffect(Unit) {
        viewModel.loadCategories().collect { loadedCategories ->
            categories = loadedCategories
        }
    }
    
    val activeFilterCount = remember(startDate, endDate, selectedCategoryIds) {
        var count = 0
        if (startDate != null || endDate != null) count++
        if (selectedCategoryIds.isNotEmpty()) count++
        count
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Filter Expenses",
                        modifier = Modifier.semantics { heading() }
                    ) 
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.semantics {
                            contentDescription = "Navigate back button"
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Active filter count
            if (activeFilterCount > 0) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.semantics {
                        contentDescription = "$activeFilterCount active filter${if (activeFilterCount > 1) "s" else ""}"
                    }
                ) {
                    Text(
                        text = "$activeFilterCount active filter${if (activeFilterCount > 1) "s" else ""}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            
            // Date range section
            Text(
                text = "Date Range",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { heading() }
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Start date
                OutlinedCard(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .semantics {
                            contentDescription = "Start date selector. ${startDate?.let { "Selected: ${formatDate(it)}" } ?: "Not set"}. Double tap to change."
                        },
                    onClick = { showStartDatePicker = true }
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Start Date",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                        Text(
                            text = startDate?.let { formatDate(it) } ?: "Not set",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                
                // End date
                OutlinedCard(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .semantics {
                            contentDescription = "End date selector. ${endDate?.let { "Selected: ${formatDate(it)}" } ?: "Not set"}. Double tap to change."
                        },
                    onClick = { showEndDatePicker = true }
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "End Date",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                        Text(
                            text = endDate?.let { formatDate(it) } ?: "Not set",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
            
            // Category selection section
            Text(
                text = "Categories",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { heading() }
            )
            
            if (categories.isEmpty()) {
                Text(
                    text = "Loading categories...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { category ->
                        CategoryCheckboxItem(
                            category = category,
                            isSelected = selectedCategoryIds.contains(category.id),
                            onSelectionChange = { isSelected ->
                                selectedCategoryIds = if (isSelected) {
                                    selectedCategoryIds + category.id
                                } else {
                                    selectedCategoryIds - category.id
                                }
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        startDate = null
                        endDate = null
                        selectedCategoryIds = emptySet()
                        viewModel.clearFilters()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .semantics {
                            contentDescription = "Clear all filters button"
                        }
                ) {
                    Text("Clear All")
                }
                
                Button(
                    onClick = {
                        val filterCriteria = FilterCriteria(
                            startDate = startDate,
                            endDate = endDate,
                            categoryIds = selectedCategoryIds
                        )
                        viewModel.applyFilter(filterCriteria)
                        onNavigateBack()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .semantics {
                            contentDescription = "Apply filters button"
                        }
                ) {
                    Text("Apply")
                }
            }
        }
    }
    
    // Date picker dialogs
    if (showStartDatePicker) {
        DatePickerDialog(
            selectedDate = startDate ?: System.currentTimeMillis(),
            onDateSelected = { startDate = it },
            onDismiss = { showStartDatePicker = false }
        )
    }
    
    if (showEndDatePicker) {
        DatePickerDialog(
            selectedDate = endDate ?: System.currentTimeMillis(),
            onDateSelected = { endDate = it },
            onDismiss = { showEndDatePicker = false }
        )
    }
}

@Composable
private fun CategoryCheckboxItem(
    category: Category,
    isSelected: Boolean,
    onSelectionChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = "${category.name} category. ${if (isSelected) "Selected" else "Not selected"}. Double tap to ${if (isSelected) "deselect" else "select"}."
            },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = onSelectionChange
            )
            
            CategoryIcon(
                category = category,
                size = 40.dp
            )
            
            Text(
                text = category.name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
