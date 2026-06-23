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
    val password: String? = null,
    val token: String? = null,
    val isConnected: Boolean = false
)
