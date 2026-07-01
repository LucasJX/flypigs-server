package com.flypigs.ntfyapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.flypigs.ntfyapp.data.local.dao.MessageDao
import com.flypigs.ntfyapp.data.local.dao.ServerDao
import com.flypigs.ntfyapp.data.local.dao.TopicDao
import com.flypigs.ntfyapp.data.local.entity.MessageEntity
import com.flypigs.ntfyapp.data.local.entity.ServerEntity
import com.flypigs.ntfyapp.data.local.entity.TopicEntity

@Database(
    entities = [MessageEntity::class, ServerEntity::class, TopicEntity::class],
    version = 6,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun serverDao(): ServerDao
    abstract fun topicDao(): TopicDao

    companion object {
        // v5 → v6: MessageEntity 加 attachment 字段
        // 用户场景消息本身不要求持久化（数据来自 ntfy 服务器），直接清空重建即可
        // 生产环境可写 MIGRATION_5_6 保留旧数据
    }
}
