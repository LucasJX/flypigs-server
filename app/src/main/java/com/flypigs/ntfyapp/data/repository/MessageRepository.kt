package com.flypigs.ntfyapp.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.flypigs.ntfyapp.data.local.dao.MessageDao
import com.flypigs.ntfyapp.data.local.entity.MessageEntity
import com.flypigs.ntfyapp.data.remote.NtfyMessage
import com.flypigs.ntfyapp.domain.model.MessageCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageRepository @Inject constructor(
    private val messageDao: MessageDao
) {

    companion object {
        private val PAGING_CONFIG = PagingConfig(
            pageSize = 30,
            prefetchDistance = 10,
            enablePlaceholders = false,
            initialLoadSize = 60
        )
    }

    // ─── Paging 3 ────────────────────────────────────────────────
    // 非搜索场景使用 PagingData — 自动按页加载，内存友好

    fun getAllMessagesPaging(): Flow<PagingData<MessageEntity>> =
        Pager(PAGING_CONFIG) { messageDao.getAllMessagesPagingSource() }.flow

    fun getUnreadMessagesPaging(): Flow<PagingData<MessageEntity>> =
        Pager(PAGING_CONFIG) { messageDao.getUnreadMessagesPagingSource() }.flow

    fun getMessagesByCategoryPaging(category: MessageCategory): Flow<PagingData<MessageEntity>> =
        Pager(PAGING_CONFIG) { messageDao.getMessagesByCategoryPagingSource(category.name) }.flow

    fun getMessagesByTopicPaging(topic: String): Flow<PagingData<MessageEntity>> =
        Pager(PAGING_CONFIG) { messageDao.getMessagesByTopicPagingSource(topic) }.flow

    // ─── 非 Paging Flow (搜索 + 组合筛选) ────────────────────────
    // 搜索场景无法使用 PagingSource (Room LIKE 查询无 invalidated 信号)
    // 组合筛选(topic+category) 也暂用 Flow<List>

    fun getAllMessages(): Flow<List<MessageEntity>> =
        messageDao.getAllMessages().distinctUntilChanged()

    fun getMessagesByCategory(category: MessageCategory): Flow<List<MessageEntity>> =
        messageDao.getMessagesByCategory(category.name).distinctUntilChanged()

    fun searchMessages(query: String): Flow<List<MessageEntity>> =
        messageDao.searchMessages(query).distinctUntilChanged()

    suspend fun getMessageById(id: String): MessageEntity? = messageDao.getMessageById(id)

    suspend fun insertMessage(ntfyMessage: NtfyMessage): MessageEntity {
        val category = classifyMessage(ntfyMessage.tags)
        val entity = MessageEntity(
            id = ntfyMessage.id,
            topic = ntfyMessage.topic,
            title = ntfyMessage.title,
            body = ntfyMessage.message,
            priority = ntfyMessage.priority ?: 3,
            tags = ntfyMessage.tags?.joinToString(","),
            timestamp = ntfyMessage.time,
            isRead = false,
            category = category
        )
        messageDao.insertMessage(entity)
        return entity
    }

    suspend fun markAsRead(id: String) = messageDao.markAsRead(id)

    suspend fun markAllAsRead() = messageDao.markAllAsRead()

    suspend fun deleteMessage(id: String) = messageDao.deleteMessage(id)

    suspend fun deleteAllMessages() = messageDao.deleteAllMessages()

    fun getMessageCount(): Flow<Int> =
        messageDao.getMessageCount().distinctUntilChanged()

    fun getMessageCountSince(startTime: Long): Flow<Int> =
        messageDao.getMessageCountSince(startTime).distinctUntilChanged()

    fun getCategoryStatsSince(startTime: Long) =
        messageDao.getCategoryStatsSince(startTime).distinctUntilChanged()

    fun getTopicStatsSince(startTime: Long) =
        messageDao.getTopicStatsSince(startTime).distinctUntilChanged()

    fun getCategoryStats() =
        messageDao.getCategoryStats().distinctUntilChanged()

    fun getDistinctCategories() =
        messageDao.getDistinctCategories().distinctUntilChanged()

    fun getTopicStats() =
        messageDao.getTopicStats().distinctUntilChanged()

    fun getDailyStats(since: Long) =
        messageDao.getDailyStats(since).distinctUntilChanged()

    fun getMessagesByTopic(topic: String) =
        messageDao.getMessagesByTopic(topic).distinctUntilChanged()

    fun getMessagesByTopicAndCategory(topic: String, category: MessageCategory) =
        messageDao.getMessagesByTopicAndCategory(topic, category.name).distinctUntilChanged()

    fun getUnreadMessages() =
        messageDao.getUnreadMessages().distinctUntilChanged()

    fun getUnreadCount() =
        messageDao.getUnreadCount().distinctUntilChanged()

    fun getCategoryStatsByTopic(topic: String) =
        messageDao.getCategoryStatsByTopic(topic).distinctUntilChanged()

    fun getMessageCountByTopic(topic: String) =
        messageDao.getMessageCountByTopic(topic).distinctUntilChanged()

    fun getUnreadCountByTopic() =
        messageDao.getUnreadCountByTopic().distinctUntilChanged()

    fun getLatestMessage(): Flow<MessageEntity?> =
        messageDao.getLatestMessage().distinctUntilChanged()

    /**
     * 根据 tags 自动分类消息
     */
    private fun classifyMessage(tags: List<String>?): String {
        if (tags.isNullOrEmpty()) return "OTHER"

        val tagSet = tags.map { it.lowercase() }.toSet()

        return when {
            tagSet.any { it in listOf("node", "change", "上线", "下线") } -> "NODE_CHANGE"
            tagSet.any { it in listOf("alert", "warning", "error", "故障") } -> "SYSTEM_ALERT"
            tagSet.any { it in listOf("recovery", "ok", "恢复", "正常") } -> "RECOVERY"
            tagSet.any { it in listOf("update", "release", "版本") } -> "UPDATE"
            tagSet.any { it in listOf("device", "设备") } -> "DEVICE"
            tagSet.any { it in listOf("sub", "subscription", "订阅") } -> "SUBSCRIPTION"
            else -> "OTHER"
        }
    }
}
