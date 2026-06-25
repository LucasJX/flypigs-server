package com.flypigs.ntfyapp.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
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

/**
 * 列表模式：Paging（非搜索） vs Flow<List>（搜索/组合筛选）
 */
enum class ListMode { PAGING, SEARCH }

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

    // ─── 列表模式判定 ────────────────────────────────────────────────
    // 搜索模式用 Flow<List>，非搜索模式用 PagingData
    val listMode: StateFlow<ListMode> = combine(_isSearching, _searchQuery) { searching, query ->
        if (searching && query.isNotBlank()) ListMode.SEARCH else ListMode.PAGING
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ListMode.PAGING)

    // ─── Paging 消息流 (非搜索场景) ──────────────────────────────────
    @OptIn(ExperimentalCoroutinesApi::class)
    val pagingMessages: Flow<PagingData<MessageEntity>> = combine(
        _selectedCategory,
        _selectedTopic,
        _selectedTab
    ) { category, topic, tab ->
        Triple(category, topic, tab)
    }.flatMapLatest { (category, topic, tab) ->
        when {
            tab == MessageTab.UNREAD -> repository.getUnreadMessagesPaging()
            topic != null -> repository.getMessagesByTopicPaging(topic)
            category != null -> repository.getMessagesByCategoryPaging(category)
            else -> repository.getAllMessagesPaging()
        }
    }.cachedIn(viewModelScope)

    // ─── 搜索消息流 (搜索场景) ────────────────────────────────────────
    @OptIn(ExperimentalCoroutinesApi::class)
    val searchMessages: StateFlow<List<MessageEntity>> = _searchQuery
        .flatMapLatest { query ->
            if (query.isNotBlank()) repository.searchMessages(query)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ─── 组合筛选消息流 (topic+category 同时有值时，PagingSource 不支持) ───
    @OptIn(ExperimentalCoroutinesApi::class)
    val combinedMessages: StateFlow<List<MessageEntity>> = combine(
        _selectedTopic, _selectedCategory
    ) { topic, category ->
        Pair(topic, category)
    }.flatMapLatest { (topic, category) ->
        if (topic != null && category != null) {
            repository.getMessagesByTopicAndCategory(topic, category)
        } else {
            flowOf(emptyList())   // 不走此分支时返回空，不会显示
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ─── 最终 UI 列表模式判定 ────────────────────────────────────────
    // topic+category 组合 → combinedMessages (Flow<List>)
    // 搜索 → searchMessages (Flow<List>)
    // 其他 → pagingMessages (PagingData)
    val needsCombinedFilter: StateFlow<Boolean> = combine(
        _selectedTopic, _selectedCategory
    ) { topic, category ->
        topic != null && category != null
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // 所有 Topic
    val topics: StateFlow<List<TopicEntity>> = topicRepository.getEnabledTopics()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 全局分类统计
    val categoryStats: StateFlow<List<CategoryCount>> = repository.getCategoryStats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 当前选中 Topic 的分类统计
    @OptIn(ExperimentalCoroutinesApi::class)
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

    // ── 事件摘要：使用聚合查询避免全量加载 ──
    data class EventSummary(
        val todayCount: Int = 0,
        val unreadCount: Int = 0,
        val latestMessage: MessageEntity? = null
    )

    val eventSummary: StateFlow<EventSummary> = combine(
        repository.getMessageCountSince(todayStartSeconds()),
        repository.getUnreadCount(),
        repository.getLatestMessage()
    ) { todayCount, unreadCount, latestMessage ->
        EventSummary(todayCount = todayCount, unreadCount = unreadCount, latestMessage = latestMessage)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), EventSummary())

    /**
     * 计算今天 0 点的 Unix 秒数（本地时区）
     */
    private fun todayStartSeconds(): Long {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis / 1000
    }
}
