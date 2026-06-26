package com.flypigs.ntfyapp.ui.screen.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.flypigs.ntfyapp.data.local.entity.MessageEntity
import com.flypigs.ntfyapp.domain.model.MessageCategory
import com.flypigs.ntfyapp.ui.LocalDrawerState
import com.flypigs.ntfyapp.ui.component.CenteredTopAppBar
import com.flypigs.ntfyapp.ui.component.MessageCard
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onOpenDrawer: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToDetail: (String) -> Unit = {}
) {
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedTopic by viewModel.selectedTopic.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val topics by viewModel.topics.collectAsState()
    val totalCount by viewModel.totalCount.collectAsState()
    val unreadCount by viewModel.unreadCount.collectAsState()
    val topicUnreadCounts by viewModel.topicUnreadCounts.collectAsState()
    val listMode by viewModel.listMode.collectAsState()
    val needsCombined by viewModel.needsCombinedFilter.collectAsState()
    val isBatchMode by viewModel.isBatchMode.collectAsState()
    val selectedIds by viewModel.selectedIds.collectAsState()
    val dynamicCategories by viewModel.dynamicCategories.collectAsState()

    // Paging 数据
    val pagingItems: LazyPagingItems<MessageEntity> = viewModel.pagingMessages.collectAsLazyPagingItems()

    // 搜索/组合筛选数据
    val searchMessages by viewModel.searchMessages.collectAsState()
    val combinedMessages by viewModel.combinedMessages.collectAsState()

    val scope = rememberCoroutineScope()
    val drawerHolder = LocalDrawerState.current

    DisposableEffect(Unit) {
        drawerHolder.value = drawerHolder.value.copy(
            content = @Composable {
                DrawerContent(
                    topics = topics,
                    totalCount = totalCount,
                    topicUnreadCounts = topicUnreadCounts,
                    selectedTopic = selectedTopic,
                    selectedCategory = selectedCategory?.name,
                    selectedTab = selectedTab,
                    onSelectTopic = { topic ->
                        viewModel.selectTopic(topic)
                        drawerHolder.value.closeDrawer()
                    }
                )
            }
        )
        onDispose {
            drawerHolder.value = drawerHolder.value.copy(content = {})
        }
    }

    Scaffold(
        topBar = {
            if (isBatchMode) {
                // 批量模式顶栏
                TopAppBar(
                    title = { Text("已选 ${selectedIds.size} 项") },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.exitBatchMode() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "退出批量模式")
                        }
                    },
                    actions = {
                        // 全选按钮（仅搜索/组合筛选模式可用）
                        val canSelectAll = needsCombined || (isSearching && searchQuery.isNotBlank())
                        IconButton(
                            onClick = {
                                val currentIds = when {
                                    needsCombined -> combinedMessages.map { it.id }
                                    isSearching -> searchMessages.map { it.id }
                                    else -> emptyList()
                                }
                                viewModel.selectAll(currentIds)
                            },
                            enabled = canSelectAll
                        ) {
                            Icon(Icons.Default.SelectAll, contentDescription = "全选")
                        }
                        IconButton(
                            onClick = { viewModel.deleteSelected() },
                            enabled = selectedIds.isNotEmpty()
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "删除")
                        }
                    }
                )
            } else {
                // 普通模式顶栏
                val subtitle = when {
                    selectedTopic != null && selectedCategory != null ->
                        "${selectedCategory!!.displayName}"
                    selectedTopic != null -> {
                        val topicName = topics.find { it.name == selectedTopic }?.displayName ?: selectedTopic!!
                        topicName
                    }
                    else -> null
                }
                CenteredTopAppBar(
                    title = "事件中心",
                    subtitle = subtitle,
                    navigationIcon = {
                        IconButton(onClick = onOpenDrawer) {
                            Icon(Icons.Default.Menu, contentDescription = "菜单")
                        }
                    },
                    actions = {
                        if (unreadCount > 0) {
                            IconButton(onClick = { viewModel.markAllAsRead() }) {
                                Icon(Icons.Default.DoneAll, contentDescription = "全部已读")
                            }
                        }
                        IconButton(onClick = { viewModel.toggleSearch() }) {
                            Icon(Icons.Default.Search, contentDescription = "搜索")
                        }
                    }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── Tab 栏 ──
            TabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedTab == MessageTab.ALL,
                    onClick = { viewModel.selectTab(MessageTab.ALL) },
                    text = { Text("全部消息") }
                )
                Tab(
                    selected = selectedTab == MessageTab.UNREAD,
                    onClick = { viewModel.selectTab(MessageTab.UNREAD) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("未读")
                            if (unreadCount > 0) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Badge { Text("$unreadCount") }
                            }
                        }
                    }
                )
            }

            // 搜索栏
            AnimatedVisibility(
                visible = isSearching,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -it / 4 })
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.search(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("搜索事件...") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )
            }

            // 分类筛选 ScrollableTabRow（仅全部消息 Tab 下显示）
            if (selectedTab == MessageTab.ALL) {
                val categoryNames = dynamicCategories.map { it.name }
                ScrollableTabRow(
                    selectedTabIndex = if (selectedCategory == null) 0 else (categoryNames.indexOf(selectedCategory?.name) + 1).coerceAtLeast(0),
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    edgePadding = 16.dp
                ) {
                    Tab(
                        selected = selectedCategory == null,
                        onClick = { viewModel.selectCategory(null) },
                        text = { Text("全部") }
                    )
                    dynamicCategories.forEach { category ->
                        Tab(
                            selected = selectedCategory?.name == category.name,
                            onClick = {
                                viewModel.selectCategory(
                                    if (selectedCategory?.name == category.name) null else category.name
                                )
                            },
                            text = { Text(category.displayName) },
                            icon = {
                                Icon(
                                    imageVector = category.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                    }
                }
            }

            // ── 消息列表 ────────────────────────────────────────────────
            // 三种模式：组合筛选 / 搜索 / Paging
            if (needsCombined && !isSearching) {
                // 组合筛选 (topic+category) — Flow<List>
                val items = combinedMessages
                if (items.isEmpty()) {
                    EmptyState(tab = selectedTab, isSearching = false, modifier = Modifier.fillMaxSize())
                } else {
                    FlowListMessageList(
                        messages = items,
                        onNavigateToDetail = onNavigateToDetail,
                        onMarkAsRead = { viewModel.markAsRead(it) },
                        isBatchMode = isBatchMode,
                        selectedIds = selectedIds,
                        onEnterBatchMode = { viewModel.enterBatchMode(it) },
                        onToggleSelection = { viewModel.toggleSelection(it) }
                    )
                }
            } else if (isSearching && searchQuery.isNotBlank()) {
                // 搜索 — Flow<List>
                val items = searchMessages
                if (items.isEmpty()) {
                    EmptyState(tab = selectedTab, isSearching = true, modifier = Modifier.fillMaxSize())
                } else {
                    FlowListMessageList(
                        messages = items,
                        onNavigateToDetail = onNavigateToDetail,
                        onMarkAsRead = { viewModel.markAsRead(it) },
                        isBatchMode = isBatchMode,
                        selectedIds = selectedIds,
                        onEnterBatchMode = { viewModel.enterBatchMode(it) },
                        onToggleSelection = { viewModel.toggleSelection(it) }
                    )
                }
            } else {
                // Paging — LazyPagingItems
                PagingMessageList(
                    pagingItems = pagingItems,
                    onNavigateToDetail = onNavigateToDetail,
                    onMarkAsRead = { viewModel.markAsRead(it) },
                    isBatchMode = isBatchMode,
                    selectedIds = selectedIds,
                    onEnterBatchMode = { viewModel.enterBatchMode(it) },
                    onToggleSelection = { viewModel.toggleSelection(it) }
                )
            }
        }
    }
}

