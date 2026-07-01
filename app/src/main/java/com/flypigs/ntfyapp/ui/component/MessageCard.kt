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
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.flypigs.ntfyapp.data.local.entity.MessageEntity
import com.flypigs.ntfyapp.domain.model.CategoryRegistry
import com.flypigs.ntfyapp.util.AttachmentUtils
import com.flypigs.ntfyapp.util.parseMarkdown
import kotlinx.coroutines.launch
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
    isSelected: Boolean = false,
    // v6 新增：附件回调（由 HomeScreen/DetailScreen 注入，导航到全屏 / 触发保存）
    onAttachmentClick: (url: String, name: String, mimeType: String?) -> Unit = { _, _, _ -> },
    onAttachmentLongClick: (url: String, name: String, mimeType: String?) -> Unit = { _, _, _ -> }
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

                // ── 附件渲染 (v6 新增) ────────────────────────────
                // - 图片类型（image/*）：用 Coil SubcomposeAsyncImage 渲染缩略图
                //   - 单击进入全屏查看器
                //   - 长按保存到相册（走 AttachmentUtils）
                // - 其他类型（pdf/zip/...）：显示附件 chip（文件名 + 大小）
                //   - 单击触发下载
                // - 暗色模式：图片周围用 surfaceContainerLow 衬底，避免深色背景"吞噬"图片
                val attachmentUrl = message.attachmentUrl
                if (!attachmentUrl.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    if (message.attachmentType?.startsWith("image/") == true) {
                        AttachmentImage(
                            url = attachmentUrl,
                            name = message.attachmentName.orEmpty(),
                            mimeType = message.attachmentType,
                            dark = isDark,
                            onClick = { onAttachmentClick(attachmentUrl, message.attachmentName.orEmpty(), message.attachmentType) },
                            onLongClick = { onAttachmentLongClick(attachmentUrl, message.attachmentName.orEmpty(), message.attachmentType) }
                        )
                    } else {
                        AttachmentChip(
                            name = message.attachmentName.orEmpty(),
                            size = message.attachmentSize ?: 0L,
                            type = message.attachmentType,
                            onClick = { onAttachmentClick(attachmentUrl, message.attachmentName.orEmpty(), message.attachmentType) }
                        )
                    }
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

// ─── 附件渲染组件 (v6 新增) ────────────────────────────────────

/**
 * 图片附件缩略图
 * - 暗色模式用 surfaceContainerLow 衬底（避免深色背景吃图）
 * - 加载中：浅色 placeholder + 居中图标
 * - 加载失败：占位图 + 重试 icon（点击重试）
 * - maxHeight 280.dp 防止超长图撑爆卡片
 * - v6: 加 onClick / onLongClick 让 HomeScreen 注入行为（进入全屏 / 保存相册）
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AttachmentImage(
    url: String,
    name: String,
    mimeType: String?,
    dark: Boolean,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {}
) {
    val shape = RoundedCornerShape(12.dp)
    val containerColor = MaterialTheme.colorScheme.surfaceContainerLow

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 280.dp)
            .clip(shape)
            .background(containerColor)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                .data(url)
                .crossfade(true)
                .build(),
            contentDescription = name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 280.dp)
                .clip(shape),
            loading = {
                Box(
                    modifier = Modifier.fillMaxWidth().height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "加载中…",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            error = {
                Box(
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Outlined.Image,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "图片加载失败",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            name,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        )
    }
}

/**
 * 非图片附件 chip（pdf/zip/视频/音频/...）
 * 显示：📎 文件名 + 大小
 * v6: 加 onClick（由 HomeScreen 注入，触发下载到 Download 目录）
 */
@Composable
private fun AttachmentChip(
    name: String,
    size: Long,
    type: String?,
    onClick: () -> Unit = {}
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.AttachFile,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (size > 0) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = formatSize(size),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

private fun formatSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    bytes < 1024 * 1024 * 1024 -> "${bytes / 1024 / 1024} MB"
    else -> "%.1f GB".format(bytes / 1024.0 / 1024.0 / 1024.0)
}
