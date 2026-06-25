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
    version = 5,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun serverDao(): ServerDao
    abstract fun topicDao(): TopicDao
}
