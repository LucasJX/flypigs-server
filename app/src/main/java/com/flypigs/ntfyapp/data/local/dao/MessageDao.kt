package com.flypigs.ntfyapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.flypigs.ntfyapp.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Query("SELECT * FROM messages ORDER BY timestamp DESC")
    fun getAllMessages(): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE id = :id")
    suspend fun getMessageById(id: String): MessageEntity?

    @Query("SELECT * FROM messages WHERE category = :category ORDER BY timestamp DESC")
    fun getMessagesByCategory(category: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE title LIKE '%' || :query || '%' OR body LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchMessages(query: String): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Query("UPDATE messages SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: String)

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteMessage(id: String)

    @Query("DELETE FROM messages")
    suspend fun deleteAllMessages()

    @Query("SELECT COUNT(*) FROM messages")
    fun getMessageCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM messages WHERE timestamp >= :startTime")
    fun getMessageCountSince(startTime: Long): Flow<Int>

    @Query("SELECT category, COUNT(*) as count FROM messages WHERE timestamp >= :startTime GROUP BY category")
    fun getCategoryStatsSince(startTime: Long): Flow<List<CategoryCount>>

    @Query("SELECT topic, COUNT(*) as count FROM messages WHERE timestamp >= :startTime GROUP BY topic")
    fun getTopicStatsSince(startTime: Long): Flow<List<TopicCount>>

    @Query("SELECT category, COUNT(*) as count FROM messages GROUP BY category")
    fun getCategoryStats(): Flow<List<CategoryCount>>

    @Query("SELECT topic, COUNT(*) as count FROM messages GROUP BY topic ORDER BY count DESC")
    fun getTopicStats(): Flow<List<TopicCount>>

    @Query("SELECT date(timestamp / 1000, 'unixepoch') as date, COUNT(*) as count FROM messages WHERE timestamp >= :since GROUP BY date ORDER BY date")
    fun getDailyStats(since: Long): Flow<List<DailyCount>>
}

data class CategoryCount(
    val category: String,
    val count: Int
)

data class TopicCount(
    val topic: String,
    val count: Int
)

data class DailyCount(
    val date: String,
    val count: Int
)
