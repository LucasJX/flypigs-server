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
 * 侧边栏内容 — M3 标准布局
 *
 * 紫色头部延伸到状态栏后面（背景在最外层 Box，padding 在内容层）
 * 内容区域用 NavigationDrawerItem（M3 原生组件）
 */
@Composable
fun DrawerContent(
    topics: List<com.flypigs.ntfyapp.data.local.entity.TopicEntity>,
    totalCount: Int,
    topicUnreadCounts: Map<String, Int>,
    selectedTopic: String?,
    selectedCategory: String?,
    selectedTab: MessageTab,
    onSelectTopic: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    // 最外层 Box：紫色背景延伸到状态栏后面
    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.primary)
    ) {
        Column(
            modifier = Modifier.fillMaxHeight()
        ) {
            // ── 头部区域：padding 避开状态栏，紫色背景已由外层 Box 提供 ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
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

            // ── 内容区域：surfaceContainer 背景 ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
            ) {
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

                // ── 底部版本：避开系统导航栏 ──
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
    }
}
