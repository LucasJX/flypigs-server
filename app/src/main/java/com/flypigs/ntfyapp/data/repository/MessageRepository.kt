package com.flypigs.ntfyapp.data.repository

import com.flypigs.ntfyapp.data.local.dao.MessageDao
import com.flypigs.ntfyapp.data.local.entity.MessageEntity
import com.flypigs.ntfyapp.data.remote.NtfyMessage
import com.flypigs.ntfyapp.domain.model.MessageCategory
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageRepository @Inject constructor(
    private val messageDao: MessageDao
) {

    fun getAllMessages(): Flow<List<MessageEntity>> = messageDao.getAllMessages()

    fun getMessagesByCategory(category: MessageCategory): Flow<List<MessageEntity>> =
        messageDao.getMessagesByCategory(category.name)

    fun searchMessages(query: String): Flow<List<MessageEntity>> =
        messageDao.searchMessages(query)

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
            category = category.name
        )
        messageDao.insertMessage(entity)
        return entity
    }

    suspend fun markAsRead(id: String) = messageDao.markAsRead(id)

    suspend fun deleteMessage(id: String) = messageDao.deleteMessage(id)

    suspend fun deleteAllMessages() = messageDao.deleteAllMessages()

    fun getMessageCount(): Flow<Int> = messageDao.getMessageCount()

    fun getMessageCountSince(startTime: Long): Flow<Int> =
        messageDao.getMessageCountSince(startTime)

    fun getCategoryStatsSince(startTime: Long) =
        messageDao.getCategoryStatsSince(startTime)

    fun getTopicStatsSince(startTime: Long) =
        messageDao.getTopicStatsSince(startTime)

    fun getCategoryStats() = messageDao.getCategoryStats()

    fun getTopicStats() = messageDao.getTopicStats()

    fun getDailyStats(since: Long) = messageDao.getDailyStats(since)

    /**
     * 根据 tags 自动分类消息
     */
    private fun classifyMessage(tags: List<String>?): MessageCategory {
        if (tags.isNullOrEmpty()) return MessageCategory.OTHER

        val tagSet = tags.map { it.lowercase() }.toSet()

        return when {
            tagSet.any { it in listOf("node", "change", "上线", "下线") } -> MessageCategory.NODE_CHANGE
            tagSet.any { it in listOf("alert", "warning", "error", "故障") } -> MessageCategory.SYSTEM_ALERT
            tagSet.any { it in listOf("recovery", "ok", "恢复", "正常") } -> MessageCategory.RECOVERY
            tagSet.any { it in listOf("update", "release", "版本") } -> MessageCategory.UPDATE
            else -> MessageCategory.OTHER
        }
    }
}
