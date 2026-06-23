package com.flypigs.ntfyapp.ui.screen.settings

import androidx.compose.foundation.layout.*
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
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val servers by viewModel.servers.collectAsState()
    val topics by viewModel.topics.collectAsState()

    var showAddServerDialog by remember { mutableStateOf(false) }
    var showAddTopicDialog by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    // Add server dialog state
    var serverName by remember { mutableStateOf("") }
    var serverUrl by remember { mutableStateOf("") }
    var serverToken by remember { mutableStateOf("") }

    // Add topic dialog state
    var topicName by remember { mutableStateOf("") }
    var topicDisplayName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
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
        }
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
                        value = serverToken,
                        onValueChange = { serverToken = it },
                        label = { Text("Token (可选)") },
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
                                token = serverToken.ifBlank { null }
                            )
                            serverName = ""
                            serverUrl = ""
                            serverToken = ""
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
