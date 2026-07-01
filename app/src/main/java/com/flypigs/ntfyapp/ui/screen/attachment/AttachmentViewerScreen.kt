package com.flypigs.ntfyapp.ui.screen.attachment

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.flypigs.ntfyapp.util.AttachmentUtils
import kotlinx.coroutines.launch
import me.saket.telephoto.zoomable.coil.ZoomableAsyncImage

/**
 * 全屏图片查看器 (v6 新增)
 *
 * 特性：
 * - 沉浸式（黑色背景 + Edge-to-Edge）
 * - 双指缩放 / 双击缩放 / 拖动平移（Telephoto ZoomableImage）
 * - 长按图片 → 弹底部动作表（保存相册）
 * - 顶部 ActionBar：关闭 + 保存
 * - 沉浸式状态栏：黑色 statusBar
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttachmentViewerScreen(
    imageUrl: String,
    imageName: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showSaveSheet by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    // 系统返回键 → 关闭查看器
    BackHandler { onNavigateBack() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // ── 主体：可缩放图片 ────────────────────────
        ZoomableAsyncImage(
            model = imageUrl,
            contentDescription = imageName,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = { showSaveSheet = true }
                    )
                }
        )

        // ── 顶部关闭按钮 ────────────────────────────
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(8.dp)
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "关闭",
                tint = Color.White
            )
        }

        // ── 顶部保存按钮 ────────────────────────────
        IconButton(
            onClick = { showSaveSheet = true },
            enabled = !isSaving,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(8.dp)
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    Icons.Outlined.Save,
                    contentDescription = "保存到相册",
                    tint = Color.White
                )
            }
        }

        // ── 底部文件名标签 ──────────────────────────
        Surface(
            color = Color.Black.copy(alpha = 0.6f),
            shape = MaterialTheme.shapes.small,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = imageName,
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }

    // ── 保存动作表 ──────────────────────────────────
    if (showSaveSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSaveSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                ListItem(
                    headlineContent = { Text("保存到相册") },
                    leadingContent = {
                        Icon(Icons.Outlined.Save, contentDescription = null)
                    },
                    modifier = Modifier.clickable {
                        showSaveSheet = false
                        isSaving = true
                        scope.launch {
                            val result = AttachmentUtils.saveImageToGallery(context, imageUrl, imageName)
                            isSaving = false
                            result
                                .onSuccess {
                                    AttachmentUtils.showSavedToast(context, "相册/Flypigs")
                                }
                                .onFailure { e ->
                                    AttachmentUtils.showErrorToast(context, "保存失败: ${e.message ?: "未知错误"}")
                                }
                        }
                    }
                )
                ListItem(
                    headlineContent = { Text("分享图片") },
                    leadingContent = {
                        Icon(Icons.Default.Share, contentDescription = null)
                    },
                    modifier = Modifier.clickable {
                        showSaveSheet = false
                        scope.launch {
                            // 先下载到 cache，再分享
                            val result = AttachmentUtils.downloadToDownloads(
                                context, imageUrl, imageName, "image/*"
                            )
                            result.onSuccess { uri ->
                                val share = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "image/*"
                                    putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(
                                    android.content.Intent.createChooser(share, "分享图片")
                                )
                            }.onFailure { e ->
                                AttachmentUtils.showErrorToast(context, "分享失败: ${e.message}")
                            }
                        }
                    }
                )
            }
        }
    }
}