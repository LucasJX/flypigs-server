# ntfy Android App — Phase 1 实现 Spec

## ⚠️ 重要：这是全新的 Android 项目
- 在 `/home/flypigs/ntfy-app/` 目录创建 Android 项目
- **不要修改** `/home/flypigs/luci-app-eventcenter/` 中的任何文件
- 这是一个独立的 Android App，不是 EventCenter 插件

## 设计文档
- 架构设计：`/home/flypigs/ntfy-app/docs/DESIGN.md`
- UI 设计：`/home/flypigs/ntfy-app/docs/UI_DESIGN.md`

## Phase 1 目标：基础框架 + 消息列表

### 1. 项目结构
```
ntfy-app/
├── app/
│   ├── src/main/
│   │   ├── java/com/flypigs/ntfyapp/
│   │   │   ├── di/AppModule.kt
│   │   │   ├── data/
│   │   │   │   ├── local/
│   │   │   │   │   ├── AppDatabase.kt
│   │   │   │   │   ├── dao/MessageDao.kt
│   │   │   │   │   └── entity/MessageEntity.kt
│   │   │   │   ├── remote/
│   │   │   │   │   ├── NtfyWebSocket.kt
│   │   │   │   │   └── NtfyMessage.kt
│   │   │   │   └── repository/MessageRepository.kt
│   │   │   ├── domain/model/
│   │   │   │   ├── Message.kt
│   │   │   │   └── MessageCategory.kt
│   │   │   ├── service/NtfyService.kt
│   │   │   ├── ui/
│   │   │   │   ├── theme/
│   │   │   │   │   ├── Color.kt
│   │   │   │   │   ├── Type.kt
│   │   │   │   │   └── Theme.kt
│   │   │   │   ├── screen/home/
│   │   │   │   │   ├── HomeScreen.kt
│   │   │   │   │   └── HomeViewModel.kt
│   │   │   │   ├── component/
│   │   │   │   │   ├── MessageCard.kt
│   │   │   │   │   └── CategoryChip.kt
│   │   │   │   └── MainActivity.kt
│   │   ├── res/
│   │   │   ├── values/strings.xml
│   │   │   └── mipmap-*/
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

### 2. 技术栈版本
```
Kotlin: 1.9.22
Compose BOM: 2024.02.00
Material 3: 1.2.0
Room: 2.6.1
OkHttp: 4.12.0
Hilt: 2.50
Lifecycle: 2.7.0
Navigation: 2.7.7
```

### 3. 核心实现

#### 3.1 数据模型
```kotlin
// MessageEntity.kt
@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val topic: String,
    val title: String?,
    val body: String,
    val priority: Int,        // 1-5
    val tags: String?,        // 逗号分隔
    val timestamp: Long,
    val isRead: Boolean = false,
    val category: String      // NODE_CHANGE, SYSTEM_ALERT, RECOVERY, UPDATE, OTHER
)

// MessageCategory.kt
enum class MessageCategory(val displayName: String, val icon: String, val color: Long) {
    NODE_CHANGE("节点变更", "📊", 0xFF1976D2),
    SYSTEM_ALERT("系统告警", "⚠️", 0xFFD32F2F),
    RECOVERY("恢复通知", "✅", 0xFF388E3C),
    UPDATE("更新通知", "📦", 0xFFF57C00),
    OTHER("其他", "💬", 0xFF757575);
}
```

#### 3.2 ntfy WebSocket 连接
```kotlin
// NtfyWebSocket.kt
class NtfyWebSocket(
    private val serverUrl: String,
    private val topic: String,
    private val token: String? = null,
    private val onMessage: (NtfyMessage) -> Unit
) {
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()
    
    private var webSocket: WebSocket? = null
    
    fun connect() {
        val wsUrl = serverUrl.replace("http://", "ws://").replace("https://", "wss://")
        val request = Request.Builder()
            .url("$wsUrl/$topic/ws")
            .apply { token?.let { addHeader("Authorization", "Bearer $it") } }
            .build()
        
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                val message = Gson().fromJson(text, NtfyMessage::class.java)
                if (message.event == "message") {
                    onMessage(message)
                }
            }
            
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                // 重连逻辑：5s, 10s, 15s, 30s, 60s
                Handler(Looper.getMainLooper()).postDelayed({ connect() }, 5000)
            }
        })
    }
    
    fun disconnect() {
        webSocket?.close(1000, "App closed")
    }
}

