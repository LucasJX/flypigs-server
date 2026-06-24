package com.flypigs.ntfyapp.data.repository

import com.flypigs.ntfyapp.data.local.dao.ServerDao
import com.flypigs.ntfyapp.data.local.dao.TopicDao
import com.flypigs.ntfyapp.data.local.entity.ServerEntity
import com.flypigs.ntfyapp.data.local.entity.TopicEntity
import com.flypigs.ntfyapp.data.remote.NtfyApi
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServerRepository @Inject constructor(
    private val serverDao: ServerDao,
    private val topicDao: TopicDao,
    private val ntfyApi: NtfyApi
) {

    fun getAllServers(): Flow<List<ServerEntity>> = serverDao.getAllServers()

    suspend fun getServerById(id: String): ServerEntity? = serverDao.getServerById(id)

    suspend fun addServer(
        url: String,
        name: String,
        username: String? = null,
        password: String? = null
    ): ServerEntity {
        // 先测试连接
        val connected = ntfyApi.testConnection(url, username, password)

        val server = ServerEntity(
            id = UUID.randomUUID().toString(),
            url = url.trimEnd('/'),
            name = name,
            username = username,
            password = password,
            isConnected = connected
        )
        serverDao.insertServer(server)

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

    suspend fun deleteServer(id: String) = serverDao.deleteServerById(id)

    suspend fun updateConnectionStatus(id: String, connected: Boolean) =
        serverDao.updateConnectionStatus(id, connected)

    /**
     * 测试指定服务器的连接状态
     */
    suspend fun testConnection(server: ServerEntity): Boolean {
        val connected = ntfyApi.testConnection(server.url, server.username, server.password)
        updateConnectionStatus(server.id, connected)
        return connected
    }
}
