package com.expensetracker.app.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expensetracker.app.domain.model.MigrationProgress
import com.expensetracker.app.presentation.viewmodel.DataMigrationUiState
import com.expensetracker.app.presentation.viewmodel.DataMigrationViewModel

/**
 * Dialog for data migration to Firestore
 * 
 * Shows migration progress, handles errors, and provides user feedback
 */
@Composable
fun MigrationDialog(
    onDismiss: () -> Unit,
    onComplete: () -> Unit,
    viewModel: DataMigrationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val migrationProgress by viewModel.migrationProgress.collectAsStateWithLifecycle()
    
    // Auto-start migration when dialog opens
    LaunchedEffect(Unit) {
        if (uiState is DataMigrationUiState.Initial) {
            viewModel.startMigration()
        }
    }
    
    // Handle completion
    LaunchedEffect(uiState) {
        if (uiState is DataMigrationUiState.Success || 
            uiState is DataMigrationUiState.Skipped ||
            uiState is DataMigrationUiState.AlreadyMigrated) {
            onComplete()
        }
    }
    
    AlertDialog(
        onDismissRequest = { 
            // Prevent dismissal during migration
            if (uiState !is DataMigrationUiState.Migrating) {
                onDismiss()
            }
        },
        title = {
            Text(
                text = when (uiState) {
                    is DataMigrationUiState.Success -> "Migration Complete"
                    is DataMigrationUiState.Error -> "Migration Failed"
                    is DataMigrationUiState.AlreadyMigrated -> "Already Migrated"
                    else -> "Migrating Your Data"
                },
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (uiState) {
                    is DataMigrationUiState.Initial,
                    is DataMigrationUiState.Migrating -> {
                        MigrationInProgressContent(migrationProgress)
                    }
                    is DataMigrationUiState.Success -> {
                        MigrationSuccessContent(migrationProgress)
                    }
                    is DataMigrationUiState.Error -> {
                        MigrationErrorContent((uiState as DataMigrationUiState.Error).message)
                    }
                    is DataMigrationUiState.AlreadyMigrated -> {
                        MigrationAlreadyCompleteContent()
                    }
                    is DataMigrationUiState.Skipped -> {
                        // This state triggers onComplete, so we won't see this
                    }
                }
            }
        },
        confirmButton = {
            when (uiState) {
                is DataMigrationUiState.Success,
                is DataMigrationUiState.AlreadyMigrated -> {
                    TextButton(onClick = onComplete) {
                        Text("Continue")
                    }
                }
                is DataMigrationUiState.Error -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { viewModel.skipMigration() }) {
                            Text("Skip")
                        }
                        Button(onClick = { viewModel.retryMigration() }) {
                            Text("Retry")
                        }
                    }
                }
                else -> {
                    // No button during migration
                }
            }
        },
        dismissButton = {
            if (uiState is DataMigrationUiState.Error) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

@Composable
private fun MigrationInProgressContent(progress: MigrationProgress) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Text(
            text = "Uploading your expenses to the cloud...",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        
        when (progress) {
            is MigrationProgress.InProgress -> {
                LinearProgressIndicator(
                    progress = progress.percentage / 100f,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Text(
                    text = "${progress.uploaded} of ${progress.total} expenses uploaded (${progress.percentage}%)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            else -> {
                CircularProgressIndicator()
                
                Text(
                    text = "Preparing migration...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Text(
            text = "Please don't close the app",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun MigrationSuccessContent(progress: MigrationProgress) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        val count = when (progress) {
            is MigrationProgress.Completed -> progress.totalMigrated
            is MigrationProgress.InProgress -> progress.total
            else -> 0
        }
        
        Text(
            text = "Successfully migrated $count expense${if (count != 1) "s" else ""} to the cloud!",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        
        Text(
            text = "Your expenses are now synced across all your devices.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun MigrationErrorContent(errorMessage: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        
        Text(
            text = "Migration failed",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.error
        )
        
        Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Text(
            text = "You can retry now or skip and migrate later from Settings.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun MigrationAlreadyCompleteContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Text(
            text = "Your data has already been migrated to the cloud.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
    }
}
