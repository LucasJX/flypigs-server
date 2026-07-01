package com.flypigs.ntfyapp.ui.screen.detail

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.flypigs.ntfyapp.ui.component.CenteredTopAppBar
import com.flypigs.ntfyapp.ui.component.MessageCard
import com.flypigs.ntfyapp.util.AttachmentUtils
import com.flypigs.ntfyapp.util.parseMarkdown
import androidx.hilt.navigation.compose.hiltViewModel
import com.flypigs.ntfyapp.domain.model.CategoryRegistry
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: DetailViewModel = hiltViewModel(),
    // v6 新增
    onNavigateToAttachment: (url: String, name: String) -> Unit = { _, _ -> }
) {
    val message by viewModel.message.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showDeleteDialog by remember { mutableStateOf(false) }

    // v6: 附件处理
    val handleAttachmentClick: (String, String, String?) -> Unit = { url, name, _ ->
        onNavigateToAttachment(url, name)
    }
    val handleAttachmentLongClick: (String, String, String?) -> Unit = { url, name, mime ->
        if (mime?.startsWith("image/") == true) {
            scope.launch {
                AttachmentUtils.saveImageToGallery(context, url, name)
                    .onSuccess { AttachmentUtils.showSavedToast(context, "相册/Flypigs") }
                    .onFailure { AttachmentUtils.showErrorToast(context, "保存失败: ${it.message ?: "未知"}") }
            }
        } else {
            scope.launch {
                AttachmentUtils.downloadToDownloads(context, url, name, mime)
                    .onSuccess { AttachmentUtils.showSavedToast(context, "Download/Flypigs") }
                    .onFailure { AttachmentUtils.showErrorToast(context, "下载失败: ${it.message ?: "未知"}") }
            }
        }
    }

    Scaffold(
        topBar = {
            CenteredTopAppBar(
                title = "消息详情",
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { padding ->
        val msg = message
        if (msg == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val category = try {
                CategoryRegistry.getCategory(msg.category)
            } catch (_: Exception) {
                CategoryRegistry.getCategory("其他")
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header with icon and title
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = category.icon,
                        contentDescription = category.displayName,
                        tint = category.color,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = msg.title ?: msg.topic,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Message body
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Text(
                        text = parseMarkdown(msg.body),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                // v6: 附件预览 — 复用 MessageCard 的附件渲染逻辑
                if (!msg.attachmentUrl.isNullOrBlank()) {
                    MessageCard(
                        message = msg,
                        onClick = { /* 在详情页点击附件不触发跳转 */ },
                        onAttachmentClick = handleAttachmentClick,
                        onAttachmentLongClick = handleAttachmentLongClick
                    )
                }

                HorizontalDivider()

                // Metadata
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetadataRow("来源", msg.topic)
                    MetadataRow("时间", formatFullTime(msg.timestamp))
                    MetadataRow("分类", category.displayName)
                    MetadataRow("优先级", getPriorityText(msg.priority))
                    if (!msg.tags.isNullOrBlank()) {
                        MetadataRow("标签", msg.tags)
                    }
                }

                HorizontalDivider()

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Copy
                    FilledTonalButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("ntfy_message", msg.body)
                            clipboard.setPrimaryClip(clip)
                        }
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("复制")
                    }

                    // Share
                    FilledTonalButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, msg.title ?: msg.topic)
                                putExtra(Intent.EXTRA_TEXT, msg.body)
                            }
                            context.startActivity(Intent.createChooser(intent, "分享消息"))
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("分享")
                    }

                    // Delete
                    FilledTonalButton(
                        onClick = { showDeleteDialog = true },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("删除")
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除这条消息吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteMessage()
                        showDeleteDialog = false
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun MetadataRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(60.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

private fun formatFullTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(timestamp * 1000))
}

private fun getPriorityText(priority: Int): String {
    return when (priority) {
        1 -> "最低"
        2 -> "低"
        3 -> "默认"
        4 -> "高"
        5 -> "紧急"
        else -> "默认"
    }
}


