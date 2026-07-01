package com.flypigs.ntfyapp.data.local.dao

import androidx.paging.PagingSource
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

    @Query("UPDATE messages SET isRead = 1 WHERE isRead = 0")
    suspend fun markAllAsRead()

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

    @Query("SELECT DISTINCT category FROM messages ORDER BY category")
    fun getDistinctCategories(): Flow<List<String>>

    @Query("SELECT DISTINCT category FROM messages WHERE topic = :topic ORDER BY category")
    fun getDistinctCategoriesByTopic(topic: String): Flow<List<String>>

    @Query("SELECT topic, COUNT(*) as count FROM messages GROUP BY topic ORDER BY count DESC")
    fun getTopicStats(): Flow<List<TopicCount>>

    @Query("SELECT date(timestamp / 1000, 'unixepoch', 'localtime') as date, COUNT(*) as count FROM messages WHERE timestamp >= :since GROUP BY date ORDER BY date")
    fun getDailyStats(since: Long): Flow<List<DailyCount>>

    @Query("SELECT * FROM messages WHERE topic = :topic ORDER BY timestamp DESC")
    fun getMessagesByTopic(topic: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE topic = :topic AND category = :category ORDER BY timestamp DESC")
    fun getMessagesByTopicAndCategory(topic: String, category: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE isRead = 0 ORDER BY timestamp DESC")
    fun getUnreadMessages(): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE isStarred = 1 ORDER BY timestamp DESC")
    fun getStarredMessages(): Flow<List<MessageEntity>>

    @Query("SELECT COUNT(*) FROM messages WHERE isRead = 0")
    fun getUnreadCount(): Flow<Int>

    @Query("UPDATE messages SET isStarred = :starred WHERE id = :id")
    suspend fun toggleStarred(id: String, starred: Boolean)

    @Query("SELECT category, COUNT(*) as count FROM messages WHERE topic = :topic GROUP BY category")
    fun getCategoryStatsByTopic(topic: String): Flow<List<CategoryCount>>

    @Query("SELECT COUNT(*) FROM messages WHERE topic = :topic")
    fun getMessageCountByTopic(topic: String): Flow<Int>

    @Query("SELECT topic, COUNT(*) as count FROM messages WHERE isRead = 0 GROUP BY topic")
    fun getUnreadCountByTopic(): Flow<List<TopicCount>>

    // ─── 聚合查询（EventSummary 使用，避免全量加载） ──────────
    @Query("SELECT * FROM messages ORDER BY timestamp DESC LIMIT 1")
    fun getLatestMessage(): Flow<MessageEntity?>

    // ─── 历史消息同步：获取指定 topic 最后一条消息的时间戳（Unix 秒） ──
    @Query("SELECT MAX(timestamp) FROM messages WHERE topic = :topic")
    suspend fun getLatestTimestampForTopic(topic: String): Long?

    // ─── 分页查询 ────────────────────────────────────────────
    @Query("SELECT * FROM messages ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    suspend fun getMessagesPaged(limit: Int, offset: Int): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE topic = :topic ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    suspend fun getMessagesByTopicPaged(topic: String, limit: Int, offset: Int): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE category = :category ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    suspend fun getMessagesByCategoryPaged(category: String, limit: Int, offset: Int): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE isRead = 0 ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    suspend fun getUnreadMessagesPaged(limit: Int, offset: Int): List<MessageEntity>

    // ─── 级联删除辅助 ──────────────────────────────────────────
    @Query("DELETE FROM messages WHERE topic IN (:topicNames)")
    suspend fun deleteMessagesByTopicNames(topicNames: List<String>)

    // ─── Paging 3 ──────────────────────────────────────────────────
    @Query("SELECT * FROM messages ORDER BY timestamp DESC")
    fun getAllMessagesPagingSource(): PagingSource<Int, MessageEntity>

    @Query("SELECT * FROM messages WHERE isRead = 0 ORDER BY timestamp DESC")
    fun getUnreadMessagesPagingSource(): PagingSource<Int, MessageEntity>

    @Query("SELECT * FROM messages WHERE category = :category ORDER BY timestamp DESC")
    fun getMessagesByCategoryPagingSource(category: String): PagingSource<Int, MessageEntity>

    @Query("SELECT * FROM messages WHERE topic = :topic ORDER BY timestamp DESC")
    fun getMessagesByTopicPagingSource(topic: String): PagingSource<Int, MessageEntity>
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
