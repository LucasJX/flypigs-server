package com.flypigs.ntfyapp.ui.screen.stats

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.flypigs.ntfyapp.ui.component.CenteredTopAppBar
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.flypigs.ntfyapp.ui.component.BarChart
import com.flypigs.ntfyapp.ui.component.LineChart
import com.flypigs.ntfyapp.ui.component.PieChart

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: StatsViewModel = hiltViewModel()
) {
    val todayCount by viewModel.todayCount.collectAsState()
    val totalCount by viewModel.totalCount.collectAsState()
    val unreadCount by viewModel.unreadCount.collectAsState()
    val categoryStats by viewModel.categoryStats.collectAsState()
    val dailyStats by viewModel.dailyStats.collectAsState()
    val topicStats by viewModel.topicStats.collectAsState()

    Scaffold(
        topBar = {
            CenteredTopAppBar(title = "分析")
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // ── 数据摘要卡片行 ──
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 10 })
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatSummaryCard(
                        count = todayCount,
                        label = "今日消息",
                        icon = Icons.Default.Today,
                        tint = MaterialTheme.colorScheme.primary,
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        modifier = Modifier.weight(1f)
                    )
                    StatSummaryCard(
                        count = totalCount,
                        label = "全部消息",
                        icon = Icons.Default.Inbox,
                        tint = MaterialTheme.colorScheme.secondary,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                        modifier = Modifier.weight(1f)
                    )
                    StatSummaryCard(
                        count = unreadCount,
                        label = "未读消息",
                        icon = Icons.Default.MarkEmailUnread,
                        tint = MaterialTheme.colorScheme.error,
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ── 消息分类饼图 ──
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(
                    animationSpec = androidx.compose.animation.core.tween(
                        durationMillis = 300,
                        delayMillis = 100
                    )
                ) + slideInVertically(
                    animationSpec = androidx.compose.animation.core.tween(
                        durationMillis = 300,
                        delayMillis = 100
                    ),
                    initialOffsetY = { it / 8 }
                )
            ) {
                ChartSection(
                    title = "消息分类",
                    icon = Icons.Default.PieChart
                ) {
                    PieChart(
                        data = categoryStats,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                }
            }

            // ── 7天趋势折线图 ──
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(
                    animationSpec = androidx.compose.animation.core.tween(
                        durationMillis = 300,
                        delayMillis = 200
                    )
                ) + slideInVertically(
                    animationSpec = androidx.compose.animation.core.tween(
                        durationMillis = 300,
                        delayMillis = 200
                    ),
                    initialOffsetY = { it / 8 }
                )
            ) {
                ChartSection(
                    title = "7天趋势",
                    icon = Icons.Default.ShowChart
                ) {
                    LineChart(
                        data = dailyStats,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                }
            }

            // ── Topic 消息量柱状图 ──
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(
                    animationSpec = androidx.compose.animation.core.tween(
                        durationMillis = 300,
                        delayMillis = 300
                    )
                ) + slideInVertically(
                    animationSpec = androidx.compose.animation.core.tween(
                        durationMillis = 300,
                        delayMillis = 300
                    ),
                    initialOffsetY = { it / 8 }
                )
            ) {
                ChartSection(
                    title = "Topic 消息量",
                    icon = Icons.Default.BarChart
                ) {
                    BarChart(
                        data = topicStats,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ── 数据摘要卡片 ──────────────────────────────

@Composable
private fun StatSummaryCard(
    count: Int,
    label: String,
    icon: ImageVector,
    tint: androidx.compose.ui.graphics.Color,
    containerColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = tint
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ── 图表区块 ──────────────────────────────────

@Composable
private fun ChartSection(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            shape = MaterialTheme.shapes.medium
        ) {
            content()
        }
    }
}
