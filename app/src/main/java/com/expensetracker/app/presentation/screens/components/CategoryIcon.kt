package com.expensetracker.app.presentation.screens.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expensetracker.app.data.local.entity.Category

@Composable
fun CategoryIcon(
    category: Category,
    size: Dp = 48.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(parseColor(category.colorHex))
            .semantics {
                contentDescription = "${category.name} category icon"
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = getIconEmoji(category.iconName),
            fontSize = (size.value * 0.5).sp,
            color = Color.White
        )
    }
}

private fun parseColor(colorHex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(colorHex))
    } catch (e: Exception) {
        Color.Gray
    }
}

private fun getIconEmoji(iconName: String): String {
    return when (iconName.lowercase()) {
        "food" -> "🍔"
        "transport" -> "🚗"
        "entertainment" -> "🎬"
        "shopping" -> "🛍️"
        "bills" -> "📄"
        "healthcare" -> "⚕️"
        "other" -> "📦"
        else -> "💰"
    }
}