// NtfyMessage.kt
data class NtfyMessage(
    val id: String = "",
    val time: Long = 0,
    val event: String = "",
    val topic: String = "",
    val title: String? = null,
    val message: String = "",
    val priority: Int? = null,
    val tags: List<String>? = null
)
```

#### 3.3 Room 数据库
```kotlin
// AppDatabase.kt
@Database(entities = [MessageEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
}

// MessageDao.kt
@Dao
interface MessageDao {
    @Query("SELECT * FROM messages ORDER BY timestamp DESC")
    fun getAllMessages(): Flow<List<MessageEntity>>
    
    @Query("SELECT * FROM messages WHERE category = :category ORDER BY timestamp DESC")
    fun getMessagesByCategory(category: String): Flow<List<MessageEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)
    
    @Query("UPDATE messages SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: String)
    
    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteMessage(id: String)
}
```

#### 3.4 消息分类逻辑
```kotlin
// 根据 tags 自动分类
fun classifyMessage(tags: List<String>?): MessageCategory {
    if (tags == null) return MessageCategory.OTHER
    
    val tagSet = tags.map { it.lowercase() }.toSet()
    
    return when {
        tagSet.any { it in listOf("node", "change", "上线", "下线") } -> MessageCategory.NODE_CHANGE
        tagSet.any { it in listOf("alert", "warning", "error", "故障") } -> MessageCategory.SYSTEM_ALERT
        tagSet.any { it in listOf("recovery", "ok", "恢复", "正常") } -> MessageCategory.RECOVERY
        tagSet.any { it in listOf("update", "release", "版本") } -> MessageCategory.UPDATE
        else -> MessageCategory.OTHER
    }
}
```

#### 3.5 ForegroundService
```kotlin
// NtfyService.kt
@AndroidEntryPoint
class NtfyService : Service() {
    
    @Inject lateinit var repository: MessageRepository
    
    private var webSocket: NtfyWebSocket? = null
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createServiceNotification())
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val serverUrl = intent?.getStringExtra("server_url") ?: DEFAULT_SERVER
        val topic = intent?.getStringExtra("topic") ?: DEFAULT_TOPIC
        val token = intent?.getStringExtra("token")
        
        webSocket = NtfyWebSocket(serverUrl, topic, token) { message ->
            // 保存到数据库
            lifecycleScope.launch {
                repository.insertMessage(message.toEntity())
                // 发送系统通知
                showNotification(message)
            }
        }
        webSocket?.connect()
        
        return START_STICKY
    }
    
    override fun onDestroy() {
        webSocket?.disconnect()
        super.onDestroy()
    }
    
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "ntfy 通知",
            NotificationManager.IMPORTANCE_HIGH
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
    
    private fun showNotification(message: NtfyMessage) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(message.title ?: message.topic)
            .setContentText(message.message)
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
```

#### 3.6 UI 组件
```kotlin
// MessageCard.kt
@Composable
fun MessageCard(
    message: MessageEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val category = MessageCategory.valueOf(message.category)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onClick() }
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 分类图标
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = Color(category.color).copy(alpha = 0.1f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(category.icon, fontSize = 20.sp)
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                // 标题
                Text(
                    text = message.title ?: message.topic,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (!message.isRead) FontWeight.Bold else FontWeight.Normal
                )
                
                // 摘要
                Text(
                    text = message.body.take(100),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // 时间和来源
                Row {
                    Text(
                        text = formatTime(message.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = message.topic,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

// CategoryChip.kt
@Composable
fun CategoryChip(
    category: MessageCategory?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Text(category?.displayName ?: "全部")
        },
        leadingIcon = if (isSelected) {
            { Icon(Icons.Filled.Done, contentDescription = null, modifier = Modifier.size(18.dp)) }
        } else null
    )
}
```

#### 3.7 HomeScreen
```kotlin
// HomeScreen.kt
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ntfy 通知") },
                actions = {
                    IconButton(onClick = { showSearch = !showSearch }) {
                        Icon(Icons.Default.Search, "搜索")
                    }
                    IconButton(onClick = { /* 设置 */ }) {
                        Icon(Icons.Default.Settings, "设置")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // 搜索栏
            if (showSearch) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { 
                        searchQuery = it
                        viewModel.search(it)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("搜索消息...") },
                    singleLine = true
                )
            }
            
            // 分类筛选
            LazyRow(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    CategoryChip(
                        category = null,
                        isSelected = selectedCategory == null,
                        onClick = { viewModel.selectCategory(null) }
                    )
                }
                items(MessageCategory.entries) { category ->
                    CategoryChip(
                        category = category,
                        isSelected = selectedCategory == category,
                        onClick = { viewModel.selectCategory(category) }
                    )
                }
            }
            
            // 消息列表
            LazyColumn {
                items(messages) { message ->
                    MessageCard(
                        message = message,
                        onClick = { /* 进入详情 */ },
                        onLongClick = { /* 弹出菜单 */ }
                    )
                }
            }
        }
    }
}
```

### 4. 权限
```xml
<!-- AndroidManifest.xml -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
```

### 5. 依赖
```kotlin
// build.gradle.kts (app)
dependencies {
    // Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2024.02.00")
    implementation(composeBom)
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    
    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    
    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")
    
    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    
    // Hilt
    implementation("com.google.dagger:hilt-android:2.50")
    ksp("com.google.dagger:hilt-compiler:2.50")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")
    
    // OkHttp
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    
    // Gson
    implementation("com.google.code.gson:gson:2.10.1")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}
```

## ⚠️ 注意事项
1. 这是全新 Android 项目，不要修改 EventCenter 任何文件
2. 在 `/home/flypigs/ntfy-app/` 创建项目结构
3. 使用 Kotlin DSL（build.gradle.kts）
4. 使用 Material 3 配色
5. 代码要能编译通过
6. 不要 git push
