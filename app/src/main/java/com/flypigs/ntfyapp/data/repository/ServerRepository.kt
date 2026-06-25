package com.flypigs.ntfyapp.data.repository

import com.flypigs.ntfyapp.data.local.SecureStorage
import com.flypigs.ntfyapp.data.local.dao.ServerDao
import com.flypigs.ntfyapp.data.local.dao.TopicDao
import com.flypigs.ntfyapp.data.local.entity.ServerEntity
import com.flypigs.ntfyapp.data.local.entity.TopicEntity
import com.flypigs.ntfyapp.data.remote.NtfyApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServerRepository @Inject constructor(
    private val serverDao: ServerDao,
    private val topicDao: TopicDao,
    private val messageDao: com.flypigs.ntfyapp.data.local.dao.MessageDao,
    private val ntfyApi: NtfyApi
) {

    fun getAllServers(): Flow<List<ServerEntity>> =
        serverDao.getAllServers().distinctUntilChanged()

    suspend fun getAllServersSuspend(): List<ServerEntity> = serverDao.getAllServersSuspend()

    suspend fun getServerById(id: String): ServerEntity? = serverDao.getServerById(id)

    /**
     * 获取服务器密码（从 SecureStorage 解密读取）
     */
    fun getServerPassword(serverId: String): String? = SecureStorage.getPassword(serverId)

    suspend fun addServer(
        url: String,
        name: String,
        username: String? = null,
        password: String? = null,
        token: String? = null
    ): ServerEntity {
        // 先测试连接
        val connected = ntfyApi.testConnection(url, username, password)

        val server = ServerEntity(
            id = UUID.randomUUID().toString(),
            url = url.trimEnd('/'),
            name = name,
            username = username,
            hasPassword = !password.isNullOrBlank(),
            token = token,
            isConnected = connected
        )
        serverDao.insertServer(server)

        // 密码存入加密存储
        if (!password.isNullOrBlank()) {
            SecureStorage.savePassword(server.id, password)
        }

        // 自动创建默认 Topic
        val defaultTopic = TopicEntity(
            id = UUID.randomUUID().toString(),
            serverId = server.id,
            name = "OpenClash",
            displayName = "OpenClash 事件",
            isEnabled = true
        )
        topicDao.insertTopic(defaultTopic)

        return server
    }

    suspend fun updateServer(server: ServerEntity) = serverDao.updateServer(server)

    suspend fun deleteServer(id: String) {
        // 级联删除：先获取关联 topic 名称 → 删除消息 → 删除 topic → 删除 server → 删除密码
        val topics = topicDao.getTopicsByServerSuspend(id)
        val topicNames = topics.map { it.name }
        if (topicNames.isNotEmpty()) {
            messageDao.deleteMessagesByTopicNames(topicNames)
        }
        // ForeignKey CASCADE 会自动删除 topic，但为确保旧 DB 兼容，也手动删除
        topics.forEach { topicDao.deleteTopicById(it.id) }
        serverDao.deleteServerById(id)
        SecureStorage.removePassword(id)  // 同时删除加密存储的密码
    }

    suspend fun updateConnectionStatus(id: String, connected: Boolean) =
        serverDao.updateConnectionStatus(id, connected)

    /**
     * 测试指定服务器的连接状态
     */
    suspend fun testConnection(server: ServerEntity): Boolean {
        val password = SecureStorage.getPassword(server.id)
        val connected = ntfyApi.testConnection(server.url, server.username, password)
        updateConnectionStatus(server.id, connected)
        return connected
    }
}
