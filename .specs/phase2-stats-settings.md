# ntfy Android App — Phase 2 实现 Spec

## ⚠️ 重要
- 项目目录：`/home/flypigs/ntfy-app/`
- **不要修改** `/home/flypigs/luci-app-eventcenter/` 中的任何文件
- 基于 Phase 1 已有代码继续开发

## Phase 2 目标：统计面板 + 设置页 + 消息详情

### 1. 统计页面（StatsScreen）

#### 功能
- 今日消息总数（大数字展示）
- 消息分类饼图（按 category 分组）
- 7天趋势折线图（按日期分组）
- Topic 消息量柱状图（按 topic 分组）

#### 数据查询
```kotlin
// StatsDao.kt 或在 MessageDao.kt 中添加
@Query("SELECT COUNT(*) FROM messages WHERE timestamp >= :startOfDay")
fun getTodayCount(startOfDay: Long): Flow<Int>

@Query("SELECT category, COUNT(*) as count FROM messages GROUP BY category")
fun getCategoryStats(): Flow<List<CategoryStat>>

@Query("SELECT date(timestamp/1000, 'unixepoch') as date, COUNT(*) as count FROM messages WHERE timestamp >= :since GROUP BY date ORDER BY date")
fun getDailyStats(since: Long): Flow<List<DailyStat>>

@Query("SELECT topic, COUNT(*) as count FROM messages GROUP BY topic ORDER BY count DESC")
fun getTopicStats(): Flow<List<TopicStat>>

data class CategoryStat(val category: String, val count: Int)
data class DailyStat(val date: String, val count: Int)
data class TopicStat(val topic: String, val count: Int)
```

#### UI 布局
```
┌─────────────────────────────┐
│ 统计面板                     │
├─────────────────────────────┤
│ ┌─────────────────────────┐ │
│ │         42              │ │
│ │       今日消息            │ │
│ └─────────────────────────┘ │
├─────────────────────────────┤
│ 消息分类                     │
│ ┌─────────────────────────┐ │
│ │  ┌───┐                  │ │
│ │  │   │ 节点变更 40%      │ │
│ │  │   │ 系统告警 25%      │ │
│ │  │   │ 恢复通知 20%      │ │
│ │  └───┘ 更新通知 15%      │ │
│ └─────────────────────────┘ │
├─────────────────────────────┤
│ 7天趋势                     │
│ ┌─────────────────────────┐ │
│ │    ╱╲                   │ │
│ │   ╱  ╲  ╱╲             │ │
│ │  ╱    ╲╱  ╲            │ │
│ │ ╱          ╲           │ │
│ │ 一 二 三 四 五 六 日      │ │
│ └─────────────────────────┘ │
├─────────────────────────────┤
│ Topic 消息量                 │
│ eventcenter ████████ 28     │
│ openclash   █████ 14        │
└─────────────────────────────┘
```

#### 图表实现
使用 Canvas 自绘（不用第三方库，保持轻量）：
```kotlin
// PieChart.kt
@Composable
fun PieChart(
    data: List<CategoryStat>,
    modifier: Modifier = Modifier
) {
    val total = data.sumOf { it.count }
    var startAngle = -90f
    
    Canvas(modifier = modifier.size(200.dp)) {
        data.forEach { stat ->
            val sweepAngle = (stat.count.toFloat() / total) * 360f
            drawArc(
                color = getCategoryColor(stat.category),
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = true
            )
            startAngle += sweepAngle
        }
    }
}

// LineChart.kt
@Composable
fun LineChart(
    data: List<DailyStat>,
    modifier: Modifier = Modifier
) {
    // Canvas 绘制折线
}

// BarChart.kt
@Composable
fun BarChart(
    data: List<TopicStat>,
    modifier: Modifier = Modifier
) {
    // Canvas 绘制柱状图
}
```

### 2. 设置页面（SettingsScreen）

#### 功能模块
1. **服务器管理**
   - 显示已配置的服务器列表
   - 添加/编辑/删除服务器
   - 测试连接

2. **Topic 管理**
   - 显示已订阅的 topic 列表
   - 添加/删除 topic
   - 启用/禁用 topic

3. **通知设置**
   - 声音开关
   - 振动开关
   - 免打扰时间

4. **外观设置**
   - 主题选择（深色/浅色/跟随系统）

5. **数据管理**
   - 清除历史消息
   - 导出配置
   - 导入配置

