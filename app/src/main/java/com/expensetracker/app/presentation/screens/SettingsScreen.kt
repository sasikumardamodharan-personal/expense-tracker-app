package com.expensetracker.app.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expensetracker.app.domain.model.Currency
import com.expensetracker.app.presentation.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCategoryManagement: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val selectedCurrency by viewModel.selectedCurrency.collectAsStateWithLifecycle()
    val exportState by viewModel.exportState.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // Handle export state
    LaunchedEffect(exportState) {
        when (val state = exportState) {
            is com.expensetracker.app.presentation.viewmodel.ExportState.Success -> {
                // Create and share CSV file
                shareCSV(context, state.csvContent)
                viewModel.resetExportState()
            }
            is com.expensetracker.app.presentation.viewmodel.ExportState.Error -> {
                android.widget.Toast.makeText(context, state.message, android.widget.Toast.LENGTH_LONG).show()
                viewModel.resetExportState()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Currency Section
            item {
                Text(
                    text = "Currency",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            items(Currency.values().toList()) { currency ->
                CurrencyItem(
                    currency = currency,
                    isSelected = currency == selectedCurrency,
                    onClick = { viewModel.setCurrency(currency) }
                )
            }

            // Categories Section
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Categories",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onNavigateToCategoryManagement)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Manage Categories",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Add, edit, or delete expense categories",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            // Data Management Section
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Data Management",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            enabled = exportState !is com.expensetracker.app.presentation.viewmodel.ExportState.Loading,
                            onClick = { viewModel.exportExpenses() }
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Export Data",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Export all expenses as CSV file",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        if (exportState is com.expensetracker.app.presentation.viewmodel.ExportState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun shareCSV(context: android.content.Context, csvContent: String) {
    try {
        // Create a temporary file
        val filename = com.expensetracker.app.util.CsvExporter.generateFilename()
        val file = java.io.File(context.cacheDir, filename)
        file.writeText(csvContent)
        
        // Create content URI using FileProvider
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        
        // Create share intent
        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            putExtra(android.content.Intent.EXTRA_SUBJECT, "Expense Tracker Data")
            putExtra(android.content.Intent.EXTRA_TEXT, "Exported expense data from Expense Tracker")
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        // Show share sheet
        context.startActivity(
            android.content.Intent.createChooser(shareIntent, "Export Expenses")
        )
    } catch (e: Exception) {
        android.widget.Toast.makeText(
            context,
            "Failed to share file: ${e.message}",
            android.widget.Toast.LENGTH_LONG
        ).show()
    }
}

@Composable
private fun CurrencyItem(
    currency: Currency,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = if (isSelected) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${currency.symbol} ${currency.displayName}",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = currency.code,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
