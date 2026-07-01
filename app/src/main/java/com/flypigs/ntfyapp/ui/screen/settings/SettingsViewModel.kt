package com.flypigs.ntfyapp.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flypigs.ntfyapp.data.local.entity.ServerEntity
import com.flypigs.ntfyapp.data.local.entity.TopicEntity
import com.flypigs.ntfyapp.data.repository.MessageRepository
import com.flypigs.ntfyapp.data.repository.ServerRepository
import com.flypigs.ntfyapp.data.repository.TopicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 单个服务器的连接测试状态
 * - IDLE: 未测试 / 测试完成已重置
 * - TESTING: 正在测试
 * - SUCCESS: 测试通过
 * - FAILED: 测试失败
 */
enum class TestState { IDLE, TESTING, SUCCESS, FAILED }

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val serverRepository: ServerRepository,
    private val topicRepository: TopicRepository,
    private val messageRepository: MessageRepository
) : ViewModel() {

    val servers: StateFlow<List<ServerEntity>> = serverRepository.getAllServers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val topics: StateFlow<List<TopicEntity>> = topicRepository.getAllTopics()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * 每台服务器的最新测试状态（key = serverId）
     * UI 用此状态显示按钮文案 / 颜色 / Snackbar
     */
    private val _testStates = MutableStateFlow<Map<String, TestState>>(emptyMap())
    val testStates: StateFlow<Map<String, TestState>> = _testStates.asStateFlow()

    fun addServer(url: String, name: String, username: String? = null, password: String? = null, token: String? = null) {
        viewModelScope.launch {
            serverRepository.addServer(url = url, name = name, username = username, password = password, token = token)
        }
    }

    fun deleteServer(id: String) {
        viewModelScope.launch {
            serverRepository.deleteServer(id)
        }
    }

    fun updateServer(id: String, url: String, name: String, username: String? = null, password: String? = null, token: String? = null) {
        viewModelScope.launch {
            val existing = serverRepository.getServerById(id)
            val hasPassword = !password.isNullOrBlank() || (existing?.hasPassword == true && password == null)
            serverRepository.updateServer(
                ServerEntity(id = id, url = url, name = name, username = username, hasPassword = hasPassword, token = token)
            )
            if (!password.isNullOrBlank()) {
                com.flypigs.ntfyapp.data.local.SecureStorage.savePassword(id, password)
            } else if (password == null && existing?.hasPassword == true) {
                // password == null 表示 UI 没改动密码字段，保留原密码
            } else if (password != null && password.isBlank()) {
                // password 空字符串表示用户主动清空了密码
                com.flypigs.ntfyapp.data.local.SecureStorage.removePassword(id)
            }
        }
    }

    /**
     * 手动测试服务器连接
     * - UI: 按钮置 TESTING，结束后 SUCCESS/FAILED，3 秒后重置回 IDLE
     * - 底层走 ServerRepository.testConnection()（调 ntfyApi.testConnection → /v1/health）
     */
    fun testConnection(server: ServerEntity) {
        viewModelScope.launch {
            // 标记测试中
            _testStates.update { it + (server.id to TestState.TESTING) }
            val ok = try {
                serverRepository.testConnection(server)
            } catch (e: Exception) {
                false
            }
            _testStates.update { it + (server.id to if (ok) TestState.SUCCESS else TestState.FAILED) }
            // 3 秒后自动重置状态（让 UI 回到可重复点击）
            delay(3000)
            _testStates.update { it - server.id }
        }
    }

    fun addTopic(serverId: String, name: String, displayName: String) {
        viewModelScope.launch {
            topicRepository.addTopic(serverId = serverId, name = name, displayName = displayName)
        }
    }

    fun deleteTopic(id: String) {
        viewModelScope.launch {
            topicRepository.deleteTopic(id)
        }
    }

    fun updateTopic(id: String, serverId: String, name: String, displayName: String, isEnabled: Boolean = true) {
        viewModelScope.launch {
            topicRepository.updateTopic(
                TopicEntity(id = id, serverId = serverId, name = name, displayName = displayName, isEnabled = isEnabled)
            )
        }
    }

    fun toggleTopic(id: String, enabled: Boolean) {
        viewModelScope.launch {
            topicRepository.toggleEnabled(id, enabled)
        }
    }

    fun clearAllMessages() {
        viewModelScope.launch {
            messageRepository.deleteAllMessages()
        }
    }
}
