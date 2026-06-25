package com.flypigs.ntfyapp.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "topics",
    foreignKeys = [ForeignKey(
        entity = ServerEntity::class,
        parentColumns = ["id"],
        childColumns = ["serverId"],
        onDelete = ForeignKey.CASCADE  // 删除 Server 时自动删除关联 Topic
    )]
)
data class TopicEntity(
    @PrimaryKey
    val id: String,
    val serverId: String,
    val name: String,
    val displayName: String,
    val isEnabled: Boolean = true
)
