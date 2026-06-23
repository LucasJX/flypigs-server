package com.flypigs.ntfyapp.ui.screen.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flypigs.ntfyapp.data.local.entity.MessageEntity
import com.flypigs.ntfyapp.data.repository.MessageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val messageRepository: MessageRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val messageId: String = savedStateHandle["messageId"] ?: ""

    private val _message = MutableStateFlow<MessageEntity?>(null)
    val message: StateFlow<MessageEntity?> = _message.asStateFlow()

    init {
        if (messageId.isNotBlank()) {
            viewModelScope.launch {
                _message.value = messageRepository.getMessageById(messageId)
                // Mark as read
                messageRepository.markAsRead(messageId)
            }
        }
    }

    fun deleteMessage() {
        viewModelScope.launch {
            messageRepository.deleteMessage(messageId)
        }
    }
}
