package com.flypigs.ntfyapp.domain.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class MessageCategory(
    val displayName: String,
    val icon: ImageVector,
    val fallbackColor: Color          // 非 Composable 上下文的 fallback
) {
    NODE_CHANGE("节点变更", Icons.Default.SwapHoriz, Color(0xFF1976D2)),
    SYSTEM_ALERT("系统告警", Icons.Default.Warning, Color(0xFFD32F2F)),
    RECOVERY("恢复通知", Icons.Default.CheckCircle, Color(0xFF388E3C)),
    UPDATE("更新通知", Icons.Default.Inventory2, Color(0xFFF57C00)),
    OTHER("其他", Icons.Default.ChatBubbleOutline, Color(0xFF757575));

    /**
     * Composable 上下文中使用 MaterialTheme 角色，自适应暗色/亮色主题
     * 非 Composable 上下文（如 Room DAO、Service）使用 fallbackColor
     */
    val color: Color
        @Composable get() = when (this) {
            NODE_CHANGE  -> MaterialTheme.colorScheme.primary
            SYSTEM_ALERT -> MaterialTheme.colorScheme.error
            RECOVERY     -> MaterialTheme.colorScheme.primaryContainer  // 绿色系
            UPDATE       -> MaterialTheme.colorScheme.tertiary
            OTHER        -> MaterialTheme.colorScheme.outline
        }
}
