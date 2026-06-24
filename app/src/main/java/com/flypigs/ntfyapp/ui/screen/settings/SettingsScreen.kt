package com.flypigs.ntfyapp.ui.screen.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.flypigs.ntfyapp.ui.component.CenteredTopAppBar
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.flypigs.ntfyapp.ui.theme.ThemeMode
import androidx.hilt.navigation.compose.hiltViewModel


// ─── 优化项 ─────────────────────────────────────────────────
data class OptimizationItem(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val check: (Context) -> Boolean,
    val action: (Context) -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // 主题设置
    val prefs = remember { context.getSharedPreferences("ntfy_prefs", Context.MODE_PRIVATE) }
    var selectedTheme by remember { mutableStateOf(ThemeMode.valueOf(prefs.getString("theme_mode", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name)) }
    fun setTheme(mode: ThemeMode) {
        selectedTheme = mode
        prefs.edit().putString("theme_mode", mode.name).apply()
    }

    // 优化项状态
    var showOptimizationGuide by remember { mutableStateOf(false) }
    var optimizationStates by remember { mutableStateOf(emptyMap<Int, Boolean>()) }

    val optimizationItems = remember {
        listOf(
            OptimizationItem(
                title = "通知权限",
                description = "允许应用发送通知",
                icon = Icons.Default.Notifications,
                check = { ctx ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        ctx.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                                android.content.pm.PackageManager.PERMISSION_GRANTED
                    } else true
                },
                action = { ctx ->
                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
                    }
                    ctx.startActivity(intent)
                }
            ),
            OptimizationItem(
                title = "忽略电池优化",
                description = "允许应用在后台运行",
                icon = Icons.Default.BatteryChargingFull,
                check = { ctx ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        val pm = ctx.getSystemService(Context.POWER_SERVICE) as PowerManager
                        pm.isIgnoringBatteryOptimizations(ctx.packageName)
                    } else true
                },
                action = { ctx ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        try {
                            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                data = Uri.parse("package:${'$'}{ctx.packageName}")
                            }
                            ctx.startActivity(intent)
                        } catch (_: Exception) {
                            ctx.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                        }
                    }
                }
            )
        )
    }

    fun refreshOptimizationStates() {
        optimizationStates = optimizationItems.mapIndexed { index, item ->
            index to item.check(context)
        }.toMap()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshOptimizationStates()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) { refreshOptimizationStates() }

    val servers by viewModel.servers.collectAsState()
    val topics by viewModel.topics.collectAsState()

    var showAddServerDialog by remember { mutableStateOf(false) }
    var showAddTopicDialog by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    // Add server dialog state
    var serverName by remember { mutableStateOf("") }
    var serverUrl by remember { mutableStateOf("") }
    var serverUsername by remember { mutableStateOf("") }
    var serverPassword by remember { mutableStateOf("") }

    // Add topic dialog state
    var topicName by remember { mutableStateOf("") }
    var topicDisplayName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            CenteredTopAppBar(title = "设置")
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Server management section
            Text(
                text = "服务器",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            servers.forEach { server ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = server.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = server.url,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (server.isConnected) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                        contentDescription = null,
                                        tint = if (server.isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (server.isConnected) "已连接" else "未连接",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (server.isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            IconButton(onClick = { viewModel.deleteServer(server.id) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    "删除",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
            OutlinedButton(
                onClick = { showAddServerDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("添加服务器")
            }

            HorizontalDivider()

            // Topic management section
            Text(
                text = "订阅 Topic",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            topics.forEach { topic ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = topic.displayName,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = topic.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = topic.isEnabled,
                            onCheckedChange = { viewModel.toggleTopic(topic.id, it) }
                        )
                        IconButton(onClick = { viewModel.deleteTopic(topic.id) }) {
                            Icon(
                                Icons.Default.Delete,
                                "删除",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
            OutlinedButton(
                onClick = { showAddTopicDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("添加 Topic")
            }


            HorizontalDivider()

            // ─── 后台消息优化 ──────────────────────────────
            val allOptimized = optimizationStates.values.all { it }
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (allOptimized)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.errorContainer
                ),
                onClick = { showOptimizationGuide = true }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        if (allOptimized) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (allOptimized) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(32.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (allOptimized) "优化建议已完成" else "后台消息优化建议",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (allOptimized) "所有优化项已设置" else "点击查看详情并优化",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            HorizontalDivider()

            // Data management section
            Text(
                text = "数据管理",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            OutlinedButton(
                onClick = { showClearConfirmDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("清除历史消息")
            }

            // ─── 主题切换 ──────────────────────────────────
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("外观主题", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    ThemeMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = ThemeMode.entries.size),
                            onClick = { setTheme(mode) },
                            selected = selectedTheme == mode,
                            label = { Text(mode.label) }
                        )
                    }
                }
            }
        }
    }


    // ─── 优化建议弹窗 ────────────────────────────────────────
    if (showOptimizationGuide) {
        AlertDialog(
            onDismissRequest = { showOptimizationGuide = false },
            title = { Text("后台消息优化", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("为确保后台正常接收消息，请完成以下设置：",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    optimizationItems.forEachIndexed { index, item ->
                        val isOptimized = optimizationStates[index] ?: false
                        Card(modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isOptimized) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(modifier = Modifier.size(24.dp).clip(CircleShape)
                                    .background(if (isOptimized) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error),
                                    contentAlignment = Alignment.Center) {
                                    Icon(if (isOptimized) Icons.Default.Check else Icons.Default.Close,
                                        null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.title, fontWeight = FontWeight.Bold)
                                    Text(item.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                FilledTonalButton(
                                    onClick = { item.action(context) },
                                    enabled = !isOptimized,
                                    modifier = Modifier.height(32.dp)
                                ) { Text(if (isOptimized) "已完成" else "去设置", fontSize = 12.sp) }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showOptimizationGuide = false }) { Text("完成") } }
        )
    }

    // Add Server Dialog
    if (showAddServerDialog) {
        AlertDialog(
            onDismissRequest = { showAddServerDialog = false },
            title = { Text("添加服务器") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = serverName,
                        onValueChange = { serverName = it },
                        label = { Text("名称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = serverUrl,
                        onValueChange = { serverUrl = it },
                        label = { Text("服务器地址") },
                        placeholder = { Text("http://192.168.1.100") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = serverUsername,
                        onValueChange = { serverUsername = it },
                        label = { Text("用户名") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = serverPassword,
                        onValueChange = { serverPassword = it },
                        label = { Text("密码") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (serverName.isNotBlank() && serverUrl.isNotBlank()) {
                            viewModel.addServer(
                                url = serverUrl,
                                name = serverName,
                                username = serverUsername.ifBlank { null },
                                password = serverPassword.ifBlank { null }
                            )
                            serverName = ""
                            serverUrl = ""
                            serverUsername = ""
                            serverPassword = ""
                            showAddServerDialog = false
                        }
                    }
                ) { Text("添加") }
            },
            dismissButton = {
                TextButton(onClick = { showAddServerDialog = false }) { Text("取消") }
            }
        )
    }

    // Add Topic Dialog
    if (showAddTopicDialog) {
        AlertDialog(
            onDismissRequest = { showAddTopicDialog = false },
            title = { Text("添加 Topic") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = topicName,
                        onValueChange = { topicName = it },
                        label = { Text("Topic 名称") },
                        placeholder = { Text("eventcenter") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = topicDisplayName,
                        onValueChange = { topicDisplayName = it },
                        label = { Text("显示名称") },
                        placeholder = { Text("事件中心") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (topicName.isNotBlank() && topicDisplayName.isNotBlank() && servers.isNotEmpty()) {
                            viewModel.addTopic(
                                serverId = servers.first().id,
                                name = topicName,
                                displayName = topicDisplayName
                            )
                            topicName = ""
                            topicDisplayName = ""
                            showAddTopicDialog = false
                        }
                    }
                ) { Text("添加") }
            },
            dismissButton = {
                TextButton(onClick = { showAddTopicDialog = false }) { Text("取消") }
            }
        )
    }

    // Clear confirm dialog
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("确认清除") },
            text = { Text("确定要清除所有历史消息吗？此操作不可恢复。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllMessages()
                        showClearConfirmDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("清除") }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) { Text("取消") }
            }
        )
    }
}