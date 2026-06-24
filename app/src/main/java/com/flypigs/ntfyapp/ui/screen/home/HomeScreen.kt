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
    val eventSummary by viewModel.eventSummary.collectAsState()

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
            // ── 事件摘要卡片 ──
            AnimatedVisibility(
                visible = !isSearching && selectedTab == MessageTab.ALL && selectedTopic == null,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 10 })
            ) {
                EventSummaryCard(
                    todayCount = eventSummary.todayCount,
                    unreadCount = eventSummary.unreadCount,
                    starredCount = eventSummary.starredCount,
                    latestMessage = eventSummary.latestMessage?.let {
                        LatestMessage(
                            title = it.title ?: it.topic,
                            time = formatSummaryTime(it.timestamp)
                        )
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

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
                Tab(
                    selected = selectedTab == MessageTab.STARRED,
                    onClick = { viewModel.selectTab(MessageTab.STARRED) },
                    text = { Text("加星") }
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
                                },
                                onLongClick = {
                                    viewModel.toggleStarred(message.id, !message.isStarred)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── 事件摘要卡片 ──────────────────────────────

data class LatestMessage(
    val title: String,
    val time: String
)

@Composable
private fun EventSummaryCard(
    todayCount: Int,
    unreadCount: Int,
    starredCount: Int,
    latestMessage: LatestMessage?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 标题行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "今日概览",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    Icons.Default.Analytics,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 统计行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SummaryItem(
                    count = todayCount,
                    label = "今日",
                    icon = Icons.Default.Today,
                    tint = MaterialTheme.colorScheme.primary
                )
                SummaryItem(
                    count = unreadCount,
                    label = "未读",
                    icon = Icons.Default.MarkEmailUnread,
                    tint = MaterialTheme.colorScheme.error
                )
                SummaryItem(
                    count = starredCount,
                    label = "加星",
                    icon = Icons.Default.Star,
                    tint = MaterialTheme.colorScheme.tertiary
                )
            }

            // 最新消息
            if (latestMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "最新: ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        latestMessage.title,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        latestMessage.time,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryItem(
    count: Int,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: androidx.compose.ui.graphics.Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = tint
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "$count",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
                    tab == MessageTab.STARRED -> Icons.Default.StarOutline
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
                    tab == MessageTab.STARRED -> "没有加星消息"
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
                    tab == MessageTab.STARRED -> "长按消息可以添加星标"
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
