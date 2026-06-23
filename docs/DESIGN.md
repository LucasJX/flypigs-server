# ntfy 通知 App 设计文档

## 1. 产品概述

### 定位
基于 ntfy 协议的 Android 通知接收 App，用于接收自建 ntfy 服务器的推送通知。

### 核心价值
- 替代 ntfy 官方 App，提供更好的 UI/UX
- 支持消息分类和统计
- 适配中文用户习惯

### 目标用户
- 自建 ntfy 服务的用户
- 需要接收服务器/设备推送通知的用户

---

## 2. 功能模块

### 2.1 通知接收（核心）
- WebSocket 长连接（OkHttp）
- 后台保活（WorkManager + ForegroundService）
- 多 topic 订阅
- 系统通知栏推送

### 2.2 消息分类
- 按 ntfy tags 自动分类
- 预设分类：
  - 📊 节点变更（tags: node, change）
  - ⚠️ 系统告警（tags: alert, warning）
  - ✅ 恢复通知（tags: recovery, ok）
  - 📦 更新通知（tags: update, release）
  - 💬 其他消息

### 2.3 统计面板
- 今日消息总数
- 按分类饼图
- 按 topic 柱状图
- 7 天趋势折线图

### 2.4 消息列表
- 时间倒序
- 筛选：按 topic / 按分类 / 按优先级
- 搜索：标题 + 内容
- 已读/未读状态
- 长按操作：删除、复制、分享

### 2.5 设置
- 服务器管理：添加/编辑/删除 ntfy 服务器
- Topic 管理：添加/删除订阅
- 通知设置：声音、振动、免打扰
- 主题：深色/浅色/跟随系统
- 数据：清除历史、导出/导入配置

---

## 3. 技术架构

### 3.1 技术栈
```
语言：Kotlin
UI：Jetpack Compose + Material 3
数据库：Room
网络：OkHttp (WebSocket)
后台：WorkManager + ForegroundService
DI：Hilt
图表：Vico (Compose 原生)
```

### 3.2 项目结构
```
app/
├── src/main/java/com/flypigs/ntfyapp/
│   ├── di/                    # Hilt 依赖注入
│   │   └── AppModule.kt
│   ├── data/
│   │   ├── local/             # Room 数据库
│   │   │   ├── AppDatabase.kt
│   │   │   ├── dao/
│   │   │   │   ├── MessageDao.kt
│   │   │   │   ├── TopicDao.kt
│   │   │   │   └── ServerDao.kt
│   │   │   └── entity/
│   │   │       ├── MessageEntity.kt
│   │   │       ├── TopicEntity.kt
│   │   │       └── ServerEntity.kt
│   │   ├── remote/            # ntfy WebSocket
│   │   │   ├── NtfyWebSocket.kt
│   │   │   └── NtfyMessage.kt
│   │   └── repository/
│   │       ├── MessageRepository.kt
│   │       ├── TopicRepository.kt
│   │       └── ServerRepository.kt
│   ├── domain/
│   │   ├── model/
│   │   │   ├── Message.kt
│   │   │   ├── Topic.kt
│   │   │   ├── Server.kt
│   │   │   └── MessageCategory.kt
│   │   └── usecase/
│   │       ├── GetMessagesUseCase.kt
│   │       ├── GetStatsUseCase.kt
│   │       └── ManageTopicUseCase.kt
│   ├── service/
│   │   ├── NtfyService.kt         # ForegroundService
│   │   └── NotificationWorker.kt  # WorkManager
│   ├── ui/
│   │   ├── theme/
│   │   │   ├── Color.kt
│   │   │   ├── Type.kt
│   │   │   └── Theme.kt
│   │   ├── navigation/
│   │   │   └── NavGraph.kt
│   │   ├── screen/
│   │   │   ├── home/              # 消息列表
│   │   │   │   ├── HomeScreen.kt
│   │   │   │   └── HomeViewModel.kt
│   │   │   ├── stats/             # 统计面板
│   │   │   │   ├── StatsScreen.kt
│   │   │   │   └── StatsViewModel.kt
│   │   │   ├── settings/          # 设置
│   │   │   │   ├── SettingsScreen.kt
│   │   │   │   └── SettingsViewModel.kt
│   │   │   └── detail/            # 消息详情
│   │   │       ├── DetailScreen.kt
│   │   │       └── DetailViewModel.kt
│   │   └── component/
│   │       ├── MessageCard.kt
│   │       ├── CategoryChip.kt
│   │       ├── StatsChart.kt
│   │       └── TopicSelector.kt
│   └── MainActivity.kt
├── src/main/res/
│   ├── values/
│   │   ├── strings.xml
│   │   └── themes.xml
│   └── mipmap-*/             # App 图标
└── build.gradle.kts
```

### 3.3 数据模型
```kotlin
// 消息
data class Message(
    val id: String,
    val topic: String,
    val title: String?,
    val body: String,
    val priority: Priority,     // min/low/default/high/urgent
    val tags: List<String>,
    val timestamp: Long,
    val isRead: Boolean,
    val category: MessageCategory
)

// 消息分类
enum class MessageCategory {
    NODE_CHANGE,    // 节点变更
    SYSTEM_ALERT,   // 系统告警
    RECOVERY,       // 恢复通知
    UPDATE,         // 更新通知
    OTHER           // 其他
}

// Topic
data class Topic(
    val id: String,
    val serverId: String,
    val name: String,
    val displayName: String,
    val isEnabled: Boolean
)

// 服务器
data class Server(
    val id: String,
    val url: String,
    val name: String,
    val username: String?,
    val password: String?,
    val token: String?
)
```

