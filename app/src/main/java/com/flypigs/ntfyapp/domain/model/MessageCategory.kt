package com.flypigs.ntfyapp.domain.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class MessageCategory(
    val displayName: String,
    val icon: ImageVector,
    val color: Color
) {
    NODE_CHANGE("节点变更", Icons.Default.SwapHoriz, Color(0xFF1976D2)),
    SYSTEM_ALERT("系统告警", Icons.Default.Warning, Color(0xFFD32F2F)),
    RECOVERY("恢复通知", Icons.Default.CheckCircle, Color(0xFF388E3C)),
    UPDATE("更新通知", Icons.Default.Inventory2, Color(0xFFF57C00)),
    OTHER("其他", Icons.Default.ChatBubbleOutline, Color(0xFF757575));
}
