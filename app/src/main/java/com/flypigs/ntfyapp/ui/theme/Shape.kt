package com.flypigs.ntfyapp.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// ──────────────────────────────────────────────
// M3 Expressive Corner Radius Specs
// Card: 28dp | Chip: 16dp | Button: 20dp
// BottomBar: 24dp | NavDrawerItem: 12dp
// ──────────────────────────────────────────────

val Shapes = Shapes(
    // Small: chips, small buttons, tags
    small = RoundedCornerShape(16.dp),
    // Medium: cards, text fields, dialogs
    medium = RoundedCornerShape(28.dp),
    // Large: bottom sheets, full-screen dialogs
    large = RoundedCornerShape(28.dp)
)

// ── 命名圆角常量，方便业务层引用 ──────────────
object AppRadius {
    val Card = 28.dp
    val Chip = 16.dp
    val Button = 20.dp
    val BottomBar = 24.dp
    val NavigationDrawerItem = 12.dp
    val Small = 12.dp
    val Medium = 16.dp
    val Large = 28.dp
}
