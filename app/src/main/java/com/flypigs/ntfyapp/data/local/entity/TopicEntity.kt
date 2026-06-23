package com.flypigs.ntfyapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "topics")
data class TopicEntity(
    @PrimaryKey
    val id: String,
    val serverId: String,
    val name: String,
    val displayName: String,
    val isEnabled: Boolean = true
)
