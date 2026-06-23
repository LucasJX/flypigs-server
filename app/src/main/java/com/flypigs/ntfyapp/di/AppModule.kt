package com.flypigs.ntfyapp.di

import android.content.Context
import androidx.room.Room
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

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "ntfy_database"
        ).fallbackToDestructiveMigration().build()
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
