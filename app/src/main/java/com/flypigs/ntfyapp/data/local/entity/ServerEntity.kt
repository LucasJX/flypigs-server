package com.flypigs.ntfyapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "servers")
data class ServerEntity(
    @PrimaryKey
    val id: String,
    val url: String,
    val name: String,
    val username: String? = null,
    val hasPassword: Boolean = false,   // 标记是否有密码，实际密码存 SecureStorage
    val token: String? = null,          // ntfy Access Token
    val isConnected: Boolean = false
)
