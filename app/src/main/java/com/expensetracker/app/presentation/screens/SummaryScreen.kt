package com.expensetracker.app.presentation.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expensetracker.app.domain.model.CategorySpending
import com.expensetracker.app.domain.model.SpendingSummary
import com.expensetracker.app.presentation.screens.components.CategoryIcon
import com.expensetracker.app.presentation.screens.components.EmptyState
import com.expensetracker.app.presentation.screens.components.ErrorMessage
import com.expensetracker.app.presentation.viewmodel.SummaryUiState
import com.expensetracker.app.presentation.viewmodel.SummaryViewModel
import com.expensetracker.app.presentation.viewmodel.TimePeriod
import java.text.NumberFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(
    onNavigateBack: () -> Unit,
    viewModel: SummaryViewModel = hiltViewModel()
) {
    // Use collectAsStateWithLifecycle for lifecycle-aware collection
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedPeriod by remember { mutableStateOf(TimePeriod.CURRENT_MONTH) }
    
    LaunchedEffect(selectedPeriod) {
        viewModel.loadSummary(selectedPeriod)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Spending Summary",
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is SummaryUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .semantics {
                                contentDescription = "Loading spending summary"
                            }
                    )
                }
                
                is SummaryUiState.Success -> {
                    if (state.summary.categoryBreakdown.isEmpty()) {
                        EmptyState(
                            message = "No expenses for this period",
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        SummaryContent(
                            summary = state.summary,
                            selectedPeriod = selectedPeriod,
                            onPeriodChange = { selectedPeriod = it }
                        )
                    }
                }
                
                is SummaryUiState.Error -> {
                    ErrorMessage(
                        message = state.message,
                        onRetry = { viewModel.loadSummary(selectedPeriod) },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryContent(
    summary: SpendingSummary,
    selectedPeriod: TimePeriod,
    onPeriodChange: (TimePeriod) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Period selector
        PeriodSelector(
            selectedPeriod = selectedPeriod,
            onPeriodChange = onPeriodChange
        )
        
        // Total spending card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {
                    contentDescription = "Total spending: ${formatAmount(summary.totalAmount)} for ${summary.period}"
                },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Total Spending",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.semantics { heading() }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = formatAmount(summary.totalAmount),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = summary.period,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        
        // Pie chart
        Text(
            text = "Spending Distribution",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { heading() }
        )
        
        SimplePieChart(
            categoryBreakdown = summary.categoryBreakdown,
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .semantics {
                    contentDescription = "Pie chart showing spending distribution across ${summary.categoryBreakdown.size} categories"
                }
        )
        
        // Category breakdown
        Text(
            text = "Category Breakdown",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { heading() }
        )
        
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            summary.categoryBreakdown.forEach { categorySpending ->
                CategorySpendingItem(categorySpending = categorySpending)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PeriodSelector(
    selectedPeriod: TimePeriod,
    onPeriodChange: (TimePeriod) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Time Period",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() }
        )
        
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedPeriod == TimePeriod.CURRENT_MONTH,
                    onClick = { onPeriodChange(TimePeriod.CURRENT_MONTH) },
                    label = { Text(TimePeriod.CURRENT_MONTH.displayName) },
                    modifier = Modifier.semantics {
                        contentDescription = "${TimePeriod.CURRENT_MONTH.displayName} time period. ${if (selectedPeriod == TimePeriod.CURRENT_MONTH) "Selected" else "Not selected"}"
                    }
                )
                FilterChip(
                    selected = selectedPeriod == TimePeriod.LAST_MONTH,
                    onClick = { onPeriodChange(TimePeriod.LAST_MONTH) },
                    label = { Text(TimePeriod.LAST_MONTH.displayName) },
                    modifier = Modifier.semantics {
                        contentDescription = "${TimePeriod.LAST_MONTH.displayName} time period. ${if (selectedPeriod == TimePeriod.LAST_MONTH) "Selected" else "Not selected"}"
                    }
                )
                FilterChip(
                    selected = selectedPeriod == TimePeriod.LAST_3_MONTHS,
                    onClick = { onPeriodChange(TimePeriod.LAST_3_MONTHS) },
                    label = { Text(TimePeriod.LAST_3_MONTHS.displayName) },
                    modifier = Modifier.semantics {
                        contentDescription = "${TimePeriod.LAST_3_MONTHS.displayName} time period. ${if (selectedPeriod == TimePeriod.LAST_3_MONTHS) "Selected" else "Not selected"}"
                    }
                )
            }
        }
    }
}

@Composable
private fun SimplePieChart(
    categoryBreakdown: List<CategorySpending>,
    modifier: Modifier = Modifier
) {
    val colors = remember(categoryBreakdown) {
        categoryBreakdown.map { parseColor(it.category.colorHex) }
    }
    
    Canvas(modifier = modifier) {
        val canvasSize = size.minDimension
        val radius = canvasSize / 2.5f
        val centerX = size.width / 2
        val centerY = size.height / 2
        
        var startAngle = -90f
        
        categoryBreakdown.forEachIndexed { index, spending ->
            val sweepAngle = (spending.percentage.toFloat() / 100f) * 360f
            
            // Draw pie slice
            drawArc(
                color = colors[index],
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = true,
                topLeft = Offset(centerX - radius, centerY - radius),
                size = Size(radius * 2, radius * 2)
            )
            
            // Draw border
            drawArc(
                color = Color.White,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = true,
                topLeft = Offset(centerX - radius, centerY - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = 2f)
            )
            
            startAngle += sweepAngle
        }
    }
}

@Composable
private fun CategorySpendingItem(
    categorySpending: CategorySpending,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = "${categorySpending.category.name}: ${formatAmount(categorySpending.amount)}, ${String.format("%.1f", categorySpending.percentage)} percent of total"
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CategoryIcon(
                category = categorySpending.category,
                size = 48.dp
            )
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = categorySpending.category.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${String.format("%.1f", categorySpending.percentage)}% of total",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Text(
                text = formatAmount(categorySpending.amount),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

private fun parseColor(colorHex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(colorHex))
    } catch (e: Exception) {
        Color.Gray
    }
}

private fun formatAmount(amount: Double): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale.getDefault())
    return formatter.format(amount)
}
