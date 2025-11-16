package com.expensetracker.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Material Design 3 Shape Theme
val Shapes = Shapes(
    // Extra Small - Used for small components like chips
    extraSmall = RoundedCornerShape(4.dp),
    // Small - Used for buttons, cards
    small = RoundedCornerShape(8.dp),
    // Medium - Used for dialogs, bottom sheets
    medium = RoundedCornerShape(12.dp),
    // Large - Used for large cards, navigation drawers
    large = RoundedCornerShape(16.dp),
    // Extra Large - Used for full-screen dialogs
    extraLarge = RoundedCornerShape(28.dp)
)
