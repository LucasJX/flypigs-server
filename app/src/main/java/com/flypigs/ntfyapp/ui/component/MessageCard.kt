package com.flypigs.ntfyapp.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.flypigs.ntfyapp.data.local.entity.MessageEntity
import com.flypigs.ntfyapp.domain.model.CategoryRegistry
import com.flypigs.ntfyapp.util.parseMarkdown
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageCard(
    message: MessageEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    isBatchMode: Boolean = false,
    isSelected: Boolean = false
) {
    val category = try {
        CategoryRegistry.getCategory(message.category)
    } catch (_: Exception) {
        CategoryRegistry.getCategory("其他")
    }

    val isDark = isSystemInDarkTheme()

    // ─── 优先级样式定义（自适应暗色模式）────────────────────
    val (borderColor, backgroundColor) = when (message.priority) {
        5 -> if (isDark) {
            Pair(Color(0xFFEF5350), Color(0xFF2D1515))   // 紧急暗色: 红边框 + 深红背景
        } else {
            Pair(Color(0xFFD32F2F), Color(0xFFFFEBEE))   // 紧急亮色
        }
        4 -> if (isDark) {
            Pair(Color(0xFFFFA726), Color(0xFF2D2010))   // 高暗色: 橙边框 + 深橙背景
        } else {
            Pair(Color(0xFFFF6F00), Color(0xFFFFF3E0))   // 高亮色
        }
        1, 2 -> if (isDark) {
            Pair(Color(0xFF616161), Color(0xFF212121))    // 低暗色
        } else {
            Pair(Color(0xFFE0E0E0), Color(0xFFF5F5F5))   // 低亮色
        }
        else -> if (isDark) {
            Pair(null, MaterialTheme.colorScheme.surfaceContainerLow)  // 默认暗色
        } else {
            Pair(null, Color.White)                                      // 默认亮色
        }
    }

    // ─── 已读/未读区分 ─────────────────────────────────
    val titleColor = if (message.isRead) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val titleWeight = if (message.isRead) FontWeight.Normal else FontWeight.Bold

    val shape = RoundedCornerShape(12.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .then(
                if (borderColor != null) {
                    Modifier.border(1.5.dp, borderColor, shape)
                } else if (!message.isRead) {
                    // 未读消息加左边竖线指示
                    Modifier
                } else {
                    Modifier
                }
            )
            .clip(shape)
            .background(backgroundColor)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // 批量模式复选框
            if (isBatchMode) {
                Icon(
                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = if (isSelected) "已选中" else "未选中",
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            // 未读指示点
            if (!message.isRead && !isBatchMode) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
            } else if (!isBatchMode) {
                // 已读占位，保持对齐
                Spacer(modifier = Modifier.width(16.dp))
            }

            // 分类图标
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = category.color.copy(alpha = 0.1f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = category.icon,
                    contentDescription = category.displayName,
                    tint = category.color,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                // 标题行
                Text(
                    text = message.title ?: message.topic,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = titleWeight,
                    color = titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // 摘要
                if (message.body.isNotBlank()) {
                    Text(
                        text = parseMarkdown(message.body.take(100)),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (message.isRead) {
                            MaterialTheme.colorScheme.outline
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 底部行: 时间 + 来源徽章
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatTime(message.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    SourceBadge(topic = message.topic)
                }
            }
        }
    }
}

@Composable
private fun SourceBadge(topic: String) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Text(
            text = topic,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun formatTime(timestamp: Long): String {
    val now = System.currentTimeMillis() / 1000
    val diff = now - timestamp

    return when {
        diff < 60 -> "刚刚"
        diff < 3600 -> "${diff / 60}分钟前"
        diff < 86400 -> "${diff / 3600}小时前"
        diff < 172800 -> "昨天"
        else -> {
            val sdf = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
            sdf.format(Date(timestamp * 1000))
        }
    }
}
