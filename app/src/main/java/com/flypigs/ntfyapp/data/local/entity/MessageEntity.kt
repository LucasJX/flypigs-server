package com.flypigs.ntfyapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey
    val id: String,
    val topic: String,
    val title: String?,
    val body: String,
    val priority: Int,        // 1-5
    val tags: String?,        // 逗号分隔
    val timestamp: Long,
    val isRead: Boolean = false,
    val isStarred: Boolean = false,
    val category: String,     // NODE_CHANGE, SYSTEM_ALERT, RECOVERY, UPDATE, OTHER
    // ── 附件 (v6 新增) ─────────────────────────────
    val attachmentName: String? = null,
    val attachmentType: String? = null,   // MIME, e.g. "image/jpeg"
    val attachmentSize: Long? = null,
    val attachmentUrl: String? = null,    // ntfy 服务器返回的 file URL
    val attachmentExpires: Long? = null   // 过期时间（Unix 秒）
)
