package com.flypigs.ntfyapp.ui.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * 侧边栏内容 — 独立 composable，供 MainActivity 的 ModalNavigationDrawer 使用
 *
 * 注意：不要在这里包裹 ModalDrawerSheet，因为 MainActivity 已经提供了外层 sheet 容器。
 * 这里只负责内容布局，insets 由各区域自行处理。
 */
@Composable
fun DrawerContent(
    topics: List<com.flypigs.ntfyapp.data.local.entity.TopicEntity>,
    totalCount: Int,
    topicUnreadCounts: Map<String, Int>,
    selectedTopic: String?,
    selectedCategory: com.flypigs.ntfyapp.domain.model.MessageCategory?,
    selectedTab: MessageTab,
    onSelectTopic: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surfaceContainer)
    ) {
        // ── 头部区域：背景色覆盖状态栏，内容用 windowInsetsPadding 避开 ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary)
                .windowInsetsPadding(WindowInsets.statusBars),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 24.dp)
            ) {
                Icon(
                    Icons.Default.Notifications,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Flypigs EventCenter",
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "统一管理事件通知",
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── 全部消息 ──
        NavigationDrawerItem(
            label = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("全部消息", fontWeight = FontWeight.Medium)
                    Text(
                        "$totalCount",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            icon = { Icon(Icons.Default.Inbox, contentDescription = null) },
            selected = selectedTopic == null && selectedCategory == null && selectedTab == MessageTab.ALL,
            onClick = { onSelectTopic(null) },
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        // ── Topic 列表 ──
        topics.forEach { topic ->
            val isTopicSelected = selectedTopic == topic.name
            val topicUnread = topicUnreadCounts[topic.name] ?: 0

            NavigationDrawerItem(
                label = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            topic.displayName,
                            fontWeight = if (isTopicSelected) FontWeight.Bold else FontWeight.Medium
                        )
                        if (topicUnread > 0) {
                            Badge { Text("$topicUnread") }
                        }
                    }
                },
                icon = { Icon(Icons.Default.Topic, contentDescription = null) },
                selected = isTopicSelected,
                onClick = { onSelectTopic(if (isTopicSelected) null else topic.name) },
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // ── 底部版本：背景覆盖导航栏，内容用 windowInsetsPadding 避开 ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "v1.0.0",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
