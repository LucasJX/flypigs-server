package com.flypigs.ntfyapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.flypigs.ntfyapp.data.local.dao.MessageDao
import com.flypigs.ntfyapp.data.local.entity.MessageEntity

@Database(
    entities = [MessageEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
}
