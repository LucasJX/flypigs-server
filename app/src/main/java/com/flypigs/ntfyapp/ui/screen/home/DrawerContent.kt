package com.flypigs.ntfyapp.ui.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Topic 品牌色 — 暗色模式自动提亮
 * 不用硬编码白底，按数据源类型给颜色，让 Drawer 一眼能区分来源
 */
private object TopicBrandColors {
    private val isDark = false  // 占位，实际用 isSystemInDarkTheme()

    fun openwrt(dark: Boolean): Pair<Color, Color> =
        if (dark) Color(0xFF4DD0E1) to Color(0xFF1A2E2E)  // 亮青 + 深青底
        else Color(0xFF00ADD8) to Color(0xFFE0F7FA)         // OpenWrt 青 + 浅青底

    fun telegram(dark: Boolean): Pair<Color, Color> =
        if (dark) Color(0xFF42A5F5) to Color(0xFF0D1F33)   // 亮蓝 + 深蓝底
        else Color(0xFF0088CC) to Color(0xFFE3F2FD)         // Telegram 蓝 + 浅蓝底

    /** 根据 topic name 返回 (foreground, background)；未知返回 null 用默认 */
    fun forTopic(name: String, dark: Boolean): Pair<Color, Color>? {
        val lower = name.lowercase()
        return when {
            "openwrt" in lower -> openwrt(dark)
            "telegram" in lower -> telegram(dark)
            else -> null
        }
    }
}

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
    val isDark = isSystemInDarkTheme()
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
                // 修复 Bug1: 项间 8dp 间距，避免胶囊挨在一起
                // 修复 Bug2: 按 topic name 自动套品牌色（OpenWrt 青 / Telegram 蓝），暗色模式自动适配
                topics.forEachIndexed { index, topic ->
                    val isTopicSelected = selectedTopic == topic.name
                    val topicUnread = topicUnreadCounts[topic.name] ?: 0
                    val brandColors = TopicBrandColors.forTopic(topic.name, isDark)
                    val iconColor = if (isTopicSelected) {
                        brandColors?.first ?: MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        brandColors?.first ?: MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    val containerColor = when {
                        isTopicSelected && brandColors != null -> brandColors.second
                        isTopicSelected -> MaterialTheme.colorScheme.secondaryContainer
                        brandColors != null -> brandColors.second.copy(alpha = 0.35f)
                        else -> Color.Transparent
                    }

                    if (index > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    NavigationDrawerItem(
                        label = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    topic.displayName,
                                    fontWeight = if (isTopicSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isTopicSelected) {
                                        brandColors?.first ?: MaterialTheme.colorScheme.onSecondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                                if (topicUnread > 0) {
                                    Badge { Text("$topicUnread") }
                                }
                            }
                        },
                        icon = {
                            Icon(
                                Icons.Default.Topic,
                                contentDescription = null,
                                tint = iconColor
                            )
                        },
                        selected = isTopicSelected,
                        onClick = { onSelectTopic(if (isTopicSelected) null else topic.name) },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = containerColor,
                            unselectedContainerColor = containerColor,
                            selectedIconColor = iconColor,
                            unselectedIconColor = iconColor
                        ),
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