// ── Paging 消息列表 ──────────────────────────────────────────
@Composable
private fun PagingMessageList(
    pagingItems: LazyPagingItems<MessageEntity>,
    onNavigateToDetail: (String) -> Unit,
    onMarkAsRead: (String) -> Unit,
    isBatchMode: Boolean = false,
    selectedIds: Set<String> = emptySet(),
    onEnterBatchMode: (String) -> Unit = {},
    onToggleSelection: (String) -> Unit = {}
) {
    when (pagingItems.loadState.refresh) {
        is LoadState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is LoadState.Error -> {
            val error = (pagingItems.loadState.refresh as LoadState.Error).error
            Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.CloudOff,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("加载失败", style = MaterialTheme.typography.titleMedium)
                    Text(
                        error.message ?: "未知错误",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    FilledTonalButton(onClick = { pagingItems.retry() }) { Text("重新加载") }
                }
            }
        }
        is LoadState.NotLoading -> {
            if (pagingItems.itemCount == 0) {
                EmptyState(tab = MessageTab.ALL, isSearching = false, modifier = Modifier.fillMaxSize())
            } else {
                LazyColumn {
                    items(
                        count = pagingItems.itemCount,
                        key = { index -> pagingItems[index]?.id ?: index }
                    ) { index ->
                        val message = pagingItems[index]
                        if (message != null) {
                            MessageCard(
                                message = message,
                                onClick = {
                                    if (isBatchMode) {
                                        onToggleSelection(message.id)
                                    } else {
                                        onMarkAsRead(message.id)
                                        onNavigateToDetail(message.id)
                                    }
                                },
                                onLongClick = { onEnterBatchMode(message.id) },
                                isBatchMode = isBatchMode,
                                isSelected = message.id in selectedIds
                            )
                        }
                    }

                    // 底部加载状态
                    pagingItems.apply {
                        when (loadState.append) {
                            is LoadState.Loading -> {
                                item {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) { CircularProgressIndicator(modifier = Modifier.size(24.dp)) }
                                }
                            }
                            is LoadState.Error -> {
                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text("加载更多失败", color = MaterialTheme.colorScheme.error)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        TextButton(onClick = { retry() }) { Text("重试") }
                                    }
                                }
                            }
                            else -> {} // NotLoading — 无操作
                        }
                    }
                }
            }
        }
    }
}

