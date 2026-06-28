package com.flypigs.ntfyapp.domain.model

data class Message(
    val id: String,
    val topic: String,
    val title: String?,
    val body: String,
    val priority: Int,
    val tags: List<String>,
    val timestamp: Long,
    val isRead: Boolean,
    val category: String
)
