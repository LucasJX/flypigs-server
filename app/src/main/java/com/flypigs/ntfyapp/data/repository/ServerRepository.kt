package com.flypigs.ntfyapp.data.repository

import com.flypigs.ntfyapp.data.local.dao.ServerDao
import com.flypigs.ntfyapp.data.local.entity.ServerEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServerRepository @Inject constructor(
    private val serverDao: ServerDao
) {

    fun getAllServers(): Flow<List<ServerEntity>> = serverDao.getAllServers()

    suspend fun getServerById(id: String): ServerEntity? = serverDao.getServerById(id)

    suspend fun addServer(
        url: String,
        name: String,
        username: String? = null,
        password: String? = null,
        token: String? = null
    ): ServerEntity {
        val server = ServerEntity(
            id = UUID.randomUUID().toString(),
            url = url.trimEnd('/'),
            name = name,
            username = username,
            password = password,
            token = token
        )
        serverDao.insertServer(server)
        return server
    }

    suspend fun updateServer(server: ServerEntity) = serverDao.updateServer(server)

    suspend fun deleteServer(id: String) = serverDao.deleteServerById(id)

    suspend fun updateConnectionStatus(id: String, connected: Boolean) =
        serverDao.updateConnectionStatus(id, connected)
}