// ── Flow<List> 消息列表 (搜索/组合筛选) ──────────────────────
@Composable
private fun FlowListMessageList(
    messages: List<MessageEntity>,
    onNavigateToDetail: (String) -> Unit,
    onMarkAsRead: (String) -> Unit,
    isBatchMode: Boolean = false,
    selectedIds: Set<String> = emptySet(),
    onEnterBatchMode: (String) -> Unit = {},
    onToggleSelection: (String) -> Unit = {}
) {
    LazyColumn {
        itemsIndexed(
            items = messages,
            key = { _, it -> it.id }
        ) { index, message ->
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(
                    animationSpec = androidx.compose.animation.core.tween(
                        durationMillis = 250,
                        delayMillis = (index * 30).coerceAtMost(300)
                    )
                ) + slideInVertically(
                    animationSpec = androidx.compose.animation.core.tween(
                        durationMillis = 250,
                        delayMillis = (index * 30).coerceAtMost(300)
                    ),
                    initialOffsetY = { it / 8 }
                )
            ) {
                MessageCard(
                    message = message,
                    onClick = {
                        if (isBatchMode) {
                            onToggleSelection(message.id)
                        } else {
                            onMarkAsRead(message.id)
                            onNavigateToDetail(message.id)
                        }
                    },
                    onLongClick = { onEnterBatchMode(message.id) },
                    isBatchMode = isBatchMode,
                    isSelected = message.id in selectedIds
                )
            }
        }
    }
}

// ── 空状态组件 ──────────────────────────────

@Composable
private fun EmptyState(
    tab: MessageTab,
    isSearching: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = when {
                    isSearching -> Icons.Default.SearchOff
                    tab == MessageTab.UNREAD -> Icons.Default.MarkEmailRead
                    else -> Icons.Default.Inbox
                },
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = when {
                    isSearching -> "未找到匹配的消息"
                    tab == MessageTab.UNREAD -> "没有未读消息"
                    else -> "暂无事件"
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = when {
                    isSearching -> "尝试使用不同的关键词搜索"
                    tab == MessageTab.UNREAD -> "所有消息都已读过"
                    else -> "当事件源产生新的事件时，将在这里显示"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            if (!isSearching && tab == MessageTab.ALL) {
                Spacer(modifier = Modifier.height(24.dp))
                FilledTonalButton(
                    onClick = { },
                    shape = MaterialTheme.shapes.small
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("发送测试通知")
                }
            }
        }
    }
}

private fun formatSummaryTime(timestamp: Long): String {
    val now = System.currentTimeMillis() / 1000
    val diff = now - timestamp
    return when {
        diff < 60 -> "刚刚"
        diff < 3600 -> "${diff / 60}分钟前"
        diff < 86400 -> "${diff / 3600}小时前"
        else -> SimpleDateFormat("MM/dd", Locale.getDefault()).format(Date(timestamp * 1000))
    }
}
