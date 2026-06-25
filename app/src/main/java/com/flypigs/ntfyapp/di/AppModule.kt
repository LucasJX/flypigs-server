package com.flypigs.ntfyapp.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.flypigs.ntfyapp.data.local.AppDatabase
import com.flypigs.ntfyapp.data.local.dao.MessageDao
import com.flypigs.ntfyapp.data.local.dao.ServerDao
import com.flypigs.ntfyapp.data.local.dao.TopicDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // v3 → v4: 添加 isStarred 字段
    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE messages ADD COLUMN isStarred INTEGER NOT NULL DEFAULT 0")
        }
    }

    // v4 → v5: password 字段改为 hasPassword + token
    // 注意：password 列暂时保留（Room 会忽略 Entity 中未映射的多余列）
    // 密码值需要在 App 初始化阶段迁移到 SecureStorage 后，再通过 v5→v6 migration 删除
    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE servers ADD COLUMN token TEXT")
            db.execSQL("ALTER TABLE servers ADD COLUMN hasPassword INTEGER NOT NULL DEFAULT 0")
            db.execSQL("UPDATE servers SET hasPassword = 1 WHERE password IS NOT NULL AND password != ''")
        }
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "ntfy_database"
        )
            .addMigrations(MIGRATION_3_4, MIGRATION_4_5)
            .fallbackToDestructiveMigration()  // 新版本迁移失败时重建（仅开发阶段）
            .build()
    }

    @Provides
    @Singleton
    fun provideMessageDao(database: AppDatabase): MessageDao {
        return database.messageDao()
    }

    @Provides
    @Singleton
    fun provideServerDao(database: AppDatabase): ServerDao {
        return database.serverDao()
    }

    @Provides
    @Singleton
    fun provideTopicDao(database: AppDatabase): TopicDao {
        return database.topicDao()
    }
}
