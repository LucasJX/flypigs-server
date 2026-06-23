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
    val category: String      // NODE_CHANGE, SYSTEM_ALERT, RECOVERY, UPDATE, OTHER
)
