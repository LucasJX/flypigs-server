package com.flypigs.ntfyapp.domain.model

import com.flypigs.ntfyapp.data.local.entity.ServerEntity
import com.flypigs.ntfyapp.data.local.entity.TopicEntity
import com.flypigs.ntfyapp.data.local.entity.MessageEntity
import com.flypigs.ntfyapp.data.local.dao.CategoryCount
import com.flypigs.ntfyapp.data.local.dao.TopicCount
import com.flypigs.ntfyapp.data.local.dao.DailyCount

/**
 * Entity → Domain 模型映射器
 * UI 层只接触 Domain 模型，不直接引用 Entity
 */
object Mappers {

    fun ServerEntity.toDomain() = Server(
        id = id,
        url = url,
        name = name,
        username = username,
        hasPassword = hasPassword,
        token = token,
        isConnected = isConnected
    )

    fun TopicEntity.toDomain() = Topic(
        id = id,
        serverId = serverId,
        name = name,
        displayName = displayName,
        isEnabled = isEnabled
    )

    fun MessageEntity.toDomain() = Message(
        id = id,
        topic = topic,
        title = title,
        body = body,
        priority = priority,
        tags = tags?.split(",")?.filter { it.isNotBlank() } ?: emptyList(),
        timestamp = timestamp,
        isRead = isRead,
        category = try {
            MessageCategory.valueOf(category)
        } catch (_: Exception) {
            MessageCategory.OTHER
        }
    )

    fun CategoryCount.toDomain() = CategoryStat(category = category, count = count)
    fun TopicCount.toDomain() = TopicStat(topic = topic, count = count)
    fun DailyCount.toDomain() = DailyStat(date = date, count = count)

    // 批量映射
    fun List<ServerEntity>.toDomainServers() = map { it.toDomain() }
    fun List<TopicEntity>.toDomainTopics() = map { it.toDomain() }
    fun List<MessageEntity>.toDomainMessages() = map { it.toDomain() }
    fun List<CategoryCount>.toDomainCategoryStats() = map { it.toDomain() }
    fun List<TopicCount>.toDomainTopicStats() = map { it.toDomain() }
    fun List<DailyCount>.toDomainDailyStats() = map { it.toDomain() }
}
