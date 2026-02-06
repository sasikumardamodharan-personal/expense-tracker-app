package com.expensetracker.app.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.expensetracker.app.domain.model.SyncStatus

/**
 * Displays a sync status indicator with icon and optional text
 */
@Composable
fun SyncStatusIndicator(
    syncStatus: SyncStatus,
    modifier: Modifier = Modifier,
    showText: Boolean = false
) {
    Row(
        modifier = modifier.semantics {
            contentDescription = when (syncStatus) {
                is SyncStatus.Synced -> "Synced"
                is SyncStatus.Syncing -> "Syncing"
                is SyncStatus.Error -> "Sync error: ${syncStatus.message}"
                is SyncStatus.Offline -> "Offline"
                is SyncStatus.Pending -> "Pending sync"
            }
        },
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SyncStatusIcon(syncStatus = syncStatus)
        
        if (showText) {
            Text(
                text = when (syncStatus) {
                    is SyncStatus.Synced -> "Synced"
                    is SyncStatus.Syncing -> "Syncing..."
                    is SyncStatus.Error -> "Error"
                    is SyncStatus.Offline -> "Offline"
                    is SyncStatus.Pending -> "Pending"
                },
                style = MaterialTheme.typography.labelSmall,
                color = getSyncStatusColor(syncStatus)
            )
        }
    }
}

/**
 * Displays just the sync status icon
 */
@Composable
fun SyncStatusIcon(
    syncStatus: SyncStatus,
    modifier: Modifier = Modifier
) {
    when (syncStatus) {
        is SyncStatus.Synced -> {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF4CAF50), // Green
                modifier = modifier.size(16.dp)
            )
        }
        
        is SyncStatus.Syncing -> {
            // Rotating sync icon
            val infiniteTransition = rememberInfiniteTransition(label = "sync_rotation")
            val rotation by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "rotation"
            )
            
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = modifier
                    .size(16.dp)
                    .rotate(rotation)
            )
        }
        
        is SyncStatus.Error -> {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = modifier.size(16.dp)
            )
        }
        
        is SyncStatus.Offline -> {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = Color(0xFFFF9800), // Orange
                modifier = modifier.size(16.dp)
            )
        }
        
        is SyncStatus.Pending -> {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = Color(0xFF9E9E9E), // Gray
                modifier = modifier.size(16.dp)
            )
        }
    }
}

/**
 * Get the color for a sync status
 */
@Composable
private fun getSyncStatusColor(syncStatus: SyncStatus): Color {
    return when (syncStatus) {
        is SyncStatus.Synced -> Color(0xFF4CAF50) // Green
        is SyncStatus.Syncing -> MaterialTheme.colorScheme.primary
        is SyncStatus.Error -> MaterialTheme.colorScheme.error
        is SyncStatus.Offline -> Color(0xFFFF9800) // Orange
        is SyncStatus.Pending -> Color(0xFF9E9E9E) // Gray
    }
}

/**
 * Displays a banner for offline status
 */
@Composable
fun OfflineBanner(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color(0xFFFF9800).copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .semantics {
                    contentDescription = "Offline mode active. Changes will sync when online."
                },
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = Color(0xFFFF9800),
                modifier = Modifier.size(20.dp)
            )
            
            Text(
                text = "Offline - Changes will sync when online",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFFF9800)
            )
        }
    }
}
