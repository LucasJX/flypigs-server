# Task 2: 首页重构 — LargeTopAppBar + 事件摘要

## 目标
从 Android 9 风格升级到 Material 3 Expressive。

## TopAppBar 改造
### 当前
- SmallTopAppBar, 标题「消息」

### 目标
- LargeTopAppBar, 高度 144dp
- 标题: 「事件中心」
- 副标题: 「今天收到 N 条事件」(动态)
- 第三行: 「OpenClash 运行正常」(绿色小圆点 + 状态)
- 右上角: 搜索图标
- 背景色: surface (不要蓝色!)

## Tab 栏改造
### 当前
- 全部消息 / 未读(Badge) / 加星

### 目标
- 全部 / 未读(Badge) / 加星 (保持)
- containerColor: surface
- 改用 ScrollableTabRow (为未来更多 tab 预留)

## 空状态改造
### 当前
- 📥 暂无消息 / 等待接收中

### 目标
- 🔔 暂无事件
- 副标题: 「当 OpenClash、PassWall 或系统组件产生新的事件时，将在这里显示」
- 按钮: [发送测试通知] (outline button)

## 底部导航改名
- 消息 → 事件
- 统计 → 分析
- 设置 → 设置 (不变)

## ⚠️ 注意事项
1. 修改 `~/ntfy-app/app/src/main/java/com/flypigs/ntfyapp/ui/screen/home/HomeScreen.kt`
2. 修改 `~/ntfy-app/app/src/main/java/com/flypigs/ntfyapp/ui/navigation/BottomNavBar.kt`
3. 不要删除 Drawer 相关代码
4. 不要删除消息列表逻辑
5. TopAppBar 背景色用 surface，不要用 primary
6. JAVA_HOME=/home/flypigs/ntfy-app/.android-sdk/jdk-17.0.2
7. 不要 git push
