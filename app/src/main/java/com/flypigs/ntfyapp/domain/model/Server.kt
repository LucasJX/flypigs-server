package com.flypigs.ntfyapp.domain.model

data class Server(
    val id: String,
    val url: String,
    val name: String,
    val username: String? = null,
    val hasPassword: Boolean = false,   // 标记是否有密码，实际密码存 SecureStorage
    val token: String? = null,
    val isConnected: Boolean = false
)