### 3.4 ntfy WebSocket 协议
```kotlin
// 连接
val request = Request.Builder()
    .url("wss://ntfy.sh/mytopic/ws")
    .build()

// 消息格式
data class NtfyMessage(
    val id: String,
    val time: Long,           // Unix timestamp
    val event: String,        // "message" | "open" | "keepalive"
    val topic: String,
    val title: String?,
    val message: String,
    val priority: Int?,       // 1-5
    val tags: List<String>?,
    val click: String?,
    val icon: String?
)
```

---

## 4. UI 设计

### 4.1 配色方案（Material 3）
```
Primary: #1976D2 (蓝)
Secondary: #26A69A (青)
Background: #FAFAFA (浅) / #121212 (深)
Surface: #FFFFFF (浅) / #1E1E1E (深)
Error: #D32F2F (红)
```

### 4.2 分类颜色
```
节点变更：蓝色 #1976D2
系统告警：红色 #D32F2F
恢复通知：绿色 #388E3C
更新通知：橙色 #F57C00
其他消息：灰色 #757575
```

### 4.3 页面结构

#### 底部导航栏
```
消息 | 统计 | 设置
```

#### 消息页（Home）
```
┌─────────────────────────────┐
│ ntfy 通知          [筛选] [搜索] │
├─────────────────────────────┤
│ [全部] [节点] [告警] [恢复] [更新] │  ← 分类筛选 chips
├─────────────────────────────┤
│ ┌─────────────────────────┐ │
│ │ 📊 香港直连1 上线         │ │
│ │ 测试-SG-01 已添加        │ │
│ │ 2分钟前 · 节点变更        │ │
│ └─────────────────────────┘ │
│ ┌─────────────────────────┐ │
│ │ ⚠️ OpenClash 进程异常     │ │
│ │ 内存占用超过 90%         │ │
│ │ 1小时前 · 系统告警        │ │
│ └─────────────────────────┘ │
│ ┌─────────────────────────┐ │
│ │ ✅ 服务已恢复            │ │
│ │ 所有节点正常运行         │ │
│ │ 3小时前 · 恢复通知        │ │
│ └─────────────────────────┘ │
└─────────────────────────────┘
```

#### 统计页（Stats）
```
┌─────────────────────────────┐
│ 统计面板                     │
├─────────────────────────────┤
│         ┌─────┐              │
│    今日  │ 42 │              │
│         └─────┘              │
├─────────────────────────────┤
│ 消息分类                     │
│ ┌─────────────────────────┐ │
│ │     🥧 饼图             │ │
│ │  节点 40%  告警 25%      │ │
│ │  恢复 20%  更新 15%      │ │
│ └─────────────────────────┘ │
├─────────────────────────────┤
│ 7天趋势                     │
│ ┌─────────────────────────┐ │
│ │     📈 折线图           │ │
│ │  6/17 6/18 6/19 6/20    │ │
│ └─────────────────────────┘ │
├─────────────────────────────┤
│ Topic 消息量                 │
│ ┌─────────────────────────┐ │
│ │ eventcenter  ██████ 28  │ │
│ │ openclash    ████ 14     │ │
│ └─────────────────────────┘ │
└─────────────────────────────┘
```

#### 设置页（Settings）
```
┌─────────────────────────────┐
│ 设置                         │
├─────────────────────────────┤
│ 服务器                       │
│ ┌─────────────────────────┐ │
│ │ 🏠 自建 ntfy            │ │
│ │ http://192.168.100.100  │ │
│ └─────────────────────────┘ │
│ [+ 添加服务器]               │
├─────────────────────────────┤
│ 订阅 Topic                   │
│ ┌─────────────────────────┐ │
│ │ ✅ eventcenter           │ │
│ │ ✅ openclash             │ │
│ │ ☐ system                 │ │
│ └─────────────────────────┘ │
│ [+ 添加 Topic]               │
├─────────────────────────────┤
│ 通知设置                     │
│ 声音                    [开] │
│ 振动                    [开] │
│ 免打扰              [关闭]   │
├─────────────────────────────┤
│ 外观                         │
│ 主题               [跟随系统] │
├─────────────────────────────┤
│ 数据                         │
│ 清除历史消息                 │
│ 导出配置                     │
│ 导入配置                     │
└─────────────────────────────┘
```

---

## 5. 与 ntfy 官方 App 对比

| 功能 | ntfy 官方 | 本 App |
|------|----------|--------|
| 消息接收 | ✅ | ✅ |
| 多服务器 | ✅ | ✅ |
| 消息分类 | ❌ | ✅ |
| 统计面板 | ❌ | ✅ |
| 已读状态 | ❌ | ✅ |
| 搜索 | ❌ | ✅ |
| 中文 UI | 部分 | ✅ |
| Material 3 | ❌ | ✅ |

---

## 6. 开发计划

### Phase 1: 基础框架
- [x] 项目结构搭建
- [x] Room 数据库设计
- [x] ntfy WebSocket 连接
- [x] 基础 UI 框架

### Phase 2: 核心功能
- [ ] 消息接收和通知
- [ ] 消息列表页
- [ ] 消息分类
- [ ] Topic 管理

### Phase 3: 高级功能
- [ ] 统计面板
- [ ] 搜索功能
- [ ] 多服务器支持
- [ ] 导入/导出配置

### Phase 4: 优化
- [ ] 性能优化
- [ ] UI 打磨
- [ ] 测试和修复
- [ ] 发布

---

## 7. 参考资源

- ntfy 官方文档：https://docs.ntfy.sh/
- ntfy Android App：https://github.com/binwiederhier/ntfy-android
- Material 3：https://m3.material.io/
- Jetpack Compose：https://developer.android.com/jetpack/compose
