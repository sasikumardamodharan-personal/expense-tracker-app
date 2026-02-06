@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)

package com.expensetracker.app.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expensetracker.app.presentation.components.MigrationDialog
import com.expensetracker.app.presentation.viewmodel.HouseholdSetupUiState
import com.expensetracker.app.presentation.viewmodel.HouseholdSetupViewModel

@OptIn(ExperimentalMaterial3Api::class)

/**
 * Screen for household setup - create or join a household
 */
@Composable
fun HouseholdSetupScreen(
    onSetupComplete: () -> Unit,
    viewModel: HouseholdSetupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    var showCreateDialog by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }
    var showMigrationDialog by remember { mutableStateOf(false) }
    
    // Handle success state - show migration dialog
    LaunchedEffect(uiState) {
        if (uiState is HouseholdSetupUiState.Success) {
            showMigrationDialog = true
        }
    }
    
    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Header
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Text(
                    text = "Setup Your Household",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                
                Text(
                    text = "Create a household to share expenses with family members, or join an existing one.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Create Household Button
                Button(
                    onClick = { showCreateDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = uiState !is HouseholdSetupUiState.Loading
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create New Household")
                }
                
                // Join Household Button
                OutlinedButton(
                    onClick = { showJoinDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = uiState !is HouseholdSetupUiState.Loading
                ) {
                    Text("Join Existing Household")
                }
                
                // Loading indicator
                if (uiState is HouseholdSetupUiState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
                
                // Error message
                if (uiState is HouseholdSetupUiState.Error) {
                    Text(
                        text = (uiState as HouseholdSetupUiState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
    
    // Create Household Dialog
    if (showCreateDialog) {
        CreateHouseholdDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { name ->
                viewModel.createHousehold(name)
                showCreateDialog = false
            }
        )
    }
    
    // Join Household Dialog
    if (showJoinDialog) {
        JoinHouseholdDialog(
            onDismiss = { showJoinDialog = false },
            onConfirm = { inviteCode ->
                viewModel.joinHousehold(inviteCode)
                showJoinDialog = false
            }
        )
    }
    
    // Migration Dialog - shown after successful household setup
    if (showMigrationDialog) {
        MigrationDialog(
            onDismiss = { 
                showMigrationDialog = false
                onSetupComplete()
            },
            onComplete = {
                showMigrationDialog = false
                onSetupComplete()
            }
        )
    }
}


/**
 * Dialog for creating a new household
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateHouseholdDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var householdName by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Create Household")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Enter a name for your household",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                OutlinedTextField(
                    value = householdName,
                    onValueChange = { householdName = it },
                    label = { Text("Household Name") },
                    placeholder = { Text("e.g., Smith Family") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            keyboardController?.hide()
                            if (householdName.isNotBlank()) {
                                onConfirm(householdName.trim())
                            }
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (householdName.isNotBlank()) {
                        onConfirm(householdName.trim())
                    }
                },
                enabled = householdName.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Dialog for joining an existing household
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JoinHouseholdDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var inviteCode by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Join Household")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Enter the invite code shared by a household member",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                OutlinedTextField(
                    value = inviteCode,
                    onValueChange = { inviteCode = it.uppercase() },
                    label = { Text("Invite Code") },
                    placeholder = { Text("e.g., ABC123") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            keyboardController?.hide()
                            if (inviteCode.isNotBlank()) {
                                onConfirm(inviteCode.trim())
                            }
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (inviteCode.isNotBlank()) {
                        onConfirm(inviteCode.trim())
                    }
                },
                enabled = inviteCode.isNotBlank()
            ) {
                Text("Join")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
