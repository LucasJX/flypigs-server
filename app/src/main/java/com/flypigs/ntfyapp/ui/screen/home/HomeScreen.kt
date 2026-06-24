package com.flypigs.ntfyapp.ui.screen.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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
    val messages by viewModel.messages.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedTopic by viewModel.selectedTopic.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val topics by viewModel.topics.collectAsState()
    val totalCount by viewModel.totalCount.collectAsState()
    val unreadCount by viewModel.unreadCount.collectAsState()
    val topicUnreadCounts by viewModel.topicUnreadCounts.collectAsState()

    val scope = rememberCoroutineScope()
    val drawerHolder = LocalDrawerState.current

    // 写入 drawer 内容到 CompositionLocal（跨路由共享）
    DisposableEffect(Unit) {
        drawerHolder.value = drawerHolder.value.copy(
            content = @Composable {
                DrawerContent(
                    topics = topics,
                    totalCount = totalCount,
                    topicUnreadCounts = topicUnreadCounts,
                    selectedTopic = selectedTopic,
                    selectedCategory = selectedCategory,
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
            val subtitle = when {
                selectedTopic != null && selectedCategory != null ->
                    "${"$"}${selectedCategory!!.displayName}"
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
                ScrollableTabRow(
                    selectedTabIndex = if (selectedCategory == null) 0 else MessageCategory.entries.indexOf(selectedCategory) + 1,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    edgePadding = 16.dp
                ) {
                    Tab(
                        selected = selectedCategory == null,
                        onClick = { viewModel.selectCategory(null) },
                        text = { Text("全部") }
                    )
                    MessageCategory.entries.forEach { category ->
                        Tab(
                            selected = selectedCategory == category,
                            onClick = {
                                viewModel.selectCategory(
                                    if (selectedCategory == category) null else category
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

            // 消息列表
            if (messages.isEmpty()) {
                EmptyState(
                    tab = selectedTab,
                    isSearching = isSearching,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
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
                                    viewModel.markAsRead(message.id)
                                    onNavigateToDetail(message.id)
                                }
                            )
                        }
                    }
                }
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
            // 大图标
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

            // 主标题
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

            // 描述文字
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

            // 操作提示（仅全部消息空状态）
            if (!isSearching && tab == MessageTab.ALL) {
                Spacer(modifier = Modifier.height(24.dp))
                FilledTonalButton(
                    onClick = { /* 可以跳转到设置页添加订阅 */ },
                    shape = MaterialTheme.shapes.small
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("发送测试通知")
                }
            }
        }
    }
}

// ── 工具函数 ──────────────────────────────

private fun formatSummaryTime(timestamp: Long): String {
    val now = System.currentTimeMillis() / 1000
    val diff = now - timestamp

    return when {
        diff < 60 -> "刚刚"
        diff < 3600 -> "${diff / 60}分钟前"
        diff < 86400 -> "${diff / 3600}小时前"
        else -> {
            val sdf = SimpleDateFormat("MM/dd", Locale.getDefault())
            sdf.format(Date(timestamp * 1000))
        }
    }
}
