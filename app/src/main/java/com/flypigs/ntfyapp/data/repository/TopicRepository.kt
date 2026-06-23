package com.flypigs.ntfyapp.data.repository

import com.flypigs.ntfyapp.data.local.dao.TopicDao
import com.flypigs.ntfyapp.data.local.entity.TopicEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TopicRepository @Inject constructor(
    private val topicDao: TopicDao
) {

    fun getAllTopics(): Flow<List<TopicEntity>> = topicDao.getAllTopics()

    fun getTopicsByServer(serverId: String): Flow<List<TopicEntity>> =
        topicDao.getTopicsByServer(serverId)

    fun getEnabledTopics(): Flow<List<TopicEntity>> = topicDao.getEnabledTopics()

    suspend fun addTopic(
        serverId: String,
        name: String,
        displayName: String
    ): TopicEntity {
        val topic = TopicEntity(
            id = UUID.randomUUID().toString(),
            serverId = serverId,
            name = name,
            displayName = displayName
        )
        topicDao.insertTopic(topic)
        return topic
    }

    suspend fun updateTopic(topic: TopicEntity) = topicDao.updateTopic(topic)

    suspend fun deleteTopic(id: String) = topicDao.deleteTopicById(id)

    suspend fun toggleEnabled(id: String, enabled: Boolean) =
        topicDao.updateEnabled(id, enabled)
}
