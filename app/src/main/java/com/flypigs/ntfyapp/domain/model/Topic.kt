package com.flypigs.ntfyapp.domain.model

data class Topic(
    val id: String,
    val serverId: String,
    val name: String,
    val displayName: String,
    val isEnabled: Boolean = true
)