#### 数据模型
```kotlin
// ServerEntity.kt
@Entity(tableName = "servers")
data class ServerEntity(
    @PrimaryKey val id: String,
    val url: String,
    val name: String,
    val username: String? = null,
    val password: String? = null,
    val token: String? = null,
    val isConnected: Boolean = false
)

// TopicEntity.kt
@Entity(tableName = "topics")
data class TopicEntity(
    @PrimaryKey val id: String,
    val serverId: String,
    val name: String,
    val displayName: String,
    val isEnabled: Boolean = true
)
```

#### UI 布局
```
┌─────────────────────────────┐
│ 设置                         │
├─────────────────────────────┤
│ 服务器                       │
│ ┌─────────────────────────┐ │
│ │ 🏠 自建 ntfy            │ │
│ │ http://192.168.100.100  │ │
│ │ 已连接 ✓                 │ │
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

### 3. 消息详情页（DetailScreen）

#### 功能
- 显示完整消息内容
- 显示元数据（来源、时间、优先级、标签）
- 操作按钮（复制、分享、删除）

#### UI 布局
```
┌─────────────────────────────┐
│ ← 返回                      │
├─────────────────────────────┤
│                             │
│ 📊 节点变更                  │
│                             │
│ 香港直连1 上线                │
│ 测试-SG-01 已添加            │
│                             │
│ ─────────────────────────── │
│ 来源：eventcenter            │
│ 时间：2026-06-23 14:21:03    │
│ 优先级：默认                  │
│ 标签：node, change           │
│                             │
│ ─────────────────────────── │
│                             │
│ [复制] [分享] [删除]          │
│                             │
└─────────────────────────────┘
```

### 4. 导航结构

```kotlin
// NavGraph.kt
@Composable
fun NtfyNavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onNavigateToSettings = { navController.navigate("settings") },
                onNavigateToDetail = { id -> navController.navigate("detail/$id") }
            )
        }
        composable("stats") {
            StatsScreen()
        }
        composable("settings") {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("detail/{messageId}") { backStackEntry ->
            val messageId = backStackEntry.arguments?.getString("messageId") ?: ""
            DetailScreen(
                messageId = messageId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
```

### 5. 底部导航栏

```kotlin
// BottomNavBar.kt
@Composable
fun BottomNavBar(navController: NavHostController) {
    val items = listOf(
        BottomNavItem("home", "消息", Icons.Default.Email),
        BottomNavItem("stats", "统计", Icons.Default.BarChart),
        BottomNavItem("settings", "设置", Icons.Default.Settings)
    )
    
    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        
        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)
```

### 6. 新增文件清单

```
app/src/main/java/com/flypigs/ntfyapp/
├── data/
│   ├── local/
│   │   ├── dao/
│   │   │   ├── MessageDao.kt      # 添加统计查询
│   │   │   ├── ServerDao.kt       # 新增
│   │   │   └── TopicDao.kt        # 新增
│   │   ├── entity/
│   │   │   ├── ServerEntity.kt    # 新增
│   │   │   └── TopicEntity.kt     # 新增
│   │   └── AppDatabase.kt         # 更新：添加新表
│   └── repository/
│       ├── ServerRepository.kt    # 新增
│       └── TopicRepository.kt     # 新增
├── domain/model/
│   ├── Server.kt                  # 新增
│   ├── Topic.kt                   # 新增
│   ├── CategoryStat.kt            # 新增
│   ├── DailyStat.kt               # 新增
│   └── TopicStat.kt               # 新增
├── ui/
│   ├── navigation/
│   │   └── NavGraph.kt            # 新增
│   ├── screen/
│   │   ├── stats/
│   │   │   ├── StatsScreen.kt     # 新增
│   │   │   └── StatsViewModel.kt  # 新增
│   │   ├── settings/
│   │   │   ├── SettingsScreen.kt  # 新增
│   │   │   └── SettingsViewModel.kt # 新增
│   │   └── detail/
│   │       ├── DetailScreen.kt    # 新增
│   │       └── DetailViewModel.kt # 新增
│   ├── component/
│   │   ├── BottomNavBar.kt        # 新增
│   │   ├── PieChart.kt            # 新增
│   │   ├── LineChart.kt           # 新增
│   │   └── BarChart.kt            # 新增
│   └── MainActivity.kt            # 更新：添加导航
```

## ⚠️ 注意事项
1. **不要修改** luci-app-eventcenter 项目
2. 图表用 Canvas 自绘，不要引入第三方图表库
3. 保持 Material 3 风格一致
4. 代码要能编译通过
5. 不要 git push
