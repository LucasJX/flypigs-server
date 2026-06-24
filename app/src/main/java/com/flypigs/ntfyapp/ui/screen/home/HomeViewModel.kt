package com.flypigs.ntfyapp.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flypigs.ntfyapp.data.local.dao.CategoryCount
import com.flypigs.ntfyapp.data.local.entity.MessageEntity
import com.flypigs.ntfyapp.data.local.entity.TopicEntity
import com.flypigs.ntfyapp.data.repository.MessageRepository
import com.flypigs.ntfyapp.data.repository.TopicRepository
import com.flypigs.ntfyapp.domain.model.MessageCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class MessageTab { ALL, UNREAD }

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: MessageRepository,
    private val topicRepository: TopicRepository
) : ViewModel() {

    private val _selectedCategory = MutableStateFlow<MessageCategory?>(null)
    val selectedCategory: StateFlow<MessageCategory?> = _selectedCategory.asStateFlow()

    private val _selectedTopic = MutableStateFlow<String?>(null)
    val selectedTopic: StateFlow<String?> = _selectedTopic.asStateFlow()

    private val _selectedTab = MutableStateFlow(MessageTab.ALL)
    val selectedTab: StateFlow<MessageTab> = _selectedTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    // 消息列表（支持 topic + category + tab + 搜索组合筛选）
    @OptIn(ExperimentalCoroutinesApi::class)
    val messages: StateFlow<List<MessageEntity>> = combine(
        _selectedCategory,
        _selectedTopic,
        _selectedTab,
        _searchQuery
    ) { category, topic, tab, query ->
        arrayOf(category, topic, tab, query)
    }.flatMapLatest { (category, topic, tab, query) ->
        @Suppress("UNCHECKED_CAST")
        val c = category as? MessageCategory
        @Suppress("UNCHECKED_CAST")
        val t = topic as? String
        @Suppress("UNCHECKED_CAST")
        val tb = tab as MessageTab
        @Suppress("UNCHECKED_CAST")
        val q = query as String

        when {
            q.isNotBlank() -> repository.searchMessages(q)
            tb == MessageTab.UNREAD -> repository.getUnreadMessages()
            t != null && c != null -> repository.getMessagesByTopicAndCategory(t, c)
            t != null -> repository.getMessagesByTopic(t)
            c != null -> repository.getMessagesByCategory(c)
            else -> repository.getAllMessages()
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // 所有 Topic
    val topics: StateFlow<List<TopicEntity>> = topicRepository.getEnabledTopics()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 全局分类统计
    val categoryStats: StateFlow<List<CategoryCount>> = repository.getCategoryStats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 当前选中 Topic 的分类统计
    private val _topicCategoryStats = _selectedTopic.flatMapLatest { topic ->
        if (topic != null) repository.getCategoryStatsByTopic(topic)
        else repository.getCategoryStats()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val topicCategoryStats: StateFlow<List<CategoryCount>> = _topicCategoryStats

    // 总消息数
    val totalCount: StateFlow<Int> = repository.getMessageCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // 未读数
    val unreadCount: StateFlow<Int> = repository.getUnreadCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // 按 Topic 的未读数
    val topicUnreadCounts: StateFlow<Map<String, Int>> = repository.getUnreadCountByTopic()
        .map { list -> list.associate { it.topic to it.count } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun selectCategory(category: MessageCategory?) {
        _selectedCategory.value = category
        _selectedTab.value = MessageTab.ALL
    }

    fun selectTopic(topic: String?) {
        _selectedTopic.value = topic
        _selectedCategory.value = null
        _selectedTab.value = MessageTab.ALL
    }

    fun selectTab(tab: MessageTab) {
        _selectedTab.value = tab
    }

    fun search(query: String) {
        _searchQuery.value = query
    }

    fun toggleSearch() {
        _isSearching.value = !_isSearching.value
        if (!_isSearching.value) {
            _searchQuery.value = ""
        }
    }

    fun markAsRead(id: String) {
        viewModelScope.launch {
            repository.markAsRead(id)
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            repository.markAllAsRead()
        }
    }

    fun deleteMessage(id: String) {
        viewModelScope.launch {
            repository.deleteMessage(id)
        }
    }

    // ── 事件摘要：最近消息统计 ──
    data class EventSummary(
        val todayCount: Int = 0,
        val unreadCount: Int = 0,
        val latestMessage: MessageEntity? = null
    )

    val eventSummary: StateFlow<EventSummary> = repository.getAllMessages()
        .map { messages ->
            val now = System.currentTimeMillis() / 1000
            val todayStart = now - (now % 86400) // 当天零点时间戳
            EventSummary(
                todayCount = messages.count { it.timestamp >= todayStart },
                unreadCount = messages.count { !it.isRead },
                latestMessage = messages.maxByOrNull { it.timestamp }
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), EventSummary())
}
