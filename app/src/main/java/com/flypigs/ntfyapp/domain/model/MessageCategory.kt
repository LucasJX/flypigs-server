package com.flypigs.ntfyapp.domain.model

enum class MessageCategory(val displayName: String, val icon: String, val color: Long) {
    NODE_CHANGE("节点变更", "📊", 0xFF1976D2),
    SYSTEM_ALERT("系统告警", "⚠️", 0xFFD32F2F),
    RECOVERY("恢复通知", "✅", 0xFF388E3C),
    UPDATE("更新通知", "📦", 0xFFF57C00),
    OTHER("其他", "💬", 0xFF757575);
}
