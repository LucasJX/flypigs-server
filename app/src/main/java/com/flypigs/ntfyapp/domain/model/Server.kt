package com.flypigs.ntfyapp.domain.model

data class Server(
    val id: String,
    val url: String,
    val name: String,
    val username: String? = null,
    val password: String? = null,
    val token: String? = null,
    val isConnected: Boolean = false
)
