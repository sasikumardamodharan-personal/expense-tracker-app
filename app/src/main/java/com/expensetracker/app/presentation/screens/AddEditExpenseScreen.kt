package com.expensetracker.app.presentation.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expensetracker.app.data.local.entity.Category
import com.expensetracker.app.presentation.screens.components.CategoryIcon
import com.expensetracker.app.presentation.screens.components.DatePickerDialog
import com.expensetracker.app.presentation.viewmodel.AddEditExpenseViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditExpenseScreen(
    onNavigateBack: () -> Unit,
    onExpenseSaved: () -> Unit,
    viewModel: AddEditExpenseViewModel = hiltViewModel()
) {
    // Use collectAsStateWithLifecycle for lifecycle-aware collection
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDatePicker by remember { mutableStateOf(false) }
    var showCategoryDropdown by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val haptic = com.expensetracker.app.util.rememberHapticFeedback()
    
    LaunchedEffect(Unit) {
        viewModel.saveSuccess.collect { success ->
            if (success) {
                haptic.performSuccess()
                onExpenseSaved()
            }
        }
    }
    
    LaunchedEffect(Unit) {
        viewModel.errorMessage.collect { message ->
            haptic.performError()
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        if (uiState.isEditMode) "Edit Expense" else "Add Expense",
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // General error message
            uiState.validationErrors["general"]?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            // Amount field
            OutlinedTextField(
                value = uiState.amount,
                onValueChange = { viewModel.updateAmount(it) },
                label = { Text("Amount") },
                placeholder = { Text("0.00") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = uiState.validationErrors.containsKey("amount"),
                supportingText = {
                    if (uiState.validationErrors.containsKey("amount")) {
                        Text(
                            text = uiState.validationErrors["amount"] ?: "",
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Text(
                            text = "Enter amount with up to 2 decimal places",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "Amount input field. ${if (uiState.validationErrors.containsKey("amount")) "Error: ${uiState.validationErrors["amount"]}" else "Enter amount with up to 2 decimal places"}"
                    },
                singleLine = true
            )
            
            // Category selector
            CategorySelector(
                selectedCategory = uiState.selectedCategory,
                categories = uiState.categories,
                onCategorySelected = { viewModel.updateCategory(it) },
                isError = uiState.validationErrors.containsKey("category"),
                errorMessage = uiState.validationErrors["category"],
                expanded = showCategoryDropdown,
                onExpandedChange = { showCategoryDropdown = it }
            )
            
            // Date picker
            DateSelector(
                selectedDate = uiState.date,
                onDateClick = { showDatePicker = true },
                isError = uiState.validationErrors.containsKey("date"),
                errorMessage = uiState.validationErrors["date"]
            )
            
            // Description field
            OutlinedTextField(
                value = uiState.description,
                onValueChange = { viewModel.updateDescription(it) },
                label = { Text("Description (Optional)") },
                isError = uiState.validationErrors.containsKey("description"),
                supportingText = {
                    if (uiState.validationErrors.containsKey("description")) {
                        Text(
                            text = uiState.validationErrors["description"] ?: "",
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Text(
                            text = "${uiState.description.length}/200 characters",
                            color = if (uiState.description.length > 180) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "Description input field. Optional. ${uiState.description.length} of 200 characters used."
                    },
                minLines = 3,
                maxLines = 5
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .semantics {
                            contentDescription = "Cancel button"
                        },
                    enabled = !uiState.isSaving
                ) {
                    Text("Cancel")
                }
                
                Button(
                    onClick = { 
                        haptic.performMediumTap()
                        viewModel.saveExpense()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .semantics {
                            contentDescription = if (uiState.isSaving) {
                                "Saving expense"
                            } else if (!uiState.validationErrors.isEmpty()) {
                                "Save button disabled. Please fix validation errors."
                            } else {
                                "Save expense button"
                            }
                        },
                    enabled = !uiState.isSaving && uiState.validationErrors.isEmpty()
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Save")
                    }
                }
            }
        }
    }
    
    // Date picker dialog
    if (showDatePicker) {
        DatePickerDialog(
            selectedDate = uiState.date,
            onDateSelected = { viewModel.updateDate(it) },
            onDismiss = { showDatePicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategorySelector(
    selectedCategory: Category?,
    categories: List<Category>,
    onCategorySelected: (Category) -> Unit,
    isError: Boolean,
    errorMessage: String?,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = onExpandedChange
        ) {
            OutlinedTextField(
                value = selectedCategory?.name ?: "",
                onValueChange = { },
                readOnly = true,
                label = { Text("Category") },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Select category"
                    )
                },
                leadingIcon = {
                    selectedCategory?.let {
                        CategoryIcon(
                            category = it,
                            size = 32.dp
                        )
                    }
                },
                isError = isError,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
                    .semantics {
                        contentDescription = "Category selector. ${selectedCategory?.name ?: "No category selected"}. ${if (isError && errorMessage != null) "Error: $errorMessage" else ""}"
                    },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
            )
            
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { onExpandedChange(false) },
                modifier = Modifier.semantics {
                    contentDescription = "Category selection menu"
                }
            ) {
                categories.forEach { category ->
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CategoryIcon(
                                    category = category,
                                    size = 32.dp
                                )
                                Text(category.name)
                            }
                        },
                        onClick = {
                            onCategorySelected(category)
                            onExpandedChange(false)
                        },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .semantics {
                                contentDescription = "Select ${category.name} category"
                            }
                    )
                }
            }
        }
        
        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}

@Composable
private fun DateSelector(
    selectedDate: Long,
    onDateClick: () -> Unit,
    isError: Boolean,
    errorMessage: String?,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = formatDate(selectedDate),
            onValueChange = { },
            readOnly = true,
            label = { Text("Date") },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "Select date"
                )
            },
            isError = isError,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onDateClick)
                .semantics {
                    contentDescription = "Date selector. Selected date: ${formatDate(selectedDate)}. Double tap to change. ${if (isError && errorMessage != null) "Error: $errorMessage" else ""}"
                }
        )
        
        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
