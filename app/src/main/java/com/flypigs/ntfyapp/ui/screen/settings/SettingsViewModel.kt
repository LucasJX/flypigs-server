package com.flypigs.ntfyapp.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flypigs.ntfyapp.data.local.entity.ServerEntity
import com.flypigs.ntfyapp.data.local.entity.TopicEntity
import com.flypigs.ntfyapp.data.repository.MessageRepository
import com.flypigs.ntfyapp.data.repository.ServerRepository
import com.flypigs.ntfyapp.data.repository.TopicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

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

    fun addServer(url: String, name: String, token: String? = null) {
        viewModelScope.launch {
            serverRepository.addServer(url = url, name = name, token = token)
        }
    }

    fun deleteServer(id: String) {
        viewModelScope.launch {
            serverRepository.deleteServer(id)
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
