package com.expensetracker.app.presentation.screens.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.expensetracker.app.domain.model.ExpenseWithCategory
import com.expensetracker.app.util.rememberHapticFeedback
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ExpenseListItem(
    expense: ExpenseWithCategory,
    onClick: () -> Unit,
    currency: com.expensetracker.app.domain.model.Currency,
    modifier: Modifier = Modifier
) {
    val haptic = rememberHapticFeedback()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    // Animate scale on press
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale"
    )
    // Remember formatters to avoid recreating them on every recomposition
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    
    // Use remember for expensive string building
    val formattedAmount = remember(expense.amount, currency) {
        com.expensetracker.app.util.CurrencyFormatter.format(expense.amount, currency)
    }
    
    val contentDesc = remember(expense, currency) {
        buildString {
            append("Expense: $formattedAmount ")
            append("for ${expense.category.name} ")
            append("on ${dateFormatter.format(Date(expense.date))}")
            if (expense.description.isNotEmpty()) {
                append(", ${expense.description}")
            }
            append(". Double tap to edit.")
        }
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    haptic.performLightTap()
                    onClick()
                }
            )
            .semantics {
                contentDescription = contentDesc
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category icon with color
            CategoryIcon(
                category = expense.category,
                size = 48.dp
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Expense details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = expense.category.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                if (expense.description.isNotEmpty()) {
                    Text(
                        text = expense.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = remember(expense.date) { dateFormatter.format(Date(expense.date)) },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Amount
            Text(
                text = formattedAmount,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
